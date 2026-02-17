package com.fincoach.core.healthv2.rules;

import java.math.BigDecimal;

public class ScoreRuleParamDefinition {
    private final String key;
    private final ScoreRuleValueType valueType;
    private final String defaultValue;
    private final BigDecimal minValue;
    private final BigDecimal maxValue;
    private final String description;

    public ScoreRuleParamDefinition(String key,
                                    ScoreRuleValueType valueType,
                                    String defaultValue,
                                    BigDecimal minValue,
                                    BigDecimal maxValue,
                                    String description) {
        this.key = key;
        this.valueType = valueType;
        this.defaultValue = defaultValue;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.description = description;
    }

    public String getKey() { return key; }
    public ScoreRuleValueType getValueType() { return valueType; }
    public String getDefaultValue() { return defaultValue; }
    public BigDecimal getMinValue() { return minValue; }
    public BigDecimal getMaxValue() { return maxValue; }
    public String getDescription() { return description; }
}
