package it.unisa.siege.core;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.evosuite.EvoSuite;
import org.evosuite.Properties;
import org.evosuite.analysis.StoredStaticPaths;
import org.evosuite.coverage.reachability.ReachabilityTarget;
import org.evosuite.coverage.reachability.StaticPath;
import org.evosuite.ga.FitnessFunction;
import org.evosuite.ga.metaheuristics.GeneticAlgorithm;
import org.evosuite.ga.stoppingconditions.MaxTimeStoppingCondition;
import org.evosuite.ga.stoppingconditions.StoppingCondition;
import org.evosuite.result.TestGenerationResult;
import org.evosuite.testcase.TestCase;
import org.evosuite.testcase.TestChromosome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SiegeRunner {
    public static final String STATUS_UNREACHABLE = "UNREACHABLE";
    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_FAILED = "FAILED";
    private static final Logger LOGGER = LoggerFactory.getLogger(SiegeRunner.class);
    private final RunConfiguration runConfiguration;

    public SiegeRunner(RunConfiguration runConfiguration) {
        this.runConfiguration = runConfiguration;
    }

    public void run() throws IOException {
        List<Pair<String, ReachabilityTarget>> targetVulnerabilities = runConfiguration.getTargetVulnerabilities();
        if (targetVulnerabilities.isEmpty()) {
            LOGGER.warn("No vulnerabilities to reach. No generation can be done.");
            return;
        }

        // Instantiate EvoSuite now just to update the logging context
        EvoSuite fakeEvoSuite = new EvoSuite();
        LOGGER.info("Analyzing project: {}.", runConfiguration.getProjectPath());
        List<String> baseCommands = new ArrayList<>(Arrays.asList(
                "-generateTests",
                "-criterion", Properties.Criterion.REACHABILITY.name(),
                "-Dbranch_awareness=true",
                "-Dalgorithm=" + Properties.Algorithm.STEADY_STATE_GA.name(),
                "-Dsearch_budget=" + runConfiguration.getBudget(),
                "-Dpopulation=" + runConfiguration.getPopulationSize(),
                "-Dinstrument_parent=false", // If this is true it seems to give problem to RMI
                "-Dinstrument_context=true",
                "-Dinstrument_method_calls=true",
                "-Dinstrument_libraries=true",
                "-Dinstrument_target_callers=false", // TODO Takes long time, to be tested again
                "-Dassertions=false",
                //"-Dcarve_object_pool=true",
                //"-Dchop_carved_exceptions=false",
                "-Dminimize=true",
                "-Dserialize_ga=true",
                "-Dserialize_result=true",
                "-Dcoverage=false",
                "-Dprint_covered_goals=true",
                "-Dprint_missed_goals=true",
                //"-Dshow_progress=false",
                "-Dtest_dir=" + runConfiguration.getTestsDirPath()
        ));

        /*
        List<String> projectDirectories = new ArrayList<>();
        if (!new File(projectPath.toFile(), "pom.xml").exists()) {
            projectDirectories.add(projectPath.toString());
            LOGGER.info("pom.xml file was not found in the target project. Adding {} in the classpath (might miss some .class files).", projectPath);
        } else {
            List<String> allMavenOutputDirectory = getAllMavenOutputDirectory(projectPath);
            if (allMavenOutputDirectory.isEmpty()) {
                throw new IllegalArgumentException("The target project has no compiled classes: it must be compiled first.");
            }
            projectDirectories.addAll(allMavenOutputDirectory);
            LOGGER.info("The target project is a Maven project with compiled sources. Analyzing .class files in {} directories.", projectDirectories.size());
            LOGGER.debug("Project directories: {}", projectDirectories);
        }
         */

        LOGGER.info("Looking for classpath files named: {}.", runConfiguration.getClasspathFileName());
        List<Path> classpathFiles = BuildHelper.findClasspathFiles(runConfiguration.getProjectPath(), runConfiguration.getClasspathFileName());
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

        List<String> classpathElements = BuildHelper.readClasspathFiles(classpathFiles);
        if (classpathFiles.isEmpty()) {
            throw new IllegalArgumentException("Could not read any project's classpath file.");
        }

        // Add these directories to the classpath before building the string
        LOGGER.debug("Collecting .class files from {} project directories and {} classpaths.", projectDirectories.size(), classpathFiles.size());
        projectDirectories.stream().map(Path::toString).forEach(d -> classpathElements.add(0, d));
        String cpString = String.join(":", classpathElements);
        List<String> allClientClasses = BuildHelper.findClassNames(projectDirectories, cpString);
        if (allClientClasses.isEmpty()) {
            throw new IllegalArgumentException("No client classes was found. No generation can be started.");
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
        File generationLogDir = null;
        if (generationLogBaseDir != null) {
            generationLogDir = Paths.get(generationLogBaseDir.getCanonicalPath(), new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss").format(new Date())).toFile();
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

        LOGGER.info("Using {} seconds budget.", runConfiguration.getBudget());
        LOGGER.info("Evolving populations of {} individuals.", runConfiguration.getPopulationSize());
        LOGGER.info("Writing tests in directory {}.", runConfiguration.getTestsDirPath().toFile().getCanonicalPath());
        LOGGER.info("Try to generate tests targeting {} vulnerabilities from a pool of {} client classes.", targetVulnerabilities.size(), allClientClasses.size());
        LOGGER.debug("Vulnerabilities ({}): {}", targetVulnerabilities.size(), targetVulnerabilities);
        LOGGER.debug("Client classes ({}): {}", allClientClasses.size(), allClientClasses);
        List<Map<String, String>> allResults = new ArrayList<>();
        for (int idx = 0; idx < targetVulnerabilities.size(); idx++) {
            Pair<String, ReachabilityTarget> vulnerability = targetVulnerabilities.get(idx);
            LOGGER.info("({}/{}) Generating tests for: {}", idx + 1, targetVulnerabilities.size(), vulnerability.getLeft());
            List<String> baseCommands2 = new ArrayList<>(baseCommands);
            // Must necessarily replace hyphens with underscores to avoid errors while compiling the tests
            baseCommands2.add("-Djunit_suffix=" + "_" + vulnerability.getLeft().replace("-", "_") + "_SiegeTest");
            baseCommands2.add("-Dsiege_target_class=" + vulnerability.getRight().getTargetClass());
            baseCommands2.add("-Dsiege_target_method=" + vulnerability.getRight().getTargetMethod());

            LOGGER.info("Doing a fake EvoSuite run to collect static paths to target {}", vulnerability.getRight());
            List<String> fakeEvoSuiteCommands = new ArrayList<>(baseCommands2);
            fakeEvoSuiteCommands.add("-class");
            fakeEvoSuiteCommands.add(allClientClasses.get(0));

            /* DEBUG
            File fakeGenerationLogFile = null;
            if (generationLogDir != null && generationLogDir.exists()) {
                fakeGenerationLogFile = Paths.get(generationLogDir.getCanonicalPath(), String.format("%s_%s.log", allClientClasses.get(0).substring(allClientClasses.get(0).lastIndexOf(".") + 1), vulnerability.getLeft())).toFile();
                try {
                    if (fakeGenerationLogFile.createNewFile()) {
                        LOGGER.info("Writing the generation log in file {}.", fakeGenerationLogFile);
                    }
                } catch (IOException e) {
                    LOGGER.warn("Failed to create the generation log file. No generation log will be written for this run.");
                }
            }
            fakeEvoSuiteCommands.add("-Dsiege_log_file=" + (fakeGenerationLogFile != null ? fakeGenerationLogFile : ""));
            */

            fakeEvoSuite.parseCommandLine(fakeEvoSuiteCommands.toArray(new String[0]));
            FileUtils.deleteDirectory(runConfiguration.getTestsDirPath().toFile());
            // NOTE If keeping the Map in StoredStaticPaths does not scale, just store the static paths for the current reachability target
            Set<StaticPath> staticPaths = StoredStaticPaths.getStaticPathsToTarget(vulnerability.getRight().getTargetClass(), vulnerability.getRight().getTargetMethod());
            LOGGER.info("Found {} static paths that could reach the target {}.", staticPaths.size(), vulnerability.getRight());
            LOGGER.debug("Static paths to target ({}): {}", staticPaths.size(), staticPaths);
            if (staticPaths.isEmpty()) {
                LOGGER.warn("No client classes seem to reach vulnerability {}. Generation will not start.", vulnerability.getLeft());
                continue;
            }

            // This function gives higher priority to the classes near the root.
            // TODO Prioritize the candidate client classes using a measure of probability of exploitation (heuristic-based)
            List<String> candidateClientClasses = selectRelevantClasses(allClientClasses, staticPaths);
            if (candidateClientClasses.isEmpty()) {
                LOGGER.warn("No client classes seems to reach vulnerability {}. Generation will not start.", vulnerability.getLeft());
                continue;
            }
            LOGGER.info("{} client classes could expose to vulnerability {}.", candidateClientClasses.size(), vulnerability.getLeft());
            LOGGER.debug("Candidate client classes ({}): {}", candidateClientClasses.size(), candidateClientClasses);

            for (String candidateClientClass : candidateClientClasses) {
                LOGGER.info("Starting the test generation from client class: {}", candidateClientClass);
                // Create the generation logging file for this run
                File generationLogFile = null;
                if (generationLogDir != null && generationLogDir.exists()) {
                    generationLogFile = Paths.get(generationLogDir.getCanonicalPath(), String.format("%s_%s.log", candidateClientClass.substring(candidateClientClass.lastIndexOf(".") + 1), vulnerability.getLeft())).toFile();
                    try {
                        if (generationLogFile.createNewFile()) {
                            LOGGER.info("Writing the generation log in file {}.", generationLogFile);
                        }
                    } catch (IOException e) {
                        LOGGER.warn("Failed to create the generation log file. No generation log will be written for this run.");
                    }
                }
                List<String> evoSuiteCommands = new ArrayList<>(baseCommands2);
                evoSuiteCommands.add("-Dsiege_log_file=" + (generationLogFile != null ? generationLogFile : ""));
                evoSuiteCommands.add("-class");
                evoSuiteCommands.add(candidateClientClass);
                List<List<TestGenerationResult<TestChromosome>>> evoSuiteResults;
                try {
                    // TODO It seems that there are instrumentation exceptions concerning javax and spring packages (they are in the inheritance tree). How to deal with them? Why are not they found in the classpath?
                    evoSuiteResults = (List<List<TestGenerationResult<TestChromosome>>>)
                            new EvoSuite().parseCommandLine(evoSuiteCommands.toArray(new String[0]));
                } catch (Exception e) {
                    // Log and go to next iteration
                    LOGGER.warn("A problem occurred while generating exploits with {}. Skipping it.", candidateClientClass);
                    LOGGER.error(ExceptionUtils.getStackTrace(e));
                    continue;
                }
                // TODO Call this only if -keepEmptyTests (a new option to add) is not set.
                deleteEmptyTestFiles(runConfiguration.getTestsDirPath());

                addResults(allResults, evoSuiteResults, vulnerability.getLeft());
            }
        }
        // Export time
        Path outFilePath = runConfiguration.getOutFilePath();
        try {
            if (outFilePath != null) {
                SiegeIO.writeToCsv(outFilePath, allResults);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to export the results on file {}. Printing on stdout instead.", outFilePath);
            LOGGER.error("\t* {}", ExceptionUtils.getStackTrace(e));
            LOGGER.info(String.valueOf(allResults));
        }
    }

    private List<String> selectRelevantClasses(List<String> clientClasses, Set<StaticPath> staticPaths) {
        Set<String> relevantClasses = new LinkedHashSet<>();
        // Sort classes by their proximity to the root of the paths
        Set<StaticPath> relevantPaths = new LinkedHashSet<>();
        for (StaticPath staticPath : staticPaths) {
            Set<String> calledClasses = new LinkedHashSet<>(clientClasses);
            calledClasses.retainAll(staticPath.getCalledClasses());
            if (!calledClasses.isEmpty()) {
                relevantPaths.add(staticPath);
            }
        }
        if (relevantPaths.isEmpty()) {
            return new ArrayList<>();
        }
        int maxLength = relevantPaths.stream()
                .mapToInt(p -> p.getCalledClasses().size())
                .max().getAsInt();
        for (int i = 0; i < maxLength; i++) {
            for (StaticPath staticPath : relevantPaths) {
                if (i < staticPath.length()) {
                    // Add only if this class comes from the client, not from its dependencies
                    String newClientClass = staticPath.get(i).getClassName();
                    if (clientClasses.contains(newClientClass)) {
                        relevantClasses.add(staticPath.get(i).getClassName());
                    }
                }
            }
        }
        return new ArrayList<>(relevantClasses);
    }

    private void addResults(List<Map<String, String>> allResults, List<List<TestGenerationResult<TestChromosome>>> resultsToAdd, String cve) {
        LOGGER.info("Results for {}", cve);
        if (resultsToAdd.size() > 0) {
            for (List<TestGenerationResult<TestChromosome>> testResults : resultsToAdd) {
                for (TestGenerationResult<TestChromosome> clientClassResult : testResults) {
                    Map<String, String> result = new LinkedHashMap<>();
                    GeneticAlgorithm<TestChromosome> algorithm = clientClassResult.getGeneticAlgorithm();
                    String clientClassUnderTest = clientClassResult.getClassUnderTest();
                    result.put("cve", cve);
                    result.put("clientClass", clientClassUnderTest);
                    Map<String, TestCase> wroteTests = clientClassResult.getTestCases();
                    if (wroteTests.isEmpty()) {
                        result.putAll(createUnreachableResult());
                        allResults.add(result);
                        LOGGER.info("|-> Could not be reached from class '{}'", clientClassUnderTest);
                        continue;
                    }

                    // Since EvoSuite does not properly handle the getCurrentValue() method in MaxTimeStoppingCondition, I use an ad hoc method.
                    // For the same reason, isFinished() is unreliable: we have to use spentBudget <= SEARCH_BUDGET
                    long spentBudget = 0;
                    long totalBudget = Properties.SEARCH_BUDGET;
                    for (StoppingCondition<TestChromosome> stoppingCondition : algorithm.getStoppingConditions()) {
                        if (stoppingCondition instanceof MaxTimeStoppingCondition) {
                            MaxTimeStoppingCondition<TestChromosome> timeStoppingCondition = (MaxTimeStoppingCondition<TestChromosome>) stoppingCondition;
                            spentBudget = timeStoppingCondition.getSpentBudget();
                            break;
                        }
                    }
                    // Get the individuals covering any goal
                    TestChromosome bestIndividual = getBestIndividual(algorithm);
                    // Use ad hoc function because getFitness() offered by EvoSuite does not "fit" our needs
                    double bestFitness = getBestFitness(bestIndividual);
                    // Check if budget is not exhausted and at least one goal was covered
                    if (spentBudget < totalBudget && bestFitness == 0) {
                        result.put("status", STATUS_SUCCESS);
                    } else {
                        result.put("status", STATUS_FAILED);
                    }
                    long iterations = algorithm.getAge() + 1;

                    result.put("entryPaths", String.valueOf(algorithm.getFitnessFunctions().size()));
                    result.put("exploitedPaths", String.valueOf(wroteTests.size()));
                    result.put("totalBudget", String.valueOf(totalBudget));
                    result.put("spentBudget", String.valueOf(spentBudget));
                    result.put("populationSize", String.valueOf(Properties.POPULATION));
                    result.put("bestFitness", String.valueOf(bestFitness));
                    result.put("iterations", String.valueOf(iterations));
                    allResults.add(result);
                    LOGGER.info("|-> Reached via {}/{} paths from class '{}'", result.get("exploitedPaths"), result.get("entryPaths"), result.get("clientClass"));
                    LOGGER.info("|-> Using {}/{} seconds, within {} iterations.", result.get("spentBudget"), result.get("totalBudget"), result.get("iterations"));
                }
            }
        } else {
            // TODO Probably this is now unneeded: to be removed. When this is removed, createUnreachableResult() can be inlined
            Map<String, String> result = new LinkedHashMap<>();
            result.put("cve", cve);
            result.put("status", STATUS_UNREACHABLE);
            allResults.add(result);
            LOGGER.info("--> Could not be reached from any client class");
        }
    }

    private Map<String, String> createUnreachableResult() {
        Map<String, String> result = new LinkedHashMap<>();
        result.put("status", STATUS_UNREACHABLE);
        result.put("entryPaths", "0");
        result.put("exploitedPaths", "0");
        result.put("totalBudget", String.valueOf(Properties.SEARCH_BUDGET));
        result.put("spentBudget", "");
        result.put("populationSize", String.valueOf(Properties.POPULATION));
        result.put("bestFitness", "");
        result.put("iterations", "");
        return result;
    }

    private TestChromosome getBestIndividual(GeneticAlgorithm<TestChromosome> algorithm) {
        List<? extends FitnessFunction<TestChromosome>> fitnessFunctions = algorithm.getFitnessFunctions();
        List<TestChromosome> population = algorithm.getPopulation();
        List<TestChromosome> coveringIndividuals = population.stream()
                .filter(tc -> fitnessFunctions.stream().anyMatch(fit -> tc.getFitness(fit) == 0))
                .collect(Collectors.toList());
        if (coveringIndividuals.size() > 0) {
            // Prefer the shortest one
            return coveringIndividuals.stream().min(Comparator.comparingInt(tc -> tc.getTestCase().size())).orElse(null);
        } else {
            // When there are no covering individuals get the top minimal fitness (among all goals)
            double minFitness = Double.MAX_VALUE;
            TestChromosome bestIndividual = null;
            for (TestChromosome tc : population) {
                double bestFit = getBestFitness(tc);
                if (bestFit < minFitness) {
                    minFitness = bestFit;
                    bestIndividual = tc;
                }
            }
            return bestIndividual;
        }
    }

    private double getBestFitness(TestChromosome individual) {
        return Collections.min(individual.getFitnessValues().values());
    }

    private void deleteEmptyTestFiles(Path testsDirPath) throws IOException {
        if (!testsDirPath.toFile().exists()) {
            return;
        }
        List<Path> outputFiles;
        try (Stream<Path> stream = Files.walk(testsDirPath)) {
            outputFiles = stream.filter(Files::isRegularFile)
                    .filter(f -> FilenameUtils.getExtension(String.valueOf(f)).equals("java")).collect(Collectors.toList());
        }
        List<Path> emptyTestFiles = outputFiles.stream()
                .filter(f -> !f.getFileName().toString().contains("scaffolding"))
                .filter(SiegeIO::isTestFileEmpty)
                .collect(Collectors.toList());
        List<Path> filesToDelete = new ArrayList<>();
        for (Path emptyTestFilePath : emptyTestFiles) {
            filesToDelete.add(emptyTestFilePath);
            String testFileName = emptyTestFilePath.toString();
            String testFileBaseName = testFileName.substring(0, testFileName.lastIndexOf("."));
            String scaffoldingFileName = testFileBaseName + "_scaffolding.java";
            Path scaffoldingFilePath = Paths.get(scaffoldingFileName);
            if (outputFiles.contains(scaffoldingFilePath)) {
                filesToDelete.add(scaffoldingFilePath);
            }
        }
        for (Path path : filesToDelete) {
            path.toFile().delete();
        }
    }

}
