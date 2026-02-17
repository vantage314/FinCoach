package com.fincoach.core.healthv2.analyzer.portfolio;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RebalanceAdviceV1Test {

    @Test
    public void testDriftTriggeredAndActions() {
        Map<String, Object> current = new LinkedHashMap<>();
        current.put("STOCK", 0.6);
        current.put("BOND", 0.4);

        Map<String, Double> target = new LinkedHashMap<>();
        target.put("STOCK", 0.4);
        target.put("BOND", 0.6);

        RebalanceAdviceV1Result result = new RebalanceAdviceV1Builder()
                .build(current, target, null, 0.05);

        assertTrue(result.isTriggered());
        Map<String, Map<String, Object>> actions = indexByAsset(result.getActions());
        assertEquals("SELL", actions.get("STOCK").get("action"));
        assertEquals(-0.2, actions.get("STOCK").get("suggestedWeightDelta"));
        assertEquals("BUY", actions.get("BOND").get("action"));
        assertEquals(0.2, actions.get("BOND").get("suggestedWeightDelta"));
    }

    @Test
    public void testTargetMissingFallback() {
        Map<String, Object> current = new LinkedHashMap<>();
        current.put("CASH", 1.0);

        RebalanceAdviceV1Result result = new RebalanceAdviceV1Builder()
                .build(current, null, null, 0.05);

        assertTrue(result.getWarnings().contains(RebalanceAdviceV1WarningCodes.REBAL_TARGET_MISSING));
        assertEquals(false, result.isTriggered());
    }

    @Test
    public void testHighCorrelationOverweightWarning() {
        Map<String, Object> current = new LinkedHashMap<>();
        current.put("A", 0.6);
        current.put("B", 0.4);

        Map<String, Double> target = new LinkedHashMap<>();
        target.put("A", 0.2);
        target.put("B", 0.1);

        CorrelationMatrixResult corr = new CorrelationMatrixResult();
        corr.setAssets(List.of("A", "B"));
        corr.setMatrix(List.of(
                List.of(1.0, 0.9),
                List.of(0.9, 1.0)
        ));

        RebalanceAdviceV1Result result = new RebalanceAdviceV1Builder()
                .build(current, target, corr, 0.05);

        assertTrue(result.getWarnings().stream().anyMatch(w -> w.startsWith(RebalanceAdviceV1WarningCodes.REBAL_HIGH_CORR_OVERWEIGHT)));
    }

    private Map<String, Map<String, Object>> indexByAsset(List<Map<String, Object>> actions) {
        Map<String, Map<String, Object>> map = new LinkedHashMap<>();
        for (Map<String, Object> action : actions) {
            map.put(action.get("asset").toString(), action);
        }
        return map;
    }
}
