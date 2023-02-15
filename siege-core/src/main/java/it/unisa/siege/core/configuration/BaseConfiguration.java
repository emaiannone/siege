package it.unisa.siege.core.configuration;

public class BaseConfiguration {
    private String configurationFile;
    private String projectDir;
    private String vulnerabilitiesFile;

    private String classpathFileName;
    private String testsDir;
    private String outDir;
    private String logDir;
    private boolean verboseLog;
    private boolean keepEmptyTests;

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
    private String metaheuristic;
    private String initialPopulationAlgorithm;
    private String crossover;
    private boolean entryMethodMutation;
    private boolean exceptionPointSampling;
    private int searchBudget;
    private int populationSize;

    public String getConfigurationFile() {
        return configurationFile;
    }

    public void setConfigurationFile(String configurationFile) {
        this.configurationFile = configurationFile;
    }

    public String getProjectDir() {
        return projectDir;
    }

    public void setProjectDir(String projectDir) {
        this.projectDir = projectDir;
    }

    public String getVulnerabilitiesFile() {
        return vulnerabilitiesFile;
    }

    public void setVulnerabilitiesFile(String vulnerabilitiesFile) {
        this.vulnerabilitiesFile = vulnerabilitiesFile;
    }

    public String getClasspathFileName() {
        return classpathFileName;
    }

    public void setClasspathFileName(String classpathFileName) {
        this.classpathFileName = classpathFileName;
    }

    public String getTestsDir() {
        return testsDir;
    }

    public void setTestsDir(String testsDir) {
        this.testsDir = testsDir;
    }

    public String getOutDir() {
        return outDir;
    }

    public void setOutDir(String outDir) {
        this.outDir = outDir;
    }

    public String getLogDir() {
        return logDir;
    }

    public void setLogDir(String logDir) {
        this.logDir = logDir;
    }

    public boolean isVerboseLog() {
        return verboseLog;
    }

    public void setVerboseLog(boolean verboseLog) {
        this.verboseLog = verboseLog;
    }

    public boolean isKeepEmptyTests() {
        return keepEmptyTests;
    }

    public void setKeepEmptyTests(boolean keepEmptyTests) {
        this.keepEmptyTests = keepEmptyTests;
    }

    public int getChromosomeLength() {
        return chromosomeLength;
    }

    public void setChromosomeLength(int chromosomeLength) {
        this.chromosomeLength = chromosomeLength;
    }

    public boolean isBranchAwareness() {
        return branchAwareness;
    }

    public void setBranchAwareness(boolean branchAwareness) {
        this.branchAwareness = branchAwareness;
    }

    public int getMaxStringLength() {
        return maxStringLength;
    }

    public void setMaxStringLength(int maxStringLength) {
        this.maxStringLength = maxStringLength;
    }

    public double getProbabilityAddCallsBeforeEntryMethod() {
        return probabilityAddCallsBeforeEntryMethod;
    }

    public void setProbabilityAddCallsBeforeEntryMethod(double probabilityAddCallsBeforeEntryMethod) {
        this.probabilityAddCallsBeforeEntryMethod = probabilityAddCallsBeforeEntryMethod;
    }

    public double getProbabilityPrimitiveReuse() {
        return probabilityPrimitiveReuse;
    }

    public void setProbabilityPrimitiveReuse(double probabilityPrimitiveReuse) {
        this.probabilityPrimitiveReuse = probabilityPrimitiveReuse;
    }

    public double getProbabilityPrimitivePool() {
        return probabilityPrimitivePool;
    }

    public void setProbabilityPrimitivePool(double probabilityPrimitivePool) {
        this.probabilityPrimitivePool = probabilityPrimitivePool;
    }

    public double getProbabilityObjectReuse() {
        return probabilityObjectReuse;
    }

    public void setProbabilityObjectReuse(double probabilityObjectReuse) {
        this.probabilityObjectReuse = probabilityObjectReuse;
    }

    public double getProbabilityDynamicPool() {
        return probabilityDynamicPool;
    }

    public void setProbabilityDynamicPool(double probabilityDynamicPool) {
        this.probabilityDynamicPool = probabilityDynamicPool;
    }

    public double getProbabilityChangeParameter() {
        return probabilityChangeParameter;
    }

    public void setProbabilityChangeParameter(double probabilityChangeParameter) {
        this.probabilityChangeParameter = probabilityChangeParameter;
    }

    public boolean isSeedFromMethodsInGoals() {
        return seedFromMethodsInGoals;
    }

    public void setSeedFromMethodsInGoals(boolean seedFromMethodsInGoals) {
        this.seedFromMethodsInGoals = seedFromMethodsInGoals;
    }

    public boolean isSeedFromBranchesInGoals() {
        return seedFromBranchesInGoals;
    }

    public void setSeedFromBranchesInGoals(boolean seedFromBranchesInGoals) {
        this.seedFromBranchesInGoals = seedFromBranchesInGoals;
    }

    public String getMetaheuristic() {
        return metaheuristic;
    }

    public void setMetaheuristic(String metaheuristic) {
        this.metaheuristic = metaheuristic;
    }

    public String getInitialPopulationAlgorithm() {
        return initialPopulationAlgorithm;
    }

    public void setInitialPopulationAlgorithm(String initialPopulationAlgorithm) {
        this.initialPopulationAlgorithm = initialPopulationAlgorithm;
    }

    public String getCrossover() {
        return crossover;
    }

    public void setCrossover(String crossover) {
        this.crossover = crossover;
    }

    public boolean isEntryMethodMutation() {
        return entryMethodMutation;
    }

    public void setEntryMethodMutation(boolean entryMethodMutation) {
        this.entryMethodMutation = entryMethodMutation;
    }

    public boolean isExceptionPointSampling() {
        return exceptionPointSampling;
    }

    public void setExceptionPointSampling(boolean exceptionPointSampling) {
        this.exceptionPointSampling = exceptionPointSampling;
    }

    public int getSearchBudget() {
        return searchBudget;
    }

    public void setSearchBudget(int searchBudget) {
        this.searchBudget = searchBudget;
    }

    public int getPopulationSize() {
        return populationSize;
    }

    public void setPopulationSize(int populationSize) {
        this.populationSize = populationSize;
    }
}
