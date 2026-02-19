package com.fincoach.core.healthv2.analyzer;

public final class InsuranceWarningCodes {
    public static final String INSURANCE_INCOME_MISSING = "INSURANCE_INCOME_MISSING";
    public static final String INSURANCE_PREMIUM_MISSING = "INSURANCE_PREMIUM_MISSING";
    public static final String INSURANCE_CURRENT_MISSING_PREFIX = "INSURANCE_CURRENT_MISSING:";
    public static final String MISSING_INSURANCE_MEDICAL_INFO = "MISSING_INSURANCE_MEDICAL_INFO";
    public static final String MISSING_DEPENDENTS_INFO = "MISSING_DEPENDENTS_INFO";
    public static final String INCOME_MISSING_FOR_PREMIUM_RATIO = "INCOME_MISSING_FOR_PREMIUM_RATIO";
    public static final String PREMIUM_RATIO_WARN = "PREMIUM_RATIO_WARN";
    public static final String PREMIUM_RATIO_DANGER = "PREMIUM_RATIO_DANGER";
    public static final String INSURANCE_GAP_HIGH = "INSURANCE_GAP_HIGH";

    private InsuranceWarningCodes() {}
}
