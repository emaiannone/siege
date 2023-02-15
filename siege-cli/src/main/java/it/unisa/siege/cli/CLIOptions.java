package it.unisa.siege.cli;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.lang3.EnumUtils;
import org.evosuite.Properties;

public class CLIOptions extends Options {
    // Input part
    public static final String CONFIGURATION_FILE_OPT = "configFile";
    public static final String PROJECT_DIR_OPT = "projectDir";
    public static final String VULNERABILITIES_OPT = "vulnerabilities";

    // Technical part
    public static final String CLASSPATH_FILE_NAME_OPT = "classpathFileName";
    public static final String TESTS_DIR_OPT = "testsDir";
    public static final String OUT_DIR_OPT = "outDir";
    public static final String LOG_DIR_OPT = "logDir";
    public static final String VERBOSE_LOG = "verboseLog";
    public static final String KEEP_EMPTY_TESTS_OPT = "keepEmptyTests";

    // Configuration part
    public static final String CHROMOSOME_LENGTH_OPT = "chromosomeLength";
    public static final String BRANCH_AWARENESS_OPT = "branchAwareness";
    public static final String MAX_STRING_LENGTH_OPT = "maxStringLength";
    public static final String P_ADD_CALLS_BEFORE_ENTRY_METHOD_OPT = "pAddCallsBeforeEntryMethod";
    public static final String P_PRIMITIVE_REUSE_OPT = "pPrimitiveReuse";
    public static final String P_PRIMITIVE_POOL_OPT = "pPrimitivePool";
    public static final String P_OBJECT_REUSE_OPT = "pObjectReuse";
    public static final String P_DYNAMIC_POOL_OPT = "pDynamicPool";
    public static final String P_CHANGE_PARAMETER_OPT = "pChangeParameter";
    public static final String SEED_FROM_METHODS_IN_GOALS_OPT = "seedFromMethodsInGoals";
    public static final String SEED_FROM_BRANCHES_IN_GOALS_OPT = "seedFromBranchesInGoals";
    public static final String METAHEURISTIC_OPT = "metaheuristic";
    public static final String INITIAL_POPULATION_ALGORITHM_OPT = "initialPopulationAlgorithm";
    public static final String CROSSOVER_OPT = "crossover";
    public static final String ENTRY_METHOD_MUTATION_OPT = "entryMutationMutation";
    public static final String EXCEPTION_POINT_SAMPLING_OPT = "exceptionPointSampling";
    public static final String BUDGET_OPT = "budget";
    public static final String POP_SIZE_OPT = "populationSize";

    public static final String HELP_OPT = "help";
    private static CLIOptions INSTANCE;

