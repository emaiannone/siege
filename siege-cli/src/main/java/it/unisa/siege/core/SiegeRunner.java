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
import org.evosuite.coverage.vulnerability.VulnerabilityDescription;
import org.evosuite.ga.FitnessFunction;
import org.evosuite.ga.metaheuristics.GeneticAlgorithm;
import org.evosuite.ga.stoppingconditions.MaxTimeStoppingCondition;
import org.evosuite.ga.stoppingconditions.StoppingCondition;
import org.evosuite.result.TestGenerationResult;
import org.evosuite.testcase.TestCase;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.utils.LoggingUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
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
        // Instantiate EvoSuite now to update the logging context
        EvoSuite evoSuite = new EvoSuite();

        String clientClass = runConfiguration.getClientClass();
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

        // TODO Accept an option for arbitrary project, and get the OutputDirectory of the given path, not the CWD
        // TODO Should run a mvn compile on the target project if possible, otherwise demand the existence of target/classes
        String outputDirectory = getOutputDirectory();
        System.out.println(outputDirectory);
        if (clientClass != null) {
            baseCommands.add("-class");
            baseCommands.add(clientClass);
        } else {
            baseCommands.add("-target");
            baseCommands.add(outputDirectory);
        }
        String projectCP = outputDirectory + ":" + getLibraryClasspath();
        baseCommands.add("-projectCP");
        baseCommands.add(projectCP);

        String project = System.getProperty("user.dir");
        String fullProjectPath = (new File(project)).getCanonicalPath();
        LOGGER.info("Going to generate exploits for {} vulnerabilities through client {}", targetVulnerabilities.size(), fullProjectPath);
        List<Map<String, String>> results = new ArrayList<>();
        for (int i = 0; i < targetVulnerabilities.size(); i++) {
            Pair<String, VulnerabilityDescription> vulnerability = targetVulnerabilities.get(i);
            LOGGER.info("({}/{}) Generating exploits for: {}", i + 1, targetVulnerabilities.size(), vulnerability.getLeft());
            List<String> evoSuiteCommands = new ArrayList<>(baseCommands);
            evoSuiteCommands.add("-Djunit_suffix=" + "_" + vulnerability.getLeft().replace("-", "_") + "_SiegeTest");
            evoSuiteCommands.add("-DvulnClass=" + vulnerability.getRight().getVulnerableClass());
            evoSuiteCommands.add("-DvulnMethod=" + vulnerability.getRight().getVulnerableMethod());
            List<List<TestGenerationResult<TestChromosome>>> evoSuiteResults;
            try {
                // TODO There is an error with InheritanceTreeGenerator, might be due the the new JDK -> I might require JDK 9 for EvoSuite, no beyond
                evoSuiteResults = (List<List<TestGenerationResult<TestChromosome>>>)
                        evoSuite.parseCommandLine(evoSuiteCommands.toArray(new String[0]));
            } catch (Exception e) {
                // Print and go to next iteration
                LOGGER.error("Error while generating exploits for " + vulnerability.getLeft() + ". Skipping.", e);
                continue;
            }
            LOGGER.info("\n-> Results for {}", vulnerability.getLeft());
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
                            LOGGER.info("--> Could not be reached from class '{}'", clientClassUnderTest);
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
                        LOGGER.info("--> Reached via {}/{} paths from class '{}'", result.get("exploitedPaths"), result.get("entryPaths"), result.get("clientClass"));
                        LOGGER.info("---> Using {}/{} seconds, within {} iterations.", result.get("spentBudget"), result.get("totalBudget"), result.get("iterations"));
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

    private void callMaven(List<String> goals) throws IOException, MavenInvocationException {
        if (System.getProperty("maven.home") == null) {
            Runtime rt = Runtime.getRuntime();
            String[] commands = {"whereis", "mvn"};
            Process proc = rt.exec(commands);
            BufferedReader stdInput = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            // Read the output from the command
            String mavenHome = stdInput.readLine().split(" ")[1];
            System.setProperty("maven.home", mavenHome);
        }
        String cwd = System.getProperty("user.dir");
        File tmpfile = File.createTempFile("tmp", ".txt");
        InvocationRequest request = new DefaultInvocationRequest();
        request.setBaseDirectory(new File(cwd));
        request.setGoals(goals);
        request.setBatchMode(true);
        new DefaultInvoker().execute(request);
    }

    // https://maven.apache.org/plugins/maven-help-plugin/evaluate-mojo.html
    private String getOutputDirectory() throws IOException, MavenInvocationException {
        File tmpfile = File.createTempFile("tmp", ".txt");
        try {
            callMaven(Arrays.asList("help:evaluate", "-Dexpression=project.build.outputDirectory", "-q", "-B", "-Doutput=" + tmpfile.getAbsolutePath()));
            return IOUtils.toString(new FileInputStream(tmpfile), StandardCharsets.UTF_8.name());
        } finally {
            tmpfile.delete();
        }
    }

    // http://maven.apache.org/plugins/maven-dependency-plugin/usage.html#dependency:build-classpath
    private String getLibraryClasspath() throws IOException, MavenInvocationException {
        File tmpfile = File.createTempFile("tmp", ".txt");
        try {
            callMaven(Arrays.asList("dependency:build-classpath", "-q", "-B", "-Dmdep.outputFile=" + tmpfile.getAbsolutePath()));
            return IOUtils.toString(new FileInputStream(tmpfile), StandardCharsets.UTF_8.name());
        } finally {
            tmpfile.delete();
        }
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
