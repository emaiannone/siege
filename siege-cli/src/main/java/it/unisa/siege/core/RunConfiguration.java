package it.unisa.siege.core;

import org.apache.commons.lang3.tuple.Pair;
import org.evosuite.coverage.vulnerability.VulnerabilityDescription;

import java.nio.file.Path;
import java.util.List;

public class RunConfiguration {
    private final String clientClass;
    private final List<Pair<String, VulnerabilityDescription>> targetVulnerabilities;
    private final int budget;
    private final int populationSize;
    private final Path outFilePath;

    public RunConfiguration(String clientClass, List<Pair<String, VulnerabilityDescription>> targetVulnerabilities, int budget, int populationSize, Path outFilePath) {
        this.clientClass = clientClass;
        this.targetVulnerabilities = targetVulnerabilities;
        this.budget = budget;
        this.populationSize = populationSize;
        this.outFilePath = outFilePath;
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
