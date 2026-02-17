package com.fincoach.core.healthv2.advice;

public final class AdviceWarningCodes {
    public static final String MISSING_ASSETS = "MISSING_ASSETS";
    public static final String MISSING_LIABILITIES = "MISSING_LIABILITIES";
    public static final String MISSING_CASHFLOW = "MISSING_CASHFLOW";
    public static final String MISSING_ALLOCATION = "MISSING_ALLOCATION";
    public static final String MISSING_TOTAL_ASSETS = "MISSING_TOTAL_ASSETS";

    public static final String LIQUID_ASSET_FALLBACK_TOTAL = "LIQUID_ASSET_FALLBACK_TOTAL";
    public static final String EMERGENCY_MONTHS_UNAVAILABLE = "EMERGENCY_MONTHS_UNAVAILABLE";
    public static final String DEBT_PAYMENT_RATIO_UNAVAILABLE = "DEBT_PAYMENT_RATIO_UNAVAILABLE";
    public static final String SURPLUS_RATE_UNAVAILABLE = "SURPLUS_RATE_UNAVAILABLE";
    public static final String REBALANCE_TEMPLATE_MISSING = "REBALANCE_TEMPLATE_MISSING";
    public static final String REBALANCE_INPUT_MISSING = "REBALANCE_INPUT_MISSING";

    private AdviceWarningCodes() {
    }
}
