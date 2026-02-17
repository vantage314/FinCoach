package com.fincoach.core.healthv2.analyzer.portfolio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RebalanceAdviceV1Builder {
    public static final double DEFAULT_THRESHOLD = 0.05;
    private static final double HIGH_CORR_THRESHOLD = 0.8;

    public RebalanceAdviceV1Result build(Map<String, Object> currentAllocation,
                                         Map<String, Double> targetAllocation,
                                         CorrelationMatrixResult corrResult) {
        return build(currentAllocation, targetAllocation, corrResult, DEFAULT_THRESHOLD);
    }

    public RebalanceAdviceV1Result build(Map<String, Object> currentAllocation,
                                         Map<String, Double> targetAllocation,
                                         CorrelationMatrixResult corrResult,
                                         double threshold) {
        RebalanceAdviceV1Result result = new RebalanceAdviceV1Result();
        result.setThreshold(round4(threshold));

        Map<String, Double> current = normalizeAllocation(currentAllocation);
        if (current.isEmpty()) {
            addWarning(result, RebalanceAdviceV1WarningCodes.REBAL_CURRENT_MISSING, "current allocation empty");
        }

        Map<String, Double> target = targetAllocation == null ? Collections.emptyMap() : targetAllocation;
        boolean targetMissing = target.isEmpty();
        if (targetMissing) {
            addWarning(result, RebalanceAdviceV1WarningCodes.REBAL_TARGET_MISSING, "target allocation missing");
            target = new LinkedHashMap<>(current);
        }

        Set<String> assets = new LinkedHashSet<>();
        assets.addAll(current.keySet());
        assets.addAll(target.keySet());

        double maxAbsDrift = 0.0;
        for (String asset : assets) {
            double currentWeight = current.getOrDefault(asset, 0.0);
            double targetWeight = target.getOrDefault(asset, 0.0);
            double drift = round4(currentWeight - targetWeight);
            maxAbsDrift = Math.max(maxAbsDrift, Math.abs(drift));

            Map<String, Object> driftItem = new LinkedHashMap<>();
            driftItem.put("asset", asset);
            driftItem.put("currentWeight", round4(currentWeight));
            driftItem.put("targetWeight", round4(targetWeight));
            driftItem.put("drift", drift);
            result.getDrifts().add(driftItem);

            Map<String, Object> action = new LinkedHashMap<>();
            action.put("asset", asset);
            if (Math.abs(drift) < threshold / 2.0) {
                action.put("action", "HOLD");
                action.put("suggestedWeightDelta", 0.0);
            } else if (drift > 0) {
                action.put("action", "SELL");
                action.put("suggestedWeightDelta", round4(-drift));
            } else if (drift < 0) {
                action.put("action", "BUY");
                action.put("suggestedWeightDelta", round4(-drift));
            } else {
                action.put("action", "HOLD");
                action.put("suggestedWeightDelta", 0.0);
            }
            result.getActions().add(action);
        }

        result.setTriggered(maxAbsDrift >= threshold && !targetMissing);
        applyCorrelationWarnings(result, corrResult);
        return result;
    }

    private Map<String, Double> normalizeAllocation(Map<String, Object> allocation) {
        Map<String, Double> result = new LinkedHashMap<>();
        if (allocation == null) {
            return result;
        }
        for (Map.Entry<String, Object> entry : allocation.entrySet()) {
            Object val = entry.getValue();
            if (val instanceof Number) {
                result.put(entry.getKey(), ((Number) val).doubleValue());
            }
        }
        return result;
    }

    private void applyCorrelationWarnings(RebalanceAdviceV1Result result, CorrelationMatrixResult corrResult) {
        List<String> overweight = new ArrayList<>();
        for (Map<String, Object> drift : result.getDrifts()) {
            Object driftVal = drift.get("drift");
            if (driftVal instanceof Number && ((Number) driftVal).doubleValue() > 0.0) {
                overweight.add(drift.get("asset").toString());
            }
        }
        if (overweight.size() < 2) {
            return;
        }

        if (corrResult == null || corrResult.getAssets() == null || corrResult.getAssets().isEmpty()
                || corrResult.getMatrix() == null || corrResult.getMatrix().isEmpty()) {
            addWarning(result, RebalanceAdviceV1WarningCodes.REBAL_CORR_UNAVAILABLE, "correlation matrix missing");
            return;
        }

        Map<String, Integer> idx = new LinkedHashMap<>();
        for (int i = 0; i < corrResult.getAssets().size(); i++) {
            idx.put(corrResult.getAssets().get(i), i);
        }

        for (int i = 0; i < overweight.size(); i++) {
            for (int j = i + 1; j < overweight.size(); j++) {
                String a = overweight.get(i);
                String b = overweight.get(j);
                Integer ia = idx.get(a);
                Integer ib = idx.get(b);
                if (ia == null || ib == null) continue;
                double corr = corrResult.getMatrix().get(ia).get(ib);
                if (corr > HIGH_CORR_THRESHOLD) {
                    addWarning(result, RebalanceAdviceV1WarningCodes.REBAL_HIGH_CORR_OVERWEIGHT + ":" + a + "," + b,
                            "corr=" + round4(corr));
                }
            }
        }
    }

    private double round4(double v) {
        return BigDecimal.valueOf(v).setScale(4, RoundingMode.HALF_UP).doubleValue();
    }

    private void addWarning(RebalanceAdviceV1Result result, String code, String detail) {
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
