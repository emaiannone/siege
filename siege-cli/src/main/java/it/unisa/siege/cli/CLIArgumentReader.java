package it.unisa.siege.cli;

import it.unisa.siege.core.configuration.BaseConfiguration;
import it.unisa.siege.core.configuration.ConfigurationDefaults;
import org.apache.commons.cli.*;

public class CLIArgumentReader {
    public static final String HEADER = "Siege: an automated test case generator targeting any method in the classpath.\n\nOptions:";
    public static final String SYNTAX = "java -jar siege.jar";
    public static final String FOOTER = "\nPlease report any issue at https://github.com/emaiannone/siege";

    public static BaseConfiguration read(String[] args) throws ParseException {
        // Fetch the indicated CLI options
        Options options = CLIOptions.getInstance();
        CommandLineParser cliParser = new DefaultParser();
        CommandLine commandLine = cliParser.parse(options, args);
        HelpFormatter helpFormatter = new HelpFormatter();

        if (commandLine.hasOption(CLIOptions.HELP_OPT)) {
            helpFormatter.printHelp(SYNTAX, HEADER, options, FOOTER, true);
            return null;
        }

        BaseConfiguration baseConfig = new BaseConfiguration();
        baseConfig.setConfigurationFile(commandLine.getOptionValue(CLIOptions.CONFIGURATION_FILE_OPT));
        baseConfig.setProjectDir(commandLine.getOptionValue(CLIOptions.PROJECT_DIR_OPT));
        baseConfig.setVulnerabilitiesFile(commandLine.getOptionValue(CLIOptions.VULNERABILITIES_OPT));

        baseConfig.setClasspathFileName(commandLine.getOptionValue(CLIOptions.CLASSPATH_FILE_NAME_OPT));
        baseConfig.setTestsDir(commandLine.getOptionValue(CLIOptions.TESTS_DIR_OPT));
        baseConfig.setOutDir(commandLine.getOptionValue(CLIOptions.OUT_DIR_OPT));
        baseConfig.setLogDir(commandLine.getOptionValue(CLIOptions.LOG_DIR_OPT));
        baseConfig.setKeepEmptyTests(commandLine.hasOption(CLIOptions.KEEP_EMPTY_TESTS_OPT));

        baseConfig.setChromosomeLength(commandLine.hasOption(CLIOptions.CHROMOSOME_LENGTH_OPT) ?
                Integer.parseInt(commandLine.getOptionValue(CLIOptions.CHROMOSOME_LENGTH_OPT)) :
                ConfigurationDefaults.CHROMOSOME_LENGTH_DEFAULT
        );
        baseConfig.setBranchAwareness(commandLine.hasOption(CLIOptions.BRANCH_AWARENESS_OPT));
        baseConfig.setMaxStringLength(commandLine.hasOption(CLIOptions.MAX_STRING_LENGTH_OPT) ?
                Integer.parseInt(commandLine.getOptionValue(CLIOptions.MAX_STRING_LENGTH_OPT)) :
                ConfigurationDefaults.MAX_STRING_LENGTH_DEFAULT
        );
        baseConfig.setProbabilityAddCallsBeforeEntryMethod(commandLine.hasOption(CLIOptions.P_ADD_CALLS_BEFORE_ENTRY_METHOD_OPT) ?
                Double.parseDouble(commandLine.getOptionValue(CLIOptions.P_ADD_CALLS_BEFORE_ENTRY_METHOD_OPT)) :
                ConfigurationDefaults.PROBABILITY_ADD_CALLS_BEFORE_ENTRY_METHOD_DEFAULT
        );
        baseConfig.setProbabilityPrimitiveReuse(commandLine.hasOption(CLIOptions.P_PRIMITIVE_REUSE_OPT) ?
                Double.parseDouble(commandLine.getOptionValue(CLIOptions.P_PRIMITIVE_REUSE_OPT)) :
                ConfigurationDefaults.PROBABILITY_PRIMITIVE_REUSE_DEFAULT
        );
        baseConfig.setProbabilityPrimitivePool(commandLine.hasOption(CLIOptions.P_PRIMITIVE_POOL_OPT) ?
                Double.parseDouble(commandLine.getOptionValue(CLIOptions.P_PRIMITIVE_POOL_OPT)) :
                ConfigurationDefaults.PROBABILITY_PRIMITIVE_POOL_DEFAULT
        );
        baseConfig.setProbabilityObjectReuse(commandLine.hasOption(CLIOptions.P_OBJECT_REUSE_OPT) ?
                Double.parseDouble(commandLine.getOptionValue(CLIOptions.P_OBJECT_REUSE_OPT)) :
                ConfigurationDefaults.PROBABILITY_OBJECT_REUSE_DEFAULT
        );
        baseConfig.setProbabilityDynamicPool(commandLine.hasOption(CLIOptions.P_DYNAMIC_POOL_OPT) ?
                Double.parseDouble(commandLine.getOptionValue(CLIOptions.P_DYNAMIC_POOL_OPT)) :
                ConfigurationDefaults.PROBABILITY_DYNAMIC_POOL_DEFAULT
        );
        baseConfig.setProbabilityChangeParameter(commandLine.hasOption(CLIOptions.P_CHANGE_PARAMETER_OPT) ?
                Double.parseDouble(commandLine.getOptionValue(CLIOptions.P_CHANGE_PARAMETER_OPT)) :
                ConfigurationDefaults.PROBABILITY_CHANGE_PARAMETER_DEFAULT
        );
        baseConfig.setSeedFromMethodsInGoals(commandLine.hasOption(CLIOptions.SEED_FROM_METHODS_IN_GOALS_OPT));
        baseConfig.setSeedFromBranchesInGoals(commandLine.hasOption(CLIOptions.SEED_FROM_BRANCHES_IN_GOALS_OPT));
        baseConfig.setMetaheuristic(commandLine.hasOption(CLIOptions.METAHEURISTIC_OPT) ?
                commandLine.getOptionValue(CLIOptions.METAHEURISTIC_OPT) :
                ConfigurationDefaults.METAHEURISTIC_DEFAULT
        );
        baseConfig.setInitialPopulationAlgorithm(commandLine.hasOption(CLIOptions.INITIAL_POPULATION_ALGORITHM_OPT) ?
                commandLine.getOptionValue(CLIOptions.INITIAL_POPULATION_ALGORITHM_OPT) :
                ConfigurationDefaults.INITIAL_POPULATION_ALGORITHM_DEFAULT
        );
        baseConfig.setCrossover(commandLine.hasOption(CLIOptions.CROSSOVER_OPT) ?
                commandLine.getOptionValue(CLIOptions.CROSSOVER_OPT) :
                ConfigurationDefaults.CROSSOVER_ALGORITHM_DEFAULT
        );
        baseConfig.setEntryMethodMutation(commandLine.hasOption(CLIOptions.ENTRY_METHOD_MUTATION_OPT));
        baseConfig.setExceptionPointSampling(commandLine.hasOption(CLIOptions.EXCEPTION_POINT_SAMPLING_OPT));
        baseConfig.setSearchBudget(commandLine.hasOption(CLIOptions.BUDGET_OPT) ?
                Integer.parseInt(commandLine.getOptionValue(CLIOptions.BUDGET_OPT)) :
                ConfigurationDefaults.SEARCH_BUDGET_DEFAULT
        );
        baseConfig.setPopulationSize(commandLine.hasOption(CLIOptions.POP_SIZE_OPT) ?
                Integer.parseInt(commandLine.getOptionValue(CLIOptions.POP_SIZE_OPT)) :
                ConfigurationDefaults.POPULATION_SIZE_DEFAULT
        );
        return baseConfig;
    }
}
