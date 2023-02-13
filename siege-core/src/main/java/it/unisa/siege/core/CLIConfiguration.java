package it.unisa.siege.core;

import java.nio.file.Path;

public class CLIConfiguration {
    private final Path configurationFilePath;
    private final Path projectPath;
    private final String classpathFileName;
    private final Path vulnerabilitiesFilePath;
    private final int budget;
    private final int populationSize;
    private final Path testsDirPath;
    private final Path outFilePath;
    private final Path logDirPath;
    private final boolean keepEmptyTests;

    public CLIConfiguration(Path configurationFilePath, Path projectPath, String classpathFileName, Path vulnerabilitiesFilePath, int budget, int populationSize, Path testsDirPath, Path outFilePath, Path logDirPath, boolean keepEmptyTests) {
        this.configurationFilePath = configurationFilePath;
        this.projectPath = projectPath;
        this.classpathFileName = classpathFileName;
        this.vulnerabilitiesFilePath = vulnerabilitiesFilePath;
        this.budget = budget;
        this.populationSize = populationSize;
        this.testsDirPath = testsDirPath;
        this.outFilePath = outFilePath;
        this.logDirPath = logDirPath;
        this.keepEmptyTests = keepEmptyTests;
    }

    public Path getConfigurationFilePath() {
        return configurationFilePath;
    }

    public Path getProjectPath() {
        return projectPath;
    }

    public String getClasspathFileName() {
        return classpathFileName;
    }

    public Path getVulnerabilitiesFilePath() {
        return vulnerabilitiesFilePath;
    }

    public int getBudget() {
        return budget;
    }

    public int getPopulationSize() {
        return populationSize;
    }

    public Path getTestsDirPath() {
        return testsDirPath;
    }

    public Path getOutFilePath() {
        return outFilePath;
    }

    public Path getLogDirPath() {
        return logDirPath;
    }

    public boolean isKeepEmptyTests() {
        return keepEmptyTests;
    }
}
