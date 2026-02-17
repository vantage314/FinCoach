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

public class DebtCashflowV1Test {

    @Test
    public void testComputeWithCompleteInputs() {
        DebtCashflowV1Builder builder = new DebtCashflowV1Builder();
        ScoreRuleSnapshot snapshot = buildSnapshot(Map.of());

        List<FcAssetEntity> assets = List.of(
                asset("CASH", "6000"),
                asset("STOCK", "4000")
        );
        List<FcLiabilityEntity> liabilities = List.of(liability("2000", "0.10"));

        FcCashflowEntity cashflow = new FcCashflowEntity();
        cashflow.setIncome(new BigDecimal("5000"));
        cashflow.setFixedExpense(new BigDecimal("2000"));
        cashflow.setVariableExpense(new BigDecimal("1000"));
        cashflow.setMonthlyDebtPayment(new BigDecimal("500"));

        DebtCashflowV1Result result = builder.build(
                assets, liabilities, cashflow,
                new BigDecimal("10000"), new BigDecimal("2000"), new BigDecimal("6000"),
                snapshot
        );

        @SuppressWarnings("unchecked")
        Map<String, Object> debtMetrics = (Map<String, Object>) result.toMetricsMap().get("debtMetrics");
        assertEquals(new BigDecimal("0.1000"), debtMetrics.get("dti"));

        @SuppressWarnings("unchecked")
        Map<String, Object> cashflowMetrics = (Map<String, Object>) result.toMetricsMap().get("cashflowMetrics");
        assertEquals(new BigDecimal("0.3000"), cashflowMetrics.get("surplusRate"));
        assertEquals(new BigDecimal("1.71"), ((BigDecimal) cashflowMetrics.get("emergencyFundMonths")).setScale(2));
    }

    @Test
    public void testMissingIncomeTriggersWarningAndStress() {
        DebtCashflowV1Builder builder = new DebtCashflowV1Builder();
        ScoreRuleSnapshot snapshot = buildSnapshot(Map.of());

        FcCashflowEntity cashflow = new FcCashflowEntity();
        cashflow.setFixedExpense(new BigDecimal("2000"));
        cashflow.setVariableExpense(new BigDecimal("1000"));

        DebtCashflowV1Result result = builder.build(
                List.of(asset("CASH", "1000")),
                List.of(),
                cashflow,
                new BigDecimal("1000"),
                BigDecimal.ZERO,
                new BigDecimal("1000"),
                snapshot
        );

        assertTrue(result.getWarnings().contains(DebtCashflowWarningCodes.MISSING_CASHFLOW_INCOME));
        assertNotNull(result.getStressLevel());
        assertTrue("MED".equals(result.getStressLevel()) || "HIGH".equals(result.getStressLevel()));
    }

    @Test
    public void testNegativeCashflowAddsAdvice() {
        DebtCashflowV1Builder builder = new DebtCashflowV1Builder();
        ScoreRuleSnapshot snapshot = buildSnapshot(Map.of());

        FcCashflowEntity cashflow = new FcCashflowEntity();
        cashflow.setIncome(new BigDecimal("3000"));
        cashflow.setFixedExpense(new BigDecimal("4000"));

        DebtCashflowV1Result result = builder.build(
                List.of(asset("CASH", "3000")),
                List.of(),
                cashflow,
                new BigDecimal("3000"),
                BigDecimal.ZERO,
                new BigDecimal("3000"),
                snapshot
        );

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> advices =
                (List<Map<String, Object>>) result.getAdvice().get("advices");
        assertTrue(containsAdviceCode(advices, "CASHFLOW_NEGATIVE"));
    }

    @Test
    public void testEmergencyFundEstimatedWhenNoCashType() {
        DebtCashflowV1Builder builder = new DebtCashflowV1Builder();
        ScoreRuleSnapshot snapshot = buildSnapshot(Map.of());

        FcCashflowEntity cashflow = new FcCashflowEntity();
        cashflow.setIncome(new BigDecimal("5000"));
        cashflow.setFixedExpense(new BigDecimal("2000"));

        DebtCashflowV1Result result = builder.build(
                List.of(asset("STOCK", "10000")),
                List.of(),
                cashflow,
                new BigDecimal("10000"),
                BigDecimal.ZERO,
                null,
                snapshot
        );

        assertTrue(result.getWarnings().contains(DebtCashflowWarningCodes.EMERGENCY_FUND_ESTIMATED));
    }

    @Test
    public void testHighDebtToAssetsAddsAdvice() {
        DebtCashflowV1Builder builder = new DebtCashflowV1Builder();
        ScoreRuleSnapshot snapshot = buildSnapshot(Map.of());

        FcCashflowEntity cashflow = new FcCashflowEntity();
        cashflow.setIncome(new BigDecimal("5000"));
        cashflow.setFixedExpense(new BigDecimal("2000"));

        DebtCashflowV1Result result = builder.build(
                List.of(asset("CASH", "3000"), asset("STOCK", "7000")),
                List.of(liability("7000", "0.12")),
                cashflow,
                new BigDecimal("10000"),
                new BigDecimal("7000"),
                new BigDecimal("3000"),
                snapshot
        );

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> advices =
                (List<Map<String, Object>>) result.getAdvice().get("advices");
        assertTrue(containsAdviceCode(advices, "DEBT_TO_ASSETS_HIGH"));
    }

    private boolean containsAdviceCode(List<Map<String, Object>> advices, String code) {
        if (advices == null) return false;
        for (Map<String, Object> item : advices) {
            if (code.equals(item.get("code"))) return true;
        }
        return false;
    }

    private FcAssetEntity asset(String type, String amount) {
        FcAssetEntity entity = new FcAssetEntity();
        entity.setType(type);
        entity.setAmount(new BigDecimal(amount));
        return entity;
    }

    private FcLiabilityEntity liability(String principal, String rate) {
        FcLiabilityEntity entity = new FcLiabilityEntity();
        entity.setPrincipal(new BigDecimal(principal));
        entity.setInterestRate(new BigDecimal(rate));
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
