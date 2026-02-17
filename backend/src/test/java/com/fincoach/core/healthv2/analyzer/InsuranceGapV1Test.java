package com.fincoach.core.healthv2.analyzer;

import com.fincoach.core.healthv2.entity.FcInsuranceProfileEntity;
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

public class InsuranceGapV1Test {

    @Test
    public void testFullInputsComputeGapsAndPremiumRatio() {
        InsuranceGapV1Builder builder = new InsuranceGapV1Builder();
        ScoreRuleSnapshot snapshot = buildSnapshot(Map.of());

        FcInsuranceProfileEntity profile = new FcInsuranceProfileEntity();
        profile.setAnnualIncome(new BigDecimal("120000"));

        Map<String, Object> coverage = new LinkedHashMap<>();
        coverage.put("hasMedical", true);
        coverage.put("accident", new BigDecimal("50000"));
        coverage.put("criticalIllness", new BigDecimal("100000"));
        coverage.put("life", new BigDecimal("500000"));
        coverage.put("annualPremiumTotal", new BigDecimal("6000"));

        InsuranceGapV1Result result = builder.build(
                profile, coverage, profile.getAnnualIncome(), null,
                BigDecimal.ZERO, new BigDecimal("6"), snapshot
        );

        @SuppressWarnings("unchecked")
        Map<String, Object> gaps = (Map<String, Object>) result.getMetrics().get("gaps");
        @SuppressWarnings("unchecked")
        Map<String, Object> accident = (Map<String, Object>) gaps.get("accident");
        assertEquals(new BigDecimal("120000.00"), accident.get("recommended"));
        assertEquals(new BigDecimal("50000"), accident.get("current"));
        assertEquals(new BigDecimal("70000.00"), accident.get("gap"));

        @SuppressWarnings("unchecked")
        Map<String, Object> premiumRatio = (Map<String, Object>) result.getMetrics().get("premiumRatio");
        assertEquals("OK", premiumRatio.get("level"));
    }

    @Test
    public void testIncomeMissing() {
        InsuranceGapV1Builder builder = new InsuranceGapV1Builder();
        ScoreRuleSnapshot snapshot = buildSnapshot(Map.of());

        InsuranceGapV1Result result = builder.build(
                null, Map.of("hasMedical", false), null, null,
                BigDecimal.ZERO, new BigDecimal("3"), snapshot
        );

        assertTrue(result.getWarnings().contains(InsuranceWarningCodes.INSURANCE_INCOME_MISSING));
        @SuppressWarnings("unchecked")
        Map<String, Object> gaps = (Map<String, Object>) result.getMetrics().get("gaps");
        @SuppressWarnings("unchecked")
        Map<String, Object> life = (Map<String, Object>) gaps.get("life");
        assertEquals("MISSING", life.get("level"));
    }

    @Test
    public void testPremiumMissing() {
        InsuranceGapV1Builder builder = new InsuranceGapV1Builder();
        ScoreRuleSnapshot snapshot = buildSnapshot(Map.of());

        FcInsuranceProfileEntity profile = new FcInsuranceProfileEntity();
        profile.setAnnualIncome(new BigDecimal("120000"));

        InsuranceGapV1Result result = builder.build(
                profile, Map.of("hasMedical", true), profile.getAnnualIncome(), null,
                BigDecimal.ZERO, new BigDecimal("6"), snapshot
        );

        assertTrue(result.getWarnings().contains(InsuranceWarningCodes.INSURANCE_PREMIUM_MISSING));
        @SuppressWarnings("unchecked")
        Map<String, Object> premiumRatio = (Map<String, Object>) result.getMetrics().get("premiumRatio");
        assertEquals("MISSING", premiumRatio.get("level"));
    }

    @Test
    public void testDependentsRaiseLifePriority() {
        InsuranceGapV1Builder builder = new InsuranceGapV1Builder();
        ScoreRuleSnapshot snapshot = buildSnapshot(Map.of());

        FcInsuranceProfileEntity profile = new FcInsuranceProfileEntity();
        profile.setAnnualIncome(new BigDecimal("100000"));
        profile.setDependentsCount(1);

        InsuranceGapV1Result result = builder.build(
                profile, Map.of("hasMedical", true), profile.getAnnualIncome(), null,
                BigDecimal.ZERO, new BigDecimal("6"), snapshot
        );

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> priority = (List<Map<String, Object>>) result.getMetrics().get("priority");
        assertEquals("MEDICAL", priority.get(0).get("type"));
        assertEquals("LIFE", priority.get(1).get("type"));
    }

    @Test
    public void testEmergencyFundLowAddsAdvice() {
        InsuranceGapV1Builder builder = new InsuranceGapV1Builder();
        ScoreRuleSnapshot snapshot = buildSnapshot(Map.of());

        FcInsuranceProfileEntity profile = new FcInsuranceProfileEntity();
        profile.setAnnualIncome(new BigDecimal("100000"));

        InsuranceGapV1Result result = builder.build(
                profile, Map.of("hasMedical", false), profile.getAnnualIncome(), null,
                BigDecimal.ZERO, new BigDecimal("2"), snapshot
        );

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> advices =
                (List<Map<String, Object>>) result.getAdvice().get("advices");
        assertTrue(containsAdviceCode(advices, "INSURANCE_EMERGENCY_LOW"));
    }

    @Test
    public void testCurrentMissing() {
        InsuranceGapV1Builder builder = new InsuranceGapV1Builder();
        ScoreRuleSnapshot snapshot = buildSnapshot(Map.of());

        FcInsuranceProfileEntity profile = new FcInsuranceProfileEntity();
        profile.setAnnualIncome(new BigDecimal("100000"));

        InsuranceGapV1Result result = builder.build(
                profile, Map.of("hasMedical", true), profile.getAnnualIncome(), null,
                BigDecimal.ZERO, new BigDecimal("6"), snapshot
        );

        assertTrue(result.getWarnings().contains(InsuranceWarningCodes.INSURANCE_CURRENT_MISSING_PREFIX + "ACCIDENT")
                || result.getWarnings().contains(InsuranceWarningCodes.INSURANCE_CURRENT_MISSING_PREFIX + "CRITICAL_ILLNESS"));
        @SuppressWarnings("unchecked")
        Map<String, Object> gaps = (Map<String, Object>) result.getMetrics().get("gaps");
        @SuppressWarnings("unchecked")
        Map<String, Object> accident = (Map<String, Object>) gaps.get("accident");
        assertEquals("MISSING", accident.get("level"));
    }

    private boolean containsAdviceCode(List<Map<String, Object>> advices, String code) {
        if (advices == null) return false;
        for (Map<String, Object> item : advices) {
            if (code.equals(item.get("code"))) return true;
        }
        return false;
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
