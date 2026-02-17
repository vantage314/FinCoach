package com.fincoach.core.healthv2.analyzer.portfolio;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RebalanceAdviceV1Result {
    private double threshold;
    private boolean triggered;
    private List<Map<String, Object>> drifts = new ArrayList<>();
    private List<Map<String, Object>> actions = new ArrayList<>();
    private List<String> warnings = new ArrayList<>();
    private List<Map<String, Object>> warningDetails = new ArrayList<>();

    public double getThreshold() { return threshold; }
    public void setThreshold(double threshold) { this.threshold = threshold; }
    public boolean isTriggered() { return triggered; }
    public void setTriggered(boolean triggered) { this.triggered = triggered; }
    public List<Map<String, Object>> getDrifts() { return drifts; }
    public void setDrifts(List<Map<String, Object>> drifts) { this.drifts = drifts; }
    public List<Map<String, Object>> getActions() { return actions; }
    public void setActions(List<Map<String, Object>> actions) { this.actions = actions; }
    public List<String> getWarnings() { return warnings; }
    public void setWarnings(List<String> warnings) { this.warnings = warnings; }
    public List<Map<String, Object>> getWarningDetails() { return warningDetails; }
    public void setWarningDetails(List<Map<String, Object>> warningDetails) { this.warningDetails = warningDetails; }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("threshold", threshold);
        map.put("triggered", triggered);
        map.put("drifts", drifts);
        map.put("actions", actions);
        map.put("warnings", warnings);
        return map;
    }
}
