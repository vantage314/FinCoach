package com.fincoach.core.healthv2.analyzer.portfolio;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;

public class ReturnSeriesAligner {

    public static final String WARN_INTERSECTION = "CORR_SERIES_ALIGNED_INTERSECTION";
    public static final String WARN_RELAXED = "CORR_SERIES_ALIGNED_RELAXED";
    public static final String WARN_INSUFFICIENT = "CORR_INSUFFICIENT_POINTS";
    public static final String WARN_GAP_RATIO_TOO_HIGH = "CORR_GAP_RATIO_TOO_HIGH";

    public static final int DEFAULT_MIN_POINTS = 20;

    private final int minPoints;

    public ReturnSeriesAligner() {
        this(DEFAULT_MIN_POINTS);
    }

    public ReturnSeriesAligner(int minPoints) {
        this.minPoints = minPoints;
    }

    public AlignedSeriesResult align(Map<String, NavigableMap<LocalDate, Double>> assetReturnSeries) {
        AlignedSeriesResult result = new AlignedSeriesResult();
        result.setWarnings(new ArrayList<>());

        if (assetReturnSeries == null || assetReturnSeries.size() < 2) {
            addWarning(result.getWarnings(), WARN_INSUFFICIENT);
            result.setEffectivePoints(0);
            result.setSymbols(Collections.emptyList());
            result.setDates(Collections.emptyList());
            result.setReturns(new double[0][0]);
            result.setMaxCandidatePoints(0);
            result.setGapRatio(0.0);
            result.setAlignmentMode("NONE");
            return result;
        }

        List<String> symbols = new ArrayList<>(assetReturnSeries.keySet());
        Collections.sort(symbols);
        result.setSymbols(symbols);
        result.setMaxCandidatePoints(calcMaxCandidatePoints(symbols, assetReturnSeries));

        List<LocalDate> intersectionDates = buildIntersectionDates(symbols, assetReturnSeries);
        if (intersectionDates.size() >= minPoints) {
            addWarning(result.getWarnings(), WARN_INTERSECTION);
            result.setAlignmentMode("INTERSECTION");
            fillResult(result, symbols, intersectionDates, assetReturnSeries);
            updateGapRatio(result);
            return result;
        }

        addWarning(result.getWarnings(), WARN_RELAXED);
        result.setAlignmentMode("RELAXED");
        List<LocalDate> relaxedDates = buildRelaxedDates(symbols, assetReturnSeries);
        fillResult(result, symbols, relaxedDates, assetReturnSeries);
        updateGapRatio(result);

        if (result.getEffectivePoints() < minPoints) {
            addWarning(result.getWarnings(), WARN_INSUFFICIENT);
        }
        return result;
    }

    private List<LocalDate> buildIntersectionDates(List<String> symbols, Map<String, NavigableMap<LocalDate, Double>> series) {
        Set<LocalDate> intersection = null;
        for (String symbol : symbols) {
            NavigableMap<LocalDate, Double> map = series.get(symbol);
            if (map == null || map.isEmpty()) {
                intersection = Collections.emptySet();
                break;
            }
            if (intersection == null) {
                intersection = new HashSet<>(map.keySet());
            } else {
                intersection.retainAll(map.keySet());
            }
            if (intersection.isEmpty()) {
                break;
            }
        }
        if (intersection == null) {
            return Collections.emptyList();
        }
        List<LocalDate> dates = new ArrayList<>(intersection);
        Collections.sort(dates);
        return dates;
    }

    private List<LocalDate> buildRelaxedDates(List<String> symbols, Map<String, NavigableMap<LocalDate, Double>> series) {
        Map<LocalDate, Integer> counts = new HashMap<>();
        for (String symbol : symbols) {
            NavigableMap<LocalDate, Double> map = series.get(symbol);
            if (map == null) continue;
            for (LocalDate date : map.keySet()) {
                counts.put(date, counts.getOrDefault(date, 0) + 1);
            }
        }

        List<LocalDate> orderedByFrequency = new ArrayList<>(counts.keySet());
        orderedByFrequency.sort(Comparator.comparingInt((LocalDate d) -> counts.getOrDefault(d, 0)).reversed()
                .thenComparing(Comparator.naturalOrder()));

        List<LocalDate> dates = new ArrayList<>();
        for (LocalDate date : orderedByFrequency) {
            boolean allHave = true;
            for (String symbol : symbols) {
                NavigableMap<LocalDate, Double> map = series.get(symbol);
                if (map == null || !map.containsKey(date)) {
                    allHave = false;
                    break;
                }
            }
            if (allHave) {
                dates.add(date);
            }
        }
        Collections.sort(dates);
        return dates;
    }

    private void fillResult(AlignedSeriesResult result,
                            List<String> symbols,
                            List<LocalDate> dates,
                            Map<String, NavigableMap<LocalDate, Double>> series) {
        int symbolCount = symbols.size();
        int dateCount = dates.size();
        double[][] returns = new double[symbolCount][dateCount];

        for (int i = 0; i < symbolCount; i++) {
            String symbol = symbols.get(i);
            NavigableMap<LocalDate, Double> map = series.get(symbol);
            for (int j = 0; j < dateCount; j++) {
                LocalDate date = dates.get(j);
                Double v = map != null ? map.get(date) : null;
                returns[i][j] = v != null ? v : 0.0;
            }
        }

        result.setDates(dates);
        result.setReturns(returns);
        result.setEffectivePoints(dateCount);
    }

    private int calcMaxCandidatePoints(List<String> symbols, Map<String, NavigableMap<LocalDate, Double>> series) {
        int max = 0;
        for (String symbol : symbols) {
            NavigableMap<LocalDate, Double> map = series.get(symbol);
            if (map != null) {
                max = Math.max(max, map.size());
            }
        }
        return max;
    }

    private void updateGapRatio(AlignedSeriesResult result) {
        int max = result.getMaxCandidatePoints();
        if (max <= 0) {
            result.setGapRatio(0.0);
            return;
        }
        result.setGapRatio(result.getEffectivePoints() / (double) max);
    }

    private void addWarning(List<String> warnings, String warning) {
        if (warnings == null || warning == null) return;
        if (!warnings.contains(warning)) {
            warnings.add(warning);
        }
    }

    public static class AlignedSeriesResult {
        private List<String> symbols;
        private List<LocalDate> dates;
        private double[][] returns;
        private int effectivePoints;
        private int maxCandidatePoints;
        private double gapRatio;
        private String alignmentMode;
        private List<String> warnings;

        public List<String> getSymbols() { return symbols; }
        public void setSymbols(List<String> symbols) { this.symbols = symbols; }
        public List<LocalDate> getDates() { return dates; }
        public void setDates(List<LocalDate> dates) { this.dates = dates; }
        public double[][] getReturns() { return returns; }
        public void setReturns(double[][] returns) { this.returns = returns; }
        public int getEffectivePoints() { return effectivePoints; }
        public void setEffectivePoints(int effectivePoints) { this.effectivePoints = effectivePoints; }
        public int getMaxCandidatePoints() { return maxCandidatePoints; }
        public void setMaxCandidatePoints(int maxCandidatePoints) { this.maxCandidatePoints = maxCandidatePoints; }
        public double getGapRatio() { return gapRatio; }
        public void setGapRatio(double gapRatio) { this.gapRatio = gapRatio; }
        public String getAlignmentMode() { return alignmentMode; }
        public void setAlignmentMode(String alignmentMode) { this.alignmentMode = alignmentMode; }
        public List<String> getWarnings() { return warnings; }
        public void setWarnings(List<String> warnings) { this.warnings = warnings; }
    }
}
