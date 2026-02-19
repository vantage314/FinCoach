package com.fincoach.core.healthv2.service;

import com.fincoach.core.healthv2.dto.CsvImportResultDTO;
import com.fincoach.core.healthv2.dto.DebtUpsertDTO;
import com.fincoach.core.healthv2.entity.FcDebtEntity;
import com.fincoach.core.healthv2.mapper.FcDebtMapper;
import com.fincoach.core.healthv2.service.impl.FcDebtServiceImpl;
import com.fincoach.core.healthv2.util.CsvReaderHelper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DebtCsvImportServiceTest {

    @Test
    public void testImportReportsInvalidRowAndPersistsValidRow() {
        CsvReaderHelper helper = new CsvReaderHelper();
        FcDebtService debtService = Mockito.mock(FcDebtService.class);
        DebtCsvImportService service = new DebtCsvImportService(helper, debtService);

        String csv = "debtType,apr,remainingBalance,monthlyPayment,termMonths,principal,startDate,endDate,externalKey\n"
                + "MORTGAGE,0.05,100000,,120,100000,2023-01-01,2033-01-01,ext-1\n"
                + ",0.05,100000,,120,100000,2023-01-01,2033-01-01,ext-2\n";
        MockMultipartFile file = new MockMultipartFile("file", "debt.csv", "text/csv", csv.getBytes());

        CsvImportResultDTO result = service.importCsv(file, 1L);

        assertEquals(1, result.getSuccessCount());
        assertEquals(1, result.getFailCount());
        assertFalse(result.getFailures().isEmpty());
        assertEquals("debtType", result.getFailures().get(0).getField());
        verify(debtService, times(1)).upsert(Mockito.eq(1L), any(DebtUpsertDTO.class));
    }

    @Test
    public void testImportWithExternalKeyUpdatesExisting() {
        CsvReaderHelper helper = new CsvReaderHelper();
        FcDebtMapper mapper = Mockito.mock(FcDebtMapper.class);
        AuditService auditService = Mockito.mock(AuditService.class);

        FcDebtServiceImpl debtService = new FcDebtServiceImpl();
        ReflectionTestUtils.setField(debtService, "debtMapper", mapper);
        ReflectionTestUtils.setField(debtService, "auditService", auditService);

        DebtCsvImportService service = new DebtCsvImportService(helper, debtService);

        FcDebtEntity existing = new FcDebtEntity();
        existing.setId(9L);
        existing.setUserId(1L);
        existing.setIsActive(1);
        when(mapper.selectOne(any())).thenReturn(existing);
        when(mapper.selectById(anyLong())).thenReturn(existing);
        ArgumentCaptor<FcDebtEntity> captor = ArgumentCaptor.forClass(FcDebtEntity.class);
        when(mapper.updateById(captor.capture())).thenReturn(1);
        doNothing().when(auditService).log(Mockito.eq(1L), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());

        String csv = "debtType,apr,remainingBalance,monthlyPayment,termMonths,principal,startDate,endDate,externalKey\n"
                + "MORTGAGE,0.05,100000,,120,100000,2023-01-01,2033-01-01,ext-1\n";
        MockMultipartFile file = new MockMultipartFile("file", "debt.csv", "text/csv", csv.getBytes());

        CsvImportResultDTO result = service.importCsv(file, 1L);

        assertEquals(1, result.getSuccessCount());
        assertNotNull(captor.getValue());
        assertEquals(9L, captor.getValue().getId());
        assertEquals("ext-1", captor.getValue().getExternalKey());
        verify(mapper, never()).insert(any());
    }
}
