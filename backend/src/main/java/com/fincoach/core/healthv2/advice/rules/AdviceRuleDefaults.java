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
    public static final String EMERGENCY_FUND_MONTHS_TARGET = "EMERGENCY_FUND_MONTHS_TARGET";
    public static final String DTI_WARN = "DTI_WARN";
    public static final String DTI_DANGER = "DTI_DANGER";
    public static final String DEBT_STRATEGY = "DEBT_STRATEGY";
    public static final String MIN_NET_FOR_EXTRA_DEBT_PAYMENT = "MIN_NET_FOR_EXTRA_DEBT_PAYMENT";

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
        map.put(EMERGENCY_FUND_MONTHS_TARGET, new AdviceRuleParamDefinition(
                EMERGENCY_FUND_MONTHS_TARGET, AdviceRuleValueType.INT, "3", "Emergency fund target months"));
        map.put(DTI_WARN, new AdviceRuleParamDefinition(
                DTI_WARN, AdviceRuleValueType.DECIMAL, "0.35", "Debt-to-income warning threshold"));
        map.put(DTI_DANGER, new AdviceRuleParamDefinition(
                DTI_DANGER, AdviceRuleValueType.DECIMAL, "0.50", "Debt-to-income danger threshold"));
        map.put(DEBT_STRATEGY, new AdviceRuleParamDefinition(
                DEBT_STRATEGY, AdviceRuleValueType.STRING, "AVALANCHE", "Debt payoff strategy"));
        map.put(MIN_NET_FOR_EXTRA_DEBT_PAYMENT, new AdviceRuleParamDefinition(
                MIN_NET_FOR_EXTRA_DEBT_PAYMENT, AdviceRuleValueType.DECIMAL, "0",
                "Min net cashflow for extra debt payment"));
        DEFAULT_PARAMS = Collections.unmodifiableMap(map);
    }

    public static Map<String, AdviceRuleParamDefinition> defaultParams() {
        return DEFAULT_PARAMS;
    }
}
