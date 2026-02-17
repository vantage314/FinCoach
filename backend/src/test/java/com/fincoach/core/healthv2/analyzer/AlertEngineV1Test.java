package com.fincoach.core.healthv2.analyzer;

import com.fincoach.core.healthv2.rules.ScoreRuleDefaults;
import com.fincoach.core.healthv2.rules.ScoreRuleParamDefinition;
import com.fincoach.core.healthv2.rules.ScoreRuleParamValue;
import com.fincoach.core.healthv2.rules.ScoreRuleSnapshot;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AlertEngineV1Test {

    @Test
    public void testRiskHighTriggersAlert() {
        AlertEngineV1 engine = new AlertEngineV1();
        AlertV1Input input = new AlertV1Input();
        input.setRiskScoreLevel("HIGH");
        input.setRiskScoreValue(85);

        AlertV1Result result = engine.build(input, buildSnapshot(Map.of()), LocalDateTime.now());

        assertTrue(containsCode(result.getAlerts(), "ALERT_RISK_HIGH"));
    }

    @Test
    public void testCashflowNegativeTriggersAlert() {
        AlertEngineV1 engine = new AlertEngineV1();
        AlertV1Input input = new AlertV1Input();
        input.setMonthlySurplus(-200.0);

        AlertV1Result result = engine.build(input, buildSnapshot(Map.of()), LocalDateTime.now());

        assertTrue(containsCode(result.getAlerts(), "ALERT_CASHFLOW_NEGATIVE"));
    }

    @Test
    public void testEmergencyFundLowTriggersAlert() {
        AlertEngineV1 engine = new AlertEngineV1();
        AlertV1Input input = new AlertV1Input();
        input.setEmergencyFundMonths(2.0);

        AlertV1Result result = engine.build(input, buildSnapshot(Map.of()), LocalDateTime.now());

        assertTrue(containsCode(result.getAlerts(), "ALERT_EMERGENCY_FUND_LOW"));
    }

    @Test
    public void testRebalanceTriggeredAlert() {
        AlertEngineV1 engine = new AlertEngineV1();
        AlertV1Input input = new AlertV1Input();
        input.setRebalanceTriggered(true);

        AlertV1Result result = engine.build(input, buildSnapshot(Map.of()), LocalDateTime.now());

        assertTrue(containsCode(result.getAlerts(), "ALERT_REBAL_TRIGGERED"));
    }

    @Test
    public void testInsuranceGapHighAlert() {
        AlertEngineV1 engine = new AlertEngineV1();
        AlertV1Input input = new AlertV1Input();
        input.setInsuranceSummaryLevel("HIGH");

        AlertV1Result result = engine.build(input, buildSnapshot(Map.of()), LocalDateTime.now());

        assertTrue(containsCode(result.getAlerts(), "ALERT_INSURANCE_GAP_HIGH"));
    }

    @Test
    public void testMultipleAlertsSortedBySeverity() {
        AlertEngineV1 engine = new AlertEngineV1();
        AlertV1Input input = new AlertV1Input();
        input.setRiskScoreLevel("HIGH");
        input.setMonthlySurplus(-100.0);
        input.setRebalanceTriggered(true);

        AlertV1Result result = engine.build(input, buildSnapshot(Map.of()), LocalDateTime.now());

        assertEquals(3, result.getOpenCount());
        assertEquals("CRITICAL", result.getAlerts().get(0).get("severity"));
    }

    private boolean containsCode(List<Map<String, Object>> alerts, String code) {
        if (alerts == null) return false;
        for (Map<String, Object> alert : alerts) {
            if (code.equals(alert.get("code"))) return true;
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
