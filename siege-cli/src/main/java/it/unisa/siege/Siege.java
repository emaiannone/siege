package it.unisa.siege;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import org.apache.commons.cli.*;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.evosuite.EvoSuite;
import org.evosuite.Properties;
import org.evosuite.coverage.vulnerability.VulnerabilityDescription;
import org.evosuite.ga.metaheuristics.GeneticAlgorithm;
import org.evosuite.result.TestGenerationResult;
import org.evosuite.testcase.TestCase;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.utils.LoggingUtils;
import org.slf4j.Logger;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Siege {
    public static final String CLASS = "class";
    public static final String TARGET = "target";
    public static final String LIBRARIES = "librariesPath";
    public static final String VULNERABILITIES = "vulnerabilities";
    public static final String VULN_CLASS = "vulnClass";
    public static final String VULN_METHOD = "vulnMethod";
    public static final String VULN_LINE = "vulnLine";
    public static final String BUDGET = "budget";
    public static final String EXPORT = "export";
    public static final String DEFAULT_TARGET = "target/classes";
    public static final String DEFAULT_LIBRARIES = "target/lib";
    public static final String EXPORTS_DIR = "siege_exports/";
    public static final String STATUS_UNREACHABLE = "UNREACHABLE";
    public static final String STATUS_REACHABLE = "REACHABLE";
    public static final List<String> headers = Arrays.asList(
            "cve",
            "status",
            "clientClass",
            "budget",
            "bestFitness",
            "iterations"
    );
    private static final Logger logger = LoggingUtils.getEvoLogger();

    public static void main(String[] args) throws IOException {
        Options options = new Options();
        options.addOption(new Option(CLASS, true, "Client class where an exploit will start from. A fully qualifying needs to be provided, e.g. org.foo.SomeClass"));
        options.addOption(new Option(TARGET, true, "Client project classpath. A directory containing all compiled Java classes needs to be provided, e.g. target/classes"));
        options.addOption(new Option(LIBRARIES, true, "Libraries root directory. A directory all the JARs required by client project needs to be provided, e.g. target/lib"));
        options.addOption(new Option(VULNERABILITIES, true, "CSV File containing descriptions of a set of known vulnerabilities. A CSV file needs to be provided."));
        options.addOption(new Option(VULN_CLASS, true, "Vulnerable class to be targeted by an exploit. A fully qualifying needs to be provided, e.g. org.foo.SomeClass"));
        options.addOption(new Option(VULN_METHOD, true, "Vulnerable method to be targeted by an exploit. A name and descriptor needs to be provided, e.g. someMethod([B[B)Z"));
        options.addOption(new Option(VULN_LINE, true, "Vulnerable line to be targeted by an exploit. A non negative number needs to be provided, e.g. 12."));
        options.addOption(new Option(BUDGET, true, "Allotted search budget. A non negative number needs to be provided, e.g. 50."));
        options.addOption(new Option(EXPORT, false, "Generate a .csv export of the results in the CWD."));

        String clientClass = null;
        String target = null;
        String libraries = null;
        String vulnerabilities = null;
        String vulnClass = null;
        String vulnMethod = null;
        String vulnLine = null;
        String budget = null;
        boolean export = false;
        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine line = parser.parse(options, args);
            clientClass = line.getOptionValue(CLASS);
            target = line.hasOption(TARGET) ? line.getOptionValue(TARGET) : DEFAULT_TARGET;
            libraries = line.hasOption(LIBRARIES) ? line.getOptionValue(LIBRARIES) : DEFAULT_LIBRARIES;
            vulnerabilities = line.getOptionValue(VULNERABILITIES);
            vulnClass = line.getOptionValue(VULN_CLASS);
            vulnMethod = line.getOptionValue(VULN_METHOD);
            vulnLine = line.getOptionValue(VULN_LINE);
            budget = line.getOptionValue(BUDGET);
            export = line.hasOption(EXPORT);
        } catch (ParseException e) {
            logger.error("Invalid options. Exiting.");
            System.exit(1);
        }

        // Get target vulnerability(ies)
        List<Pair<String, VulnerabilityDescription>> vulnerabilityList = new ArrayList<>();
        if (vulnerabilities != null) {
            try {
                vulnerabilityList.addAll(readAndParseCsv(vulnerabilities));
            } catch (IOException e) {
                logger.error("Vulnerabilities file not found. Exiting.");
                System.exit(1);
            }
        } else {
            if (vulnClass == null && vulnMethod == null && vulnLine == null) {
                logger.error("Either vulnerabilities or vulnClass, vulnMethod and vulnLine needs to be specified.");
                System.exit(1);
            }
            if (vulnClass == null || vulnMethod == null || vulnLine == null) {
                logger.error("All vulnClass, vulnMethod and vulnLine needs to be specified togeher.");
                System.exit(1);
            }
            vulnerabilityList.add(new ImmutablePair<>(vulnClass, new VulnerabilityDescription(vulnClass, vulnMethod, Integer.parseInt(vulnLine))));
        }

        // TODO Bound population?
        // TODO Is there a way not to stop the generation when all the TC have the same fitness? Which MH is the best?
        List<String> baseCommands = new ArrayList<>(Arrays.asList(
                "-generateTests",
                "-criterion", Properties.Criterion.VULNERABILITY.name(),
                "-Dstrategy=" + Properties.Strategy.ONEBRANCH.name(),
                "-Dinstrument_context=true",
                "-Dinstrument_method_calls=true",
                "-Dinstrument_libraries=true",
                "-Dassertions=false",
                "-Dminimize=true",
                "-Dpopulation=" + 100,
                "-Djunit_suffix=SiegeTest"
        ));
        if (clientClass != null) {
            baseCommands.add("-class");
            baseCommands.add(clientClass);
        } else {
            baseCommands.add("-target");
            baseCommands.add(target);
        }
        baseCommands.add("-projectCP");
        baseCommands.add(buildProjectClasspath(target, libraries));
        // Default budget is 60 (set by EvoSuite if -Dsearch_budget is not specified)
        if (budget != null) {
            baseCommands.add("-Dsearch_budget=" + budget);
        }
        String project = System.getProperty("user.dir");
        String canonicalProjectPath = (new File(project)).getCanonicalPath();
        generateExploits(canonicalProjectPath, vulnerabilityList, baseCommands, export);
        System.exit(0);
    }

    private static void generateExploits(String project, List<Pair<String, VulnerabilityDescription>> vulnerabilityList, List<String> baseCommands, boolean export) {
        EvoSuite evoSuite = new EvoSuite();
        List<List<String>> exportResults = new ArrayList<>();
        logger.info("* Generating Exploits through {}\n", project);
        for (Pair<String, VulnerabilityDescription> vulnerability : vulnerabilityList) {
            logger.info("* Target Vulnerability: {}", vulnerability.getLeft());
            List<String> evoSuiteCommands = new ArrayList<>(baseCommands);
            evoSuiteCommands.add("-DvulnClass=" + vulnerability.getRight().getVulnerableClass());
            evoSuiteCommands.add("-DvulnMethod=" + vulnerability.getRight().getVulnerableMethod());
            evoSuiteCommands.add("-DvulnLine=" + vulnerability.getRight().getVulnerableLine());
            try {
                List<List<TestGenerationResult>> fullResults = (List<List<TestGenerationResult>>) evoSuite.parseCommandLine(evoSuiteCommands.toArray(new String[0]));
                String status;
                logger.info("\n* Results for {}", vulnerability.getLeft());
                if (fullResults.size() == 0) {
                    status = STATUS_UNREACHABLE;
                    logger.info("* Status: {}", status);
                    if (export) {
                        exportResults.add(Arrays.asList(
                                vulnerability.getLeft(), status, null, String.valueOf(Properties.SEARCH_BUDGET), null, null
                        ));
                    }
                } else {
                    for (List<TestGenerationResult> testResults : fullResults) {
                        for (TestGenerationResult clientClassResult : testResults) {
                            status = String.valueOf(clientClassResult.getTestGenerationStatus());
                            String clientClass = clientClassResult.getClassUnderTest();
                            GeneticAlgorithm<?> ga = clientClassResult.getGeneticAlgorithm();
                            TestChromosome best = (TestChromosome) ga.getBestIndividual();
                            int gaIterations = ga.getAge() + 1;

                            // Oddly, spent budget cannot be taken easily since EvoSuite does not keep track of the stopping conditions in ga object
                            if (export) {
                                exportResults.add(Arrays.asList(
                                        vulnerability.getLeft(), status, clientClass, String.valueOf(Properties.SEARCH_BUDGET), String.valueOf(best.getFitness()), String.valueOf(gaIterations)
                                ));
                            }
                            logger.info("* Results of client class {}", clientClassResult.getClassUnderTest());
                            logger.info("* Status: {}", status);
                            logger.info("* GA terminated within {} seconds. Iteration {}, Population Size {}", Properties.SEARCH_BUDGET, gaIterations, ga.getPopulationSize());
                            logger.info("* Best Individual ID {} (Iteration {}, Evals {}), scored {}:", best.getTestCase().getID(), best.getAge(), best.getNumberOfEvaluations(), best.getFitness());
                            logger.info(best.getTestCase().toCode());
                            TestCase minimizedTC = clientClassResult.getTestCase("test0");
                            if (minimizedTC != null) {
                                logger.info("* Best Individual post Minimization:");
                                logger.info(minimizedTC.toCode());
                            } else {
                                logger.info("* Best Individual could not be minimized: not all goals were covered.");
                            }
                        }
                    }
                }
            } catch (Exception e) {
                // Print and go to next iteration
                logger.error("Error while generating exploits for {}: {}", vulnerability.getLeft(), e);
            }
            logger.info("");
        }
        if (export) {
            String projectName = project.substring(project.lastIndexOf('/') + 1);
            writeToCsv(projectName, exportResults);
        }
    }

    private static String buildProjectClasspath(String target, String librariesPath) {
        StringBuilder jarsPaths = new StringBuilder();
        File jarsDir = new File(librariesPath);
        if (jarsDir.isDirectory()) {
            for (File file : jarsDir.listFiles()) {
                if (file.isFile() && file.getName().contains(".jar")) {
                    jarsPaths.append(":");
                    jarsPaths.append(file.getAbsolutePath());
                }
            }
        }
        return target + jarsPaths;
    }

    private static List<Pair<String, VulnerabilityDescription>> readAndParseCsv(String file) throws IOException {
        List<Pair<String, VulnerabilityDescription>> vulnerabilityList = new ArrayList<>();
        try (CSVReader reader = new CSVReaderBuilder(new FileReader(file)).withSkipLines(1).build()) {
            String[] values;
            while ((values = reader.readNext()) != null) {
                vulnerabilityList.add(new ImmutablePair<>(values[0], new VulnerabilityDescription(values[2], values[3], Integer.parseInt(values[4]))));
            }
        }
        return vulnerabilityList;
    }

    private static void writeToCsv(String filename, List<List<String>> content) {
        File exportDir = new File(EXPORTS_DIR);
        if (!exportDir.exists()) {
            exportDir.mkdir();
        }
        String fullFilename = EXPORTS_DIR + filename + ".csv";
        File exportFile = new File(fullFilename);
        // If the file does exists yet, put the headers before
        if (!exportFile.exists()) {
            content.add(0, headers);
        }
        try (PrintWriter csvWriter = new PrintWriter(new FileOutputStream(exportFile, true))) {
            for (List<String> line : content) {
                String csvLine = String.join(",", line);
                csvWriter.println(csvLine);
            }
        } catch (FileNotFoundException e) {
            logger.error("Cannot write to " + fullFilename + ": quit exporting.");
        }
    }
}
