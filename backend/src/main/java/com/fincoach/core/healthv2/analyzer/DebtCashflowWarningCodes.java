package com.fincoach.core.healthv2.analyzer;

public final class DebtCashflowWarningCodes {
    public static final String MISSING_DEBT_ITEMS = "MISSING_DEBT_ITEMS";
    public static final String MISSING_CASHFLOW_INCOME = "MISSING_CASHFLOW_INCOME";
    public static final String MISSING_CASHFLOW_EXPENSE = "MISSING_CASHFLOW_EXPENSE";
    public static final String MISSING_LIQUID_ASSETS = "MISSING_LIQUID_ASSETS";
    public static final String MISSING_TOTAL_ASSETS = "MISSING_TOTAL_ASSETS";
    public static final String EMERGENCY_FUND_ESTIMATED = "EMERGENCY_FUND_ESTIMATED";
    public static final String CASHFLOW_INSUFFICIENT_DATA = "CASHFLOW_INSUFFICIENT_DATA";
    public static final String DEBT_MISSING_PAYMENT = "DEBT_MISSING_PAYMENT";
    public static final String DTI_INSUFFICIENT_INCOME = "DTI_INSUFFICIENT_INCOME";
    public static final String EMERGENCY_FUND_UNKNOWN = "EMERGENCY_FUND_UNKNOWN";
    public static final String EMERGENCY_FUND_INSUFFICIENT_EXPENSE = "EMERGENCY_FUND_INSUFFICIENT_EXPENSE";
    public static final String DTI_WARN = "DTI_WARN";
    public static final String DTI_DANGER = "DTI_DANGER";
    public static final String EMERGENCY_FUND_CRITICAL = "EMERGENCY_FUND_CRITICAL";
    public static final String EMERGENCY_FUND_LOW = "EMERGENCY_FUND_LOW";
    public static final String CASHFLOW_NEGATIVE = "CASHFLOW_NEGATIVE";

    private DebtCashflowWarningCodes() {}
}
