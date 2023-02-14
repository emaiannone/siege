package it.unisa.siege.core;

import it.unisa.siege.core.configuration.ProjectConfiguration;
import it.unisa.siege.core.configuration.ProjectConfigurationBuilder;
import it.unisa.siege.core.configuration.Vulnerability;
import it.unisa.siege.core.configuration.YAMLConfigurationFileParser;
import it.unisa.siege.core.preprocessing.ProjectBuilder;
import it.unisa.siege.core.preprocessing.RootProximityEntryPointFinder;
import it.unisa.siege.core.results.GenerationResult;
import it.unisa.siege.core.results.ProjectResult;
import it.unisa.siege.core.results.VulnerabilityResult;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.evosuite.EvoSuite;
import org.evosuite.Properties;
import org.evosuite.analysis.StoredStaticPaths;
import org.evosuite.result.TestGenerationResult;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.utils.StaticPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class SiegeLauncher {
    private static final Logger LOGGER = LoggerFactory.getLogger(SiegeLauncher.class);
    private static final String DATE_FORMAT = "yyyy_MM_dd_HH_mm_ss";

    private static final Path TESTS_DIR_PATH_DEFAULT = Paths.get("./siege_tests");
    private static final Path OUT_DIR_PATH_DEFAULT = Paths.get("./siege_results");
    private static final Path LOG_DIR_PATH_DEFAULT = Paths.get("./siege_logs");

    private final BaseConfiguration baseConfig;
    private final List<ProjectConfiguration> projectConfigs;
    private final Path baseTestsDirPath;
    private final Path baseOutDirPath;
    private final Path baseLogDirPath;

    public SiegeLauncher(BaseConfiguration baseConfig) throws Exception {
        this.baseConfig = baseConfig;
        String siegeStartTime = new SimpleDateFormat(DATE_FORMAT).format(new Date());
        this.projectConfigs = new ArrayList<>();
        String configFile = baseConfig.getConfigurationFile();
        Path configFilePath = configFile != null ? Paths.get(configFile) : null;
        if (configFilePath != null && Files.exists(configFilePath)) {
            LOGGER.info("Running Siege with configuration file: {}.", configFilePath);
            LOGGER.info("Unspecified options will be given default values.");
            projectConfigs.addAll(YAMLConfigurationFileParser.parseConfigFile(baseConfig));
        } else {
            LOGGER.info("Running Siege with command-line options.");
            LOGGER.info("Unspecified options will be given default values.");
            // TODO Add new CLI options (according to the content of ProjectConfiguration) and send it there
            ProjectConfiguration projectConfig = new ProjectConfigurationBuilder()
                    .setProjectDir(baseConfig.getProject())
                    .setVulnerabilitiesFile(baseConfig.getVulnerabilitiesFileName())
                    .setSearchBudget(baseConfig.getBudget())
                    .setPopulationSize(baseConfig.getPopSize())
                    .build();
            projectConfigs.add(projectConfig);
        }

        String testsDir = baseConfig.getTestsDir();
        Path testsDirPath = testsDir != null ? Paths.get(testsDir) : null;
        baseTestsDirPath = testsDirPath != null ? Paths.get(testsDirPath.toString(), siegeStartTime) : TESTS_DIR_PATH_DEFAULT;
        if (!Files.exists(baseTestsDirPath)) {
            Files.createDirectories(baseTestsDirPath);
        }

        String outDir = baseConfig.getOutDir();
        Path outDirPath = outDir != null ? Paths.get(outDir) : null;
        baseOutDirPath = outDirPath != null ? Paths.get(outDirPath.toString(), siegeStartTime) : OUT_DIR_PATH_DEFAULT;
        if (!Files.exists(baseOutDirPath)) {
            Files.createDirectories(baseOutDirPath);
        }

        String logDir = baseConfig.getLogDir();
        Path logDirPath = logDir != null ? Paths.get(logDir) : null;
        baseLogDirPath = logDirPath != null ? Paths.get(logDirPath.toString(), siegeStartTime) : LOG_DIR_PATH_DEFAULT;
        if (!Files.exists(baseLogDirPath)) {
            Files.createDirectories(baseLogDirPath);
        }
    }

    public void launch() {
        for (ProjectConfiguration projectConfig : projectConfigs) {
            try {
                analyzeProject(projectConfig);
            } catch (Exception e) {
                LOGGER.error(ExceptionUtils.getStackTrace(e));
            }
        }
    }

    private void analyzeProject(ProjectConfiguration projectConfig) throws Exception {
        ProjectResult projectResult = new ProjectResult(projectConfig);
        String runStartTime = new SimpleDateFormat(DATE_FORMAT).format(new Date());
        projectResult.setStartTime(runStartTime);

        List<Vulnerability> vulnerabilities = projectConfig.getVulnerabilities();
        if (vulnerabilities.isEmpty()) {
            LOGGER.info("No vulnerabilities to reach. No generation can be done.");
            String endTime = new SimpleDateFormat(DATE_FORMAT).format(new Date());
            LOGGER.info("Terminating Siege run at: {}", endTime);
            projectResult.setEndTime(endTime);
            return;
        }

        // Instantiate EvoSuite now just to update the logging context
        EvoSuite fakeEvoSuite = new EvoSuite();
        Path projectPath = projectConfig.getProjectPath();
        String projectName = projectPath.getFileName().toString();
        LOGGER.info("({}) Analyzing project: {}", runStartTime, projectName);
        Pair<String, List<String>> processClasspathRes = ProjectBuilder.processClasspath(projectPath, baseConfig.getClasspathFileName());
        String classpathString = processClasspathRes.getLeft();
        List<String> clientClasses = processClasspathRes.getRight();

        // TODO Log more details about the project configuration here...
        LOGGER.info("Using {} seconds budget.", projectConfig.getSearchBudget());
        LOGGER.info("Evolving populations of {} individuals.", projectConfig.getPopulationSize());

        Path projectTestsDirPath = Paths.get(baseTestsDirPath.toString(), projectName);
        Path projectOutDirPath = Paths.get(baseOutDirPath.toString(), projectName);
        Path projectLogDirPath = Paths.get(baseLogDirPath.toString(), projectName);
        LOGGER.info("Writing tests in directory {}.", projectTestsDirPath.toFile().getCanonicalPath());
        LOGGER.info("Writing results in directory {}.", projectOutDirPath.toFile().getCanonicalPath());
        LOGGER.info("Writing generation log in directory {}.", projectLogDirPath.toFile().getCanonicalPath());

        List<String> baseCommands = new ArrayList<>(Arrays.asList(
                // Asks to reach a specific class-method pair in any class in the classpath (e.g., a library)
                "-criterion", Properties.Criterion.REACHABILITY.name(),
                // Asks to evolve test cases, not test suites
                "-generateTests",
                // Siege's tests do not need assertions
                "-Dassertions=false",
                // We run a test minimization at the end of the generation that should reduce the best test length
                "-Dminimize=true",
                // Needed to receive all the info from the RMI client at the end of the generation
                "-Dserialize_ga=true",
                "-Dserialize_result=true",
                "-Dcoverage=false",
                "-Dprint_covered_goals=true",
                "-Dprint_missed_goals=true",
                // Various instrumentation options required, so they should not be touched at all
                "-Dinstrument_parent=false", // If this is true it seems to give problem to RMI
                "-Dinstrument_context=true",
                "-Dinstrument_method_calls=true",
                "-Dinstrument_libraries=true",
                "-Dinstrument_target_callers=false", // NOTE This takes long time, should be tested again as it should increase the number of control dependencies found
                // Number of statements in test cases
                "-Dchromosome_length=" + projectConfig.getChromosomeLength(),
                "-Dchop_max_length=false",
                // If enabled, we add the list of control dependencies to solve in the goals for REACHABILITY. The extraction is not always possible for all the classes in the static paths. In that case, the list is empty, and behave like usual. This is done to give more guidance with the fitness function.
                "-Dreachability_branch_awareness=" + projectConfig.isReachabilityBranchAwareness(),
                // This enables the use of a new structure of individuals: the initial test cases should have a method that calls the entry method (according to the static paths in the goals)
                "-Dtest_factory=" + projectConfig.getInitialPopulationGenerationAlgorithm(),
                // Probability of adding more calls before calling the entry method
                "-Dp_add_calls_before_entry_method=" + projectConfig.getProbabilityAddCallsBeforeEntryMethod(),
                // Reduce at minimum the probability of creating new variables, but reuse existing ones
                "-Dprimitive_reuse_probability=" + projectConfig.getProbabilityPrimitiveReuse(),
                "-Dobject_reuse_probability=" + projectConfig.getProbabilityObjectReuse(),
                // Allowing maximum length to strings to use in tests
                "-Dstring_length=" + projectConfig.getMaxStringLength(),
                "-Dmax_string=" + projectConfig.getMaxStringLength(),
                // High probability of sampling primitives from a constant pool
                "-Dprimitive_pool=" + projectConfig.getProbabilityPrimitivePool(),
                // The probability of using constants carved during test execution (i.e., not those carved statically)
                "-Ddynamic_pool=" + projectConfig.getProbabilityDynamicPool(),
                // Seed constants from methods or branches from coverage goals of REACHABILITY criterion. If "methods" is active, it has the precedence over "branches". Set both as false to use the ordinary EvoSuite pool
                "-Dreachability_seed_from_methods_in_goals=" + projectConfig.isReachabilitySeedFromMethodsInGoals(),
                "-Dreachability_seed_from_branches_in_goals=" + projectConfig.isReachabilitySeedFromBranchesInGoals(),
                // We use the Steady State GA as runner
                "-Dalgorithm=" + projectConfig.getAlgorithm(),
                // This custom crossover function crosses tests using the points where the tests crashed. For tests not crashing, it behaves like an ordinary single point crossover
                "-Dcrossover_function=" + projectConfig.getCrossoverAlgorithm(),
                // We ask to use exception points to sample which statements to give priority for crossover or mutation
                "-Dexception_point_sampling=" + projectConfig.isExceptionPointSampling(),
                // Use our custom mutation algorithm
                "-Dreachability_entry_method_mutation=" + projectConfig.isReachabilityEntryMethodMutation(),
                // We want an increased probability of changing parameters of a method call
                "-Dp_change_parameter=" + projectConfig.getProbabilityChangeParameter(),
                // Search operators, can be modified and expect different performance
                "-Dsearch_budget=" + projectConfig.getSearchBudget(),
                "-Dpopulation=" + projectConfig.getPopulationSize(),
                // Where to export the generated tests
                "-Dtest_dir=" + projectTestsDirPath,
                "-projectCP", classpathString
        ));

        LOGGER.info("Generating tests targeting {} vulnerabilities from a pool of {} client classes.", vulnerabilities.size(), clientClasses.size());
        LOGGER.debug("Vulnerabilities ({}): {}", vulnerabilities.size(), vulnerabilities.stream().map(Vulnerability::getCve).collect(Collectors.joining(",")));
        LOGGER.debug("Client classes ({}): {}", clientClasses.size(), clientClasses);
        for (int idx = 0; idx < vulnerabilities.size(); idx++) {
            Vulnerability vulnerability = vulnerabilities.get(idx);
            LOGGER.info("({}/{}) Generating tests for: {}", idx + 1, vulnerabilities.size(), vulnerability.getCve());
            List<String> baseCommandsExtended = new ArrayList<>(baseCommands);
            // Must necessarily replace hyphens with underscores to avoid errors while compiling the tests
            baseCommandsExtended.add("-Djunit_suffix=" + "_" + vulnerability.getCve().replace("-", "_") + "_SiegeTest");
            baseCommandsExtended.add("-Dreachability_target_class=" + vulnerability.getTargetClass());
            baseCommandsExtended.add("-Dreachability_target_method=" + vulnerability.getTargetMethod());

            LOGGER.info("Collecting static paths to: {}", vulnerability.getTargetClass() + vulnerability.getTargetMethod());
            List<String> fakeEvoSuiteCommands = new ArrayList<>(baseCommandsExtended);
            fakeEvoSuiteCommands.add("-class");
            fakeEvoSuiteCommands.add(clientClasses.get(0));
            fakeEvoSuite.parseCommandLine(fakeEvoSuiteCommands.toArray(new String[0]));
            Files.deleteIfExists(projectTestsDirPath);
            // NOTE If keeping the Map in StoredStaticPaths does not scale, just store the static paths for the current reachability target
            Set<StaticPath> staticPaths = StoredStaticPaths.getPathsToTarget(vulnerability.getTargetClass(), vulnerability.getTargetMethod());
            LOGGER.info("Found {} static paths that could reach: {}.", staticPaths.size(), vulnerability.getTargetClass() + vulnerability.getTargetMethod());
            LOGGER.debug("Static paths to target ({}): {}", staticPaths.size(), staticPaths);
            if (staticPaths.isEmpty()) {
                LOGGER.warn("No client classes seem to reach vulnerability {}. Generation will not start.", vulnerability.getCve());
                continue;
            }

            // This method gives higher priority to the classes near the root.
            List<String> entryClasses = new RootProximityEntryPointFinder().findEntryPoints(clientClasses, staticPaths);
            if (entryClasses.isEmpty()) {
                LOGGER.warn("No client classes seems to reach vulnerability {}. Generation will not start.", vulnerability.getCve());
                continue;
            }
            LOGGER.info("{} client classes could expose to vulnerability {}.", entryClasses.size(), vulnerability.getCve());
            LOGGER.debug("Entry point client classes ({}): {}", entryClasses.size(), entryClasses);

            VulnerabilityResult vulnerabilityResult = new VulnerabilityResult(vulnerability);
            for (int i = 0; i < entryClasses.size(); i++) {
                String entryClass = entryClasses.get(i);
                LOGGER.info("({}/{}) Starting the generation from class: {}", i + 1, entryClasses.size(), entryClass);
                GenerationResult generationResult = generateFromClass(entryClass, vulnerability.getCve(), projectLogDirPath, baseCommandsExtended);
                if (generationResult != null) {
                    vulnerabilityResult.addGenerationResult(generationResult);
                }
                if (!baseConfig.isKeepEmptyTests()) {
                    SiegeIOHelper.deleteEmptyTestFiles(projectTestsDirPath);
                }
            }
            projectResult.addVulnerabilityResult(vulnerabilityResult);
        }
        String endTime = new SimpleDateFormat(DATE_FORMAT).format(new Date());
        LOGGER.info("Terminating Siege run at: {}", endTime);
        projectResult.setEndTime(endTime);

        // TODO Export to JSON file into projectOutDirPath. For now, we print for debugging purposes
        System.out.printf("DEBUG: Export to %s%n", projectOutDirPath);
        System.out.println(projectResult);
    }

    private GenerationResult generateFromClass(String entryClass, String cve, Path baseLogDirPath, List<String> baseCommands) throws IOException {
        // Create the generation logging file for this run
        Path logFilePath = null;
        if (baseLogDirPath != null) {
            Path logDirPath = Paths.get(baseLogDirPath.toString(), cve, entryClass);
            if (!Files.exists(logDirPath)) {
                Files.createDirectories(logDirPath);
            }
            String entryClassFileName = String.format("%s_%s.log", entryClass.substring(entryClass.lastIndexOf(".") + 1), cve);
            logFilePath = Paths.get(logDirPath.toString(), entryClassFileName);
            try {
                Files.createFile(logFilePath);
                LOGGER.info("Writing the generation log in file: {}.", logFilePath);
            } catch (IOException e) {
                LOGGER.warn("Failed to create the generation log file. No generation log will be written for this run.");
            }
        }

        List<String> evoSuiteCommands = new ArrayList<>(baseCommands);
        evoSuiteCommands.add("-Dgeneration_log_file=" + (logFilePath != null ? logFilePath : ""));
        evoSuiteCommands.add("-class");
        evoSuiteCommands.add(entryClass);
        List<List<TestGenerationResult<TestChromosome>>> evoSuiteResults;
        try {
            evoSuiteResults = (List<List<TestGenerationResult<TestChromosome>>>)
                    new EvoSuite().parseCommandLine(evoSuiteCommands.toArray(new String[0]));
        } catch (Exception e) {
            // Log and go to next iteration
            LOGGER.warn("A problem occurred while generating exploits with {}. Skipping it.", entryClass);
            LOGGER.error(ExceptionUtils.getStackTrace(e));
            return null;
        }
        // NOTE Only the first result is useful as we run ES on just one class at a time
        List<TestGenerationResult<TestChromosome>> testResults = !evoSuiteResults.isEmpty() ? evoSuiteResults.get(0) : null;
        if (testResults != null) {
            TestGenerationResult<TestChromosome> clientClassResult = !testResults.isEmpty() ? testResults.get(0) : null;
            if (clientClassResult != null) {
                return new GenerationResult(entryClass, clientClassResult);
            }
        }
        return null;
    }

}
