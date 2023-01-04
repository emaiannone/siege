package it.unisa.siege.core;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.evosuite.EvoSuite;
import org.evosuite.Properties;
import org.evosuite.TestGenerationContext;
import org.evosuite.classpath.ResourceList;
import org.evosuite.coverage.reachability.ReachabilityTarget;
import org.evosuite.ga.FitnessFunction;
import org.evosuite.ga.metaheuristics.GeneticAlgorithm;
import org.evosuite.ga.stoppingconditions.MaxTimeStoppingCondition;
import org.evosuite.ga.stoppingconditions.StoppingCondition;
import org.evosuite.result.TestGenerationResult;
import org.evosuite.testcase.TestCase;
import org.evosuite.testcase.TestChromosome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
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

    public void run() throws MavenInvocationException, IOException {
        // Instantiate EvoSuite now just to update the logging context
        EvoSuite evoSuite = new EvoSuite();
        LOGGER.info("Going to use {} seconds budget.", runConfiguration.getBudget());
        LOGGER.info("Going to evolve populations of {} individuals.", runConfiguration.getPopulationSize());
        LOGGER.info("Going to write tests in directory {}.", runConfiguration.getTestsDirPath().toFile().getCanonicalPath());
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

        Path project = runConfiguration.getProject();
        String projectDirectory;
        String classpath;
        if (!new File(project.toFile(), "pom.xml").exists()) {
            projectDirectory = project.toString();
            LOGGER.info("pom.xml file was not found in the target project. Going to analyze .class files in {}.", projectDirectory);
            classpath = runConfiguration.getClasspath();
            if (classpath == null) {
                throw new IllegalArgumentException("The project's classpath must be supplied if the target project is not Maven-based.");
            }
            LOGGER.info("The project's classpath was supplied via command-line argument. Going to use {}.", classpath);
        } else {
            // TODO Might need an option that automatically compiles the project (with the supplied Maven executable) first if target/class does not exist yet
            projectDirectory = getMavenOutputDirectory(project);
            if (!Files.exists(Paths.get(projectDirectory))) {
                throw new IllegalArgumentException("The target project must be compiled first.");
            }
            LOGGER.info("The target project is a Maven project with compiled sources. Going to analyze .class files in {}.", projectDirectory);
            classpath = getMavenClasspath(project);
            LOGGER.info("The project's classpath was collected automatically. Going to use {}", classpath);
        }
        baseCommands.add("-projectCP");
        baseCommands.add(projectDirectory + ":" + classpath);

        List<String> classNames = new ArrayList<>();
        String clientClass = runConfiguration.getClientClass();
        if (clientClass != null) {
            classNames.add(clientClass);
        } else {
            classNames = getClassNames(projectDirectory, classpath);
            // baseCommands.add("-target");
            // baseCommands.add(outputDirectory);
        }

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
                    LOGGER.info("Going to write the generation log in directory {}.", generationLogDir.getCanonicalPath());
                } else {
                    LOGGER.warn("Failed to create the generation log directory. No generation log will be written for all runs.");
                }
            } else {
                LOGGER.info("Going to write the generation log in directory {}.", generationLogDir.getCanonicalPath());
            }
        }

        List<Pair<String, ReachabilityTarget>> targetVulnerabilities = runConfiguration.getTargetVulnerabilities();
        LOGGER.info("Going to generate tests targeting {} vulnerabilities from {} classes.", targetVulnerabilities.size(), classNames.size());
        LOGGER.debug("Vulnerabilities: {}", targetVulnerabilities);
        LOGGER.debug("Client classes: {}", classNames);
        List<Map<String, String>> allResults = new ArrayList<>();
        for (int i = 0; i < targetVulnerabilities.size(); i++) {
            Pair<String, ReachabilityTarget> vulnerability = targetVulnerabilities.get(i);
            LOGGER.info("({}/{}) Generating tests for: {}", i + 1, targetVulnerabilities.size(), vulnerability.getLeft());
            List<String> baseCommands2 = new ArrayList<>(baseCommands);
            // NOTE Must replace hyphens with underscores to avoid errors while compiling the tests
            baseCommands2.add("-Djunit_suffix=" + "_" + vulnerability.getLeft().replace("-", "_") + "_SiegeTest");
            baseCommands2.add("-Dsiege_target_class=" + vulnerability.getRight().getTargetClass());
            baseCommands2.add("-Dsiege_target_method=" + vulnerability.getRight().getTargetMethod());
            // TODO Before looping, should do a pre-analysis to filter out classes that do not statically reach any target, and sort them by a measure of probability to prioritize
            for (String className : classNames) {
                LOGGER.info("Starting from class: {}", className);
                // TODO Set ES target class
                // Create the generation logging file for this run
                File generationLogFile = null;
                if (generationLogDir != null && generationLogDir.exists()) {
                    generationLogFile = Paths.get(generationLogDir.getCanonicalPath(), String.format("%s_%s.log", className.substring(className.lastIndexOf(".") + 1), vulnerability.getLeft())).toFile();
                    try {
                        if (generationLogFile.createNewFile()) {
                            LOGGER.info("Going to write the generation log in file {}.", generationLogFile);
                        }
                    } catch (IOException e) {
                        LOGGER.warn("Failed to create the generation log file. No generation log will be written for this run.");
                    }
                }
                List<String> evoSuiteCommands = new ArrayList<>(baseCommands2);
                evoSuiteCommands.add("-Dsiege_log_file=" + (generationLogFile != null ? generationLogFile : ""));
                evoSuiteCommands.add("-class");
                evoSuiteCommands.add(className);
                List<List<TestGenerationResult<TestChromosome>>> evoSuiteResults;
                try {
                    evoSuiteResults = (List<List<TestGenerationResult<TestChromosome>>>)
                            evoSuite.parseCommandLine(evoSuiteCommands.toArray(new String[0]));
                } catch (Exception e) {
                    // Log and go to next iteration
                    LOGGER.warn("A problem occurred while generating exploits for {}. Skipping it.", vulnerability.getLeft());
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

    private void callMaven(List<String> goals, Path directory) throws IOException, MavenInvocationException {
        if (System.getProperty("maven.home") == null) {
            // Try to find mvn location
            ProcessBuilder whichMvn = new ProcessBuilder("which", "mvn");
            Process proc = whichMvn.start();
            BufferedReader stdInput = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            String mavenHome = stdInput.readLine();
            if (mavenHome == null) {
                throw new IOException("Could not find Maven. Must supply the directory where Maven can be found via -Dmaven.home JVM property");
            } else {
                System.setProperty("maven.home", mavenHome);
            }
        }
        InvocationRequest request = new DefaultInvocationRequest();
        request.setBaseDirectory(directory.toFile());
        request.setGoals(goals);
        request.setBatchMode(true);
        new DefaultInvoker().execute(request);
    }

    // https://maven.apache.org/plugins/maven-help-plugin/evaluate-mojo.html
    private String getMavenOutputDirectory(Path directory) throws IOException, MavenInvocationException {
        File tmpfile = File.createTempFile("tmp", ".txt");
        try {
            callMaven(Arrays.asList("help:evaluate", "-Dexpression=project.build.outputDirectory", "-q", "-B", "-Doutput=" + tmpfile.getAbsolutePath()), directory);
            return IOUtils.toString(Files.newInputStream(tmpfile.toPath()), StandardCharsets.UTF_8);
        } finally {
            tmpfile.delete();
        }
    }

    // http://maven.apache.org/plugins/maven-dependency-plugin/usage.html#dependency:build-classpath
    private String getMavenClasspath(Path directory) throws IOException, MavenInvocationException {
        File tmpfile = File.createTempFile("tmp", ".txt");
        try {
            callMaven(Arrays.asList("dependency:build-classpath", "-q", "-B", "-Dmdep.outputFile=" + tmpfile.getAbsolutePath()), directory);
            return IOUtils.toString(Files.newInputStream(tmpfile.toPath()), StandardCharsets.UTF_8);
        } finally {
            tmpfile.delete();
        }
    }

    private List<String> getClassNames(String projectDirectory, String classpath) {
        String oldPropertiesCP = Properties.CP;
        Properties.CP = classpath;
        ResourceList resourceList = ResourceList.getInstance(TestGenerationContext.getInstance().getClassLoaderForSUT());
        ArrayList<String> classNames = new ArrayList<>(resourceList.getAllClasses(projectDirectory, false));
        Properties.CP = oldPropertiesCP;
        return classNames;
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
                    if (wroteTests.size() == 0) {
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
