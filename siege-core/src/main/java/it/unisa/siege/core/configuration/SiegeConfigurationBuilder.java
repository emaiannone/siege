package it.unisa.siege.core.configuration;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvValidationException;
import org.evosuite.Properties;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class SiegeConfigurationBuilder {
    private Path projectPath;
    private Path vulnerabilitiesFilePath;
    private int chromosomeLength;
    private boolean reachabilityBranchAwareness;
    private int maxStringLength;
    private double probabilityAddCallsBeforeEntryMethod;
    private double probabilityPrimitiveReuse;
    private double probabilityPrimitivePool;
    private double probabilityObjectReuse;
    private double probabilityDynamicPool;
    private double probabilityChangeParameter;
    private boolean reachabilitySeedFromMethodsInGoals;
    private boolean reachabilitySeedFromBranchesInGoals;
    private String gaType;
    private String initialPopulationGenerationAlgorithm;
    private String crossoverAlgorithm;
    private boolean reachabilityEntryMethodMutation;
    private boolean exceptionPointSampling;
    private int searchBudget;
    private int populationSize;

    public SiegeConfigurationBuilder() {
        this.projectPath = null;
        this.vulnerabilitiesFilePath = null;
        this.chromosomeLength = 50;
        this.reachabilityBranchAwareness = true;
        this.maxStringLength = 32767;
        this.probabilityAddCallsBeforeEntryMethod = 0.5;
        this.probabilityPrimitiveReuse = 0.95;
        this.probabilityPrimitivePool = 0.95;
        this.probabilityObjectReuse = 0.95;
        this.probabilityDynamicPool = 0.0;
        this.probabilityChangeParameter = 0.5;
        this.reachabilitySeedFromMethodsInGoals = true;
        this.reachabilitySeedFromBranchesInGoals = true;
        this.gaType = Properties.Algorithm.STEADY_STATE_GA.name();
        this.initialPopulationGenerationAlgorithm = Properties.TestFactory.REACHABILITY_ENTRY_METHOD.name();
        this.crossoverAlgorithm = Properties.CrossoverFunction.REACHABILITY_ENTRY_METHOD.name();
        this.reachabilityEntryMethodMutation = true;
        this.exceptionPointSampling = true;
        this.searchBudget = 60;
        this.populationSize = 100;
    }

    public SiegeConfigurationBuilder setProjectPath(Path projectPath) {
        this.projectPath = projectPath;
        return this;
    }

    public SiegeConfigurationBuilder setVulnerabilitiesFilePath(Path vulnerabilitiesFilePath) {
        this.vulnerabilitiesFilePath = vulnerabilitiesFilePath;
        return this;
    }

    public SiegeConfigurationBuilder setChromosomeLength(int chromosomeLength) {
        this.chromosomeLength = chromosomeLength;
        return this;
    }

    public SiegeConfigurationBuilder setReachabilityBranchAwareness(boolean reachabilityBranchAwareness) {
        this.reachabilityBranchAwareness = reachabilityBranchAwareness;
        return this;
    }

    public SiegeConfigurationBuilder setMaxStringLength(int maxStringLength) {
        this.maxStringLength = maxStringLength;
        return this;
    }

    public SiegeConfigurationBuilder setProbabilityAddCallsBeforeEntryMethod(double probabilityAddCallsBeforeEntryMethod) {
        this.probabilityAddCallsBeforeEntryMethod = probabilityAddCallsBeforeEntryMethod;
        return this;
    }

    public SiegeConfigurationBuilder setProbabilityPrimitiveReuse(double probabilityPrimitiveReuse) {
        this.probabilityPrimitiveReuse = probabilityPrimitiveReuse;
        return this;
    }

    public SiegeConfigurationBuilder setProbabilityPrimitivePool(double probabilityPrimitivePool) {
        this.probabilityPrimitivePool = probabilityPrimitivePool;
        return this;
    }

    public SiegeConfigurationBuilder setProbabilityObjectReuse(double probabilityObjectReuse) {
        this.probabilityObjectReuse = probabilityObjectReuse;
        return this;
    }

    public SiegeConfigurationBuilder setProbabilityDynamicPool(double probabilityDynamicPool) {
        this.probabilityDynamicPool = probabilityDynamicPool;
        return this;
    }

    public SiegeConfigurationBuilder setProbabilityChangeParameter(double probabilityChangeParameter) {
        this.probabilityChangeParameter = probabilityChangeParameter;
        return this;
    }

    public SiegeConfigurationBuilder setReachabilitySeedFromMethodsInGoals(boolean reachabilitySeedFromMethodsInGoals) {
        this.reachabilitySeedFromMethodsInGoals = reachabilitySeedFromMethodsInGoals;
        return this;
    }

    public SiegeConfigurationBuilder setReachabilitySeedFromBranchesInGoals(boolean reachabilitySeedFromBranchesInGoals) {
        this.reachabilitySeedFromBranchesInGoals = reachabilitySeedFromBranchesInGoals;
        return this;
    }

    public SiegeConfigurationBuilder setGaType(String gaType) {
        this.gaType = gaType;
        return this;
    }

    public SiegeConfigurationBuilder setInitialPopulationGenerationAlgorithm(String initialPopulationGenerationAlgorithm) {
        this.initialPopulationGenerationAlgorithm = initialPopulationGenerationAlgorithm;
        return this;
    }

    public SiegeConfigurationBuilder setCrossoverAlgorithm(String crossoverAlgorithm) {
        this.crossoverAlgorithm = crossoverAlgorithm;
        return this;
    }

    public SiegeConfigurationBuilder setReachabilityEntryMethodMutation(boolean reachabilityEntryMethodMutation) {
        this.reachabilityEntryMethodMutation = reachabilityEntryMethodMutation;
        return this;
    }

    public SiegeConfigurationBuilder setExceptionPointSampling(boolean exceptionPointSampling) {
        this.exceptionPointSampling = exceptionPointSampling;
        return this;
    }

    public SiegeConfigurationBuilder setSearchBudget(int searchBudget) {
        this.searchBudget = searchBudget;
        return this;
    }

    public SiegeConfigurationBuilder setPopulationSize(int populationSize) {
        this.populationSize = populationSize;
        return this;
    }

    public SiegeConfiguration build() throws IOException {
        SiegeConfiguration siegeConfiguration = new SiegeConfiguration();
        siegeConfiguration.setProjectPath(projectPath);
        siegeConfiguration.setVulnerabilities(buildVulnerabilities());
        siegeConfiguration.setChromosomeLength(chromosomeLength);
        siegeConfiguration.setReachabilityBranchAwareness(reachabilityBranchAwareness);
        siegeConfiguration.setMaxStringLength(maxStringLength);
        siegeConfiguration.setProbabilityAddCallsBeforeEntryMethod(probabilityAddCallsBeforeEntryMethod);
        siegeConfiguration.setProbabilityPrimitiveReuse(probabilityPrimitiveReuse);
        siegeConfiguration.setProbabilityPrimitivePool(probabilityPrimitivePool);
        siegeConfiguration.setProbabilityObjectReuse(probabilityObjectReuse);
        siegeConfiguration.setProbabilityDynamicPool(probabilityDynamicPool);
        siegeConfiguration.setProbabilityChangeParameter(probabilityChangeParameter);
        siegeConfiguration.setReachabilitySeedFromMethodsInGoals(reachabilitySeedFromMethodsInGoals);
        siegeConfiguration.setReachabilitySeedFromBranchesInGoals(reachabilitySeedFromBranchesInGoals);
        siegeConfiguration.setGaType(gaType);
        siegeConfiguration.setInitialPopulationGenerationAlgorithm(initialPopulationGenerationAlgorithm);
        siegeConfiguration.setCrossoverAlgorithm(crossoverAlgorithm);
        siegeConfiguration.setReachabilityEntryMethodMutation(reachabilityEntryMethodMutation);
        siegeConfiguration.setExceptionPointSampling(exceptionPointSampling);
        siegeConfiguration.setSearchBudget(searchBudget);
        siegeConfiguration.setPopulationSize(populationSize);
        return siegeConfiguration;
    }

    private List<Vulnerability> buildVulnerabilities() throws IOException {
        try {
            List<Vulnerability> vulnerabilities = new ArrayList<>();
            try (CSVReader reader = new CSVReaderBuilder(new FileReader(vulnerabilitiesFilePath.toFile()))
                    .withSkipLines(1)
                    .build()) {
                String[] values;
                while ((values = reader.readNext()) != null) {
                    vulnerabilities.add(new Vulnerability(values[0], values[2], values[3]));
                }
            }
            return vulnerabilities;
        } catch (CsvValidationException e) {
            throw new IOException("Cannot parse the CSV containing the vulnerabilities.", e);
        }
    }
}
