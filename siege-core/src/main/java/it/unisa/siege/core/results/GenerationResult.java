package it.unisa.siege.core.results;

import it.unisa.siege.core.common.Exportable;
import org.evosuite.ga.FitnessFunction;
import org.evosuite.ga.metaheuristics.GeneticAlgorithm;
import org.evosuite.ga.stoppingconditions.MaxTimeStoppingCondition;
import org.evosuite.ga.stoppingconditions.StoppingCondition;
import org.evosuite.result.TestGenerationResult;
import org.evosuite.testcase.TestChromosome;

import java.util.*;
import java.util.stream.Collectors;

public class GenerationResult implements Exportable<Map<String, Object>> {
    private final String entryClass;
    private final int entryPaths;
    private final int spentBudget;
    private final double bestFitness;
    private final int iterations;
    private final boolean succeeded;

    public GenerationResult(String entryClass, TestGenerationResult<TestChromosome> clientClassResult) {
        this.entryClass = entryClass;
        GeneticAlgorithm<TestChromosome> algorithm = clientClassResult.getGeneticAlgorithm();
        entryPaths = algorithm.getFitnessFunctions().size();
        // Since EvoSuite does not properly handle the getCurrentValue() method in MaxTimeStoppingCondition, I use an ad hoc method.
        spentBudget = calculateSpentBudget(algorithm);
        // Get the individuals covering any goal
        TestChromosome bestIndividual = getBestIndividual(algorithm);
        // Use ad hoc function because getFitness() offered by EvoSuite does not "fit" our needs
        bestFitness = bestIndividual != null ? getBestFitness(bestIndividual) : Double.MAX_VALUE;
        iterations = algorithm.getAge() + 1;
        succeeded = !clientClassResult.getTestCases().isEmpty();
    }

    @Override
    public Map<String, Object> export() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("entryClass", entryClass);
        map.put("entryPaths", entryPaths);
        map.put("spentBudget", spentBudget);
        map.put("bestFitness", bestFitness);
        map.put("iterations", iterations);
        map.put("succeeded", succeeded);
        return map;
    }

    private int calculateSpentBudget(GeneticAlgorithm<TestChromosome> algorithm) {
        // For the same reason, isFinished() is unreliable: we have to use spentBudget <= SEARCH_BUDGET
        for (StoppingCondition<TestChromosome> stoppingCondition : algorithm.getStoppingConditions()) {
            if (stoppingCondition instanceof MaxTimeStoppingCondition) {
                MaxTimeStoppingCondition<TestChromosome> timeStoppingCondition = (MaxTimeStoppingCondition<TestChromosome>) stoppingCondition;
                return (int) timeStoppingCondition.getSpentBudget();
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
