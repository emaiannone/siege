package it.unisa.siege.core;

import it.unisa.siege.core.configuration.SiegeConfiguration;
import it.unisa.siege.core.configuration.SiegeConfigurationBuilder;
import it.unisa.siege.core.configuration.Vulnerability;
import it.unisa.siege.core.preprocessing.ProjectBuilder;
import it.unisa.siege.core.preprocessing.RootProximityEntryPointFinder;
import it.unisa.siege.core.results.GenerationResult;
import it.unisa.siege.core.results.ProjectResult;
import it.unisa.siege.core.results.VulnerabilityResult;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.evosuite.EvoSuite;
import org.evosuite.Properties;
import org.evosuite.analysis.StoredStaticPaths;
import org.evosuite.result.TestGenerationResult;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.utils.StaticPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class SiegeLauncher {
    private static final Logger LOGGER = LoggerFactory.getLogger(SiegeLauncher.class);
    private static final String DATE_FORMAT = "yyyy_MM_dd_HH_mm_ss";

    private final CLIConfiguration cliConfig;
    private final List<SiegeConfiguration> siegeConfigs;

    private File generationLogDir;
    private String classpathString;
    private List<String> clientClasses;


    public SiegeLauncher(CLIConfiguration cliConfig) throws Exception {
        this.cliConfig = cliConfig;
        String siegeStartTime = new SimpleDateFormat(DATE_FORMAT).format(new Date());
        this.siegeConfigs = new ArrayList<>();
        Path configFilePath = cliConfig.getConfigurationFilePath();
        if (configFilePath != null) {
            siegeConfigs.addAll(readConfigFile());
            LOGGER.info("Running Siege with configuration file: {}.", configFilePath);
            LOGGER.info("Unspecified options will be given default values.");
        } else {
            LOGGER.info("Running Siege with command-line options.");
            LOGGER.info("Unspecified options will be given default values.");
            SiegeConfigurationBuilder siegeConfigurationBuilder = new SiegeConfigurationBuilder();
            siegeConfigurationBuilder
                    .setProjectPath(cliConfig.getProjectPath())
                    .setVulnerabilitiesFilePath(cliConfig.getVulnerabilitiesFilePath());
            // TODO Supply all the other CLI options here, when they will be added
            if (cliConfig.getBudget() < 1) {
                LOGGER.info("The search budget cannot be less than 1. Using default value.");
            } else {
                siegeConfigurationBuilder.setSearchBudget(cliConfig.getBudget());
            }
            if (cliConfig.getPopulationSize() < 2) {
                LOGGER.info("The population size cannot be less than 2. Using default value.");
            } else {
                siegeConfigurationBuilder.setPopulationSize(cliConfig.getPopulationSize());
            }
            SiegeConfiguration siegeConfig = siegeConfigurationBuilder.build();
            siegeConfigs.add(siegeConfig);
        }

        // Create the generation logging directory
        File generationLogBaseDir = cliConfig.getLogDirPath().toFile();
        if (!generationLogBaseDir.exists()) {
            if (!generationLogBaseDir.mkdirs()) {
                generationLogBaseDir = null;
                LOGGER.warn("Failed to create the generation log directory. No generation details will be logged.");
            }
        }
        if (generationLogBaseDir != null) {
            generationLogDir = Paths.get(generationLogBaseDir.getCanonicalPath(), siegeStartTime).toFile();
            if (!generationLogDir.exists()) {
                if (generationLogDir.mkdirs()) {
                    LOGGER.info("Writing the generation log in directory {}.", generationLogDir.getCanonicalPath());
                } else {
                    LOGGER.warn("Failed to create the generation log directory. No generation log will be written for all runs.");
                }
            } else {
                LOGGER.info("Writing the generation log in directory {}.", generationLogDir.getCanonicalPath());
            }
        }
        if (cliConfig.getOutFilePath() != null) {
            File outFileDir = Paths.get(cliConfig.getOutFilePath().getParent().toString(), siegeStartTime).toFile();
            if (!outFileDir.exists()) {
                if (outFileDir.mkdirs()) {
                    LOGGER.info("Writing the results in directory {}.", outFileDir.getCanonicalPath());
                } else {
                    LOGGER.warn("Failed to create the results directory. The results will be printed to stdout.");
                }
            } else {
                LOGGER.info("Writing the results in directory {}.", outFileDir.getCanonicalPath());
            }
        }
    }

    public void launch() {
        List<ProjectResult> projectResults = new ArrayList<>();
        for (SiegeConfiguration siegeConfig : siegeConfigs) {
            try {
                ProjectResult projectResult = runSession(siegeConfig);
                projectResults.add(projectResult);
            } catch (Exception e) {
                LOGGER.error(ExceptionUtils.getStackTrace(e));
            }
        }
        // TODO Export projectResults into JSON. For now, we print for debugging purposes
        System.out.println(projectResults);
    }

    private List<SiegeConfiguration> readConfigFile() {
        // TODO File YAML file as is (use SiegeIOHelper), use default values when not found! Use the builder, call build(), change values, recall build() again

        // DEBUG
        return new ArrayList<>();
    }

    private ProjectResult runSession(SiegeConfiguration siegeConfig) throws Exception {
        ProjectResult projectResult = new ProjectResult(siegeConfig);
        String runStartTime = new SimpleDateFormat(DATE_FORMAT).format(new Date());
        projectResult.setStartTime(runStartTime);

        List<Vulnerability> targetVulnerabilities = siegeConfig.getVulnerabilities();
        if (targetVulnerabilities.isEmpty()) {
            LOGGER.info("No vulnerabilities to reach. No generation can be done.");
            String endTime = new SimpleDateFormat(DATE_FORMAT).format(new Date());
            LOGGER.info("Terminating Siege run at: {}", endTime);
            projectResult.setEndTime(endTime);
            return projectResult;
        }

        // Instantiate EvoSuite now just to update the logging context
        EvoSuite fakeEvoSuite = new EvoSuite();
        Path projectPath = siegeConfig.getProjectPath();
        LOGGER.info("({}) Analyzing project: {}", runStartTime, projectPath);

        processClasspath(projectPath);
        Path outTestDirPath = cliConfig.getTestsDirPath();
        LOGGER.info("Writing tests in directory {}.", outTestDirPath.toFile().getCanonicalPath());

        List<String> baseCommands = new ArrayList<>(Arrays.asList(
                // Asks to reach a specific class-method pair in any class in the classpath (e.g., a library)
                "-criterion", Properties.Criterion.REACHABILITY.name(),
                // Asks to evolve test cases, not test suites
                "-generateTests",
                // Number of statements in test cases
                "-Dchromosome_length=" + siegeConfig.getChromosomeLength(),
                "-Dchop_max_length=false",
                // If enabled, we add the list of control dependencies to solve in the goals for REACHABILITY. The extraction is not always possible for all the classes in the static paths. In that case, the list is empty, and behave like usual. This is done to give more guidance with the fitness function.
                "-Dreachability_branch_awareness=" + siegeConfig.isReachabilityBranchAwareness(),
                // This enables the use of a new structure of individuals: the initial test cases should have a method that calls the entry method (according to the static paths in the goals)
                "-Dtest_factory=" + siegeConfig.getInitialPopulationGenerationAlgorithm(),
                // Probability of adding more calls before calling the entry method
                "-Dp_add_calls_before_entry_method=" + siegeConfig.getProbabilityAddCallsBeforeEntryMethod(),
                // Reduce at minimum the probability of creating new variables, but reuse existing ones
                "-Dprimitive_reuse_probability=" + siegeConfig.getProbabilityPrimitiveReuse(),
                "-Dobject_reuse_probability=" + siegeConfig.getProbabilityObjectReuse(),
                // Allowing maximum length to strings to use in tests
                "-Dstring_length=" + siegeConfig.getMaxStringLength(),
                "-Dmax_string=" + siegeConfig.getMaxStringLength(),
                // High probability of sampling primitives from a constant pool
                "-Dprimitive_pool=" + siegeConfig.getProbabilityPrimitivePool(),
                // Use only the static pool, i.e., the pool of constants carved statically
                "-Ddynamic_pool=" + siegeConfig.getProbabilityDynamicPool(),
                // Seed constants from methods or branches from coverage goals of REACHABILITY criterion. If "methods" is active, it has the precedence over "branches". Set both as false to use the ordinary EvoSuite pool
                "-Dreachability_seed_from_methods_in_goals=" + siegeConfig.isReachabilitySeedFromMethodsInGoals(),
                "-Dreachability_seed_from_branches_in_goals=" + siegeConfig.isReachabilitySeedFromBranchesInGoals(),
                // We use the Steady State GA as runner
                "-Dalgorithm=" + siegeConfig.getGaType(),
                // This custom crossover function crosses tests using the points where the tests crashed. For tests not crashing, it behaves like an ordinary single point crossover
                "-Dcrossover_function=" + siegeConfig.getCrossoverAlgorithm(),
                // We ask to use exception points to sample which statements to give priority for crossover or mutation
                "-Dexception_point_sampling=" + siegeConfig.isExceptionPointSampling(),
                // Use our custom mutation algorithm
                "-Dreachability_entry_method_mutation=" + siegeConfig.isReachabilityEntryMethodMutation(),
                // We want an increased probability of changing parameters of a method call
                "-Dp_change_parameter=" + siegeConfig.getProbabilityChangeParameter(),
                // Search operators, can be modified and expect different performance
                "-Dsearch_budget=" + siegeConfig.getSearchBudget(),
                "-Dpopulation=" + siegeConfig.getPopulationSize(),
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
                "-Dinstrument_target_callers=false", // TODO This takes long time, should be tested again as it should increase the number of control dependencies found
                // Where to export the generated tests
                "-Dtest_dir=" + outTestDirPath,
                "-projectCP", classpathString
        ));

        // TODO Log more details about the configuration here...
        LOGGER.info("Using {} seconds budget.", siegeConfig.getSearchBudget());
        LOGGER.info("Evolving populations of {} individuals.", siegeConfig.getPopulationSize());

        LOGGER.info("Generating tests targeting {} vulnerabilities from a pool of {} client classes.", targetVulnerabilities.size(), clientClasses.size());
        LOGGER.debug("Vulnerabilities ({}): {}", targetVulnerabilities.size(), targetVulnerabilities.stream().map(Vulnerability::getCve).collect(Collectors.joining(",")));
        LOGGER.debug("Client classes ({}): {}", clientClasses.size(), clientClasses);
        for (int idx = 0; idx < targetVulnerabilities.size(); idx++) {
            Vulnerability vulnerability = targetVulnerabilities.get(idx);
            LOGGER.info("({}/{}) Generating tests for: {}", idx + 1, targetVulnerabilities.size(), vulnerability.getCve());
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
            FileUtils.deleteDirectory(outTestDirPath.toFile());
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
                GenerationResult generationResult = runGenerationFromClass(entryClass, vulnerability.getCve(), baseCommandsExtended);
                if (generationResult != null) {
                    vulnerabilityResult.addGenerationResult(generationResult);
                }
            }
            projectResult.addCVEResult(vulnerabilityResult);
        }
        String endTime = new SimpleDateFormat(DATE_FORMAT).format(new Date());
        LOGGER.info("Terminating Siege run at: {}", endTime);
        projectResult.setEndTime(endTime);
        return projectResult;
    }

    private GenerationResult runGenerationFromClass(String entryClass, String cve, List<String> baseCommands) throws IOException {
        // Create the generation logging file for this run
        File generationLogFile = null;
        if (generationLogDir != null && generationLogDir.exists()) {
            generationLogFile = Paths.get(generationLogDir.getCanonicalPath(), String.format("%s_%s.log", entryClass.substring(entryClass.lastIndexOf(".") + 1), cve)).toFile();
            try {
                if (generationLogFile.createNewFile()) {
                    LOGGER.info("Writing the generation log in file: {}.", generationLogFile);
                }
            } catch (IOException e) {
                LOGGER.warn("Failed to create the generation log file. No generation log will be written for this run.");
            }
        }
        List<String> evoSuiteCommands = new ArrayList<>(baseCommands);
        evoSuiteCommands.add("-Dgeneration_log_file=" + (generationLogFile != null ? generationLogFile : ""));
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
        if (!cliConfig.isKeepEmptyTests()) {
            SiegeIOHelper.deleteEmptyTestFiles(cliConfig.getTestsDirPath());
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

    private void processClasspath(Path projectPath) {
        LOGGER.info("Looking for classpath files named: {}.", cliConfig.getClasspathFileName());
        List<Path> classpathFiles = ProjectBuilder.findClasspathFiles(projectPath, cliConfig.getClasspathFileName());
        if (classpathFiles.isEmpty()) {
            throw new IllegalArgumentException("No project's classpath file was found.");
        }
        LOGGER.debug("Found {} classpath files: {}.", classpathFiles.size(), classpathFiles);
        LOGGER.info("Looking for directories with .class files under {}.", projectPath);
        // For each folder where classpath file is found, use Maven to determine the build directory (e.g., target/classes). This solution requires setting the maven.home property. We might find a different solution in the future.
        List<Path> projectDirectories = classpathFiles.stream()
                .map(Path::getParent)
                .map(ProjectBuilder::getMavenOutputDirectory)
                .filter(Objects::nonNull)
                .filter(p -> p.toFile().exists())
                .collect(Collectors.toList());
        LOGGER.debug("Found {} project directories: {}", projectDirectories.size(), projectDirectories);
        List<String> classpathElements = ProjectBuilder.readClasspathFiles(classpathFiles);
        if (classpathFiles.isEmpty()) {
            throw new IllegalArgumentException("Could not read any project's classpath file.");
        }
        // Add these directories to the classpath before building the string
        LOGGER.debug("Collecting .class files from {} project directories and {} classpaths.", projectDirectories.size(), classpathFiles.size());
        projectDirectories.stream()
                .sorted(Collections.reverseOrder())
                .map(Path::toString)
                .forEach(d -> classpathElements.add(0, d));
        classpathString = String.join(":", classpathElements);
        clientClasses = ProjectBuilder.findClasses(projectDirectories, classpathString);
        if (clientClasses.isEmpty()) {
            throw new IllegalArgumentException("No client classes were found. No generations can be started.");
        }
        LOGGER.debug("Full project's classpath ({}): {}", classpathElements.size(), classpathString);
    }

    /*
    private void export() {
        // Export time
        List<Map<String, String>> resultsToExport = sessionResults.export();
        try {
            Path outFilePath = Paths.get(outFileDir.getCanonicalPath(), cliConfiguration.getOutFilePath().getFileName().toString());
            SiegeIOHelper.writeToCsv(outFilePath, resultsToExport);
        } catch (IOException e) {
            LOGGER.error("\t* {}", ExceptionUtils.getStackTrace(e));
            LOGGER.info(String.valueOf(resultsToExport));
        }
    }
     */

}
