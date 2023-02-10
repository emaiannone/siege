package it.unisa.siege.core;

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

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class SiegeRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(SiegeRunner.class);
    private static final String DATE_FORMAT = "yyyy_MM_dd_HH_mm_ss";
    private final RunConfiguration runConfiguration;
    private final SiegeResults siegeResults;
    private final String startTime;
    private EvoSuite fakeEvoSuite;
    private List<String> baseCommands;
    private List<Pair<String, ReachabilityTarget>> targetVulnerabilities;
    private List<Path> classpathFiles;
    private List<String> classpathElements;
    private List<String> clientClasses;
    private File generationLogDir;
    private File outFileDir;

    public SiegeRunner(RunConfiguration runConfiguration) throws Exception {
        this.runConfiguration = runConfiguration;
        this.siegeResults = new SiegeResults();
        this.startTime = new SimpleDateFormat(DATE_FORMAT).format(new Date());
        preprocess();
    }

    public void run() throws Exception {
        LOGGER.info("Starting Siege at: {}.", startTime);
        LOGGER.info("Generating tests targeting {} vulnerabilities from a pool of {} client classes.", targetVulnerabilities.size(), clientClasses.size());
        LOGGER.debug("Vulnerabilities ({}): {}", targetVulnerabilities.size(), targetVulnerabilities);
        LOGGER.debug("Client classes ({}): {}", clientClasses.size(), clientClasses);
        for (int idx = 0; idx < targetVulnerabilities.size(); idx++) {
            Pair<String, ReachabilityTarget> vulnerability = targetVulnerabilities.get(idx);
            LOGGER.info("({}/{}) Generating tests for: {}", idx + 1, targetVulnerabilities.size(), vulnerability.getLeft());
            List<String> baseCommandsExtended = new ArrayList<>(baseCommands);
            // Must necessarily replace hyphens with underscores to avoid errors while compiling the tests
            baseCommandsExtended.add("-Djunit_suffix=" + "_" + vulnerability.getLeft().replace("-", "_") + "_SiegeTest");
            baseCommandsExtended.add("-Dreachability_target_class=" + vulnerability.getRight().getTargetClass());
            baseCommandsExtended.add("-Dreachability_target_method=" + vulnerability.getRight().getTargetMethod());
            LOGGER.info("Collecting static paths to: {}", vulnerability.getRight());
            LOGGER.debug("Preparing the fake EvoSuite run");
            List<String> fakeEvoSuiteCommands = new ArrayList<>(baseCommandsExtended);
            fakeEvoSuiteCommands.add("-class");
            fakeEvoSuiteCommands.add(clientClasses.get(0));

            fakeEvoSuite.parseCommandLine(fakeEvoSuiteCommands.toArray(new String[0]));
            FileUtils.deleteDirectory(runConfiguration.getTestsDirPath().toFile());
            // NOTE If keeping the Map in StoredStaticPaths does not scale, just store the static paths for the current reachability target
            Set<StaticPath> staticPaths = StoredStaticPaths.getPathsToTarget(vulnerability.getRight().getTargetClass(), vulnerability.getRight().getTargetMethod());
            LOGGER.info("Found {} static paths that could reach: {}.", staticPaths.size(), vulnerability.getRight());
            LOGGER.debug("Static paths to target ({}): {}", staticPaths.size(), staticPaths);
            if (staticPaths.isEmpty()) {
                LOGGER.warn("No client classes seem to reach vulnerability {}. Generation will not start.", vulnerability.getLeft());
                continue;
            }

            // TODO Make another EntryPointFinder class that prioritize using a measure of probability of exploitation (heuristic-based). The EntryPointFinder type is selected with a CLI option and a factory
            // This method gives higher priority to the classes near the root.
            List<String> entryPoints = new RootProximityEntryPointFinder().findEntryPoints(clientClasses, staticPaths);
            if (entryPoints.isEmpty()) {
                LOGGER.warn("No client classes seems to reach vulnerability {}. Generation will not start.", vulnerability.getLeft());
                continue;
            }
            LOGGER.info("{} client classes could expose to vulnerability {}.", entryPoints.size(), vulnerability.getLeft());
            LOGGER.debug("Entry point client classes ({}): {}", entryPoints.size(), entryPoints);
            for (String entryPoint : entryPoints) {
                LOGGER.info("Starting the generation from class: {}", entryPoint);
                // Create the generation logging file for this run
                File generationLogFile = null;
                if (generationLogDir != null && generationLogDir.exists()) {
                    generationLogFile = Paths.get(generationLogDir.getCanonicalPath(), String.format("%s_%s.log", entryPoint.substring(entryPoint.lastIndexOf(".") + 1), vulnerability.getLeft())).toFile();
                    try {
                        if (generationLogFile.createNewFile()) {
                            LOGGER.info("Writing the generation log in file: {}.", generationLogFile);
                        }
                    } catch (IOException e) {
                        LOGGER.warn("Failed to create the generation log file. No generation log will be written for this run.");
                    }
                }
                List<String> evoSuiteCommands = new ArrayList<>(baseCommandsExtended);
                evoSuiteCommands.add("-Dgeneration_log_file=" + (generationLogFile != null ? generationLogFile : ""));
                evoSuiteCommands.add("-class");
                evoSuiteCommands.add(entryPoint);
                List<List<TestGenerationResult<TestChromosome>>> evoSuiteResults;
                try {
                    evoSuiteResults = (List<List<TestGenerationResult<TestChromosome>>>)
                            new EvoSuite().parseCommandLine(evoSuiteCommands.toArray(new String[0]));
                } catch (Exception e) {
                    // Log and go to next iteration
                    LOGGER.warn("A problem occurred while generating exploits with {}. Skipping it.", entryPoint);
                    LOGGER.error(ExceptionUtils.getStackTrace(e));
                    continue;
                }
                if (!runConfiguration.isKeepEmptyTests()) {
                    SiegeIOHelper.deleteEmptyTestFiles(runConfiguration.getTestsDirPath());
                }
                siegeResults.addResults(vulnerability.getLeft(), evoSuiteResults);
                export();
            }
        }
        String endTime = new SimpleDateFormat(DATE_FORMAT).format(new Date());
        LOGGER.info("Terminating Siege at: {}.", endTime);
    }

    private void preprocess() throws Exception {
        targetVulnerabilities = runConfiguration.getTargetVulnerabilities();
        if (targetVulnerabilities.isEmpty()) {
            LOGGER.warn("No vulnerabilities to reach. No generation can be done.");
            return;
        }

        // Instantiate EvoSuite now just to update the logging context
        fakeEvoSuite = new EvoSuite();
        LOGGER.info("Analyzing project: {}.", runConfiguration.getProjectPath());
        baseCommands = new ArrayList<>(Arrays.asList(
                // Asks to reach a specific class-method pair in any class in the classpath (e.g., a library)
                "-criterion", Properties.Criterion.REACHABILITY.name(),
                // Asks to evolve test cases long 100 statements, not test suites
                "-generateTests",
                "-Dchromosome_length=50",
                "-Dchop_max_length=false",
                // If enabled, we add the list of control dependencies to solve in the goals for REACHABILITY. The extraction is not always possible for all the classes in the static paths. In that case, the list is empty, and behave like usual. This is done to give more guidance with the fitness function.
                "-Dreachability_branch_awareness=true",
                // This enables the use of a new structure of individuals: the initial test cases should have a method that calls the entry method (according to the static paths in the goals)
                "-Dtest_factory", Properties.TestFactory.REACHABILITY_ENTRY_METHOD.name(),
                // Probability of adding more calls before calling the entry method
                "-Dp_add_calls_before_entry_method=0.5",
                // Reduce at minimum the probability of creating new variables, but reuse existing ones
                "-Dprimitive_reuse_probability=0.95",
                "-Dobject_reuse_probability=0.95",
                // Allowing maximum length to strings to use in tests
                "-Dstring_length=32767",
                "-Dmax_string=32767",
                // High probability of sampling primitives from a constant pool
                "-Dprimitive_pool=0.95",
                // Use only the static pool, i.e., the pool of constants carved statically
                "-Ddynamic_pool=0.0",
                // Seed constants from methods or branches from coverage goals of REACHABILITY criterion. If "methods" is active, it has the precedence over "branches". Set both as false to use the ordinary EvoSuite pool
                "-Dreachability_seed_from_methods_in_goals=true",
                "-Dreachability_seed_from_branches_in_goals=true",
                // We use the Steady State GA as runner
                "-Dalgorithm=" + Properties.Algorithm.STEADY_STATE_GA.name(),
                // This custom crossover function crosses tests using the points where the tests crashed. For tests not crashing, it behaves like an ordinary single point crossover
                "-Dcrossover_function=" + Properties.CrossoverFunction.REACHABILITY_ENTRY_METHOD.name(),
                // We ask to use exception points to sample which statements to give priority for crossover or mutation
                "-Dexception_point_sampling=true",
                // Use our custom mutation algorithm
                "-Dreachability_entry_method_mutation=true",
                // We want an increased probability of changing parameters of a method call
                "-Dp_change_parameter=0.5",
                // Search operators, can be modified and expect different performance
                "-Dsearch_budget=" + runConfiguration.getBudget(),
                "-Dpopulation=" + runConfiguration.getPopulationSize(),
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
                "-Dtest_dir=" + runConfiguration.getTestsDirPath()
        ));

        LOGGER.info("Looking for classpath files named: {}.", runConfiguration.getClasspathFileName());
        classpathFiles = BuildHelper.findClasspathFiles(runConfiguration.getProjectPath(), runConfiguration.getClasspathFileName());
        if (classpathFiles.isEmpty()) {
            throw new IllegalArgumentException("No project's classpath file was found.");
        }
        LOGGER.debug("Found {} classpath files: {}.", classpathFiles.size(), classpathFiles);

        LOGGER.info("Looking for directories with .class files under {}.", runConfiguration.getProjectPath());
        // For each folder where classpath file is found, use Maven to determine the build directory (e.g., target/classes). This solution requires setting the maven.home property. We might find a different solution in the future.
        List<Path> projectDirectories = classpathFiles.stream()
                .map(Path::getParent)
                .map(BuildHelper::getMavenOutputDirectory)
                .filter(Objects::nonNull)
                .filter(p -> p.toFile().exists())
                .collect(Collectors.toList());
        LOGGER.debug("Found {} project directories: {}", projectDirectories.size(), projectDirectories);

        classpathElements = BuildHelper.readClasspathFiles(classpathFiles);
        if (classpathFiles.isEmpty()) {
            throw new IllegalArgumentException("Could not read any project's classpath file.");
        }

        // Add these directories to the classpath before building the string
        LOGGER.debug("Collecting .class files from {} project directories and {} classpaths.", projectDirectories.size(), classpathFiles.size());
        projectDirectories.stream()
                .sorted(Collections.reverseOrder())
                .map(Path::toString)
                .forEach(d -> classpathElements.add(0, d));
        String cpString = String.join(":", classpathElements);
        clientClasses = BuildHelper.findClasses(projectDirectories, cpString);
        if (clientClasses.isEmpty()) {
            throw new IllegalArgumentException("No client classes were found. No generations can be started.");
        }
        LOGGER.debug("Full project's classpath ({}): {}", classpathElements.size(), cpString);
        baseCommands.add("-projectCP");
        baseCommands.add(cpString);

        // Create the generation logging directory
        File generationLogBaseDir = runConfiguration.getLogDirPath().toFile();
        if (!generationLogBaseDir.exists()) {
            if (!generationLogBaseDir.mkdirs()) {
                generationLogBaseDir = null;
                LOGGER.warn("Failed to create the generation log directory. No generation details will be logged.");
            }
        }
        if (generationLogBaseDir != null) {
            generationLogDir = Paths.get(generationLogBaseDir.getCanonicalPath(), startTime).toFile();
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
        if (runConfiguration.getOutFilePath() != null) {
            outFileDir = Paths.get(runConfiguration.getOutFilePath().getParent().toString(), startTime).toFile();
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
        LOGGER.info("Using {} seconds budget.", runConfiguration.getBudget());
        LOGGER.info("Evolving populations of {} individuals.", runConfiguration.getPopulationSize());
        LOGGER.info("Writing tests in directory {}.", runConfiguration.getTestsDirPath().toFile().getCanonicalPath());
    }

    private void export() {
        // Export time
        List<Map<String, String>> resultsToExport = siegeResults.export();
        try {
            Path outFilePath = Paths.get(outFileDir.getCanonicalPath(), runConfiguration.getOutFilePath().getFileName().toString());
            SiegeIOHelper.writeToCsv(outFilePath, resultsToExport);
        } catch (IOException e) {
            LOGGER.error("\t* {}", ExceptionUtils.getStackTrace(e));
            LOGGER.info(String.valueOf(resultsToExport));
        }
    }

}
