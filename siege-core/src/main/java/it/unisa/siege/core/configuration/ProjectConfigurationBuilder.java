package it.unisa.siege.core.configuration;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvValidationException;
import org.apache.commons.lang3.EnumUtils;
import org.evosuite.Properties;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class ProjectConfigurationBuilder {
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
    private String algorithm;
    private String initialPopulationGenerationAlgorithm;
    private String crossoverAlgorithm;
    private boolean reachabilityEntryMethodMutation;
    private boolean exceptionPointSampling;
    private int searchBudget;
    private int populationSize;

    public ProjectConfigurationBuilder() {
        reset();
    }

    public void reset() {
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
        this.algorithm = GA_TYPE_DEFAULT;
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

    public ProjectConfigurationBuilder setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
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
        projectConfig.setProjectPath(Validator.validateProjectDir(projectDir));
        projectConfig.setVulnerabilities(Validator.validateVulnerabilitiesFile(vulnerabilitiesFile));
        projectConfig.setChromosomeLength(Validator.validateChromosomeLength(chromosomeLength));
        projectConfig.setReachabilityBranchAwareness(reachabilityBranchAwareness);
        projectConfig.setMaxStringLength(Validator.validateMaxStringLength(maxStringLength));
        projectConfig.setProbabilityAddCallsBeforeEntryMethod(Validator.validateProbabilityAddCallsBeforeEntryMethod(probabilityAddCallsBeforeEntryMethod));
        projectConfig.setProbabilityPrimitiveReuse(Validator.validateProbabilityPrimitiveReuse(probabilityPrimitiveReuse));
        projectConfig.setProbabilityPrimitivePool(Validator.validateProbabilityPrimitivePool(probabilityPrimitivePool));
        projectConfig.setProbabilityObjectReuse(Validator.validateProbabilityPrimitiveObjectReuse(probabilityObjectReuse));
        projectConfig.setProbabilityDynamicPool(Validator.validateProbabilityDynamicPool(probabilityDynamicPool));
        projectConfig.setProbabilityChangeParameter(Validator.validateProbabilityChangeParameter(probabilityChangeParameter));
        projectConfig.setReachabilitySeedFromMethodsInGoals(reachabilitySeedFromMethodsInGoals);
        projectConfig.setReachabilitySeedFromBranchesInGoals(reachabilitySeedFromBranchesInGoals);
        projectConfig.setAlgorithm(Validator.validateAlgorithm(algorithm));
        projectConfig.setInitialPopulationGenerationAlgorithm(Validator.validateInitialPopulationGenerationAlgorithm(initialPopulationGenerationAlgorithm));
        projectConfig.setCrossoverAlgorithm(Validator.validateCrossoverAlgorithm(crossoverAlgorithm));
        projectConfig.setReachabilityEntryMethodMutation(reachabilityEntryMethodMutation);
        projectConfig.setExceptionPointSampling(exceptionPointSampling);
        projectConfig.setSearchBudget(Validator.validateSearchBudget(searchBudget));
        projectConfig.setPopulationSize(Validator.validatePopulationSize(populationSize));
        return projectConfig;
    }

    private static class Validator {
        private static final int CHROMOSOME_LENGTH_MIN = 2;
        private static final int SEARCH_BUDGET_MIN = 2;
        private static final int POPULATION_SIZE_MIN = 2;
        private static final int MAX_STRING_LENGTH_MIN = 2;
        private static final int MAX_STRING_LENGTH_MAX = 32767;

        private static Path validateProjectDir(String projectDir) {
            Path projectPath = projectDir != null ? Paths.get(projectDir) : null;
            if (projectPath == null || !Files.exists(projectPath)) {
                throw new IllegalStateException("The project directory path does not exist.");
            }
            return projectPath;
        }

        private static List<Vulnerability> validateVulnerabilitiesFile(String vulnerabilitiesFile) {
            Path vulnerabilitiesPath = vulnerabilitiesFile != null ? Paths.get(vulnerabilitiesFile) : null;
            if (vulnerabilitiesPath == null || !Files.exists(vulnerabilitiesPath)) {
                throw new IllegalStateException("The file with vulnerabilities does not exist.");
            }
            try {
                List<Vulnerability> vulnerabilities = readVulnerabilities(vulnerabilitiesPath);
                if (vulnerabilities.isEmpty()) {
                    throw new IllegalStateException("The file with vulnerabilities was empty.");
                }
                return vulnerabilities;
            } catch (IOException e) {
                throw new IllegalStateException("The file with vulnerabilities has an invalid structure.", e);
            }
        }

        private static List<Vulnerability> readVulnerabilities(Path vulnerabilitiesPath) throws IOException {
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
                throw new IOException("Cannot parse the CSV file containing the vulnerabilities.", e);
            }
        }

        private static int validateChromosomeLength(int chromosomeLength) {
            if (chromosomeLength < CHROMOSOME_LENGTH_MIN) {
                throw new IllegalStateException(String.format("The chromosome length cannot be less than %s.", CHROMOSOME_LENGTH_MIN));
            }
            return chromosomeLength;
        }

        private static int validateSearchBudget(int searchBudget) {
            if (searchBudget < SEARCH_BUDGET_MIN) {
                throw new IllegalStateException(String.format("The search budget cannot be less than %s.", SEARCH_BUDGET_MIN));
            }
            return searchBudget;
        }

        private static int validatePopulationSize(int popSize) {
            if (popSize < POPULATION_SIZE_MIN) {
                throw new IllegalStateException(String.format("The population size cannot be less than %s.", POPULATION_SIZE_MIN));
            }
            return popSize;
        }

        private static int validateMaxStringLength(int maxStringLength) {
            if (maxStringLength < MAX_STRING_LENGTH_MIN) {
                throw new IllegalStateException(String.format("The max string length cannot be less than %s.", MAX_STRING_LENGTH_MIN));
            }
            if (maxStringLength > MAX_STRING_LENGTH_MAX) {
                throw new IllegalStateException(String.format("The max string length cannot be more than %s.", MAX_STRING_LENGTH_MAX));
            }
            return maxStringLength;
        }

        private static double validateProbabilityAddCallsBeforeEntryMethod(double probabilityAddCallsBeforeEntryMethod) {
            return validateProbability(probabilityAddCallsBeforeEntryMethod, "The probability of adding calls before entry method call in tests");
        }

        private static double validateProbabilityPrimitiveReuse(double probabilityPrimitiveReuse) {
            return validateProbability(probabilityPrimitiveReuse, "The probability of reusing primitive values in tests");
        }

        private static double validateProbabilityPrimitivePool(double probabilityPrimitivePool) {
            return validateProbability(probabilityPrimitivePool, "The probability of using primitive values from a constant pool built statically");
        }

        private static double validateProbabilityPrimitiveObjectReuse(double probabilityObjectReuse) {
            return validateProbability(probabilityObjectReuse, "The probability of reusing objects in tests");
        }

        private static double validateProbabilityDynamicPool(double probabilityDynamicPool) {
            return validateProbability(probabilityDynamicPool, "The probability of using primitive values from a constant pool built dynamically");
        }

        private static double validateProbabilityChangeParameter(double probabilityChangeParameter) {
            return validateProbability(probabilityChangeParameter, "The probability of changing parameters of method calls during mutation");
        }

        private static double validateProbability(double prob, String baseText) {
            if (prob < 0.0) {
                throw new IllegalStateException(String.format("%s cannot be less than %s.", baseText, 0.0));
            }
            if (prob > 1.0) {
                throw new IllegalStateException(String.format("%s cannot be more than %s.", baseText, 0.0));
            }
            return prob;
        }

        private static Properties.Algorithm validateAlgorithm(String algorithm) {
            if (!EnumUtils.isValidEnum(Properties.Algorithm.class, algorithm)) {
                throw new IllegalStateException(String.format("%s is not a supported algorithm type.", algorithm));
            }
            return EnumUtils.getEnum(Properties.Algorithm.class, algorithm);
        }

        private static Properties.TestFactory validateInitialPopulationGenerationAlgorithm(String initialPopulationGenerationAlgorithm) {
            if (!EnumUtils.isValidEnum(Properties.TestFactory.class, initialPopulationGenerationAlgorithm)) {
                throw new IllegalStateException(String.format("%s is not a supported initial population generation algorithm.", initialPopulationGenerationAlgorithm));
            }
            return EnumUtils.getEnum(Properties.TestFactory.class, initialPopulationGenerationAlgorithm);
        }

        private static Properties.CrossoverFunction validateCrossoverAlgorithm(String crossoverAlgorithm) {
            if (!EnumUtils.isValidEnum(Properties.CrossoverFunction.class, crossoverAlgorithm)) {
                throw new IllegalStateException(String.format("%s is not a supported crossover algorithm.", crossoverAlgorithm));
            }
            return EnumUtils.getEnum(Properties.CrossoverFunction.class, crossoverAlgorithm);
        }
    }
}
