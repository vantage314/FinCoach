package com.fincoach.core.healthv2.util;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class HealthV2ConfigDefaults {

    private HealthV2ConfigDefaults() {}

    // Annual risk-free rate (2%)
    public static final double DEFAULT_RF_ANNUAL = 0.02;

    // High correlation threshold
    public static final double DEFAULT_CORR_HIGH_THRESHOLD = 0.75;

    public static final String DEFAULT_SCORE_RULE_WEIGHTS_JSON =
            "{\"health\":{\"Liquidity\":25,\"DebtHealth\":25,\"Diversification\":20,\"RiskAdjustedReturn\":15,\"Behavior\":15},"
                    + "\"risk\":{\"EquityRatio\":25,\"MaxDrawdown\":20,\"Concentration\":20,\"CashflowFragility\":20,\"NetWorthNegative\":15},"
                    + "\"behavior\":{\"DataSufficiency\":40,\"Consistency\":30,\"DisciplineProxy\":30}}";

    public static final String DEFAULT_SCORE_RULE_THRESHOLDS_JSON =
            "{\"emergencyMonthsGood\":6,\"emergencyMonthsOk\":3,\"dtiWarning\":0.4,\"dtiDanger\":0.5,"
                    + "\"highInterestRate\":0.12,\"concentrationHigh\":0.7}";

    private static final Map<String, String> ALERT_RULE_DEFAULT_THRESHOLDS;
    private static final Map<String, String> ADVICE_TEMPLATE_DEFAULTS;
    private static final Map<String, String> INSURANCE_PARAM_DEFAULTS;

    static {
        Map<String, String> alertDefaults = new LinkedHashMap<>();
        alertDefaults.put("DTI_WARNING", "{\"threshold\":0.4}");
        alertDefaults.put("DTI_CRITICAL", "{\"threshold\":0.6}");
        alertDefaults.put("EMERGENCY_LOW", "{\"months\":1}");
        alertDefaults.put("EMERGENCY_WARN", "{\"months\":3}");
        alertDefaults.put("MAX_DRAWDOWN", "{\"threshold\":0.3}");
        alertDefaults.put("CONCENTRATION", "{\"threshold\":0.7}");
        ALERT_RULE_DEFAULT_THRESHOLDS = Collections.unmodifiableMap(alertDefaults);

        Map<String, String> templateDefaults = new LinkedHashMap<>();
        templateDefaults.put("EMERGENCY_LOW",
                "\u26A0\uFE0F \u4F18\u5148\u5EFA\u7ACB\u5E94\u6025\u91D1\uFF1A\u73B0\u91D1\u50A8\u5907\u4EC5{months}\u4E2A\u6708\uFF0C\u5EFA\u8BAE\u81F3\u5C11\u79EF\u7D2F3-6\u4E2A\u6708\u3002");
        templateDefaults.put("EMERGENCY_OK",
                "\u2705 \u5E94\u6025\u91D1\u5065\u5EB7\uFF1A\u8986\u76D6{months}\u4E2A\u6708\u652F\u51FA\u3002");
        templateDefaults.put("DTI_HIGH",
                "\u26A0\uFE0F \u8D1F\u503A\u538B\u529B\u504F\u5927\uFF1ADTI={dtiPct}%\uFF0C\u5EFA\u8BAE\u4F18\u5316\u503A\u52A1\u7ED3\u6784\u3002");
        templateDefaults.put("CASHFLOW_PROTECT",
                "\u26A0\uFE0F \u73B0\u91D1\u6D41\u7D27\u5F20\u6A21\u5F0F\uFF1A\u6682\u505C\u989D\u5916\u8FD8\u6B3E\uFF0C\u4F18\u5148\u4FDD\u969C\u751F\u6D3B\u4E0E\u5E94\u6025\u3002");
        templateDefaults.put("DEBT_HIGH_INTEREST",
                "\uD83D\uDCB0 \u5B58\u5728\u9AD8\u606F\u8D1F\u503A\uFF0C\u5EFA\u8BAE\u4F18\u5148\u507F\u8FD8\u4EE5\u51CF\u5C11\u5229\u606F\u652F\u51FA\u3002");
        templateDefaults.put("GOAL_AT_RISK",
                "\u26A0\uFE0F {count}\u4E2A\u8D22\u52A1\u76EE\u6807\u5B58\u5728\u98CE\u9669\uFF0C\u5EFA\u8BAE\u8C03\u6574\u6295\u5165\u6216\u671F\u9650\u3002");
        templateDefaults.put("INSURANCE_INCOMPLETE",
                "\uD83D\uDCA1 \u5EFA\u8BAE\u5B8C\u5584\u4FDD\u9669\u6863\u6848\uFF0C\u4EE5\u4FBF\u8BC4\u4F30\u4FDD\u969C\u7F3A\u53E3\u3002");
        templateDefaults.put("NO_CASHFLOW",
                "\uD83D\uDCA1 \u8BF7\u5F55\u5165\u73B0\u91D1\u6D41\u6570\u636E\u4EE5\u8BC4\u4F30\u5E94\u6025\u50A8\u5907\u3002");
        templateDefaults.put("NO_GOALS",
                "\uD83D\uDCA1 \u5EFA\u8BAE\u8BBE\u5B9A\u81F3\u5C11\u4E00\u4E2A\u8D22\u52A1\u76EE\u6807\u3002");
        ADVICE_TEMPLATE_DEFAULTS = Collections.unmodifiableMap(templateDefaults);

        Map<String, String> insuranceDefaults = new LinkedHashMap<>();
        insuranceDefaults.put("INCOME_MULTIPLIER",
                "{\"accident\":2,\"criticalIllness\":3,\"criticalIllnessHigh\":5,\"life\":5,\"lifeHigh\":7}");
        insuranceDefaults.put("PREMIUM_RATIO",
                "{\"min\":0.05,\"max\":0.10,\"minHigh\":0.07,\"maxHigh\":0.12}");
        insuranceDefaults.put("PRIORITY_DEFAULT",
                "[\"MEDICAL\",\"ACCIDENT\",\"CRITICAL_ILLNESS\",\"LIFE\"]");
        insuranceDefaults.put("PRIORITY_HIGH_RESP",
                "[\"MEDICAL\",\"CRITICAL_ILLNESS\",\"LIFE\",\"ACCIDENT\"]");
        INSURANCE_PARAM_DEFAULTS = Collections.unmodifiableMap(insuranceDefaults);
    }

    public static String defaultAlertThresholds(String ruleKey) {
        return ALERT_RULE_DEFAULT_THRESHOLDS.getOrDefault(ruleKey, "{}");
    }

    public static String defaultAdviceTemplate(String sceneKey) {
        return ADVICE_TEMPLATE_DEFAULTS.getOrDefault(sceneKey, "");
    }

    public static String defaultInsuranceParam(String paramKey) {
        return INSURANCE_PARAM_DEFAULTS.getOrDefault(paramKey, "{}");
    }
}
