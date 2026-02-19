package com.fincoach.core.healthv2.advice.rules;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class AdviceRuleDefaults {
    private AdviceRuleDefaults() {}

    public static final String DEFAULT_CODE = "DEFAULT";
    public static final int DEFAULT_VERSION = 1;

    public static final String REBALANCE_DRIFT_PCT = "REBALANCE_DRIFT_PCT";
    public static final String RISK_SCORE_LOW = "RISK_SCORE_LOW";
    public static final String RISK_SCORE_MID = "RISK_SCORE_MID";
    public static final String RISK_SCORE_HIGH = "RISK_SCORE_HIGH";
    public static final String BEHAVIOR_SCORE_WEIGHTS_JSON = "BEHAVIOR_SCORE_WEIGHTS_JSON";
    public static final String MAX_POSITIONS = "MAX_POSITIONS";

    private static final Map<String, AdviceRuleParamDefinition> DEFAULT_PARAMS;

    static {
        Map<String, AdviceRuleParamDefinition> map = new LinkedHashMap<>();
        map.put(REBALANCE_DRIFT_PCT, new AdviceRuleParamDefinition(
                REBALANCE_DRIFT_PCT, AdviceRuleValueType.DECIMAL, "0.05", "Rebalance drift threshold"));
        map.put(RISK_SCORE_LOW, new AdviceRuleParamDefinition(
                RISK_SCORE_LOW, AdviceRuleValueType.INT, "30", "Risk score low band"));
        map.put(RISK_SCORE_MID, new AdviceRuleParamDefinition(
                RISK_SCORE_MID, AdviceRuleValueType.INT, "60", "Risk score mid band"));
        map.put(RISK_SCORE_HIGH, new AdviceRuleParamDefinition(
                RISK_SCORE_HIGH, AdviceRuleValueType.INT, "80", "Risk score high band"));
        map.put(BEHAVIOR_SCORE_WEIGHTS_JSON, new AdviceRuleParamDefinition(
                BEHAVIOR_SCORE_WEIGHTS_JSON, AdviceRuleValueType.JSON,
                "{\"tradeFreq\":0.4,\"concentration\":0.3,\"cashDrag\":0.3}",
                "Behavior score weights"));
        map.put(MAX_POSITIONS, new AdviceRuleParamDefinition(
                MAX_POSITIONS, AdviceRuleValueType.INT, "8", "Max positions"));
        DEFAULT_PARAMS = Collections.unmodifiableMap(map);
    }

    public static Map<String, AdviceRuleParamDefinition> defaultParams() {
        return DEFAULT_PARAMS;
    }
}
