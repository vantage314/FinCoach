package com.fincoach.core.healthv2.advice.rules;

public enum AdviceRuleValueType {
    STRING,
    INT,
    DECIMAL,
    BOOL,
    JSON;

    public static AdviceRuleValueType from(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String value = raw.trim().toUpperCase();
        try {
            return AdviceRuleValueType.valueOf(value);
        } catch (Exception e) {
            return null;
        }
    }
}
