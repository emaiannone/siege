package it.unisa.siege.core;

import it.unisa.siege.core.common.SiegeIOHelper;
import it.unisa.siege.core.configuration.*;
import it.unisa.siege.core.preprocessing.ProjectBuilder;
import it.unisa.siege.core.preprocessing.TargetDistanceEntryPointFinder;
import it.unisa.siege.core.results.GenerationResult;
import it.unisa.siege.core.results.ProjectResult;
import it.unisa.siege.core.results.VulnerabilityResult;
import org.apache.commons.io.FileUtils;
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
    private static final String DATE_FORMAT_STDOUT = "dd/MM/yyyy HH:mm:ss";
    private static final String DATE_FORMAT_FILE = "yyyy_MM_dd_HH_mm_ss";

    private final BaseConfiguration baseConfig;
    private final List<ProjectConfiguration> projectConfigs;
    private final Path baseTestsDirPath;
    private final Path baseOutDirPath;
    private final Path baseLogDirPath;

    public SiegeLauncher(BaseConfiguration baseConfig) throws Exception {
        this.baseConfig = baseConfig;
        Date siegeStartTime = new Date();
        this.projectConfigs = new ArrayList<>();
        String configFile = baseConfig.getConfigurationFile();
        Path configFilePath = configFile != null ? Paths.get(configFile) : null;
        if (configFilePath != null && Files.exists(configFilePath)) {
            if (!SiegeIOHelper.isYamlFile(configFilePath.toFile())) {
                throw new IOException("The configuration file is not a YAML file.");
            }
            LOGGER.info("Running Siege with configuration file: {}", configFilePath);
            projectConfigs.addAll(YAMLConfigurationFileParser.parseConfigFile(baseConfig));
            LOGGER.info("Found {} projects to analyze", projectConfigs.size());
            LOGGER.debug("Projects ({}): {}", projectConfigs.size(), projectConfigs.stream().map(ProjectConfiguration::getProjectPath).collect(Collectors.toList()));
        } else {
            LOGGER.info("Running Siege with command-line options.");
            ProjectConfiguration projectConfig = new ProjectConfigurationBuilder()
                    .setProjectDir(baseConfig.getProjectDir())
                    .setVulnerabilitiesFile(baseConfig.getVulnerabilitiesFile())
                    .setChromosomeLength(baseConfig.getChromosomeLength())
                    .setBranchAwareness(baseConfig.isBranchAwareness())
                    .setMaxStringLength(baseConfig.getMaxStringLength())
                    .setProbabilityAddCallsBeforeEntryMethod(baseConfig.getProbabilityAddCallsBeforeEntryMethod())
                    .setProbabilityPrimitiveReuse(baseConfig.getProbabilityPrimitiveReuse())
                    .setProbabilityPrimitivePool(baseConfig.getProbabilityPrimitivePool())
                    .setProbabilityObjectReuse(baseConfig.getProbabilityObjectReuse())
                    .setProbabilityDynamicPool(baseConfig.getProbabilityDynamicPool())
                    .setProbabilityChangeParameter(baseConfig.getProbabilityChangeParameter())
                    .setSeedFromMethodsInGoals(baseConfig.isSeedFromMethodsInGoals())
                    .setSeedFromBranchesInGoals(baseConfig.isSeedFromBranchesInGoals())
                    .setMetaheuristic(baseConfig.getMetaheuristic())
                    .setInitialPopulationAlgorithm(baseConfig.getInitialPopulationAlgorithm())
                    .setCrossover(baseConfig.getCrossover())
                    .setEntryMethodMutation(baseConfig.isEntryMethodMutation())
                    .setExceptionPointSampling(baseConfig.isExceptionPointSampling())
                    .setSearchBudget(baseConfig.getSearchBudget())
                    .setPopulationSize(baseConfig.getPopulationSize())
                    .build();
            projectConfigs.add(projectConfig);
        }

        String testsDir = baseConfig.getTestsDir();
        Path testsDirPath = testsDir != null ? Paths.get(testsDir) : ConfigurationDefaults.TESTS_DIR_PATH_DEFAULT;
        baseTestsDirPath = Paths.get(testsDirPath.toString(), new SimpleDateFormat(DATE_FORMAT_FILE).format(siegeStartTime));
        if (!Files.exists(baseTestsDirPath)) {
            Files.createDirectories(baseTestsDirPath);
        }

        String outDir = baseConfig.getOutDir();
        Path outDirPath = outDir != null ? Paths.get(outDir) : ConfigurationDefaults.OUT_DIR_PATH_DEFAULT;
        baseOutDirPath = Paths.get(outDirPath.toString(), new SimpleDateFormat(DATE_FORMAT_FILE).format(siegeStartTime));
        if (!Files.exists(baseOutDirPath)) {
            Files.createDirectories(baseOutDirPath);
        }

        String logDir = baseConfig.getLogDir();
        Path logDirPath = logDir != null ? Paths.get(logDir) : ConfigurationDefaults.LOG_DIR_PATH_DEFAULT;
        baseLogDirPath = Paths.get(logDirPath.toString(), new SimpleDateFormat(DATE_FORMAT_FILE).format(siegeStartTime));
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
        Date projectAnalysisStartTime = new Date();
        projectResult.setStartTime(projectAnalysisStartTime);
        Path projectPath = projectConfig.getProjectPath();
        String projectName = projectPath.getFileName().toString();
        LOGGER.info("({}) Starting analysis for project: {}", new SimpleDateFormat(DATE_FORMAT_STDOUT).format(projectAnalysisStartTime), projectName);
        List<Vulnerability> vulnerabilities = projectConfig.getVulnerabilities();
        if (vulnerabilities.isEmpty()) {
            LOGGER.info("No vulnerabilities to reach. No generation can be done.");
            Date projectAnalysisEndTime = new Date();
            LOGGER.info("Terminating Siege run at: {}", new SimpleDateFormat(DATE_FORMAT_STDOUT).format(projectAnalysisEndTime));
            projectResult.setEndTime(projectAnalysisEndTime);
            return;
        }

        // Instantiate EvoSuite now just to update the logging context
        EvoSuite fakeEvoSuite = new EvoSuite();
        Pair<String, List<String>> processClasspathRes = ProjectBuilder.processClasspath(projectPath, baseConfig.getClasspathFileName());
        String classpathString = processClasspathRes.getLeft();
        List<String> clientClasses = processClasspathRes.getRight();

        // TODO Log details about the project configuration?
        //LOGGER.info("Using {} meta-heuristic.", projectConfig.getMetaheuristic());
        //LOGGER.info("Evolving population of {} individuals long at most {} statements.", projectConfig.getPopulationSize(), projectConfig.getChromosomeLength());
        //LOGGER.info("Using {} seconds budget.", projectConfig.getSearchBudget());

        Path projectTestsDirPath = Paths.get(baseTestsDirPath.toString(), projectName);
        Path projectOutFilePath = Paths.get(baseOutDirPath.toString(), String.format("%s.json", projectName));
        Path projectLogDirPath = Paths.get(baseLogDirPath.toString(), projectName);
        LOGGER.info("Writing tests in directory: {}", projectTestsDirPath.toFile().getCanonicalPath());
        LOGGER.info("Writing results in file: {}", projectOutFilePath.toFile().getCanonicalPath());
        LOGGER.info("Writing the generation logs in directory: {}", projectLogDirPath.toFile().getCanonicalPath());

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
                // Do not remove statements beyond the limits, if this happens
                "-Dchop_max_length=false",
                "-Dtest_dir=" + projectTestsDirPath,
                "-projectCP", classpathString,
                // All the properties we want to control
                "-Dchromosome_length=" + projectConfig.getChromosomeLength(),
                "-Dreachability_branch_awareness=" + projectConfig.isBranchAwareness(),
                "-Dtest_factory=" + projectConfig.getInitialPopulationAlgorithm(),
                "-Dp_add_calls_before_entry_method=" + projectConfig.getProbabilityAddCallsBeforeEntryMethod(),
                "-Dprimitive_reuse_probability=" + projectConfig.getProbabilityPrimitiveReuse(),
                "-Dobject_reuse_probability=" + projectConfig.getProbabilityObjectReuse(),
                "-Dstring_length=" + projectConfig.getMaxStringLength(),
                "-Dmax_string=" + projectConfig.getMaxStringLength(),
                "-Dprimitive_pool=" + projectConfig.getProbabilityPrimitivePool(),
                "-Ddynamic_pool=" + projectConfig.getProbabilityDynamicPool(),
                "-Dreachability_seed_from_methods_in_goals=" + projectConfig.isSeedFromMethodsInGoals(),
                "-Dreachability_seed_from_branches_in_goals=" + projectConfig.isSeedFromBranchesInGoals(),
                "-Dalgorithm=" + projectConfig.getMetaheuristic(),
                "-Dcrossover_function=" + projectConfig.getCrossover(),
                "-Dreachability_entry_method_mutation=" + projectConfig.isEntryMethodMutation(),
                "-Dexception_point_sampling=" + projectConfig.isExceptionPointSampling(),
                "-Dp_change_parameter=" + projectConfig.getProbabilityChangeParameter(),
                "-Dsearch_budget=" + projectConfig.getSearchBudget(),
                "-Dpopulation=" + projectConfig.getPopulationSize(),
                "-Dgeneration_log_verbose=" + baseConfig.isVerboseLog()
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
            FileUtils.deleteQuietly(projectTestsDirPath.toFile());
            // NOTE If keeping the Map in StoredStaticPaths does not scale, just store the static paths for the current reachability target
            Set<StaticPath> staticPaths = StoredStaticPaths.getPathsToTarget(vulnerability.getTargetClass(), vulnerability.getTargetMethod());
            LOGGER.info("Found {} static paths that could reach: {}.", staticPaths.size(), vulnerability.getTargetClass() + vulnerability.getTargetMethod());
            LOGGER.debug("Static paths to target ({}): {}", staticPaths.size(), staticPaths);
            if (staticPaths.isEmpty()) {
                LOGGER.warn("No client classes seem to reach vulnerability {}. Generation will not start.", vulnerability.getCve());
                continue;
            }

            List<String> entryClasses = new TargetDistanceEntryPointFinder().findEntryPoints(clientClasses, staticPaths);
            //List<String> entryClasses = new RootProximityEntryPointFinder().findEntryPoints(clientClasses, staticPaths);
            if (entryClasses.isEmpty()) {
                LOGGER.warn("No client classes seems to reach vulnerability {}. Generation will not start.", vulnerability.getCve());
                continue;
            }
            LOGGER.info("{} client classes could expose to vulnerability {}.", entryClasses.size(), vulnerability.getCve());
            LOGGER.debug("Entry point client classes ({}): {}", entryClasses.size(), entryClasses);

            VulnerabilityResult vulnerabilityResult = new VulnerabilityResult(vulnerability);
            projectResult.addVulnerabilityResult(vulnerabilityResult);
            for (int i = 0; i < entryClasses.size(); i++) {
                String entryClass = entryClasses.get(i);
                LOGGER.info("({}/{}) Starting the generation from class: {}", i + 1, entryClasses.size(), entryClass);

                // Create the generation logging file for this run
                Path logDirPath = Paths.get(projectLogDirPath.toString(), vulnerability.getCve());
                if (!Files.exists(logDirPath)) {
                    Files.createDirectories(logDirPath);
                }
                String entryClassFileName = String.format("%s.log", entryClass.substring(entryClass.lastIndexOf(".") + 1));
                Path logFilePath = Paths.get(logDirPath.toString(), entryClassFileName);
                try {
                    Files.createFile(logFilePath);
                    LOGGER.debug("Writing the generation log in file: {}", logFilePath);
                } catch (IOException e) {
                    LOGGER.warn("Failed to create the generation log file. No log will be written for this generation.");
                    logFilePath = null;
                }

                GenerationResult generationResult = generateFromClass(entryClass, logFilePath, baseCommandsExtended);
                if (generationResult != null) {
                    vulnerabilityResult.addGenerationResult(generationResult);
                }
                if (!baseConfig.isKeepEmptyTests()) {
                    SiegeIOHelper.deleteEmptyTestFiles(projectTestsDirPath);
                }

                // Update the end time after every client class
                projectResult.setEndTime(new Date());

                // Export results obtained so far for this project
                SiegeIOHelper.writeToJson(projectResult.export(), projectOutFilePath);
            }
        }
        LOGGER.info("({}) Finished analyzing project: {}", new SimpleDateFormat(DATE_FORMAT_STDOUT).format(projectResult.getEndTime()), projectName);
    }

    private GenerationResult generateFromClass(String entryClass, Path logFilePath, List<String> baseCommands) {
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
