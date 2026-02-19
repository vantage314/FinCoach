package com.fincoach.core.healthv2.advice.rules;

import java.math.BigDecimal;

public class AdviceRuleParamValue {
    private final String key;
    private final AdviceRuleValueType valueType;
    private final String rawValue;
    private final BigDecimal decimalValue;
    private final Integer intValue;
    private final Boolean boolValue;
    private final String stringValue;
    private final String source;

    private AdviceRuleParamValue(String key,
                                 AdviceRuleValueType valueType,
                                 String rawValue,
                                 BigDecimal decimalValue,
                                 Integer intValue,
                                 Boolean boolValue,
                                 String stringValue,
                                 String source) {
        this.key = key;
        this.valueType = valueType;
        this.rawValue = rawValue;
        this.decimalValue = decimalValue;
        this.intValue = intValue;
        this.boolValue = boolValue;
        this.stringValue = stringValue;
        this.source = source;
    }

    public static AdviceRuleParamValue fromDefinition(AdviceRuleParamDefinition def, String source) {
        return parse(def.getKey(), def.getValueType(), def.getDefaultValue(), source);
    }

    public static AdviceRuleParamValue parse(String key,
                                             AdviceRuleValueType type,
                                             String raw,
                                             String source) {
        if (type == null) return null;
        BigDecimal decimalValue = null;
        Integer intValue = null;
        Boolean boolValue = null;
        String stringValue = null;
        try {
            switch (type) {
                case STRING, JSON -> stringValue = raw;
                case INT -> intValue = raw == null ? null : Integer.parseInt(raw.trim());
                case DECIMAL -> decimalValue = raw == null ? null : new BigDecimal(raw.trim());
                case BOOL -> boolValue = raw == null ? null : Boolean.parseBoolean(raw.trim());
                default -> stringValue = raw;
            }
        } catch (Exception e) {
            return null;
        }
        return new AdviceRuleParamValue(key, type, raw, decimalValue, intValue, boolValue, stringValue, source);
    }

    public String getKey() { return key; }
    public AdviceRuleValueType getValueType() { return valueType; }
    public String getRawValue() { return rawValue; }
    public BigDecimal getDecimalValue() { return decimalValue; }
    public Integer getIntValue() { return intValue; }
    public Boolean getBoolValue() { return boolValue; }
    public String getStringValue() { return stringValue; }
    public String getSource() { return source; }
}
