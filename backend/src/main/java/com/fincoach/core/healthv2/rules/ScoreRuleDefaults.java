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

    public static final String ASSET_LIABILITY_RATIO_GOOD = "ASSET_LIABILITY_RATIO_GOOD";
    public static final String LIQUIDITY_RATIO_GOOD = "LIQUIDITY_RATIO_GOOD";

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
                EMERGENCY_MONTHS_MIN, ScoreRuleValueType.INT, "3",
                BigDecimal.ZERO, new BigDecimal("24"), "Min emergency months"));

        map.put(ASSET_LIABILITY_RATIO_GOOD, new ScoreRuleParamDefinition(
                ASSET_LIABILITY_RATIO_GOOD, ScoreRuleValueType.DECIMAL, "2.00",
                BigDecimal.ZERO, new BigDecimal("100"), "Asset/liability good ratio"));
        map.put(LIQUIDITY_RATIO_GOOD, new ScoreRuleParamDefinition(
                LIQUIDITY_RATIO_GOOD, ScoreRuleValueType.DECIMAL, "0.20",
                BigDecimal.ZERO, BigDecimal.ONE, "Liquidity ratio good threshold"));

        DEFAULT_PARAMS = Collections.unmodifiableMap(map);
    }

    public static Map<String, ScoreRuleParamDefinition> defaultParams() {
        return DEFAULT_PARAMS;
    }
}
