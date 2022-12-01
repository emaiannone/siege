package it.unisa.siege.cli;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

public class CLIOptions extends Options {
    public static final String PROJECT_OPT = "project";
    public static final String CLASSPATH_OPT = "classpath";
    public static final String CLIENT_CLASS_OPT = "clientClass";
    public static final String VULNERABILITIES_OPT = "vulnerabilities";
    public static final String BUDGET_OPT = "budget";
    public static final String POP_SIZE_OPT = "populationSize";
    public static final String OUT_FILE_OPT = "outFile";
    public static final String HELP_OPT = "help";
    public static final int BUDGET_DEFAUlT = 60;
    public static final int POP_SIZE_DEFAUlT = 100;
    private static CLIOptions INSTANCE;

    private CLIOptions() {
        Option projectOpt = Option.builder(PROJECT_OPT)
                .hasArg(true)
                .desc("Path to the project to inspect. If the path points to the root of a Maven-based project (i.e., where a pom.xml file is located), the inspected classes will be taken from the default output directory (e.g., target/classes) using the locally-installed Maven (hence, the project must be compiled beforehand). Alternatively, the path can also point to a directory with .class files to inspect.")
                .build();

        Option classpathOpt = Option.builder(CLASSPATH_OPT)
                .hasArg(true)
                .desc(String.format("Classpath string to use if the supplied project directory (via option -%s) was not a Maven-based project (this option is ignore in that case).", PROJECT_OPT))
                .build();

        Option clientClassOpt = Option.builder(CLIENT_CLASS_OPT)
                .hasArg(true)
                .desc(String.format("The fully-qualified name of a specific class to analyze (e.g., org.foo.SomeClassName). The class must be among the .class files found in the supplied project (via option -%s). If not specified, all the classes will be analyzed.", projectOpt.getOpt()))
                .build();

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

        Option outFileOpt = Option.builder(OUT_FILE_OPT)
                .hasArg(true)
                .desc("Path to a .csv file where the results will be written. If not specified, the results are printed on the standard output.")
                .build();

        Option helpOpt = Option.builder(HELP_OPT)
                .hasArg(false)
                .desc("Show the options available, ignoring all other options used.")
                .build();

        addOption(projectOpt);
        addOption(classpathOpt);
        addOption(clientClassOpt);
        addOption(vulnerabilitiesOpt);
        addOption(budgetOpt);
        addOption(populationOpt);
        addOption(outFileOpt);
        addOption(helpOpt);
    }

    public static CLIOptions getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new CLIOptions();
        }
        return INSTANCE;
    }

}
