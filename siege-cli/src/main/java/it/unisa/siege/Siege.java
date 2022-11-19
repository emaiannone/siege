package it.unisa.siege;

import com.opencsv.exceptions.CsvValidationException;
import org.apache.commons.cli.*;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.evosuite.EvoSuite;
import org.evosuite.Properties;
import org.evosuite.coverage.vulnerability.VulnerabilityDescription;
import org.evosuite.utils.LoggingUtils;
import org.slf4j.Logger;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class Siege {
    private static final Logger logger = LoggingUtils.getEvoLogger();

    public static final String CLIENT_CLASS_OPT = "clientClass";
    public static final String VULNERABILITIES_OPT = "vulnerabilities";
    public static final String BUDGET_OPT = "budget";
    public static final String POPULATION_SIZE_OPT = "populationSize";
    public static final String EXPORT_OPT = "export";

    public static final String EXPORTS_DIR = "siege_report/";

    public static void main(String[] args) throws Exception {
        Options options = new Options();
        options.addOption(new Option(CLIENT_CLASS_OPT, true, "Client class fully-qualified name (e.g. org.foo.SomeClass) from which the exploit generation will start. If not specified, the Maven-specified output directory will be used and all client classes inside considered."));
        options.addOption(new Option(VULNERABILITIES_OPT, true, "Path to a CSV file containing the descriptions of the list of known vulnerabilities to reach."));
        options.addOption(new Option(BUDGET_OPT, true, "A non-negative number indicating the time budget expressed in seconds."));
        options.addOption(new Option(POPULATION_SIZE_OPT, true, "A positive number indicating the number of test cases in each generation."));
        options.addOption(new Option(EXPORT_OPT, false, "Flag to require the output to be written on a CSV file instead of stdout."));

        CommandLineParser parser = new DefaultParser();
        CommandLine line = null;
        try {
            line = parser.parse(options, args);
        } catch (ParseException e) {
            throw new RuntimeException("Invalid options given. Exiting.", e);
        }
        String clientClass = line.getOptionValue(CLIENT_CLASS_OPT);
        //String clientProjectRoot = line.getOptionValue(CLIENT_PROJECT_ROOT);
        //String targetClasses = line.hasOption(TARGET_CLASSES) ? line.getOptionValue(TARGET_CLASSES) : DEFAULT_TARGET_CLASSES;
        //String targetLibraries = line.hasOption(TARGET_LIBRARIES) ? line.getOptionValue(TARGET_LIBRARIES) : DEFAULT_TARGET_LIBRARIES;
        String vulnerabilities = line.getOptionValue(VULNERABILITIES_OPT);

        // Get the target vulnerability(ies)
        List<Pair<String, VulnerabilityDescription>> vulnerabilityList = new ArrayList<>();
        try {
            vulnerabilityList.addAll(SiegeIO.readAndParseCsv(vulnerabilities));
        } catch (FileNotFoundException e) {
            throw new IOException("Cannot find the CSV containing the vulnerabilities. Exiting.", e);
        } catch (CsvValidationException e) {
            throw new IOException("Cannot parse the CSV containing the vulnerabilities. Exiting.", e);
        }

        // TODO Which Meta-Heuristic would fit better?
        List<String> evosuiteCommands = new ArrayList<>(Arrays.asList(
                "-generateTests",
                "-criterion", Properties.Criterion.VULNERABILITY.name(),
                "-Dalgorithm=" + Properties.Algorithm.STEADY_STATE_GA.name(),
                "-Dinstrument_parent=false", // If this is true it seems to give problem to RMI
                "-Dinstrument_context=true",
                "-Dinstrument_method_calls=true",
                "-Dinstrument_libraries=true",
                "-Dassertions=false",
                "-Dminimize=true",
                "-Dserialize_ga=true",
                "-Dserialize_result=true",
                "-Dcoverage=false",
                "-Dprint_covered_goals=true",
                "-Dprint_missed_goals=true",
                //"-Dshow_progress=false",
                "-Dtest_dir=siege_tests"
        ));

        // Default budget is 60 (set by EvoSuite if -Dsearch_budget is not specified)
        String budget = line.getOptionValue(BUDGET_OPT);
        if (budget != null) {
            if (Integer.parseInt(budget) < 1) {
                throw new IllegalArgumentException("Time budget cannot be less than 1 second. Exiting.");
            } else {
                evosuiteCommands.add("-Dsearch_budget=" + budget);
            }
        }
        String populationSize = line.getOptionValue(POPULATION_SIZE_OPT);
        if (populationSize != null) {
            if (Integer.parseInt(populationSize) < 2) {
                throw new IllegalArgumentException("Population cannot be made of less than 2 individuals. Exiting.");
            } else {
                evosuiteCommands.add("-Dpopulation=" + populationSize);
            }
        } else {
            evosuiteCommands.add("-Dpopulation=" + 100);
        }
        boolean exportCsv = line.hasOption(EXPORT_OPT);

        String outputDirectory = getOutputDirectory();
        if (clientClass != null) {
            evosuiteCommands.add("-class");
            evosuiteCommands.add(clientClass);
        } else {
            evosuiteCommands.add("-target");
            evosuiteCommands.add(outputDirectory);
        }
        String projectCP = outputDirectory + ":" + getLibraryClasspath();
        evosuiteCommands.add("-projectCP");
        evosuiteCommands.add(projectCP);

        // TODO Accept an option for arbitrary project path instead of CWD only
        String project = System.getProperty("user.dir");
        String fullProjectPath = (new File(project)).getCanonicalPath();
        // We have to instantiate EvoSuite to prepare the logging utils, so that the setup is done once for all
        ExploitGenerator exploitGenerator = new ExploitGenerator(new EvoSuite());
        logger.info("[SIEGE] Going to generate exploits for {} CVEs through client project {}", vulnerabilityList.size(), fullProjectPath);
        List<Map<String, String>> results = null;
        try {
            results = exploitGenerator.generateExploits(vulnerabilityList, evosuiteCommands);
        } catch (Exception e) {
            // Should anything unhandled happen, exit with error
            e.printStackTrace();
            System.exit(1);
        }
        try {
            if (results != null && exportCsv) {
                String projectName = project.substring(project.lastIndexOf('/') + 1);
                SiegeIO.writeToCsv(EXPORTS_DIR, projectName, results);
            }
        } catch (IOException e) {
            logger.error("Failed to export the results. Printing on stdout instead.", e);
            logger.info(String.valueOf(results));
        } finally {
            System.exit(0);
        }
    }

    private static void callMaven(List<String> goals) throws IOException, MavenInvocationException {
        if (System.getProperty("maven.home") == null) {
            Runtime rt = Runtime.getRuntime();
            String[] commands = {"whereis", "mvn"};
            Process proc = rt.exec(commands);
            BufferedReader stdInput = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            // Read the output from the command
            String mavenHome = stdInput.readLine().split(" ")[1];
            System.setProperty("maven.home", mavenHome);
        }
        String cwd = System.getProperty("user.dir");
        File tmpfile = File.createTempFile("tmp", ".txt");
        InvocationRequest request = new DefaultInvocationRequest();
        request.setBaseDirectory(new File(cwd));
        request.setGoals(goals);
        request.setBatchMode(true);
        new DefaultInvoker().execute(request);
    }

    // https://maven.apache.org/plugins/maven-help-plugin/evaluate-mojo.html
    private static String getOutputDirectory() throws IOException, MavenInvocationException {
        File tmpfile = File.createTempFile("tmp", ".txt");
        callMaven(Arrays.asList("help:evaluate", "-Dexpression=project.build.outputDirectory", "-q", "-B", "-Doutput=" + tmpfile.getAbsolutePath()));
        String outputDir = IOUtils.toString(new FileInputStream(tmpfile), StandardCharsets.UTF_8.name());
        tmpfile.delete();
        return outputDir;
    }

    // http://maven.apache.org/plugins/maven-dependency-plugin/usage.html#dependency:build-classpath
    private static String getLibraryClasspath() throws IOException, MavenInvocationException {
        File tmpfile = File.createTempFile("tmp", ".txt");
        callMaven(Arrays.asList("dependency:build-classpath", "-q", "-B", "-Dmdep.outputFile=" + tmpfile.getAbsolutePath()));
        String libraryClasspath = IOUtils.toString(new FileInputStream(tmpfile), StandardCharsets.UTF_8.name());
        tmpfile.delete();
        return libraryClasspath;
    }

    /*
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
     */
}
