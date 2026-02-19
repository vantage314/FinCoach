package com.fincoach.core.healthv2.advice.rules;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AdviceRuleSnapshot {
    public static final String SOURCE_DB_ACTIVE = "DB_ACTIVE";
    public static final String SOURCE_FALLBACK_DEFAULT = "FALLBACK_DEFAULT";

    private final Long ruleSetId;
    private final String code;
    private final int version;
    private final String source;
    private final Map<String, AdviceRuleParamValue> params;
    private final List<String> warnings;
    private final List<String> missingParams;

    public AdviceRuleSnapshot(Long ruleSetId,
                              String code,
                              int version,
                              String source,
                              Map<String, AdviceRuleParamValue> params,
                              List<String> warnings,
                              List<String> missingParams) {
        this.ruleSetId = ruleSetId;
        this.code = code;
        this.version = version;
        this.source = source;
        this.params = params == null ? new LinkedHashMap<>() : params;
        this.warnings = warnings == null ? new ArrayList<>() : warnings;
        this.missingParams = missingParams == null ? new ArrayList<>() : missingParams;
    }

    public Long getRuleSetId() { return ruleSetId; }
    public String getCode() { return code; }
    public int getVersion() { return version; }
    public String getSource() { return source; }
    public Map<String, AdviceRuleParamValue> getParams() { return params; }
    public List<String> getWarnings() { return warnings; }
    public List<String> getMissingParams() { return missingParams; }

    public BigDecimal getDecimal(String key, BigDecimal fallback) {
        AdviceRuleParamValue value = params.get(key);
        if (value == null) return fallback;
        if (value.getDecimalValue() != null) return value.getDecimalValue();
        if (value.getIntValue() != null) return BigDecimal.valueOf(value.getIntValue());
        if (value.getBoolValue() != null) return value.getBoolValue() ? BigDecimal.ONE : BigDecimal.ZERO;
        return fallback;
    }

    public Integer getInt(String key, Integer fallback) {
        AdviceRuleParamValue value = params.get(key);
        if (value == null) return fallback;
        if (value.getIntValue() != null) return value.getIntValue();
        if (value.getDecimalValue() != null) return value.getDecimalValue().intValue();
        if (value.getBoolValue() != null) return value.getBoolValue() ? 1 : 0;
        return fallback;
    }

    public Boolean getBool(String key, Boolean fallback) {
        AdviceRuleParamValue value = params.get(key);
        if (value == null) return fallback;
        if (value.getBoolValue() != null) return value.getBoolValue();
        if (value.getIntValue() != null) return value.getIntValue() != 0;
        if (value.getDecimalValue() != null) return value.getDecimalValue().compareTo(BigDecimal.ZERO) != 0;
        return fallback;
    }

    public String getString(String key, String fallback) {
        AdviceRuleParamValue value = params.get(key);
        if (value == null) return fallback;
        if (value.getStringValue() != null) return value.getStringValue();
        if (value.getDecimalValue() != null) return value.getDecimalValue().toPlainString();
        if (value.getIntValue() != null) return String.valueOf(value.getIntValue());
        if (value.getBoolValue() != null) return String.valueOf(value.getBoolValue());
        return fallback;
    }
}