    private CLIOptions() {
        addOption(Option.builder(CONFIGURATION_FILE_OPT)
                .hasArg(true)
                .desc("Path to a YAML file containing all the configurations which Siege will be run with. The keys in this file are the same as of the command-line options. When supplied, the other command-line options are used as default (if they are supplied too).")
                .build()
        );

        addOption(Option.builder(PROJECT_DIR_OPT)
                .hasArg(true)
                .desc(String.format("Path to the project's directory to inspect. The classes to inspect are determined by the classpath of each project's module (if any), which must be built beforehand (i.e., a set of classpath text files must be available). The classpath files are determined by the option -%s", CLASSPATH_FILE_NAME_OPT))
                .build()
        );

        addOption(Option.builder(VULNERABILITIES_OPT)
                .hasArg(true)
                .desc("Path to the .csv file containing the list of known vulnerabilities to reach and their description with the target class and method.")
                .build()
        );

        addOption(Option.builder(CLASSPATH_FILE_NAME_OPT)
                .hasArg(true)
                .desc(String.format("Name of the text files containing the classpath of the project's modules to inspect. In practice, Siege will look for files named according to -%s option inside the supplied project's directory (either via -%s option or via the configuration file via -%s option) recursively. Such files can be built automatically by running `mvn dependency:build-classpath` or written manually.", CLASSPATH_FILE_NAME_OPT, PROJECT_DIR_OPT, CONFIGURATION_FILE_OPT))
                .build()
        );

        addOption(Option.builder(TESTS_DIR_OPT)
                .hasArg(true)
                .desc("Path to a directory where the Siege's JUnit test files will be stored. Inside this directory, one subdirectory per project will be created. If not supplied, a Siege-specific directory in the current working directory will be used.")
                .build()
        );

        addOption(Option.builder(OUT_DIR_OPT)
                .hasArg(true)
                .desc("Path to a directory where Siege will store the results. Inside this directory, one subdirectory per project will be created. If not supplied, a Siege-specific directory in the current working directory will be used.")
                .build()
        );

        addOption(Option.builder(LOG_DIR_OPT)
                .hasArg(true)
                .desc("Path to a directory where Siege will store the detail of the generations. Inside this directory, one subdirectory per run and per project will be created. If not supplied, a Siege-specific directory in the current working directory will be used.")
                .build()
        );

        addOption(Option.builder(VERBOSE_LOG)
                .hasArg(false)
                .desc("Flag that indicates whether to print extra information in generation logs, e.g., test codes, specific scores for the fitness score. Useful for debugging.")
                .build()
        );

        addOption(Option.builder(KEEP_EMPTY_TESTS_OPT)
                .hasArg(false)
                .desc("Flag that indicates whether to keep the empty tests, which results from failed generations.")
                .build()
        );

        addOption(Option.builder(CHROMOSOME_LENGTH_OPT)
                .hasArg(true)
                .desc("Integer indicating the maximum number of statements each test case have. If invalid, a default value is used.")
                .build()
        );

        addOption(Option.builder(BRANCH_AWARENESS_OPT)
                .hasArg(false)
                .desc("Flag indicating whether the fitness function must take the list of control dependencies into account in addition to the static paths. The extraction might not be possible for some classes appearing the static paths: in these cases, the list of control dependencies is empty.")
                .build()
        );

        addOption(Option.builder(SEED_FROM_METHODS_IN_GOALS_OPT)
                .hasArg(false)
                .desc(String.format("Flag indicating whether the constants must be seeded from methods in the coverage goals. This option has the precedence over the -%s option.", SEED_FROM_BRANCHES_IN_GOALS_OPT))
                .build()
        );

        addOption(Option.builder(SEED_FROM_BRANCHES_IN_GOALS_OPT)
                .hasArg(false)
                .desc(String.format("Flag indicating whether the constants must be seeded from branches in the coverage goals. This option gives the precedence to the -%s option.", SEED_FROM_METHODS_IN_GOALS_OPT))
                .build()
        );

        addOption(Option.builder(METAHEURISTIC_OPT)
                .hasArg(true)
                .desc(String.format("Name of the meta-heuristic to use, selected among: {%s}.", EnumUtils.getEnumList(Properties.Algorithm.class)))
                .build()
        );

        addOption(Option.builder(INITIAL_POPULATION_ALGORITHM_OPT)
                .hasArg(true)
                .desc(String.format("Name of the algorithm for generating the initial population, selected among: {%s}.", EnumUtils.getEnumList(Properties.TestFactory.class)))
                .build()
        );

        addOption(Option.builder(CROSSOVER_OPT)
                .hasArg(true)
                .desc(String.format("Name of the algorithm for crossing individuals, selected among: {%s}.", EnumUtils.getEnumList(Properties.CrossoverFunction.class)))
                .build()
        );

        addOption(Option.builder(ENTRY_METHOD_MUTATION_OPT)
                .hasArg(false)
                .desc("Flag indicating whether the mutation must take into account the call to an entry method.")
                .build()
        );

        addOption(Option.builder(EXCEPTION_POINT_SAMPLING_OPT)
                .hasArg(false)
                .desc("Flag indicating whether the point where the test cases crashed has higher probability of being modified during crossover and mutation.")
                .build()
        );

        addOption(Option.builder(MAX_STRING_LENGTH_OPT)
                .hasArg(true)
                .desc("Integer indicating the maximum length of strings used for constants in tests. If invalid, a default value is used.")
                .build()
        );

        addOption(Option.builder(P_ADD_CALLS_BEFORE_ENTRY_METHOD_OPT)
                .hasArg(true)
                .desc("Probability of adding method calls before calling the entry method.")
                .build()
        );

        addOption(Option.builder(P_PRIMITIVE_REUSE_OPT)
                .hasArg(true)
                .desc("Probability of reusing primitives values in tests instead of creating new ones.")
                .build()
        );

        addOption(Option.builder(P_PRIMITIVE_POOL_OPT)
                .hasArg(true)
                .desc("Probability of using primitive values from a constant pool carved statically.")
                .build()
        );

        addOption(Option.builder(P_OBJECT_REUSE_OPT)
                .hasArg(true)
                .desc("Probability of reusing object values in tests instead of creating new ones.")
                .build()
        );

        addOption(Option.builder(P_DYNAMIC_POOL_OPT)
                .hasArg(true)
                .desc("Probability of using primitive values from a constant pool carved dynamically.")
                .build()
        );

        addOption(Option.builder(P_CHANGE_PARAMETER_OPT)
                .hasArg(true)
                .desc("Probability of changing parameters of method calls during mutation.")
                .build()
        );

        addOption(Option.builder(BUDGET_OPT)
                .hasArg(true)
                .desc("Integer indicating the maximum time budget in seconds given for generating tests for each pair of target and client class. If invalid, a default value is used.")
                .build()
        );

        addOption(Option.builder(POP_SIZE_OPT)
                .hasArg(true)
                .desc("Integer indicating the number of test cases in each generation. If invalid, a default value is used.")
                .build()
        );

        addOption(Option.builder(HELP_OPT)
                .hasArg(false)
                .desc("Show the options available, ignoring all other options used.")
                .build()
        );
    }

    public static CLIOptions getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new CLIOptions();
        }
        return INSTANCE;
    }

}
