package com.fincoach.core.healthv2.analyzer.portfolio;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CorrelationMatrixResult {
    private List<String> assets = new ArrayList<>();
    private List<List<Double>> matrix = new ArrayList<>();
    private String method = "pearson";
    private int sampleSize;
    private String startDate;
    private String endDate;
    private List<String> warnings = new ArrayList<>();
    private List<Map<String, Object>> warningDetails = new ArrayList<>();

    public List<String> getAssets() { return assets; }
    public void setAssets(List<String> assets) { this.assets = assets; }
    public List<List<Double>> getMatrix() { return matrix; }
    public void setMatrix(List<List<Double>> matrix) { this.matrix = matrix; }
    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }
    public int getSampleSize() { return sampleSize; }
    public void setSampleSize(int sampleSize) { this.sampleSize = sampleSize; }
    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }
    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }
    public List<String> getWarnings() { return warnings; }
    public void setWarnings(List<String> warnings) { this.warnings = warnings; }
    public List<Map<String, Object>> getWarningDetails() { return warningDetails; }
    public void setWarningDetails(List<Map<String, Object>> warningDetails) { this.warningDetails = warningDetails; }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("assets", assets);
        map.put("matrix", matrix);
        map.put("method", method);
        map.put("sampleSize", sampleSize);
        map.put("startDate", startDate);
        map.put("endDate", endDate);
        return map;
    }
}
