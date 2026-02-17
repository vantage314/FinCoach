package com.fincoach.core.healthv2.advice;

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

public class RebalanceEngineTest {

    @Test
    public void testRebalanceDeviationTriggersAdvice() {
        ScoreRuleSnapshot snapshot = buildSnapshot(Map.of(
                ScoreRuleDefaults.REB_THRESHOLD, "0.05"
        ));
        ScoreRuleSetRegistry scoreRegistry = Mockito.mock(ScoreRuleSetRegistry.class);
        when(scoreRegistry.get()).thenReturn(snapshot);

        RebalanceTemplateRegistry templateRegistry = Mockito.mock(RebalanceTemplateRegistry.class);
        RebalanceTemplateSnapshot templateSnapshot = new RebalanceTemplateSnapshot(
                1L, "BALANCED", 1, RebalanceTemplateSnapshot.SOURCE_DB_ACTIVE,
                Map.of("CASH", 0.15, "BOND", 0.30, "STOCK", 0.35, "ETF", 0.10, "OTHER", 0.10),
                List.of());
        when(templateRegistry.getActive()).thenReturn(templateSnapshot);

        AdviceEngineV2 engine = new AdviceEngineV2(scoreRegistry, templateRegistry);

        Map<String, Object> allocation = new LinkedHashMap<>();
        allocation.put("STOCK", new BigDecimal("0.70"));
        allocation.put("BOND", new BigDecimal("0.10"));
        allocation.put("CASH", new BigDecimal("0.05"));

        AdviceEngineResult result = engine.build(List.of(), List.of(), null, allocation, new BigDecimal("100000"));
        AdviceDTO rebalance = result.getAdvices().stream()
                .filter(a -> "REBALANCE_RECOMMENDATION".equals(a.getCode()))
                .findFirst()
                .orElseThrow();

        assertEquals("P1", rebalance.getPriority());
        Object deviations = rebalance.getEvidence().get("deviations");
        assertTrue(deviations instanceof List);
        assertTrue(((List<?>) deviations).size() > 0);
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
