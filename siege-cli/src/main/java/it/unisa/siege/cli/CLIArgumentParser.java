package it.unisa.siege.cli;

import it.unisa.siege.core.RunConfiguration;
import it.unisa.siege.core.SiegeIO;
import org.apache.commons.cli.*;
import org.apache.commons.lang3.tuple.Pair;
import org.evosuite.coverage.reachability.ReachabilityTarget;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class CLIArgumentParser {
    public static final String HEADER = "Siege: an automated test case generator targeting any method in the classpath.\n\nOptions:";
    public static final String SYNTAX = "java -jar siege.jar";
    public static final String FOOTER = "\nPlease report any issue at https://github.com/emaiannone/siege";

    public static RunConfiguration parse(String[] args) throws ParseException, IOException {
        // Fetch the indicated CLI options
        Options options = CLIOptions.getInstance();
        CommandLineParser cliParser = new DefaultParser();
        CommandLine commandLine = cliParser.parse(options, args);
        HelpFormatter helpFormatter = new HelpFormatter();

        if (commandLine.hasOption(CLIOptions.HELP_OPT)) {
            helpFormatter.printHelp(SYNTAX, HEADER, options, FOOTER, true);
            return null;
        }

        String project = commandLine.getOptionValue(CLIOptions.PROJECT_OPT);
        if (project == null) {
            throw new IOException("The project path was not specified.");
        }
        Path projectPath = Paths.get(project);
        if (!Files.exists(projectPath)) {
            throw new IOException("The supplied project path must point to an existing directory.");
        }

        String classpathFile = commandLine.getOptionValue(CLIOptions.CLASSPATH_FILE_OPT);
        if (classpathFile == null) {
            throw new IOException("The project's classpath file was not specified.");
        }
        Path classpathFilePath = Paths.get(classpathFile);
        if (!Files.exists(classpathFilePath)) {
            throw new IOException("The supplied classpath file must point to an existing file.");
        }

        String clientClass = commandLine.getOptionValue(CLIOptions.CLIENT_CLASS_OPT);

        // Get the target vulnerability(ies)
        String vulnerabilitiesFilePath = commandLine.getOptionValue(CLIOptions.VULNERABILITIES_OPT);
        List<Pair<String, ReachabilityTarget>> vulnerabilityList;
        try {
            vulnerabilityList = new ArrayList<>(SiegeIO.readAndParseCsv(Paths.get(vulnerabilitiesFilePath)));
        } catch (IOException e) {
            throw new IOException("Cannot find or parse the CSV containing the vulnerabilities.", e);
        }

        String budgetArg = commandLine.getOptionValue(CLIOptions.BUDGET_OPT);
        int budget;
        if (budgetArg == null) {
            budget = CLIOptions.BUDGET_DEFAUlT;
        } else {
            try {
                budget = Integer.parseInt(budgetArg);
            } catch (NumberFormatException e) {
                throw new IOException("Time budget should be a parsable integer.");
            }
            if (budget < 1) {
                throw new IOException("Time budget cannot be less than 1 second.");
            }
        }

        String popSizeArg = commandLine.getOptionValue(CLIOptions.POP_SIZE_OPT);
        int popSize;
        if (popSizeArg == null) {
            popSize = CLIOptions.POP_SIZE_DEFAUlT;
        } else {
            try {
                popSize = Integer.parseInt(popSizeArg);
            } catch (NumberFormatException e) {
                throw new IOException("Population size should be a parsable integer.");
            }
            if (popSize < 2) {
                throw new IOException("Population size should not be less than 2.");
            }
        }

        String baseTestsDirArg = commandLine.getOptionValue(CLIOptions.TESTS_DIR_OPT);
        Path baseTestsDirPath = Paths.get(baseTestsDirArg != null ? baseTestsDirArg : CLIOptions.TESTS_DIR_DEFAULT);
        Path testsDirPath = Paths.get(baseTestsDirPath.toString(), projectPath.getFileName().toString());

        String outFileArg = commandLine.getOptionValue(CLIOptions.OUT_FILE_OPT);
        Path outFilePath = outFileArg != null ? Paths.get(outFileArg) : null;

        String logDirArg = commandLine.getOptionValue(CLIOptions.LOG_DIR_OPT);
        Path logDirPath = logDirArg != null ? Paths.get(logDirArg) : null;

        return new RunConfiguration(projectPath, classpathFilePath, clientClass, vulnerabilityList, budget, popSize, testsDirPath, outFilePath, logDirPath);
    }
}
