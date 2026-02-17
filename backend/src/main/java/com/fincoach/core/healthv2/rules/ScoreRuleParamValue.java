package com.fincoach.core.healthv2.rules;

import com.fincoach.core.healthv2.entity.FcScoreRuleParamEntity;

import java.math.BigDecimal;

public class ScoreRuleParamValue {
    private final String key;
    private final ScoreRuleValueType valueType;
    private final String rawValue;
    private final BigDecimal decimalValue;
    private final Integer intValue;
    private final Boolean boolValue;
    private final String stringValue;
    private final BigDecimal minValue;
    private final BigDecimal maxValue;
    private final String description;
    private final String source;

    private ScoreRuleParamValue(String key,
                                ScoreRuleValueType valueType,
                                String rawValue,
                                BigDecimal decimalValue,
                                Integer intValue,
                                Boolean boolValue,
                                String stringValue,
                                BigDecimal minValue,
                                BigDecimal maxValue,
                                String description,
                                String source) {
        this.key = key;
        this.valueType = valueType;
        this.rawValue = rawValue;
        this.decimalValue = decimalValue;
        this.intValue = intValue;
        this.boolValue = boolValue;
        this.stringValue = stringValue;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.description = description;
        this.source = source;
    }

    public static ScoreRuleParamValue fromDefinition(ScoreRuleParamDefinition def, String source) {
        return parse(def.getKey(), def.getValueType(), def.getDefaultValue(),
                def.getMinValue(), def.getMaxValue(), def.getDescription(), source);
    }

    public static ScoreRuleParamValue fromEntity(FcScoreRuleParamEntity entity,
                                                 ScoreRuleParamDefinition def,
                                                 String source,
                                                 String[] error) {
        ScoreRuleValueType type = ScoreRuleValueType.from(entity.getValueType());
        if (type == null && def != null) {
            type = def.getValueType();
        }
        ScoreRuleParamValue parsed = parse(
                entity.getParamKey(),
                type,
                entity.getParamValue(),
                toBigDecimal(entity.getMinValue(), def == null ? null : def.getMinValue()),
                toBigDecimal(entity.getMaxValue(), def == null ? null : def.getMaxValue()),
                entity.getDescription() != null ? entity.getDescription() : (def == null ? null : def.getDescription()),
                source);
        if (parsed == null && def != null) {
            if (error != null && error.length > 0) {
                error[0] = "invalid";
            }
            return fromDefinition(def, "DEFAULT");
        }
        return parsed;
    }

    private static ScoreRuleParamValue parse(String key,
                                             ScoreRuleValueType type,
                                             String raw,
                                             BigDecimal minValue,
                                             BigDecimal maxValue,
                                             String description,
                                             String source) {
        if (type == null) {
            return null;
        }
        BigDecimal decimalValue = null;
        Integer intValue = null;
        Boolean boolValue = null;
        String stringValue = null;
        try {
            switch (type) {
                case STRING -> stringValue = raw;
                case INT -> intValue = raw == null ? null : Integer.parseInt(raw.trim());
                case DECIMAL -> decimalValue = raw == null ? null : new BigDecimal(raw.trim());
                case BOOL -> boolValue = raw == null ? null : Boolean.parseBoolean(raw.trim());
                default -> stringValue = raw;
            }
        } catch (Exception e) {
            return null;
        }
        return new ScoreRuleParamValue(key, type, raw, decimalValue, intValue, boolValue, stringValue,
                minValue, maxValue, description, source);
    }

    private static BigDecimal toBigDecimal(String raw, BigDecimal fallback) {
        if (raw == null || raw.isBlank()) return fallback;
        try {
            return new BigDecimal(raw.trim());
        } catch (Exception e) {
            return fallback;
        }
    }

    public String getKey() { return key; }
    public ScoreRuleValueType getValueType() { return valueType; }
    public String getRawValue() { return rawValue; }
    public BigDecimal getDecimalValue() { return decimalValue; }
    public Integer getIntValue() { return intValue; }
    public Boolean getBoolValue() { return boolValue; }
    public String getStringValue() { return stringValue; }
    public BigDecimal getMinValue() { return minValue; }
    public BigDecimal getMaxValue() { return maxValue; }
    public String getDescription() { return description; }
    public String getSource() { return source; }
}
