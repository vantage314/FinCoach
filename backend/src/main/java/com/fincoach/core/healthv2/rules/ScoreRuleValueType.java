package com.fincoach.core.healthv2.rules;

public enum ScoreRuleValueType {
    STRING,
    INT,
    DECIMAL,
    BOOL;

    public static ScoreRuleValueType from(String raw) {
        if (raw == null) return null;
        try {
            return ScoreRuleValueType.valueOf(raw.trim().toUpperCase());
        } catch (Exception e) {
            return null;
        }
    }
}
