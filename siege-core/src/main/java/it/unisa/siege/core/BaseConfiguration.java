package it.unisa.siege.core;

public class BaseConfiguration {
    private final String configurationFile;
    private final String project;
    private final String classpathFileName;
    private final String vulnerabilitiesFileName;
    private final int budget;
    private final int popSize;
    private final String testsDir;
    private final String outDir;
    private final String logDir;
    private final boolean keepEmptyTests;

    public BaseConfiguration(String configurationFile, String project, String classpathFileName, String vulnerabilitiesFileName, int budget, int popSize, String testsDir, String outDir, String logDir, boolean keepEmptyTests) {
        this.configurationFile = configurationFile;
        this.project = project;
        this.classpathFileName = classpathFileName;
        this.vulnerabilitiesFileName = vulnerabilitiesFileName;
        this.budget = budget;
        this.popSize = popSize;
        this.testsDir = testsDir;
        this.outDir = outDir;
        this.logDir = logDir;
        this.keepEmptyTests = keepEmptyTests;
    }

    public String getConfigurationFile() {
        return configurationFile;
    }

    public String getProject() {
        return project;
    }

    public String getClasspathFileName() {
        return classpathFileName;
    }

    public String getVulnerabilitiesFileName() {
        return vulnerabilitiesFileName;
    }

    public int getBudget() {
        return budget;
    }

    public int getPopSize() {
        return popSize;
    }

    public String getTestsDir() {
        return testsDir;
    }

    public String getOutDir() {
        return outDir;
    }

    public String getLogDir() {
        return logDir;
    }

    public boolean isKeepEmptyTests() {
        return keepEmptyTests;
    }
}
