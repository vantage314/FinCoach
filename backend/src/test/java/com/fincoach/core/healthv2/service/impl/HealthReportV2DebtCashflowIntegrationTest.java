package com.fincoach.core.healthv2.service.impl;

import com.fincoach.core.healthv2.analyzer.DebtCashflowWarningCodes;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class HealthReportV2DebtCashflowIntegrationTest {

    @Test
    public void testHighDtiAdjustsScoresAndWarns() {
        HealthReportV2ServiceImpl service = new HealthReportV2ServiceImpl();
        List<String> warnings = new ArrayList<>();
        List<java.util.Map<String, Object>> warningDetails = new ArrayList<>();

        HealthReportV2ServiceImpl.ScoreAdjustmentResult result = service.applyDebtCashflowAdjustments(
                50, 60, new BigDecimal("0.50"), new BigDecimal("5"), true,
                new BigDecimal("1000"), warnings, warningDetails);

        assertEquals(65, result.riskScore());
        assertEquals(45, result.healthScore());
        assertTrue(warnings.contains(DebtCashflowWarningCodes.DTI_DANGER));
    }

    @Test
    public void testNegativeCashflowAdjustsScoresAndWarns() {
        HealthReportV2ServiceImpl service = new HealthReportV2ServiceImpl();
        List<String> warnings = new ArrayList<>();
        List<java.util.Map<String, Object>> warningDetails = new ArrayList<>();

        HealthReportV2ServiceImpl.ScoreAdjustmentResult result = service.applyDebtCashflowAdjustments(
                40, 60, new BigDecimal("0.20"), new BigDecimal("4"), true,
                new BigDecimal("-1"), warnings, warningDetails);

        assertEquals(48, result.riskScore());
        assertEquals(52, result.healthScore());
        assertTrue(warnings.contains(DebtCashflowWarningCodes.CASHFLOW_NEGATIVE));
    }

    @Test
    public void testCashflowInsufficientDataWarning() {
        HealthReportV2ServiceImpl service = new HealthReportV2ServiceImpl();
        List<String> warnings = new ArrayList<>();
        List<java.util.Map<String, Object>> warningDetails = new ArrayList<>();

        HealthReportV2ServiceImpl.CashflowMonthSummary summary = service.computeCashflowMonthSummary(
                Collections.emptyList(), warnings, warningDetails);

        assertEquals(0, summary.monthsCount());
        assertEquals(50, summary.stabilityScore());
        assertTrue(warnings.contains(DebtCashflowWarningCodes.CASHFLOW_INSUFFICIENT_DATA));
    }

    @Test
    public void testEmergencyFundUnknownWarning() {
        HealthReportV2ServiceImpl service = new HealthReportV2ServiceImpl();
        List<String> warnings = new ArrayList<>();
        List<java.util.Map<String, Object>> warningDetails = new ArrayList<>();

        HealthReportV2ServiceImpl.EmergencyFundSummary summary = service.computeEmergencyFundSummary(
                BigDecimal.ZERO, false, new BigDecimal("1000"), warnings, warningDetails);

        assertFalse(summary.valid());
        assertEquals(0, summary.months().compareTo(BigDecimal.ZERO));
        assertTrue(warnings.contains(DebtCashflowWarningCodes.EMERGENCY_FUND_UNKNOWN));
    }
}
