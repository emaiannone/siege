package it.unisa.siege.core;

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
import org.evosuite.coverage.vulnerability.VulnerabilityDescription;
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
        // Instantiate EvoSuite now only to update the logging context
        EvoSuite evoSuite = new EvoSuite();

        List<Pair<String, VulnerabilityDescription>> targetVulnerabilities = runConfiguration.getTargetVulnerabilities();
        List<String> baseCommands = new ArrayList<>(Arrays.asList(
                "-generateTests",
                "-criterion", Properties.Criterion.VULNERABILITY.name(),
                "-Dalgorithm=" + Properties.Algorithm.STEADY_STATE_GA.name(),
                "-Dsearch_budget=" + runConfiguration.getBudget(),
                "-Dpopulation=" + runConfiguration.getPopulationSize(),
                "-Dinstrument_parent=false", // If this is true it seems to give problem to RMI
                "-Dinstrument_context=true",
                "-Dinstrument_method_calls=true",
                "-Dinstrument_libraries=true",
                "-Dassertions=false",
                "-Dminimize=true",
                "-Dserialize_ga=true",
                "-Dserialize_result=true",
                "-Dcoverage=false",
                "-Dprint_covered_goals=true",
                "-Dprint_missed_goals=true",
                //"-Dshow_progress=false",
                "-Dtest_dir=siege_tests"
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
        File generationLogDir = runConfiguration.getLogDirPath().toFile();
        if (!generationLogDir.exists()) {
            if (generationLogDir.mkdirs()) {
                LOGGER.info("Set {} as the generation log directory.", generationLogDir.getCanonicalPath());
            } else {
                generationLogDir = null;
                LOGGER.warn("Failed to create the generation log directory. No generation details will be logged.");
            }
        }

        LOGGER.info("Going to generate tests targeting {} vulnerabilities from {} classes.", targetVulnerabilities.size(), classNames.size());
        LOGGER.debug("Vulnerabilities: {}", targetVulnerabilities);
        LOGGER.debug("Client classes: {}", classNames);
        List<Map<String, String>> results = new ArrayList<>();
        for (int i = 0; i < targetVulnerabilities.size(); i++) {
            Pair<String, VulnerabilityDescription> vulnerability = targetVulnerabilities.get(i);
            LOGGER.info("({}/{}) Going to generate tests to reach: {}", i + 1, targetVulnerabilities.size(), vulnerability.getLeft());
            // Create the generation logging file for this run
            File generationLogFile = null;
            if (generationLogDir != null && generationLogDir.exists()) {
                generationLogFile = Paths.get(generationLogDir.getCanonicalPath(), new SimpleDateFormat("yyyy_MM_dd_h_mm_ss").format(new Date()) + ".log").toFile();
                try {
                    if (generationLogFile.createNewFile()) {
                        LOGGER.info("Set {} as the generation log file.", generationLogFile);
                    }
                } catch (IOException e) {
                    LOGGER.warn("Failed to create the generation log file. No generation details will be logged.");
                }
            }

            List<String> evoSuiteCommands = new ArrayList<>(baseCommands);
            evoSuiteCommands.add("-Djunit_suffix=" + "_" + vulnerability.getLeft().replace("-", "_") + "_SiegeTest");
            evoSuiteCommands.add("-DsiegeTargetClass=" + vulnerability.getRight().getVulnerableClass());
            evoSuiteCommands.add("-DsiegeTargetMethod=" + vulnerability.getRight().getVulnerableMethod());
            evoSuiteCommands.add("-DsiegeLogFile=" + (generationLogFile != null ? generationLogFile : ""));
            // TODO Before looping, should do a pre-analysis to filter out classes that do not statically reach any target, and sort them by probability
            for (String className : classNames) {
                LOGGER.info("Starting the generation from class: {}", className);
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
                LOGGER.info("Results for {}", vulnerability.getLeft());
                if (evoSuiteResults.size() > 0) {
                    for (List<TestGenerationResult<TestChromosome>> testResults : evoSuiteResults) {
                        for (TestGenerationResult<TestChromosome> clientClassResult : testResults) {
                            Map<String, String> result = new LinkedHashMap<>();
                            GeneticAlgorithm<TestChromosome> algorithm = clientClassResult.getGeneticAlgorithm();
                            String clientClassUnderTest = clientClassResult.getClassUnderTest();
                            result.put("cve", vulnerability.getLeft());
                            result.put("clientClass", clientClassUnderTest);
                            Map<String, TestCase> wroteTests = clientClassResult.getTestCases();
                            if (wroteTests.size() == 0) {
                                result.putAll(createUnreachableResult());
                                results.add(result);
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
                            results.add(result);
                            LOGGER.info("|-> Reached via {}/{} paths from class '{}'", result.get("exploitedPaths"), result.get("entryPaths"), result.get("clientClass"));
                            LOGGER.info("|-> Using {}/{} seconds, within {} iterations.", result.get("spentBudget"), result.get("totalBudget"), result.get("iterations"));
                        }
                    }
                } else {
                    // TODO Probably this is now unneeded: to be removed. When this is removed, createUnreachableResult() can be inlined
                    Map<String, String> result = new LinkedHashMap<>();
                    result.put("cve", vulnerability.getLeft());
                    result.put("status", STATUS_UNREACHABLE);
                    results.add(result);
                    LOGGER.info("--> Could not be reached from any client class");
                }
            }
        }

        // Export time
        Path outFilePath = runConfiguration.getOutFilePath();
        try {
            if (outFilePath != null) {
                SiegeIO.writeToCsv(outFilePath, results);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to export the results on file {}. Printing on stdout instead.", outFilePath);
            LOGGER.error("\t* {}", ExceptionUtils.getStackTrace(e));
            LOGGER.info(String.valueOf(results));
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

    /*
    private static String buildProjectClasspath(String target, String librariesPath) {
        StringBuilder jarsPaths = new StringBuilder();
        File jarsDir = new File(librariesPath);
        if (jarsDir.isDirectory()) {
            for (File file : jarsDir.listFiles()) {
                if (file.isFile() && file.getName().contains(".jar")) {
                    jarsPaths.append(":");
                    jarsPaths.append(file.getAbsolutePath());
                }
            }
        }
        return target + jarsPaths;
    }
     */

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

}
