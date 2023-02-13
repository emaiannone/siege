package it.unisa.siege.core.configuration;

import java.nio.file.Path;
import java.util.List;

public class ProjectConfiguration {
    private Path projectPath;
    private List<Vulnerability> vulnerabilities;
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

    public boolean isReachabilityBranchAwareness() {
        return reachabilityBranchAwareness;
    }

    void setReachabilityBranchAwareness(boolean reachabilityBranchAwareness) {
        this.reachabilityBranchAwareness = reachabilityBranchAwareness;
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

    public boolean isReachabilitySeedFromMethodsInGoals() {
        return reachabilitySeedFromMethodsInGoals;
    }

    void setReachabilitySeedFromMethodsInGoals(boolean reachabilitySeedFromMethodsInGoals) {
        this.reachabilitySeedFromMethodsInGoals = reachabilitySeedFromMethodsInGoals;
    }

    public boolean isReachabilitySeedFromBranchesInGoals() {
        return reachabilitySeedFromBranchesInGoals;
    }

    void setReachabilitySeedFromBranchesInGoals(boolean reachabilitySeedFromBranchesInGoals) {
        this.reachabilitySeedFromBranchesInGoals = reachabilitySeedFromBranchesInGoals;
    }

    public String getGaType() {
        return gaType;
    }

    void setGaType(String gaType) {
        this.gaType = gaType;
    }

    public String getInitialPopulationGenerationAlgorithm() {
        return initialPopulationGenerationAlgorithm;
    }

    void setInitialPopulationGenerationAlgorithm(String initialPopulationGenerationAlgorithm) {
        this.initialPopulationGenerationAlgorithm = initialPopulationGenerationAlgorithm;
    }

    public String getCrossoverAlgorithm() {
        return crossoverAlgorithm;
    }

    void setCrossoverAlgorithm(String crossoverAlgorithm) {
        this.crossoverAlgorithm = crossoverAlgorithm;
    }

    public boolean isReachabilityEntryMethodMutation() {
        return reachabilityEntryMethodMutation;
    }

    void setReachabilityEntryMethodMutation(boolean reachabilityEntryMethodMutation) {
        this.reachabilityEntryMethodMutation = reachabilityEntryMethodMutation;
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
