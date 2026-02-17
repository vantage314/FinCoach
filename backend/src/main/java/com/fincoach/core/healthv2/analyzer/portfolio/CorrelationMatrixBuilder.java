package com.fincoach.core.healthv2.analyzer.portfolio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;

public class CorrelationMatrixBuilder {
    public static final int MIN_POINTS = 10;

    public CorrelationMatrixResult build(PortfolioInput input) {
        CorrelationMatrixResult result = new CorrelationMatrixResult();
        if (input == null) {
            addWarning(result, CorrelationWarningCodes.CORR_HISTORY_UNAVAILABLE, "input null");
            return result;
        }

        Map<String, NavigableMap<LocalDate, Double>> byDate = input.getReturnsByAssetDate();
        if (byDate != null && !byDate.isEmpty()) {
            return buildFromDateSeries(byDate, result);
        }

        Map<String, List<Double>> byKey = input.getReturnsByAssetKey();
        if (byKey != null && !byKey.isEmpty()) {
            return buildFromSeries(byKey, result);
        }

        addWarning(result, CorrelationWarningCodes.CORR_HISTORY_UNAVAILABLE, "returns series missing");
        return result;
    }

    private CorrelationMatrixResult buildFromDateSeries(Map<String, NavigableMap<LocalDate, Double>> byDate,
                                                        CorrelationMatrixResult result) {
        Map<String, NavigableMap<LocalDate, Double>> filtered = new LinkedHashMap<>();
        for (Map.Entry<String, NavigableMap<LocalDate, Double>> entry : byDate.entrySet()) {
            String asset = entry.getKey();
            NavigableMap<LocalDate, Double> series = entry.getValue();
            int points = series == null ? 0 : series.size();
            if (points < MIN_POINTS) {
                addWarning(result, CorrelationWarningCodes.CORR_INSUFFICIENT_POINTS + ":" + asset,
                        "points=" + points);
                continue;
            }
            filtered.put(asset, series);
        }

        if (filtered.size() < 2) {
            addWarning(result, CorrelationWarningCodes.CORR_NOT_ENOUGH_ASSETS,
                    "availableAssets=" + filtered.size());
            return result;
        }

        ReturnSeriesAligner aligner = new ReturnSeriesAligner(MIN_POINTS);
        ReturnSeriesAligner.AlignedSeriesResult aligned = aligner.align(filtered);
        if (aligned.getReturns().length < 2) {
            addWarning(result, CorrelationWarningCodes.CORR_NOT_ENOUGH_ASSETS,
                    "alignedAssets=" + aligned.getReturns().length);
            return result;
        }
        if (aligned.getEffectivePoints() < MIN_POINTS) {
            addWarning(result, CorrelationWarningCodes.CORR_INSUFFICIENT_POINTS,
                    "alignedPoints=" + aligned.getEffectivePoints());
            return result;
        }

        List<String> assets = aligned.getSymbols();
        double[][] returns = aligned.getReturns();
        result.setAssets(assets);
        result.setMatrix(buildMatrix(assets, returns));
        result.setSampleSize(aligned.getEffectivePoints());
        if (aligned.getDates() != null && !aligned.getDates().isEmpty()) {
            result.setStartDate(aligned.getDates().get(0).toString());
            result.setEndDate(aligned.getDates().get(aligned.getDates().size() - 1).toString());
        }
        return result;
    }

    private CorrelationMatrixResult buildFromSeries(Map<String, List<Double>> byKey,
                                                    CorrelationMatrixResult result) {
        Map<String, List<Double>> filtered = new LinkedHashMap<>();
        for (Map.Entry<String, List<Double>> entry : byKey.entrySet()) {
            String asset = entry.getKey();
            List<Double> series = entry.getValue();
            int points = series == null ? 0 : series.size();
            if (points < MIN_POINTS) {
                addWarning(result, CorrelationWarningCodes.CORR_INSUFFICIENT_POINTS + ":" + asset,
                        "points=" + points);
                continue;
            }
            filtered.put(asset, series);
        }

        if (filtered.size() < 2) {
            addWarning(result, CorrelationWarningCodes.CORR_NOT_ENOUGH_ASSETS,
                    "availableAssets=" + filtered.size());
            return result;
        }

        List<String> assets = new ArrayList<>(filtered.keySet());
        Collections.sort(assets);
        int sampleSize = minSeriesLength(assets, filtered);
        if (sampleSize < MIN_POINTS) {
            addWarning(result, CorrelationWarningCodes.CORR_INSUFFICIENT_POINTS,
                    "minSeries=" + sampleSize);
            return result;
        }

        result.setAssets(assets);
        result.setSampleSize(sampleSize);
        result.setMatrix(buildMatrix(assets, filtered, sampleSize));
        return result;
    }

