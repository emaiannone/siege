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
    private final Path outFilePath;

    public RunConfiguration(Path project, String classpath, String clientClass, List<Pair<String, VulnerabilityDescription>> targetVulnerabilities, int budget, int populationSize, Path outFilePath) {
        this.project = project;
        this.classpath = classpath;
        this.clientClass = clientClass;
        this.targetVulnerabilities = targetVulnerabilities;
        this.budget = budget;
        this.populationSize = populationSize;
        this.outFilePath = outFilePath;
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

    public Path getOutFilePath() {
        return outFilePath;
    }
}
