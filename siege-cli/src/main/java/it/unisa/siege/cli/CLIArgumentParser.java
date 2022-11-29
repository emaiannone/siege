package it.unisa.siege.cli;

import com.opencsv.exceptions.CsvValidationException;
import it.unisa.siege.core.RunConfiguration;
import it.unisa.siege.core.SiegeIO;
import org.apache.commons.cli.*;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.evosuite.coverage.vulnerability.VulnerabilityDescription;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class CLIArgumentParser {
    public static final String HEADER = "Siege: an automated test case generator targeting any method in the classpath.\n\nOptions:";
    public static final String SYNTAX = "java -jar siege.jar";
    public static final String FOOTER = "\nPlease report any issue at https://github.com/emaiannone/siege";

    private static final Logger LOGGER = LogManager.getLogger();

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

        String clientClass = commandLine.getOptionValue(CLIOptions.CLIENT_CLASS_OPT);

        String vulnerabilities = commandLine.getOptionValue(CLIOptions.VULNERABILITIES_OPT);
        // Get the target vulnerability(ies)
        List<Pair<String, VulnerabilityDescription>> vulnerabilityList;
        try {
            vulnerabilityList = new ArrayList<>(SiegeIO.readAndParseCsv(vulnerabilities));
        } catch (FileNotFoundException e) {
            throw new IOException("Cannot find the CSV containing the vulnerabilities.", e);
        } catch (CsvValidationException e) {
            throw new IOException("Cannot parse the CSV containing the vulnerabilities.", e);
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

        String outFileArg = commandLine.getOptionValue(CLIOptions.OUT_FILE_OPT);
        Path outFilePath = outFileArg != null ? Paths.get(outFileArg) : null;
        return new RunConfiguration(clientClass, vulnerabilityList, budget, popSize, outFilePath);
    }
}
