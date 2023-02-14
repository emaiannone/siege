package it.unisa.siege.core.results;

import it.unisa.siege.core.configuration.ProjectConfiguration;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ProjectResult {
    private final ProjectConfiguration projectConfig;
    private final List<VulnerabilityResult> vulnerabilityResults;
    private Date startTime;
    private Date endTime;

    public ProjectResult(ProjectConfiguration projectConfig) {
        this.projectConfig = projectConfig;
        vulnerabilityResults = new ArrayList<>();
    }

    @Override
    public String toString() {
        return "ProjectResult{" +
                "projectConfig=" + projectConfig +
                ", vulnerabilityResults=" + vulnerabilityResults +
                ", startTime='" + startTime + '\'' +
                ", endTime='" + endTime + '\'' +
                '}';
    }

    public void addVulnerabilityResult(VulnerabilityResult vulnerabilityResult) {
        vulnerabilityResults.add(vulnerabilityResult);
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }
}
