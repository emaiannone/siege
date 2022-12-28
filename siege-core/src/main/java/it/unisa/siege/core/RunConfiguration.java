package it.unisa.siege.core;

import org.apache.commons.lang3.tuple.Pair;
import org.evosuite.coverage.vulnerability.VulnerabilityDescription;

import java.nio.file.Path;
import java.util.List;

public class RunConfiguration {
    private final Path project;
    private final String classpath;
    private final String clientClass;
    private final List<Pair<String, VulnerabilityDescription>> targetVulnerabilities;
    private final int budget;
    private final int populationSize;
    private final Path testsDirPath;
    private final Path outFilePath;
    private final Path logDirPath;

    public RunConfiguration(Path project, String classpath, String clientClass, List<Pair<String, VulnerabilityDescription>> targetVulnerabilities, int budget, int populationSize, Path testsDirPath, Path outFilePath, Path logDirPath) {
        this.project = project;
        this.classpath = classpath;
        this.clientClass = clientClass;
        this.targetVulnerabilities = targetVulnerabilities;
        this.budget = budget;
        this.populationSize = populationSize;
        this.testsDirPath = testsDirPath;
        this.outFilePath = outFilePath;
        this.logDirPath = logDirPath;
    }

    public Path getProject() {
        return project;
    }

    public String getClasspath() {
        return classpath;
    }

    public String getClientClass() {
        return clientClass;
    }

    public List<Pair<String, VulnerabilityDescription>> getTargetVulnerabilities() {
        return targetVulnerabilities;
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
}
