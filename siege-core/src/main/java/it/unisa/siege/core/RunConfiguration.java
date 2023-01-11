package it.unisa.siege.core;

import org.apache.commons.lang3.tuple.Pair;
import org.evosuite.coverage.reachability.ReachabilityTarget;

import java.nio.file.Path;
import java.util.List;

public class RunConfiguration {
    private final Path projectPath;
    private final String classpathFileName;
    private final List<Pair<String, ReachabilityTarget>> targetVulnerabilities;
    private final int budget;
    private final int populationSize;
    private final Path testsDirPath;
    private final Path outFilePath;
    private final Path logDirPath;

    public RunConfiguration(Path projectPath, String classpathFileName, List<Pair<String, ReachabilityTarget>> targetVulnerabilities, int budget, int populationSize, Path testsDirPath, Path outFilePath, Path logDirPath) {
        this.projectPath = projectPath;
        this.classpathFileName = classpathFileName;
        this.targetVulnerabilities = targetVulnerabilities;
        this.budget = budget;
        this.populationSize = populationSize;
        this.testsDirPath = testsDirPath;
        this.outFilePath = outFilePath;
        this.logDirPath = logDirPath;
    }

    public Path getProjectPath() {
        return projectPath;
    }

    public String getClasspathFileName() {
        return classpathFileName;
    }

    public List<Pair<String, ReachabilityTarget>> getTargetVulnerabilities() {
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
