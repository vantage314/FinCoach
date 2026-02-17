package com.fincoach.core.healthv2.analyzer;

import com.fincoach.core.healthv2.entity.FcAssetEntity;
import com.fincoach.core.healthv2.entity.FcCashflowEntity;
import com.fincoach.core.healthv2.entity.FcLiabilityEntity;
import com.fincoach.core.healthv2.rules.ScoreRuleDefaults;
import com.fincoach.core.healthv2.rules.ScoreRuleParamDefinition;
import com.fincoach.core.healthv2.rules.ScoreRuleParamValue;
import com.fincoach.core.healthv2.rules.ScoreRuleSnapshot;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ScoringV1Test {

    @Test
    public void testScoresWithCompleteMetrics() {
        ScoreEngine engine = new ScoreEngine();
        ScoreRuleSnapshot snapshot = buildSnapshot(Map.of());

        List<FcAssetEntity> assets = List.of(
                asset("CASH", "10000"),
                asset("STOCK", "70000"),
                asset("BOND", "20000")
        );
        List<FcLiabilityEntity> liabilities = List.of();

        FcCashflowEntity cashflow = new FcCashflowEntity();
        cashflow.setIncome(new BigDecimal("20000"));
        cashflow.setFixedExpense(new BigDecimal("8000"));
        cashflow.setVariableExpense(new BigDecimal("2000"));
        cashflow.setMonthlyDebtPayment(new BigDecimal("1000"));

        Map<String, Object> performance = new LinkedHashMap<>();
        performance.put("sharpe", 1.2);
        performance.put("maxDrawdown", 0.1);
        performance.put("volatility", 0.1);

        Map<String, Object> allocation = new LinkedHashMap<>();
        allocation.put("STOCK", new BigDecimal("0.7"));
        allocation.put("BOND", new BigDecimal("0.2"));
        allocation.put("CASH", new BigDecimal("0.1"));

        Map<String, Object> concentration = new LinkedHashMap<>();
        concentration.put("topType", "STOCK");
        concentration.put("topRatio", new BigDecimal("0.3"));

        Map<String, Object> behaviorStats = new LinkedHashMap<>();
        behaviorStats.put("eventCount30d", 5);

        Map<String, Object> result = engine.compute(
                assets, liabilities, cashflow, List.of(), null,
                performance, allocation, concentration,
                new BigDecimal("6"), new BigDecimal("0.2"),
                new BigDecimal("100000"), new BigDecimal("10000"),
                behaviorStats, snapshot
        );

        @SuppressWarnings("unchecked")
        Map<String, Object> scores = (Map<String, Object>) result.get("scores");
        assertNotNull(scores);
        @SuppressWarnings("unchecked")
        Map<String, Object> riskScore = (Map<String, Object>) scores.get("riskScore");
        assertNotNull(riskScore);
        assertEquals("LOW", riskScore.get("level"));
        assertTrue(((Number) riskScore.get("value")).intValue() >= 0);
        assertTrue(((Number) riskScore.get("value")).intValue() <= 100);
    }

    @Test
    public void testMissingSharpeAndMddWarnings() {
        ScoreEngine engine = new ScoreEngine();
        ScoreRuleSnapshot snapshot = buildSnapshot(Map.of());

        Map<String, Object> performance = new LinkedHashMap<>();
        Map<String, Object> allocation = new LinkedHashMap<>();
        allocation.put("STOCK", new BigDecimal("1.0"));
        Map<String, Object> concentration = new LinkedHashMap<>();
        concentration.put("topType", "STOCK");
        concentration.put("topRatio", new BigDecimal("1.0"));

        Map<String, Object> result = engine.compute(
                List.of(asset("STOCK", "10000")),
                List.of(),
                null,
                List.of(),
                null,
                performance,
                allocation,
                concentration,
                null,
                null,
                new BigDecimal("10000"),
                BigDecimal.ZERO,
                Map.of(),
                snapshot
        );

        @SuppressWarnings("unchecked")
        Map<String, Object> scores = (Map<String, Object>) result.get("scores");
        @SuppressWarnings("unchecked")
        Map<String, Object> riskScore = (Map<String, Object>) scores.get("riskScore");
        @SuppressWarnings("unchecked")
        List<String> warnings = (List<String>) riskScore.get("warnings");
        assertTrue(warnings.contains(ScoreWarningCodes.SCORE_MISSING_SHARPE));
        assertTrue(warnings.contains(ScoreWarningCodes.SCORE_MISSING_MDD));
    }

    @Test
    public void testConcentrationRaisesRiskScore() {
        ScoreEngine engine = new ScoreEngine();
        ScoreRuleSnapshot snapshot = buildSnapshot(Map.of());

        Map<String, Object> performance = new LinkedHashMap<>();
        performance.put("sharpe", 0.5);
        performance.put("maxDrawdown", 0.2);
        performance.put("volatility", 0.2);

        Map<String, Object> allocation = new LinkedHashMap<>();
        allocation.put("STOCK", new BigDecimal("1.0"));

        Map<String, Object> concentrationHigh = new LinkedHashMap<>();
        concentrationHigh.put("topType", "STOCK");
        concentrationHigh.put("topRatio", new BigDecimal("0.9"));

        Map<String, Object> concentrationLow = new LinkedHashMap<>();
        concentrationLow.put("topType", "STOCK");
        concentrationLow.put("topRatio", new BigDecimal("0.2"));

        int riskHigh = (Integer) engine.compute(
                List.of(asset("STOCK", "10000")),
                List.of(),
                null,
                List.of(),
                null,
                performance,
                allocation,
                concentrationHigh,
                null,
                null,
                new BigDecimal("10000"),
                BigDecimal.ZERO,
                Map.of("eventCount30d", 5),
                snapshot
        ).get("riskScore");

        int riskLow = (Integer) engine.compute(
                List.of(asset("STOCK", "10000")),
                List.of(),
                null,
                List.of(),
                null,
                performance,
                allocation,
                concentrationLow,
                null,
                null,
                new BigDecimal("10000"),
                BigDecimal.ZERO,
                Map.of("eventCount30d", 5),
                snapshot
        ).get("riskScore");

        assertTrue(riskHigh > riskLow);
    }

    @Test
    public void testBehaviorDefaultWhenDataMissing() {
        ScoreEngine engine = new ScoreEngine();
        ScoreRuleSnapshot snapshot = buildSnapshot(Map.of());

        Map<String, Object> result = engine.compute(
                List.of(asset("CASH", "5000")),
                List.of(),
                null,
                List.of(),
                null,
                Map.of(),
                Map.of(),
                Map.of(),
                null,
                null,
                new BigDecimal("5000"),
                BigDecimal.ZERO,
                null,
                snapshot
        );

        @SuppressWarnings("unchecked")
        Map<String, Object> scores = (Map<String, Object>) result.get("scores");
        @SuppressWarnings("unchecked")
        Map<String, Object> behaviorScore = (Map<String, Object>) scores.get("behaviorScore");
        assertEquals(60, ((Number) behaviorScore.get("value")).intValue());
        @SuppressWarnings("unchecked")
        List<String> warnings = (List<String>) behaviorScore.get("warnings");
        assertTrue(warnings.contains(ScoreWarningCodes.SCORE_BEHAVIOR_DATA_UNAVAILABLE));
    }

    private FcAssetEntity asset(String type, String amount) {
        FcAssetEntity entity = new FcAssetEntity();
        entity.setType(type);
        entity.setAmount(new BigDecimal(amount));
        return entity;
    }

    private ScoreRuleSnapshot buildSnapshot(Map<String, String> overrides) {
        Map<String, ScoreRuleParamValue> params = new LinkedHashMap<>();
        for (ScoreRuleParamDefinition def : ScoreRuleDefaults.defaultParams().values()) {
            String value = overrides.getOrDefault(def.getKey(), def.getDefaultValue());
            com.fincoach.core.healthv2.entity.FcScoreRuleParamEntity entity =
                    new com.fincoach.core.healthv2.entity.FcScoreRuleParamEntity();
            entity.setParamKey(def.getKey());
            entity.setParamValue(value);
            entity.setValueType(def.getValueType().name());
            ScoreRuleParamValue parsed = ScoreRuleParamValue.fromEntity(entity, def, "TEST", new String[1]);
            params.put(def.getKey(), parsed);
        }
        return new ScoreRuleSnapshot(null,
                ScoreRuleDefaults.DEFAULT_CODE,
                ScoreRuleDefaults.DEFAULT_VERSION,
                ScoreRuleSnapshot.SOURCE_FALLBACK_DEFAULT,
                params,
                List.of(),
                List.of());
    }
}
