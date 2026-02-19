package com.fincoach.core.healthv2.service.impl;

import com.fincoach.core.healthv2.analyzer.DebtCashflowWarningCodes;
import com.fincoach.core.healthv2.service.AlertService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

public class HealthReportV2AlertEmitTest {

    @Test
    public void testEmitAlertsForDtiDanger() {
        HealthReportV2ServiceImpl service = new HealthReportV2ServiceImpl();
        AlertService alertService = Mockito.mock(AlertService.class);
        ReflectionTestUtils.setField(service, "alertService", alertService);

        List<String> warnings = new ArrayList<>();
        warnings.add(DebtCashflowWarningCodes.DTI_DANGER);

        service.emitHealthAlerts(1L, warnings, new BigDecimal("0.55"), new BigDecimal("2"),
                new BigDecimal("100"), null);

        ArgumentCaptor<String> typeCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(alertService).raiseAlert(eq(1L), typeCaptor.capture(), eq("DANGER"),
                any(), any(), eq("HEALTHV2"), any());
        assertEquals("DTI_DANGER", typeCaptor.getValue());
    }
}
