package it.unisa.siege.core.configuration;

import org.evosuite.Properties;

import java.nio.file.Path;
import java.util.List;

public class ProjectConfiguration {
    private Path projectPath;
    private List<Vulnerability> vulnerabilities;
    private int chromosomeLength;
    private boolean branchAwareness;
    private int maxStringLength;
    private double probabilityAddCallsBeforeEntryMethod;
    private double probabilityPrimitiveReuse;
    private double probabilityPrimitivePool;
    private double probabilityObjectReuse;
    private double probabilityDynamicPool;
    private double probabilityChangeParameter;
    private boolean seedFromMethodsInGoals;
    private boolean seedFromBranchesInGoals;
    private Properties.Algorithm metaheuristic;
    private Properties.TestFactory initialPopulationAlgorithm;
    private Properties.CrossoverFunction crossover;
    private boolean entryMethodMutation;
    private boolean exceptionPointSampling;
    private int searchBudget;
    private int populationSize;

    ProjectConfiguration() {
    }

    public Path getProjectPath() {
        return projectPath;
    }

    void setProjectPath(Path projectPath) {
        this.projectPath = projectPath;
    }

    public List<Vulnerability> getVulnerabilities() {
        return vulnerabilities;
    }

    void setVulnerabilities(List<Vulnerability> vulnerabilities) {
        this.vulnerabilities = vulnerabilities;
    }

    public int getChromosomeLength() {
        return chromosomeLength;
    }

    void setChromosomeLength(int chromosomeLength) {
        this.chromosomeLength = chromosomeLength;
    }

    public boolean isBranchAwareness() {
        return branchAwareness;
    }

    void setBranchAwareness(boolean branchAwareness) {
        this.branchAwareness = branchAwareness;
    }

    public int getMaxStringLength() {
        return maxStringLength;
    }

    void setMaxStringLength(int maxStringLength) {
        this.maxStringLength = maxStringLength;
    }

    public double getProbabilityAddCallsBeforeEntryMethod() {
        return probabilityAddCallsBeforeEntryMethod;
    }

    void setProbabilityAddCallsBeforeEntryMethod(double probabilityAddCallsBeforeEntryMethod) {
        this.probabilityAddCallsBeforeEntryMethod = probabilityAddCallsBeforeEntryMethod;
    }

    public double getProbabilityPrimitiveReuse() {
        return probabilityPrimitiveReuse;
    }

    void setProbabilityPrimitiveReuse(double probabilityPrimitiveReuse) {
        this.probabilityPrimitiveReuse = probabilityPrimitiveReuse;
    }

    public double getProbabilityPrimitivePool() {
        return probabilityPrimitivePool;
    }

    void setProbabilityPrimitivePool(double probabilityPrimitivePool) {
        this.probabilityPrimitivePool = probabilityPrimitivePool;
    }

    public double getProbabilityObjectReuse() {
        return probabilityObjectReuse;
    }

    void setProbabilityObjectReuse(double probabilityObjectReuse) {
        this.probabilityObjectReuse = probabilityObjectReuse;
    }

    public double getProbabilityDynamicPool() {
        return probabilityDynamicPool;
    }

    void setProbabilityDynamicPool(double probabilityDynamicPool) {
        this.probabilityDynamicPool = probabilityDynamicPool;
    }

    public double getProbabilityChangeParameter() {
        return probabilityChangeParameter;
    }

    void setProbabilityChangeParameter(double probabilityChangeParameter) {
        this.probabilityChangeParameter = probabilityChangeParameter;
    }

    public boolean isSeedFromMethodsInGoals() {
        return seedFromMethodsInGoals;
    }

    void setSeedFromMethodsInGoals(boolean seedFromMethodsInGoals) {
        this.seedFromMethodsInGoals = seedFromMethodsInGoals;
    }

    public boolean isSeedFromBranchesInGoals() {
        return seedFromBranchesInGoals;
    }

    void setSeedFromBranchesInGoals(boolean seedFromBranchesInGoals) {
        this.seedFromBranchesInGoals = seedFromBranchesInGoals;
    }

    public Properties.Algorithm getMetaheuristic() {
        return metaheuristic;
    }

    public ProjectConfiguration setMetaheuristic(Properties.Algorithm metaheuristic) {
        this.metaheuristic = metaheuristic;
        return this;
    }

    public Properties.TestFactory getInitialPopulationAlgorithm() {
        return initialPopulationAlgorithm;
    }

    public ProjectConfiguration setInitialPopulationAlgorithm(Properties.TestFactory initialPopulationAlgorithm) {
        this.initialPopulationAlgorithm = initialPopulationAlgorithm;
        return this;
    }

    public Properties.CrossoverFunction getCrossover() {
        return crossover;
    }

    public ProjectConfiguration setCrossover(Properties.CrossoverFunction crossover) {
        this.crossover = crossover;
        return this;
    }

    public boolean isEntryMethodMutation() {
        return entryMethodMutation;
    }

    void setEntryMethodMutation(boolean entryMethodMutation) {
        this.entryMethodMutation = entryMethodMutation;
    }

    public boolean isExceptionPointSampling() {
        return exceptionPointSampling;
    }

    void setExceptionPointSampling(boolean exceptionPointSampling) {
        this.exceptionPointSampling = exceptionPointSampling;
    }

    public int getSearchBudget() {
        return searchBudget;
    }

    void setSearchBudget(int searchBudget) {
        this.searchBudget = searchBudget;
    }

    public int getPopulationSize() {
        return populationSize;
    }

    void setPopulationSize(int populationSize) {
        this.populationSize = populationSize;
    }
}
