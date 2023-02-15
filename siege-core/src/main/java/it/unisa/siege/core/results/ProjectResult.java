package it.unisa.siege.core.results;

import it.unisa.siege.core.common.Exportable;
import it.unisa.siege.core.configuration.ProjectConfiguration;

import java.util.*;
import java.util.stream.Collectors;

public class ProjectResult implements Exportable<Map<String, Object>> {
    private final ProjectConfiguration projectConfig;
    private final List<VulnerabilityResult> vulnerabilityResults;
    private Date startTime;
    private Date endTime;

    public ProjectResult(ProjectConfiguration projectConfig) {
        this.projectConfig = projectConfig;
        vulnerabilityResults = new ArrayList<>();
    }

    @Override
    public Map<String, Object> export() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("start", startTime);
        map.put("end", endTime);
        Map<String, Object> projectConfigExport = projectConfig.export();
        map.put("projectPath", projectConfigExport.remove("projectPath"));
        map.put("vulnerabilities", projectConfigExport.remove("vulnerabilities"));
        map.put("configuration", projectConfigExport);
        map.put("vulnerabilityResults", vulnerabilityResults.stream().map(VulnerabilityResult::export).collect(Collectors.toList()));
        return map;
    }

    public void addVulnerabilityResult(VulnerabilityResult vulnerabilityResult) {
        vulnerabilityResults.add(vulnerabilityResult);
    }

    public ProjectConfiguration getProjectConfig() {
        return projectConfig;
    }

    public List<VulnerabilityResult> getVulnerabilityResults() {
        return Collections.unmodifiableList(vulnerabilityResults);
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }
}
