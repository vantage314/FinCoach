package com.fincoach.core.healthv2.service;

import com.fincoach.core.healthv2.dto.CsvImportResultDTO;
import com.fincoach.core.healthv2.entity.FcCashflowMonthEntity;
import com.fincoach.core.healthv2.mapper.FcCashflowMonthMapper;
import com.fincoach.core.healthv2.service.impl.FcCashflowMonthServiceImpl;
import com.fincoach.core.healthv2.util.CsvReaderHelper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CashflowMonthCsvImportServiceTest {

    @Test
    public void testImportUpsertSameMonthIsIdempotent() {
        CsvReaderHelper helper = new CsvReaderHelper();
        FcCashflowMonthMapper mapper = Mockito.mock(FcCashflowMonthMapper.class);
        AuditService auditService = Mockito.mock(AuditService.class);

        FcCashflowMonthServiceImpl service = new FcCashflowMonthServiceImpl();
        ReflectionTestUtils.setField(service, "cashflowMonthMapper", mapper);
        ReflectionTestUtils.setField(service, "auditService", auditService);

        CashflowMonthCsvImportService importService = new CashflowMonthCsvImportService(helper, service);

        FcCashflowMonthEntity existing = new FcCashflowMonthEntity();
        existing.setId(1L);
        existing.setUserId(1L);
        existing.setMonth("2026-01");
        existing.setIncome(new BigDecimal("1000"));
        existing.setExpense(new BigDecimal("500"));
        when(mapper.selectOne(any())).thenReturn(null, existing);
        when(mapper.insert(any())).thenReturn(1);
        when(mapper.updateById(any())).thenReturn(1);
        doNothing().when(auditService).log(Mockito.eq(1L), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());

        String csv = "month,income,expense\n"
                + "2026-01,1000,500\n"
                + "2026-01,1200,600\n";
        MockMultipartFile file = new MockMultipartFile("file", "cashflow.csv", "text/csv", csv.getBytes());

        CsvImportResultDTO result = importService.importCsv(file, 1L);

        assertEquals(2, result.getSuccessCount());
        verify(mapper, times(1)).insert(any());
        verify(mapper, times(1)).updateById(any());
    }

    @Test
    public void testInvalidRowReported() {
        CsvReaderHelper helper = new CsvReaderHelper();
        FcCashflowMonthService cashflowMonthService = Mockito.mock(FcCashflowMonthService.class);
        CashflowMonthCsvImportService service = new CashflowMonthCsvImportService(helper, cashflowMonthService);

        String csv = "month,income,expense\n"
                + "2026-01,-1,10\n";
        MockMultipartFile file = new MockMultipartFile("file", "cashflow.csv", "text/csv", csv.getBytes());

        CsvImportResultDTO result = service.importCsv(file, 1L);

        assertEquals(0, result.getSuccessCount());
        assertEquals(1, result.getFailCount());
        assertFalse(result.getFailures().isEmpty());
        verify(cashflowMonthService, never()).upsert(anyLong(), any());
    }
}
