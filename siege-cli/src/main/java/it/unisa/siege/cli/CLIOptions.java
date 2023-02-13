package it.unisa.siege.cli;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

public class CLIOptions extends Options {
    // TODO Add more options based on the things we want to experiment
    public static final String CONFIGURATION_FILE_OPT = "configFile";
    public static final String PROJECT_OPT = "project";
    public static final String CLASSPATH_FILE_NAME_OPT = "classpathFileName";
    public static final String VULNERABILITIES_OPT = "vulnerabilities";
    public static final String BUDGET_OPT = "budget";
    public static final String POP_SIZE_OPT = "populationSize";
    public static final String TESTS_DIR_OPT = "testsDir";
    public static final String OUT_DIR_OPT = "outDir";
    public static final String LOG_DIR_OPT = "logDir";
    public static final String KEEP_EMPTY_TESTS_OPT = "keepEmptyTests";
    public static final String HELP_OPT = "help";
    private static CLIOptions INSTANCE;

    private CLIOptions() {
        Option configFileOpt = Option.builder(CONFIGURATION_FILE_OPT)
                .hasArg(true)
                .desc("Path to a YAML file containing all the configuration to test. The keys of this file are the same as of the command-line options. If supplied, the other options are used as default.")
                .build();

        Option projectOpt = Option.builder(PROJECT_OPT)
                .hasArg(true)
                .desc("Path to the project to inspect. If the path points to the root of a Maven-based project (i.e., where a pom.xml file is located), the inspected classes will be taken from the default output directory (e.g., target/classes) using the locally-installed Maven (hence, the project must be compiled beforehand). Alternatively, the path can also point to a directory with .class files to inspect.")
                .build();

        Option classpathFileNameOpt = Option.builder(CLASSPATH_FILE_NAME_OPT)
                .hasArg(true)
                .desc(String.format("Name of the text files containing the classpath of the project's modules to inspect. This file name is searched within the supplied project directory (via option -%s) recursively. Such files could be automatically obtained by running `mvn dependency:build-classpath` or derived manually.", projectOpt.getOpt()))
                .build();

        Option vulnerabilitiesOpt = Option.builder(VULNERABILITIES_OPT)
                .hasArg(true)
                .desc("Path to a .csv file containing the descriptions of the list of known vulnerabilities to reach.")
                .build();

        Option budgetOpt = Option.builder(BUDGET_OPT)
                .hasArg(true)
                .desc("An integer indicating the maximum time budget in seconds given for generating tests for each pair of target and client class. If invalid, a default value is used.")
                .build();

        Option populationOpt = Option.builder(POP_SIZE_OPT)
                .hasArg(true)
                .desc("An integer indicating the number of test cases in each generation. If invalid, a default value is used.")
                .build();

        Option testsDirOpt = Option.builder(TESTS_DIR_OPT)
                .hasArg(true)
                .desc("Path to a directory where the Siege's JUnit test files will be stored.")
                .build();

        Option outDirOpt = Option.builder(OUT_DIR_OPT)
                .hasArg(true)
                .desc("Path to a directory where Siege will store the results. If not specified, the results are printed on the standard output.")
                .build();

        Option logDirOpt = Option.builder(LOG_DIR_OPT)
                .hasArg(true)
                .desc("Path to a directory where Siege will store the detail of the generations.")
                .build();

        Option keepEmptyTestsOpt = Option.builder(KEEP_EMPTY_TESTS_OPT)
                .hasArg(false)
                .desc("Flag that indicates whether to keep the empty test (i.e., when the generation fails to produce any valid test).")
                .build();

        Option helpOpt = Option.builder(HELP_OPT)
                .hasArg(false)
                .desc("Show the options available, ignoring all other options used.")
                .build();

        addOption(configFileOpt);
        addOption(projectOpt);
        addOption(classpathFileNameOpt);
        addOption(vulnerabilitiesOpt);
        addOption(budgetOpt);
        addOption(populationOpt);
        addOption(testsDirOpt);
        addOption(outDirOpt);
        addOption(logDirOpt);
        addOption(keepEmptyTestsOpt);
        addOption(helpOpt);
    }

    public static CLIOptions getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new CLIOptions();
        }
        return INSTANCE;
    }

}
