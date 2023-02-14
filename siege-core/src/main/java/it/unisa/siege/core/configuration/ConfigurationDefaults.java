package it.unisa.siege.core.configuration;

import org.evosuite.Properties;

import java.nio.file.Path;
import java.nio.file.Paths;

public class ConfigurationDefaults {
    public static final String PROJECT_DIR_DEFAULT = null;
    public static final String VULNERABILITIES_FILE_DEFAULT = null;
    public static final int CHROMOSOME_LENGTH_DEFAULT = 50;
    public static final boolean BRANCH_AWARENESS_DEFAULT = true;
    public static final int MAX_STRING_LENGTH_DEFAULT = 32767;
    public static final double PROBABILITY_ADD_CALLS_BEFORE_ENTRY_METHOD_DEFAULT = 0.5;
    public static final double PROBABILITY_PRIMITIVE_REUSE_DEFAULT = 0.95;
    public static final double PROBABILITY_PRIMITIVE_POOL_DEFAULT = 0.95;
    public static final double PROBABILITY_OBJECT_REUSE_DEFAULT = 0.95;
    public static final double PROBABILITY_DYNAMIC_POOL_DEFAULT = 0.0;
    public static final double PROBABILITY_CHANGE_PARAMETER_DEFAULT = 0.5;
    public static final boolean SEED_FROM_METHODS_IN_GOALS_DEFAULT = true;
    public static final boolean SEED_FROM_BRANCHES_IN_GOALS_DEFAULT = true;
    public static final String METAHEURISTIC_DEFAULT = Properties.Algorithm.STEADY_STATE_GA.name();
    public static final String INITIAL_POPULATION_ALGORITHM_DEFAULT = Properties.TestFactory.REACHABILITY_ENTRY_METHOD.name();
    public static final String CROSSOVER_ALGORITHM_DEFAULT = Properties.CrossoverFunction.REACHABILITY_ENTRY_METHOD.name();
    public static final boolean ENTRY_METHOD_MUTATION_DEFAULT = true;
    public static final boolean EXCEPTION_POINT_SAMPLING_DEFAULT = true;
    public static final int SEARCH_BUDGET_DEFAULT = 60;
    public static final int POPULATION_SIZE_DEFAULT = 100;

    public static final Path TESTS_DIR_PATH_DEFAULT = Paths.get("./siege_tests");
    public static final Path OUT_DIR_PATH_DEFAULT = Paths.get("./siege_results");
    public static final Path LOG_DIR_PATH_DEFAULT = Paths.get("./siege_logs");
}
