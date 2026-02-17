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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class ScoreRuleScoringIntegrationTest {

    @Test
    public void testRuleWeightChangesAffectRiskScore() {
        ScoreEngine engine = new ScoreEngine();

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

        Map<String, Object> performance = new HashMap<>();
        performance.put("sharpe", 0.3);
        performance.put("maxDrawdown", 0.35);
        performance.put("volatility", 0.25);

        Map<String, Object> allocation = new LinkedHashMap<>();
        allocation.put("STOCK", new BigDecimal("0.7"));
        allocation.put("BOND", new BigDecimal("0.2"));
        allocation.put("CASH", new BigDecimal("0.1"));

        Map<String, Object> concentration = new LinkedHashMap<>();
        concentration.put("topType", "STOCK");
        concentration.put("topRatio", new BigDecimal("0.7"));

        BigDecimal emergencyMonths = new BigDecimal("2");
        BigDecimal dti = new BigDecimal("0.5");
        BigDecimal totalAssets = new BigDecimal("100000");
        BigDecimal totalDebt = new BigDecimal("40000");
        Map<String, Object> behaviorStats = new HashMap<>();
        behaviorStats.put("eventCount30d", 5);

        ScoreRuleSnapshot defaultSnapshot = buildSnapshot(Map.of());
        ScoreRuleSnapshot heavyMddSnapshot = buildSnapshot(Map.of(
                ScoreRuleDefaults.W_MDD, "0.80",
                ScoreRuleDefaults.W_SHARPE, "0.10",
                ScoreRuleDefaults.W_VOL, "0.05",
                ScoreRuleDefaults.W_DIVERSIFICATION, "0.05"
        ));

        int riskDefault = (Integer) engine.compute(
                assets, liabilities, cashflow, List.of(), null,
                performance, allocation, concentration,
                emergencyMonths, dti, totalAssets, totalDebt, behaviorStats, defaultSnapshot
        ).get("riskScore");

        int riskHeavyMdd = (Integer) engine.compute(
                assets, liabilities, cashflow, List.of(), null,
                performance, allocation, concentration,
                emergencyMonths, dti, totalAssets, totalDebt, behaviorStats, heavyMddSnapshot
        ).get("riskScore");

        assertNotEquals(riskDefault, riskHeavyMdd);
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
