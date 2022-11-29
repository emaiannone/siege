package it.unisa.siege.cli;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

public class CLIOptions extends Options {
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
        Option clientClassOpt = Option.builder(CLIENT_CLASS_OPT)
                .hasArg(true)
                .desc("Client class fully-qualified name (e.g. org.foo.SomeClass) from which the exploit generation will start. If not specified, the Maven-specified output directory will be used and all client classes inside considered.")
                .build();

        Option vulnerabilitiesOpt = Option.builder(VULNERABILITIES_OPT)
                .hasArg(true)
                .desc("Path to a CSV file containing the descriptions of the list of known vulnerabilities to reach.")
                .build();

        Option budgetOpt = Option.builder(BUDGET_OPT)
                .hasArg(true)
                .desc(String.format("A non-negative number indicating the time budget expressed in seconds. If not specified defaults to %s", BUDGET_DEFAUlT))
                .build();

        Option populationOpt = Option.builder(POP_SIZE_OPT)
                .hasArg(true)
                .desc(String.format("A positive number indicating the number of test cases in each generation. If not specified defaults to %s", POP_SIZE_DEFAUlT))
                .build();

        Option outFileOpt = Option.builder(OUT_FILE_OPT)
                .hasArg(true)
                .desc("Path to a CSV file where to write the results. If not specified the results are written on the standard output.")
                .build();

        Option helpOpt = Option.builder(HELP_OPT)
                .hasArg(false)
                .desc("Show the options available. Invalidates all other options if used.")
                .build();

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
