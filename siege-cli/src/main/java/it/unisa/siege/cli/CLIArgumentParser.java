package it.unisa.siege.cli;

import it.unisa.siege.core.CLIConfiguration;
import org.apache.commons.cli.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class CLIArgumentParser {
    public static final String HEADER = "Siege: an automated test case generator targeting any method in the classpath.\n\nOptions:";
    public static final String SYNTAX = "java -jar siege.jar";
    public static final String FOOTER = "\nPlease report any issue at https://github.com/emaiannone/siege";

    public static CLIConfiguration parse(String[] args) throws ParseException, IOException {
        // Fetch the indicated CLI options
        Options options = CLIOptions.getInstance();
        CommandLineParser cliParser = new DefaultParser();
        CommandLine commandLine = cliParser.parse(options, args);
        HelpFormatter helpFormatter = new HelpFormatter();

        if (commandLine.hasOption(CLIOptions.HELP_OPT)) {
            helpFormatter.printHelp(SYNTAX, HEADER, options, FOOTER, true);
            return null;
        }

        Path configurationFilePath;
        String configurationFile = commandLine.getOptionValue(CLIOptions.CONFIGURATION_FILE_OPT);
        if (configurationFile != null) {
            configurationFilePath = Paths.get(configurationFile);
            if (!Files.exists(configurationFilePath)) {
                configurationFilePath = null;
            }
        } else {
            configurationFilePath = null;
        }

        String project = commandLine.getOptionValue(CLIOptions.PROJECT_OPT);
        if (project == null) {
            throw new IOException("The project path was not specified.");
        }
        Path projectPath = Paths.get(project);
        if (!Files.exists(projectPath)) {
            throw new IOException("The supplied project path must point to an existing directory.");
        }

        String classpathFileName = commandLine.getOptionValue(CLIOptions.CLASSPATH_FILE_NAME_OPT);
        if (classpathFileName == null) {
            throw new IOException("The project's classpath file was not specified.");
        }

        // Get the target vulnerability(ies)
        String vulnerabilitiesFileName = commandLine.getOptionValue(CLIOptions.VULNERABILITIES_OPT);
        if (vulnerabilitiesFileName == null) {
            throw new IOException("The CSV containing the vulnerabilities was not specified.");
        }
        Path vulnerabilitiesFilePath = Paths.get(vulnerabilitiesFileName);
        if (!Files.exists(vulnerabilitiesFilePath)) {
            throw new IOException("The supplied CSV file with vulnerabilities does not exist.");
        }

        String budgetArg = commandLine.getOptionValue(CLIOptions.BUDGET_OPT);
        int budget;
        try {
            budget = Integer.parseInt(budgetArg);
        } catch (NumberFormatException e) {
            budget = 0;
        }

        String popSizeArg = commandLine.getOptionValue(CLIOptions.POP_SIZE_OPT);
        int popSize;
        try {
            popSize = Integer.parseInt(popSizeArg);
        } catch (NumberFormatException e) {
            popSize = 0;
        }

        String baseTestsDirArg = commandLine.getOptionValue(CLIOptions.TESTS_DIR_OPT);
        Path baseTestsDirPath = Paths.get(baseTestsDirArg != null ? baseTestsDirArg : CLIOptions.TESTS_DIR_DEFAULT);
        Path testsDirPath = Paths.get(baseTestsDirPath.toString(), projectPath.getFileName().toString());

        String outFileArg = commandLine.getOptionValue(CLIOptions.OUT_FILE_OPT);
        Path outFilePath = outFileArg != null ? Paths.get(outFileArg) : null;

        String logDirArg = commandLine.getOptionValue(CLIOptions.LOG_DIR_OPT);
        Path logDirPath = logDirArg != null ? Paths.get(logDirArg) : null;

        boolean keepEmptyTests = commandLine.hasOption(CLIOptions.KEEP_EMPTY_TESTS_OPT);

        return new CLIConfiguration(configurationFilePath, projectPath, classpathFileName, vulnerabilitiesFilePath, budget, popSize, testsDirPath, outFilePath, logDirPath, keepEmptyTests);
    }
}
