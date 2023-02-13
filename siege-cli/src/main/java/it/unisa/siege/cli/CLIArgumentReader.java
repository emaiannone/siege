package it.unisa.siege.cli;

import it.unisa.siege.core.BaseConfiguration;
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

        String configurationFile = commandLine.getOptionValue(CLIOptions.CONFIGURATION_FILE_OPT);
        String project = commandLine.getOptionValue(CLIOptions.PROJECT_OPT);
        String classpathFileName = commandLine.getOptionValue(CLIOptions.CLASSPATH_FILE_NAME_OPT);
        String vulnerabilitiesFileName = commandLine.getOptionValue(CLIOptions.VULNERABILITIES_OPT);
        int budgetArg = Integer.parseInt(commandLine.getOptionValue(CLIOptions.BUDGET_OPT));
        int popSizeArg = Integer.parseInt(commandLine.getOptionValue(CLIOptions.POP_SIZE_OPT));
        String testsDirArg = commandLine.getOptionValue(CLIOptions.TESTS_DIR_OPT);
        String outDirArg = commandLine.getOptionValue(CLIOptions.OUT_DIR_OPT);
        String logDirArg = commandLine.getOptionValue(CLIOptions.LOG_DIR_OPT);
        boolean keepEmptyTests = commandLine.hasOption(CLIOptions.KEEP_EMPTY_TESTS_OPT);

        return new BaseConfiguration(configurationFile, project, classpathFileName, vulnerabilitiesFileName, budgetArg, popSizeArg, testsDirArg, outDirArg, logDirArg, keepEmptyTests);
    }
}
