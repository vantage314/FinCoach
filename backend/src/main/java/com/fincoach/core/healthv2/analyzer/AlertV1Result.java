package com.fincoach.core.healthv2.analyzer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AlertV1Result {
    private List<Map<String, Object>> alerts = new ArrayList<>();
    private List<String> warnings = new ArrayList<>();
    private List<Map<String, Object>> warningDetails = new ArrayList<>();
    private int openCount;
    private int criticalCount;
    private List<String> topCodes = new ArrayList<>();
    private String lastCreatedAt;

    public List<Map<String, Object>> getAlerts() { return alerts; }
    public void setAlerts(List<Map<String, Object>> alerts) { this.alerts = alerts; }
    public List<String> getWarnings() { return warnings; }
    public void setWarnings(List<String> warnings) { this.warnings = warnings; }
    public List<Map<String, Object>> getWarningDetails() { return warningDetails; }
    public void setWarningDetails(List<Map<String, Object>> warningDetails) { this.warningDetails = warningDetails; }
    public int getOpenCount() { return openCount; }
    public void setOpenCount(int openCount) { this.openCount = openCount; }
    public int getCriticalCount() { return criticalCount; }
    public void setCriticalCount(int criticalCount) { this.criticalCount = criticalCount; }
    public List<String> getTopCodes() { return topCodes; }
    public void setTopCodes(List<String> topCodes) { this.topCodes = topCodes; }
    public String getLastCreatedAt() { return lastCreatedAt; }
    public void setLastCreatedAt(String lastCreatedAt) { this.lastCreatedAt = lastCreatedAt; }

    public Map<String, Object> toMetricsMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("alerts", alerts == null ? new ArrayList<>() : alerts);
        map.put("warnings", warnings == null ? new ArrayList<>() : warnings);
        return map;
    }
}
