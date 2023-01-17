package it.unisa.siege.core;

import org.evosuite.Properties;
import org.evosuite.ga.FitnessFunction;
import org.evosuite.ga.metaheuristics.GeneticAlgorithm;
import org.evosuite.ga.stoppingconditions.MaxTimeStoppingCondition;
import org.evosuite.ga.stoppingconditions.StoppingCondition;
import org.evosuite.result.TestGenerationResult;
import org.evosuite.testcase.TestCase;
import org.evosuite.testcase.TestChromosome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class SiegeResults {
    public static final String OUTCOME_SUCCESS = "SUCCESS";
    public static final String OUTCOME_FAILED = "FAILED";
    private static final String HEADER_ENTRY_PATHS = "entryPaths";
    private static final String HEADER_TOTAL_BUDGET = "totalBudget";
    private static final String HEADER_SPENT_BUDGET = "spentBudget";
    private static final String HEADER_POPULATION_SIZE = "populationSize";
    private static final String HEADER_BEST_FITNESS = "bestFitness";
    private static final String HEADER_ITERATIONS = "iterations";
    private static final String HEADER_OUTCOME = "outcome";


    private static final Logger LOGGER = LoggerFactory.getLogger(SiegeRunner.class);
    private final List<Map<String, String>> allResults;

    public SiegeResults() {
        allResults = new ArrayList<>();
    }

    public List<Map<String, String>> export() {
        return Collections.unmodifiableList(allResults);
    }

    public void addResults(String cve, List<List<TestGenerationResult<TestChromosome>>> resultsToAdd) {
        LOGGER.info("Results for {}", cve);
        for (List<TestGenerationResult<TestChromosome>> testResults : resultsToAdd) {
            for (TestGenerationResult<TestChromosome> clientClassResult : testResults) {
                Map<String, String> result = new LinkedHashMap<>();
                GeneticAlgorithm<TestChromosome> algorithm = clientClassResult.getGeneticAlgorithm();
                String clientClassUnderTest = clientClassResult.getClassUnderTest();
                result.put("cve", cve);
                result.put("clientClass", clientClassUnderTest);
                Map<String, TestCase> wroteTests = clientClassResult.getTestCases();
                long totalBudget = Properties.SEARCH_BUDGET;
                // Since EvoSuite does not properly handle the getCurrentValue() method in MaxTimeStoppingCondition, I use an ad hoc method.
                long spentBudget = calculateSpentBudget(algorithm);
                // Get the individuals covering any goal
                TestChromosome bestIndividual = getBestIndividual(algorithm);
                // Use ad hoc function because getFitness() offered by EvoSuite does not "fit" our needs
                double bestFitness = bestIndividual != null ? getBestFitness(bestIndividual) : Double.MAX_VALUE;

                result.put(HEADER_ENTRY_PATHS, String.valueOf(algorithm.getFitnessFunctions().size()));
                result.put(HEADER_TOTAL_BUDGET, String.valueOf(totalBudget));
                result.put(HEADER_SPENT_BUDGET, String.valueOf(spentBudget));
                result.put(HEADER_POPULATION_SIZE, String.valueOf(Properties.POPULATION));
                result.put(HEADER_BEST_FITNESS, String.valueOf(bestFitness != Double.MAX_VALUE ? bestFitness : "MAX"));
                result.put(HEADER_ITERATIONS, String.valueOf(algorithm.getAge() + 1));
                if (wroteTests.isEmpty()) {
                    result.put(HEADER_OUTCOME, OUTCOME_FAILED);
                    LOGGER.info("|-> Could not be reached from class '{}'", clientClassUnderTest);
                } else {
                    // Check if budget is not exhausted and at least one goal was covered
                    result.put(HEADER_OUTCOME, spentBudget < totalBudget && bestFitness == 0 ? OUTCOME_SUCCESS : OUTCOME_FAILED);
                    LOGGER.info("|-> Reached via {}/{} paths from class '{}'", result.get("exploitedPaths"), result.get("entryPaths"), result.get("clientClass"));
                    LOGGER.info("|-> Using {}/{} seconds, within {} iterations.", result.get("spentBudget"), result.get("totalBudget"), result.get("iterations"));
                }
                allResults.add(result);
            }
        }
    }

    private long calculateSpentBudget(GeneticAlgorithm<TestChromosome> algorithm) {
        // For the same reason, isFinished() is unreliable: we have to use spentBudget <= SEARCH_BUDGET
        for (StoppingCondition<TestChromosome> stoppingCondition : algorithm.getStoppingConditions()) {
            if (stoppingCondition instanceof MaxTimeStoppingCondition) {
                MaxTimeStoppingCondition<TestChromosome> timeStoppingCondition = (MaxTimeStoppingCondition<TestChromosome>) stoppingCondition;
                return timeStoppingCondition.getSpentBudget();
            }
        }
        return 0;
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
