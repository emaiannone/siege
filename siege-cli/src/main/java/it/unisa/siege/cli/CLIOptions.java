package it.unisa.siege.cli;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

public class CLIOptions extends Options {
    public static final String PROJECT_OPT = "project";
    public static final String CLASSPATH_FILE_NAME_OPT = "classpathFileName";
    //public static final String CLIENT_CLASS_OPT = "clientClass";
    public static final String VULNERABILITIES_OPT = "vulnerabilities";
    public static final String BUDGET_OPT = "budget";
    public static final String POP_SIZE_OPT = "populationSize";
    public static final String TESTS_DIR_OPT = "testsDir";
    public static final String OUT_FILE_OPT = "outFile";
    public static final String LOG_DIR_OPT = "logDir";
    public static final String KEEP_EMPTY_TESTS_OPT = "keepEmptyTests";
    public static final String HELP_OPT = "help";
    public static final int BUDGET_DEFAUlT = 60;
    public static final int POP_SIZE_DEFAUlT = 100;
    public static final String TESTS_DIR_DEFAULT = "./siege_tests";
    private static CLIOptions INSTANCE;

    private CLIOptions() {
        Option projectOpt = Option.builder(PROJECT_OPT)
                .hasArg(true)
                .desc("Path to the project to inspect. If the path points to the root of a Maven-based project (i.e., where a pom.xml file is located), the inspected classes will be taken from the default output directory (e.g., target/classes) using the locally-installed Maven (hence, the project must be compiled beforehand). Alternatively, the path can also point to a directory with .class files to inspect.")
                .build();

        Option classpathFileNameOpt = Option.builder(CLASSPATH_FILE_NAME_OPT)
                .hasArg(true)
                .desc(String.format("Name of the text files containing the classpath of the project's modules to inspect. This file name is searched within the supplied project directory (via option -%s) recursively. Such files could be automatically obtained by running `mvn dependency:build-classpath` or derived manually.", projectOpt.getOpt()))
                .build();

        /*
        Option clientClassOpt = Option.builder(CLIENT_CLASS_OPT)
                .hasArg(true)
                .desc(String.format("The fully-qualified name of a specific class to analyze (e.g., org.foo.SomeClassName). The class must be among the .class files found in the supplied project (via option -%s). If not specified, all the classes will be analyzed.", projectOpt.getOpt()))
                .build();
         */

        Option vulnerabilitiesOpt = Option.builder(VULNERABILITIES_OPT)
                .hasArg(true)
                .desc("Path to a .csv file containing the descriptions of the list of known vulnerabilities to reach.")
                .build();

        Option budgetOpt = Option.builder(BUDGET_OPT)
                .hasArg(true)
                .desc(String.format("An integer indicating the maximum time budget in seconds given for generating tests for each pair of target and client class. Must be greater than 0. If not specified, it defaults to %s", BUDGET_DEFAUlT))
                .build();

        Option populationOpt = Option.builder(POP_SIZE_OPT)
                .hasArg(true)
                .desc(String.format("An integer indicating the number of test cases in each generation. Must be greater than 1. If not specified, it defaults to %s", POP_SIZE_DEFAUlT))
                .build();

        Option testsDirOpt = Option.builder(TESTS_DIR_OPT)
                .hasArg(true)
                .desc("Path to a directory where the Siege's JUnit test files will be stored.")
                .build();

        Option outFileOpt = Option.builder(OUT_FILE_OPT)
                .hasArg(true)
                .desc("Path to a .csv file where the results will be written. If not specified, the results are printed on the standard output.")
                .build();

        Option logDirOpt = Option.builder(LOG_DIR_OPT)
                .hasArg(true)
                .desc("Path to a directory where the detail of the generations will be stored.")
                .build();

        Option keepEmptyTestsOpt = Option.builder(KEEP_EMPTY_TESTS_OPT)
                .hasArg(false)
                .desc("Flag that indicates whether to keep the empty test (i.e., when the generation fails to produce any valid test).")
                .build();

        Option helpOpt = Option.builder(HELP_OPT)
                .hasArg(false)
                .desc("Show the options available, ignoring all other options used.")
                .build();

        addOption(projectOpt);
        addOption(classpathFileNameOpt);
        addOption(vulnerabilitiesOpt);
        addOption(budgetOpt);
        addOption(populationOpt);
        addOption(testsDirOpt);
        addOption(outFileOpt);
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