    private List<List<Double>> buildMatrix(List<String> assets, double[][] returns) {
        int n = assets.size();
        List<List<Double>> matrix = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            List<Double> row = new ArrayList<>();
            for (int j = 0; j < n; j++) {
                double v;
                if (i == j) {
                    v = 1.0;
                } else {
                    v = pearson(returns[i], returns[j]);
                }
                row.add(round4(v));
            }
            matrix.add(row);
        }
        return matrix;
    }

    private List<List<Double>> buildMatrix(List<String> assets, Map<String, List<Double>> series, int sampleSize) {
        int n = assets.size();
        List<List<Double>> matrix = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            String a = assets.get(i);
            List<Double> row = new ArrayList<>();
            for (int j = 0; j < n; j++) {
                String b = assets.get(j);
                double v;
                if (i == j) {
                    v = 1.0;
                } else {
                    v = pearson(series.get(a), series.get(b), sampleSize);
                }
                row.add(round4(v));
            }
            matrix.add(row);
        }
        return matrix;
    }

    private double pearson(double[] x, double[] y) {
        int len = Math.min(x.length, y.length);
        if (len < 2) return 0.0;
        double sumX = 0.0;
        double sumY = 0.0;
        for (int i = 0; i < len; i++) {
            sumX += x[i];
            sumY += y[i];
        }
        double meanX = sumX / len;
        double meanY = sumY / len;

        double sumXY = 0.0;
        double sumX2 = 0.0;
        double sumY2 = 0.0;
        for (int i = 0; i < len; i++) {
            double dx = x[i] - meanX;
            double dy = y[i] - meanY;
            sumXY += dx * dy;
            sumX2 += dx * dx;
            sumY2 += dy * dy;
        }
        if (sumX2 == 0.0 || sumY2 == 0.0) return 0.0;
        double r = sumXY / Math.sqrt(sumX2 * sumY2);
        if (Double.isNaN(r) || Double.isInfinite(r)) return 0.0;
        return Math.max(-1.0, Math.min(1.0, r));
    }

    private double pearson(List<Double> x, List<Double> y, int sampleSize) {
        if (x == null || y == null) return 0.0;
        int len = Math.min(sampleSize, Math.min(x.size(), y.size()));
        if (len < 2) return 0.0;
        double sumX = 0.0;
        double sumY = 0.0;
        int count = 0;
        for (int i = 0; i < len; i++) {
            Double vx = x.get(i);
            Double vy = y.get(i);
            if (vx == null || vy == null) continue;
            sumX += vx;
            sumY += vy;
            count++;
        }
        if (count < 2) return 0.0;
        double meanX = sumX / count;
        double meanY = sumY / count;

        double sumXY = 0.0;
        double sumX2 = 0.0;
        double sumY2 = 0.0;
        for (int i = 0; i < len; i++) {
            Double vx = x.get(i);
            Double vy = y.get(i);
            if (vx == null || vy == null) continue;
            double dx = vx - meanX;
            double dy = vy - meanY;
            sumXY += dx * dy;
            sumX2 += dx * dx;
            sumY2 += dy * dy;
        }
        if (sumX2 == 0.0 || sumY2 == 0.0) return 0.0;
        double r = sumXY / Math.sqrt(sumX2 * sumY2);
        if (Double.isNaN(r) || Double.isInfinite(r)) return 0.0;
        return Math.max(-1.0, Math.min(1.0, r));
    }

    private int minSeriesLength(List<String> assets, Map<String, List<Double>> series) {
        int min = Integer.MAX_VALUE;
        for (String asset : assets) {
            List<Double> s = series.get(asset);
            if (s == null) return 0;
            min = Math.min(min, s.size());
        }
        return min == Integer.MAX_VALUE ? 0 : min;
    }

    private double round4(double v) {
        return BigDecimal.valueOf(v).setScale(4, RoundingMode.HALF_UP).doubleValue();
    }

    private void addWarning(CorrelationMatrixResult result, String code, String detail) {
        if (result == null || code == null) return;
        if (!result.getWarnings().contains(code)) {
            result.getWarnings().add(code);
        }
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("code", code);
        map.put("detail", detail == null ? "" : detail);
        result.getWarningDetails().add(map);
    }
}
