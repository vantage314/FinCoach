package com.fincoach.core.healthv2.advice.rules;

public class AdviceRuleParamDefinition {
    private final String key;
    private final AdviceRuleValueType valueType;
    private final String defaultValue;
    private final String description;

    public AdviceRuleParamDefinition(String key,
                                     AdviceRuleValueType valueType,
                                     String defaultValue,
                                     String description) {
        this.key = key;
        this.valueType = valueType;
        this.defaultValue = defaultValue;
        this.description = description;
    }

    public String getKey() { return key; }
    public AdviceRuleValueType getValueType() { return valueType; }
    public String getDefaultValue() { return defaultValue; }
    public String getDescription() { return description; }
}
