package com.fincoach.core.healthv2.analyzer;

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

public class DebtOptimizerV1Test {

    @Test
    public void testAvalancheStrategyWithRates() {
        DebtOptimizerV1Builder builder = new DebtOptimizerV1Builder();
        ScoreRuleSnapshot snapshot = buildSnapshot(Map.of(
                ScoreRuleDefaults.DEBT_OPTIMIZER_STRATEGY, "AVALANCHE"
        ));

        FcLiabilityEntity highRate = liability(1L, "CREDIT_CARD", "5000", "0.20", "300");
        FcLiabilityEntity lowRate = liability(2L, "CAR_LOAN", "8000", "0.08", "400");

        DebtOptimizerV1Result result = builder.build(
                List.of(lowRate, highRate),
                new BigDecimal("2000"),
                "LOW",
                snapshot
        );

        assertEquals("AVALANCHE", result.getStrategy());
        assertEquals("CREDIT_CARD", result.getTopDebtName());
        assertEquals(2, result.getPlan().size());
        assertEquals("1", result.getPlan().get(0).get("debtId"));
    }

    @Test
    public void testAllRatesMissingFallbackToSnowball() {
        DebtOptimizerV1Builder builder = new DebtOptimizerV1Builder();
        ScoreRuleSnapshot snapshot = buildSnapshot(Map.of(
                ScoreRuleDefaults.DEBT_OPTIMIZER_STRATEGY, "AVALANCHE"
        ));

        FcLiabilityEntity a = liability(1L, "CONSUMER_LOAN", "2000", null, "200");
        FcLiabilityEntity b = liability(2L, "CAR_LOAN", "1000", null, "150");

        DebtOptimizerV1Result result = builder.build(
                List.of(a, b),
                new BigDecimal("1000"),
                "LOW",
                snapshot
        );

        assertEquals("SNOWBALL", result.getStrategy());
        assertTrue(result.getWarnings().contains(DebtOptimizerWarningCodes.DEBT_RATE_MISSING_ALL));
    }

    @Test
    public void testPartialRateUsesMixed() {
        DebtOptimizerV1Builder builder = new DebtOptimizerV1Builder();
        ScoreRuleSnapshot snapshot = buildSnapshot(Map.of());

        FcLiabilityEntity withRate = liability(1L, "CREDIT_CARD", "3000", "0.18", "200");
        FcLiabilityEntity noRate = liability(2L, "OTHER", "1000", null, "100");

        DebtOptimizerV1Result result = builder.build(
                List.of(noRate, withRate),
                new BigDecimal("800"),
                "LOW",
                snapshot
        );

        assertEquals("MIXED", result.getStrategy());
        assertTrue(result.getWarnings().contains(DebtOptimizerWarningCodes.DEBT_RATE_PARTIAL));
        assertEquals("CREDIT_CARD", result.getTopDebtName());
    }

    @Test
    public void testSurplusNotPositiveForcesPayDebtFirst() {
        DebtOptimizerV1Builder builder = new DebtOptimizerV1Builder();
        ScoreRuleSnapshot snapshot = buildSnapshot(Map.of());

        FcLiabilityEntity debt = liability(1L, "CREDIT_CARD", "3000", "0.18", "200");

        DebtOptimizerV1Result result = builder.build(
                List.of(debt),
                new BigDecimal("-100"),
                "MED",
                snapshot
        );

        assertEquals(0, result.getBudgetForExtraPayment().compareTo(BigDecimal.ZERO));
        assertTrue(result.getWarnings().contains(DebtOptimizerWarningCodes.DEBT_OPTIMIZER_SURPLUS_NOT_POSITIVE));
        assertEquals("PAY_DEBT_FIRST", result.getTradeoffHint().get("recommendation"));
    }

    @Test
    public void testTradeoffRecommendations() {
        DebtOptimizerV1Builder builder = new DebtOptimizerV1Builder();
        ScoreRuleSnapshot snapshot = buildSnapshot(Map.of(
                ScoreRuleDefaults.DEBT_OPTIMIZER_EXPECTED_RETURN, "0.06",
                ScoreRuleDefaults.DEBT_OPTIMIZER_RETURN_MARGIN, "0.01"
        ));

        FcLiabilityEntity highRate = liability(1L, "CREDIT_CARD", "3000", "0.12", "200");
        DebtOptimizerV1Result payFirst = builder.build(
                List.of(highRate),
                new BigDecimal("500"),
                "LOW",
                snapshot
        );
        assertEquals("PAY_DEBT_FIRST", payFirst.getTradeoffHint().get("recommendation"));

        FcLiabilityEntity lowRate = liability(2L, "MORTGAGE", "3000", "0.02", "200");
        DebtOptimizerV1Result investFirst = builder.build(
                List.of(lowRate),
                new BigDecimal("500"),
                "LOW",
                snapshot
        );
        assertEquals("INVEST_FIRST", investFirst.getTradeoffHint().get("recommendation"));
    }

    @Test
    public void testPayoffEstimateUnavailable() {
        DebtOptimizerV1Builder builder = new DebtOptimizerV1Builder();
        ScoreRuleSnapshot snapshot = buildSnapshot(Map.of());

        FcLiabilityEntity noPayment = liability(1L, "CREDIT_CARD", "3000", "0.18", null);
        DebtOptimizerV1Result result = builder.build(
                List.of(noPayment),
                new BigDecimal("500"),
                "LOW",
                snapshot
        );

        assertTrue(result.getWarnings().contains(DebtOptimizerWarningCodes.DEBT_PAYOFF_ESTIMATE_UNAVAILABLE));
        assertNotNull(result.getPlan());
        assertEquals(null, result.getPlan().get(0).get("estimatedMonthsToPayoff"));
    }

    private FcLiabilityEntity liability(Long id,
                                        String type,
                                        String principal,
                                        String rate,
                                        String monthlyPayment) {
        FcLiabilityEntity entity = new FcLiabilityEntity();
        entity.setId(id);
        entity.setType(type);
        entity.setPrincipal(principal == null ? null : new BigDecimal(principal));
        entity.setInterestRate(rate == null ? null : new BigDecimal(rate));
        entity.setMonthlyPayment(monthlyPayment == null ? null : new BigDecimal(monthlyPayment));
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
