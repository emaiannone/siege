package it.unisa.siege.core.configuration;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvValidationException;
import org.evosuite.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class ProjectConfigurationBuilder {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProjectConfigurationBuilder.class);

    private static final String PROJECT_DIR_DEFAULT = null;
    private static final String VULNERABILITIES_FILE_DEFAULT = null;
    private static final int CHROMOSOME_LENGTH_DEFAULT = 50;
    private static final boolean REACHABILITY_BRANCH_AWARENESS_DEFAULT = true;
    private static final int MAX_STRING_LENGTH_DEFAULT = 32767;
    private static final double PROBABILITY_ADD_CALLS_BEFORE_ENTRY_METHOD_DEFAULT = 0.5;
    private static final double PROBABILITY_PRIMITIVE_REUSE_DEFAULT = 0.95;
    private static final double PROBABILITY_PRIMITIVE_POOL_DEFAULT = 0.95;
    private static final double PROBABILITY_OBJECT_REUSE_DEFAULT = 0.95;
    private static final double PROBABILITY_DYNAMIC_POOL_DEFAULT = 0.0;
    private static final double PROBABILITY_CHANGE_PARAMETER_DEFAULT = 0.5;
    private static final boolean REACHABILITY_SEED_FROM_METHODS_IN_GOALS_DEFAULT = true;
    private static final boolean REACHABILITY_SEED_FROM_BRANCHES_IN_GOALS_DEFAULT = true;
    private static final String GA_TYPE_DEFAULT = Properties.Algorithm.STEADY_STATE_GA.name();
    private static final String INITIAL_POPULATION_GENERATION_ALGORITHM_DEFAULT = Properties.TestFactory.REACHABILITY_ENTRY_METHOD.name();
    private static final String CROSSOVER_ALGORITHM_DEFAULT = Properties.CrossoverFunction.REACHABILITY_ENTRY_METHOD.name();
    private static final boolean REACHABILITY_ENTRY_METHOD_MUTATION_DEFAULT = true;
    private static final boolean EXCEPTION_POINT_SAMPLING_DEFAULT = true;
    private static final int SEARCH_BUDGET_DEFAULT = 60;
    private static final int POPULATION_SIZE_DEFAULT = 100;

    private String projectDir;
    private String vulnerabilitiesFile;
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

    public ProjectConfigurationBuilder() {
        this.projectDir = PROJECT_DIR_DEFAULT;
        this.vulnerabilitiesFile = VULNERABILITIES_FILE_DEFAULT;
        this.chromosomeLength = CHROMOSOME_LENGTH_DEFAULT;
        this.reachabilityBranchAwareness = REACHABILITY_BRANCH_AWARENESS_DEFAULT;
        this.maxStringLength = MAX_STRING_LENGTH_DEFAULT;
        this.probabilityAddCallsBeforeEntryMethod = PROBABILITY_ADD_CALLS_BEFORE_ENTRY_METHOD_DEFAULT;
        this.probabilityPrimitiveReuse = PROBABILITY_PRIMITIVE_REUSE_DEFAULT;
        this.probabilityPrimitivePool = PROBABILITY_PRIMITIVE_POOL_DEFAULT;
        this.probabilityObjectReuse = PROBABILITY_OBJECT_REUSE_DEFAULT;
        this.probabilityDynamicPool = PROBABILITY_DYNAMIC_POOL_DEFAULT;
        this.probabilityChangeParameter = PROBABILITY_CHANGE_PARAMETER_DEFAULT;
        this.reachabilitySeedFromMethodsInGoals = REACHABILITY_SEED_FROM_METHODS_IN_GOALS_DEFAULT;
        this.reachabilitySeedFromBranchesInGoals = REACHABILITY_SEED_FROM_BRANCHES_IN_GOALS_DEFAULT;
        this.gaType = GA_TYPE_DEFAULT;
        this.initialPopulationGenerationAlgorithm = INITIAL_POPULATION_GENERATION_ALGORITHM_DEFAULT;
        this.crossoverAlgorithm = CROSSOVER_ALGORITHM_DEFAULT;
        this.reachabilityEntryMethodMutation = REACHABILITY_ENTRY_METHOD_MUTATION_DEFAULT;
        this.exceptionPointSampling = EXCEPTION_POINT_SAMPLING_DEFAULT;
        this.searchBudget = SEARCH_BUDGET_DEFAULT;
        this.populationSize = POPULATION_SIZE_DEFAULT;
    }

    public ProjectConfigurationBuilder setProjectDir(String projectDir) {
        this.projectDir = projectDir;
        return this;
    }

    public ProjectConfigurationBuilder setVulnerabilitiesFile(String vulnerabilitiesFile) {
        this.vulnerabilitiesFile = vulnerabilitiesFile;
        return this;
    }

    public ProjectConfigurationBuilder setChromosomeLength(int chromosomeLength) {
        this.chromosomeLength = chromosomeLength;
        return this;
    }

    public ProjectConfigurationBuilder setReachabilityBranchAwareness(boolean reachabilityBranchAwareness) {
        this.reachabilityBranchAwareness = reachabilityBranchAwareness;
        return this;
    }

    public ProjectConfigurationBuilder setMaxStringLength(int maxStringLength) {
        this.maxStringLength = maxStringLength;
        return this;
    }

    public ProjectConfigurationBuilder setProbabilityAddCallsBeforeEntryMethod(double probabilityAddCallsBeforeEntryMethod) {
        this.probabilityAddCallsBeforeEntryMethod = probabilityAddCallsBeforeEntryMethod;
        return this;
    }

    public ProjectConfigurationBuilder setProbabilityPrimitiveReuse(double probabilityPrimitiveReuse) {
        this.probabilityPrimitiveReuse = probabilityPrimitiveReuse;
        return this;
    }

    public ProjectConfigurationBuilder setProbabilityPrimitivePool(double probabilityPrimitivePool) {
        this.probabilityPrimitivePool = probabilityPrimitivePool;
        return this;
    }

    public ProjectConfigurationBuilder setProbabilityObjectReuse(double probabilityObjectReuse) {
        this.probabilityObjectReuse = probabilityObjectReuse;
        return this;
    }

    public ProjectConfigurationBuilder setProbabilityDynamicPool(double probabilityDynamicPool) {
        this.probabilityDynamicPool = probabilityDynamicPool;
        return this;
    }

    public ProjectConfigurationBuilder setProbabilityChangeParameter(double probabilityChangeParameter) {
        this.probabilityChangeParameter = probabilityChangeParameter;
        return this;
    }

    public ProjectConfigurationBuilder setReachabilitySeedFromMethodsInGoals(boolean reachabilitySeedFromMethodsInGoals) {
        this.reachabilitySeedFromMethodsInGoals = reachabilitySeedFromMethodsInGoals;
        return this;
    }

    public ProjectConfigurationBuilder setReachabilitySeedFromBranchesInGoals(boolean reachabilitySeedFromBranchesInGoals) {
        this.reachabilitySeedFromBranchesInGoals = reachabilitySeedFromBranchesInGoals;
        return this;
    }

    public ProjectConfigurationBuilder setGaType(String gaType) {
        this.gaType = gaType;
        return this;
    }

    public ProjectConfigurationBuilder setInitialPopulationGenerationAlgorithm(String initialPopulationGenerationAlgorithm) {
        this.initialPopulationGenerationAlgorithm = initialPopulationGenerationAlgorithm;
        return this;
    }

    public ProjectConfigurationBuilder setCrossoverAlgorithm(String crossoverAlgorithm) {
        this.crossoverAlgorithm = crossoverAlgorithm;
        return this;
    }

    public ProjectConfigurationBuilder setReachabilityEntryMethodMutation(boolean reachabilityEntryMethodMutation) {
        this.reachabilityEntryMethodMutation = reachabilityEntryMethodMutation;
        return this;
    }

    public ProjectConfigurationBuilder setExceptionPointSampling(boolean exceptionPointSampling) {
        this.exceptionPointSampling = exceptionPointSampling;
        return this;
    }

    public ProjectConfigurationBuilder setSearchBudget(int searchBudget) {
        this.searchBudget = searchBudget;
        return this;
    }

    public ProjectConfigurationBuilder setPopulationSize(int populationSize) {
        this.populationSize = populationSize;
        return this;
    }

    public ProjectConfiguration build() throws IOException {
        ProjectConfiguration projectConfig = new ProjectConfiguration();

        Path projectPath = projectDir != null ? Paths.get(projectDir) : null;
        if (projectPath == null || !Files.exists(projectPath)) {
            throw new IOException("The supplied project path must point to an existing directory.");
        }
        projectConfig.setProjectPath(projectPath);

        Path vulnerabilitiesPath = vulnerabilitiesFile != null ? Paths.get(vulnerabilitiesFile) : null;
        if (vulnerabilitiesPath == null || !Files.exists(vulnerabilitiesPath)) {
            throw new IOException("The supplied list of vulnerabilities must point to an existing file.");
        }
        List<Vulnerability> vulnerabilities = buildVulnerabilities(vulnerabilitiesPath);
        if (vulnerabilities.isEmpty()) {
            throw new IOException("The supplied list of vulnerabilities was empty.");
        }
        projectConfig.setVulnerabilities(vulnerabilities);

        // TODO Keep validating data, rolling back to defaults (when possible, otherwise raise exception when critical data are missing)
        projectConfig.setChromosomeLength(chromosomeLength);
        projectConfig.setReachabilityBranchAwareness(reachabilityBranchAwareness);
        projectConfig.setMaxStringLength(maxStringLength);
        projectConfig.setProbabilityAddCallsBeforeEntryMethod(probabilityAddCallsBeforeEntryMethod);
        projectConfig.setProbabilityPrimitiveReuse(probabilityPrimitiveReuse);
        projectConfig.setProbabilityPrimitivePool(probabilityPrimitivePool);
        projectConfig.setProbabilityObjectReuse(probabilityObjectReuse);
        projectConfig.setProbabilityDynamicPool(probabilityDynamicPool);
        projectConfig.setProbabilityChangeParameter(probabilityChangeParameter);
        projectConfig.setReachabilitySeedFromMethodsInGoals(reachabilitySeedFromMethodsInGoals);
        projectConfig.setReachabilitySeedFromBranchesInGoals(reachabilitySeedFromBranchesInGoals);
        projectConfig.setGaType(gaType);
        projectConfig.setInitialPopulationGenerationAlgorithm(initialPopulationGenerationAlgorithm);
        projectConfig.setCrossoverAlgorithm(crossoverAlgorithm);
        projectConfig.setReachabilityEntryMethodMutation(reachabilityEntryMethodMutation);
        projectConfig.setExceptionPointSampling(exceptionPointSampling);
        if (searchBudget < 1) {
            LOGGER.info("The search budget must be greater than 1. Using default value ({}).", SEARCH_BUDGET_DEFAULT);
            searchBudget = SEARCH_BUDGET_DEFAULT;
        }
        projectConfig.setSearchBudget(searchBudget);
        if (populationSize < 2) {
            LOGGER.info("The population size must be greater than 2. Using default value ({}).", POPULATION_SIZE_DEFAULT);
            populationSize = POPULATION_SIZE_DEFAULT;
        }
        projectConfig.setPopulationSize(populationSize);
        return projectConfig;
    }

    private List<Vulnerability> buildVulnerabilities(Path vulnerabilitiesPath) throws IOException {
        try {
            List<Vulnerability> vulnerabilities = new ArrayList<>();
            try (CSVReader reader = new CSVReaderBuilder(new FileReader(vulnerabilitiesPath.toFile())).withSkipLines(1).build()) {
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
