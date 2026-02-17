package com.fincoach.core.healthv2.advice;

import com.fincoach.core.healthv2.entity.FcAssetEntity;
import com.fincoach.core.healthv2.entity.FcCashflowEntity;
import com.fincoach.core.healthv2.rebalance.RebalanceTemplateSnapshot;
import com.fincoach.core.healthv2.rebalance.RebalanceTemplateRegistry;
import com.fincoach.core.healthv2.rules.ScoreRuleDefaults;
import com.fincoach.core.healthv2.rules.ScoreRuleParamDefinition;
import com.fincoach.core.healthv2.rules.ScoreRuleParamValue;
import com.fincoach.core.healthv2.rules.ScoreRuleSetRegistry;
import com.fincoach.core.healthv2.rules.ScoreRuleSnapshot;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

public class AdviceEngineCashflowDebtTest {

    @Test
    public void testCashflowDebtAdvicesTriggered() {
        ScoreRuleSnapshot snapshot = buildSnapshot(Map.of(
                ScoreRuleDefaults.EMERGENCY_MONTHS_MIN, "6",
                ScoreRuleDefaults.DEBT_PAYMENT_RATIO_MAX, "0.35",
                ScoreRuleDefaults.SURPLUS_RATE_MIN, "0.10",
                ScoreRuleDefaults.REB_THRESHOLD, "0.05"
        ));

        ScoreRuleSetRegistry scoreRegistry = Mockito.mock(ScoreRuleSetRegistry.class);
        when(scoreRegistry.get()).thenReturn(snapshot);

        RebalanceTemplateRegistry templateRegistry = Mockito.mock(RebalanceTemplateRegistry.class);
        RebalanceTemplateSnapshot templateSnapshot = new RebalanceTemplateSnapshot(
                1L, "BALANCED", 1, RebalanceTemplateSnapshot.SOURCE_DB_ACTIVE,
                Map.of("CASH", 0.15, "BOND", 0.30, "STOCK", 0.35), List.of());
        when(templateRegistry.getActive()).thenReturn(templateSnapshot);

        AdviceEngineV2 engine = new AdviceEngineV2(scoreRegistry, templateRegistry);

        FcCashflowEntity cashflow = new FcCashflowEntity();
        cashflow.setIncome(new BigDecimal("10000"));
        cashflow.setFixedExpense(new BigDecimal("4000"));
        cashflow.setVariableExpense(new BigDecimal("2000"));
        cashflow.setMonthlyDebtPayment(new BigDecimal("3600"));

        List<FcAssetEntity> assets = List.of(asset("CASH", "10000"));
        Map<String, Object> allocation = Map.of("STOCK", new BigDecimal("0.7"));

        AdviceEngineResult result = engine.build(assets, List.of(), cashflow, allocation, new BigDecimal("100000"));

        Map<String, AdviceDTO> byCode = indexByCode(result);
        assertEquals("P0", byCode.get("EMERGENCY_FUND_LOW").getPriority());
        assertEquals("P0", byCode.get("DEBT_PAYMENT_RATIO_HIGH").getPriority());
        assertEquals("P1", byCode.get("SURPLUS_RATE_LOW").getPriority());
    }

    private AdviceDTO getAdvice(AdviceEngineResult result, String code) {
        return result.getAdvices().stream().filter(a -> code.equals(a.getCode())).findFirst().orElse(null);
    }

    private Map<String, AdviceDTO> indexByCode(AdviceEngineResult result) {
        Map<String, AdviceDTO> map = new LinkedHashMap<>();
        for (AdviceDTO advice : result.getAdvices()) {
            map.put(advice.getCode(), advice);
        }
        return map;
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
