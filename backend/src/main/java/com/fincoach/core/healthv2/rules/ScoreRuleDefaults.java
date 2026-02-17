package com.fincoach.core.healthv2.rules;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ScoreRuleDefaults {

    private ScoreRuleDefaults() {}

    public static final String DEFAULT_CODE = "DEFAULT";
    public static final String DEFAULT_NAME = "Default Rule Set";
    public static final int DEFAULT_VERSION = 1;

    public static final String W_SHARPE = "W_SHARPE";
    public static final String W_MDD = "W_MDD";
    public static final String W_VOL = "W_VOL";
    public static final String W_DIVERSIFICATION = "W_DIVERSIFICATION";

    public static final String MDD_BAD = "MDD_BAD";
    public static final String MDD_OK = "MDD_OK";
    public static final String SHARPE_OK = "SHARPE_OK";
    public static final String SHARPE_GOOD = "SHARPE_GOOD";

    public static final String CASH_FLOW_RATE_MIN = "CASH_FLOW_RATE_MIN";
    public static final String DEBT_RATIO_MAX = "DEBT_RATIO_MAX";
    public static final String EMERGENCY_MONTHS_MIN = "EMERGENCY_MONTHS_MIN";
    public static final String DEBT_PAYMENT_RATIO_MAX = "DEBT_PAYMENT_RATIO_MAX";
    public static final String SURPLUS_RATE_MIN = "SURPLUS_RATE_MIN";
    public static final String REB_THRESHOLD = "REB_THRESHOLD";

    public static final String ASSET_LIABILITY_RATIO_GOOD = "ASSET_LIABILITY_RATIO_GOOD";
    public static final String LIQUIDITY_RATIO_GOOD = "LIQUIDITY_RATIO_GOOD";
    public static final String HEALTH_W_LIQUIDITY = "HEALTH_W_LIQUIDITY";
    public static final String HEALTH_W_DEBT = "HEALTH_W_DEBT";
    public static final String HEALTH_W_DIVERSIFICATION = "HEALTH_W_DIVERSIFICATION";
    public static final String HEALTH_W_RAR = "HEALTH_W_RAR";
    public static final String HEALTH_W_BEHAVIOR = "HEALTH_W_BEHAVIOR";
    public static final String RISK_LEVEL_HIGH_MIN = "RISK_LEVEL_HIGH_MIN";
    public static final String RISK_LEVEL_MED_MIN = "RISK_LEVEL_MED_MIN";
    public static final String HEALTH_LEVEL_HIGH_MIN = "HEALTH_LEVEL_HIGH_MIN";
    public static final String HEALTH_LEVEL_MED_MIN = "HEALTH_LEVEL_MED_MIN";
    public static final String BEHAVIOR_LEVEL_HIGH_MIN = "BEHAVIOR_LEVEL_HIGH_MIN";
    public static final String BEHAVIOR_LEVEL_MED_MIN = "BEHAVIOR_LEVEL_MED_MIN";
    public static final String DTI_HIGH = "DTI_HIGH";
    public static final String DTI_MED = "DTI_MED";
    public static final String EMERGENCY_MONTHS_LOW = "EMERGENCY_MONTHS_LOW";
    public static final String EMERGENCY_MONTHS_OK = "EMERGENCY_MONTHS_OK";
    public static final String DEBT_TO_ASSETS_HIGH = "DEBT_TO_ASSETS_HIGH";
    public static final String LIQUID_ASSET_ESTIMATE_RATIO = "LIQUID_ASSET_ESTIMATE_RATIO";
    public static final String DEBT_OPTIMIZER_STRATEGY = "DEBT_OPTIMIZER_STRATEGY";
    public static final String DEBT_OPTIMIZER_EXTRA_PAY_RATIO = "DEBT_OPTIMIZER_EXTRA_PAY_RATIO";
    public static final String DEBT_OPTIMIZER_EXPECTED_RETURN = "DEBT_OPTIMIZER_EXPECTED_RETURN";
    public static final String DEBT_OPTIMIZER_RETURN_MARGIN = "DEBT_OPTIMIZER_RETURN_MARGIN";

    private static final Map<String, ScoreRuleParamDefinition> DEFAULT_PARAMS;

    static {
        Map<String, ScoreRuleParamDefinition> map = new LinkedHashMap<>();
        map.put(W_SHARPE, new ScoreRuleParamDefinition(
                W_SHARPE, ScoreRuleValueType.DECIMAL, "0.35",
                BigDecimal.ZERO, BigDecimal.ONE, "Sharpe weight"));
        map.put(W_MDD, new ScoreRuleParamDefinition(
                W_MDD, ScoreRuleValueType.DECIMAL, "0.35",
                BigDecimal.ZERO, BigDecimal.ONE, "Max drawdown weight"));
        map.put(W_VOL, new ScoreRuleParamDefinition(
                W_VOL, ScoreRuleValueType.DECIMAL, "0.15",
                BigDecimal.ZERO, BigDecimal.ONE, "Volatility weight"));
        map.put(W_DIVERSIFICATION, new ScoreRuleParamDefinition(
                W_DIVERSIFICATION, ScoreRuleValueType.DECIMAL, "0.15",
                BigDecimal.ZERO, BigDecimal.ONE, "Diversification weight"));

        map.put(MDD_BAD, new ScoreRuleParamDefinition(
                MDD_BAD, ScoreRuleValueType.DECIMAL, "0.40",
                BigDecimal.ZERO, BigDecimal.ONE, "Max drawdown bad threshold"));
        map.put(MDD_OK, new ScoreRuleParamDefinition(
                MDD_OK, ScoreRuleValueType.DECIMAL, "0.20",
                BigDecimal.ZERO, BigDecimal.ONE, "Max drawdown ok threshold"));
        map.put(SHARPE_OK, new ScoreRuleParamDefinition(
                SHARPE_OK, ScoreRuleValueType.DECIMAL, "0.50",
                new BigDecimal("-5"), new BigDecimal("5"), "Sharpe ok threshold"));
        map.put(SHARPE_GOOD, new ScoreRuleParamDefinition(
                SHARPE_GOOD, ScoreRuleValueType.DECIMAL, "1.00",
                new BigDecimal("-5"), new BigDecimal("5"), "Sharpe good threshold"));

        map.put(CASH_FLOW_RATE_MIN, new ScoreRuleParamDefinition(
                CASH_FLOW_RATE_MIN, ScoreRuleValueType.DECIMAL, "0.10",
                BigDecimal.ZERO, BigDecimal.ONE, "Min cash flow rate"));
        map.put(DEBT_RATIO_MAX, new ScoreRuleParamDefinition(
                DEBT_RATIO_MAX, ScoreRuleValueType.DECIMAL, "0.50",
                BigDecimal.ZERO, BigDecimal.ONE, "Max debt ratio"));
        map.put(EMERGENCY_MONTHS_MIN, new ScoreRuleParamDefinition(
                EMERGENCY_MONTHS_MIN, ScoreRuleValueType.INT, "6",
                BigDecimal.ZERO, new BigDecimal("24"), "Min emergency months"));
        map.put(DEBT_PAYMENT_RATIO_MAX, new ScoreRuleParamDefinition(
                DEBT_PAYMENT_RATIO_MAX, ScoreRuleValueType.DECIMAL, "0.35",
                BigDecimal.ZERO, BigDecimal.ONE, "Max debt payment ratio"));
        map.put(SURPLUS_RATE_MIN, new ScoreRuleParamDefinition(
                SURPLUS_RATE_MIN, ScoreRuleValueType.DECIMAL, "0.10",
                BigDecimal.ZERO, BigDecimal.ONE, "Min surplus rate"));
        map.put(REB_THRESHOLD, new ScoreRuleParamDefinition(
                REB_THRESHOLD, ScoreRuleValueType.DECIMAL, "0.05",
                BigDecimal.ZERO, BigDecimal.ONE, "Rebalance deviation threshold"));

        map.put(ASSET_LIABILITY_RATIO_GOOD, new ScoreRuleParamDefinition(
                ASSET_LIABILITY_RATIO_GOOD, ScoreRuleValueType.DECIMAL, "2.00",
                BigDecimal.ZERO, new BigDecimal("100"), "Asset/liability good ratio"));
        map.put(LIQUIDITY_RATIO_GOOD, new ScoreRuleParamDefinition(
                LIQUIDITY_RATIO_GOOD, ScoreRuleValueType.DECIMAL, "0.20",
                BigDecimal.ZERO, BigDecimal.ONE, "Liquidity ratio good threshold"));

        map.put(HEALTH_W_LIQUIDITY, new ScoreRuleParamDefinition(
                HEALTH_W_LIQUIDITY, ScoreRuleValueType.DECIMAL, "0.25",
                BigDecimal.ZERO, BigDecimal.ONE, "Health score liquidity weight"));
        map.put(HEALTH_W_DEBT, new ScoreRuleParamDefinition(
                HEALTH_W_DEBT, ScoreRuleValueType.DECIMAL, "0.25",
                BigDecimal.ZERO, BigDecimal.ONE, "Health score debt weight"));
        map.put(HEALTH_W_DIVERSIFICATION, new ScoreRuleParamDefinition(
                HEALTH_W_DIVERSIFICATION, ScoreRuleValueType.DECIMAL, "0.20",
                BigDecimal.ZERO, BigDecimal.ONE, "Health score diversification weight"));
        map.put(HEALTH_W_RAR, new ScoreRuleParamDefinition(
                HEALTH_W_RAR, ScoreRuleValueType.DECIMAL, "0.15",
                BigDecimal.ZERO, BigDecimal.ONE, "Health score risk-adjusted return weight"));
        map.put(HEALTH_W_BEHAVIOR, new ScoreRuleParamDefinition(
                HEALTH_W_BEHAVIOR, ScoreRuleValueType.DECIMAL, "0.15",
                BigDecimal.ZERO, BigDecimal.ONE, "Health score behavior weight"));

        map.put(RISK_LEVEL_HIGH_MIN, new ScoreRuleParamDefinition(
                RISK_LEVEL_HIGH_MIN, ScoreRuleValueType.INT, "70",
                BigDecimal.ZERO, new BigDecimal("100"), "Risk score high level min"));
        map.put(RISK_LEVEL_MED_MIN, new ScoreRuleParamDefinition(
                RISK_LEVEL_MED_MIN, ScoreRuleValueType.INT, "40",
                BigDecimal.ZERO, new BigDecimal("100"), "Risk score medium level min"));
        map.put(HEALTH_LEVEL_HIGH_MIN, new ScoreRuleParamDefinition(
                HEALTH_LEVEL_HIGH_MIN, ScoreRuleValueType.INT, "70",
                BigDecimal.ZERO, new BigDecimal("100"), "Health score high level min"));
        map.put(HEALTH_LEVEL_MED_MIN, new ScoreRuleParamDefinition(
                HEALTH_LEVEL_MED_MIN, ScoreRuleValueType.INT, "40",
                BigDecimal.ZERO, new BigDecimal("100"), "Health score medium level min"));
        map.put(BEHAVIOR_LEVEL_HIGH_MIN, new ScoreRuleParamDefinition(
                BEHAVIOR_LEVEL_HIGH_MIN, ScoreRuleValueType.INT, "70",
                BigDecimal.ZERO, new BigDecimal("100"), "Behavior score high level min"));
        map.put(BEHAVIOR_LEVEL_MED_MIN, new ScoreRuleParamDefinition(
                BEHAVIOR_LEVEL_MED_MIN, ScoreRuleValueType.INT, "40",
                BigDecimal.ZERO, new BigDecimal("100"), "Behavior score medium level min"));

        map.put(DTI_HIGH, new ScoreRuleParamDefinition(
                DTI_HIGH, ScoreRuleValueType.DECIMAL, "0.40",
                BigDecimal.ZERO, BigDecimal.ONE, "DTI high threshold"));
        map.put(DTI_MED, new ScoreRuleParamDefinition(
                DTI_MED, ScoreRuleValueType.DECIMAL, "0.20",
                BigDecimal.ZERO, BigDecimal.ONE, "DTI medium threshold"));
        map.put(EMERGENCY_MONTHS_LOW, new ScoreRuleParamDefinition(
                EMERGENCY_MONTHS_LOW, ScoreRuleValueType.DECIMAL, "3.0",
                BigDecimal.ZERO, new BigDecimal("24"), "Emergency fund low threshold"));
        map.put(EMERGENCY_MONTHS_OK, new ScoreRuleParamDefinition(
                EMERGENCY_MONTHS_OK, ScoreRuleValueType.DECIMAL, "6.0",
                BigDecimal.ZERO, new BigDecimal("24"), "Emergency fund ok threshold"));
        map.put(DEBT_TO_ASSETS_HIGH, new ScoreRuleParamDefinition(
                DEBT_TO_ASSETS_HIGH, ScoreRuleValueType.DECIMAL, "0.60",
                BigDecimal.ZERO, BigDecimal.ONE, "Debt to assets high threshold"));
        map.put(LIQUID_ASSET_ESTIMATE_RATIO, new ScoreRuleParamDefinition(
                LIQUID_ASSET_ESTIMATE_RATIO, ScoreRuleValueType.DECIMAL, "0.10",
                BigDecimal.ZERO, BigDecimal.ONE, "Liquid asset estimate ratio"));
        map.put(DEBT_OPTIMIZER_STRATEGY, new ScoreRuleParamDefinition(
                DEBT_OPTIMIZER_STRATEGY, ScoreRuleValueType.STRING, "AVALANCHE",
                null, null, "Debt optimizer default strategy"));
        map.put(DEBT_OPTIMIZER_EXTRA_PAY_RATIO, new ScoreRuleParamDefinition(
                DEBT_OPTIMIZER_EXTRA_PAY_RATIO, ScoreRuleValueType.DECIMAL, "0.50",
                BigDecimal.ZERO, BigDecimal.ONE, "Debt optimizer extra payment ratio"));
        map.put(DEBT_OPTIMIZER_EXPECTED_RETURN, new ScoreRuleParamDefinition(
                DEBT_OPTIMIZER_EXPECTED_RETURN, ScoreRuleValueType.DECIMAL, "0.06",
                BigDecimal.ZERO, BigDecimal.ONE, "Expected investment return"));
        map.put(DEBT_OPTIMIZER_RETURN_MARGIN, new ScoreRuleParamDefinition(
                DEBT_OPTIMIZER_RETURN_MARGIN, ScoreRuleValueType.DECIMAL, "0.01",
                BigDecimal.ZERO, BigDecimal.ONE, "Debt vs investment margin"));

        DEFAULT_PARAMS = Collections.unmodifiableMap(map);
    }

    public static Map<String, ScoreRuleParamDefinition> defaultParams() {
        return DEFAULT_PARAMS;
    }
}
