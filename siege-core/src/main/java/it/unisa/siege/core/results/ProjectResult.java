package it.unisa.siege.core.results;

import it.unisa.siege.core.configuration.SiegeConfiguration;

import java.util.ArrayList;
import java.util.List;

public class ProjectResult {
    private final SiegeConfiguration siegeConfiguration;
    private final List<VulnerabilityResult> vulnerabilityResults;
    private String startTime;
    private String endTime;

    public ProjectResult(SiegeConfiguration siegeConfiguration) {
        this.siegeConfiguration = siegeConfiguration;
        vulnerabilityResults = new ArrayList<>();
    }

    @Override
    public String toString() {
        return "ProjectResult{" +
                "siegeConfiguration=" + siegeConfiguration +
                ", vulnerabilityResults=" + vulnerabilityResults +
                ", startTime='" + startTime + '\'' +
                ", endTime='" + endTime + '\'' +
                '}';
    }

    public void addCVEResult(VulnerabilityResult vulnerabilityResult) {
        vulnerabilityResults.add(vulnerabilityResult);
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

}
