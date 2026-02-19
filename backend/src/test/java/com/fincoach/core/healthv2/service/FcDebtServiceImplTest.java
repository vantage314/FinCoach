package com.fincoach.core.healthv2.service;

import com.fincoach.core.healthv2.dto.DebtUpsertDTO;
import com.fincoach.core.healthv2.entity.FcDebtEntity;
import com.fincoach.core.healthv2.mapper.FcDebtMapper;
import com.fincoach.core.healthv2.service.impl.FcDebtServiceImpl;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

public class FcDebtServiceImplTest {

    @Test
    public void testUpsertComputesMonthlyPaymentWhenMissing() {
        FcDebtMapper mapper = Mockito.mock(FcDebtMapper.class);
        AuditService auditService = Mockito.mock(AuditService.class);
        FcDebtServiceImpl service = new FcDebtServiceImpl();
        ReflectionTestUtils.setField(service, "debtMapper", mapper);
        ReflectionTestUtils.setField(service, "auditService", auditService);

        DebtUpsertDTO dto = new DebtUpsertDTO();
        dto.setDebtType("MORTGAGE");
        dto.setPrincipal(new BigDecimal("200000"));
        dto.setRemainingBalance(new BigDecimal("200000"));
        dto.setApr(new BigDecimal("0.06"));
        dto.setTermMonths(360);

        ArgumentCaptor<FcDebtEntity> captor = ArgumentCaptor.forClass(FcDebtEntity.class);
        when(mapper.insert(captor.capture())).thenReturn(1);
        doNothing().when(auditService).log(Mockito.eq(1L), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());

        FcDebtEntity saved = service.upsert(1L, dto);
        assertNotNull(saved.getMonthlyPayment());

        BigDecimal expected = calcPayment(new BigDecimal("200000"), new BigDecimal("0.06"), 360);
        BigDecimal actual = captor.getValue().getMonthlyPayment();
        assertNotNull(actual);
        assertTrue(expected.subtract(actual).abs().compareTo(new BigDecimal("0.01")) <= 0);
    }

    @Test
    public void testListReturnsActiveOnlySortedByAprDesc() {
        FcDebtMapper mapper = Mockito.mock(FcDebtMapper.class);
        AuditService auditService = Mockito.mock(AuditService.class);
        FcDebtServiceImpl service = new FcDebtServiceImpl();
        ReflectionTestUtils.setField(service, "debtMapper", mapper);
        ReflectionTestUtils.setField(service, "auditService", auditService);

        FcDebtEntity a = new FcDebtEntity();
        a.setId(1L);
        a.setApr(new BigDecimal("0.05"));
        a.setIsActive(1);
        FcDebtEntity b = new FcDebtEntity();
        b.setId(2L);
        b.setApr(new BigDecimal("0.10"));
        b.setIsActive(1);
        FcDebtEntity c = new FcDebtEntity();
        c.setId(3L);
        c.setApr(new BigDecimal("0.08"));
        c.setIsActive(0);
        when(mapper.selectList(any())).thenReturn(List.of(a, b, c));

        List<FcDebtEntity> result = service.listActiveByUserId(1L);
        assertEquals(2, result.size());
        assertEquals(new BigDecimal("0.10"), result.get(0).getApr());
        assertEquals(new BigDecimal("0.05"), result.get(1).getApr());
    }

    @Test
    public void testSoftDeleteMarksInactive() {
        FcDebtMapper mapper = Mockito.mock(FcDebtMapper.class);
        AuditService auditService = Mockito.mock(AuditService.class);
        FcDebtServiceImpl service = new FcDebtServiceImpl();
        ReflectionTestUtils.setField(service, "debtMapper", mapper);
        ReflectionTestUtils.setField(service, "auditService", auditService);

        FcDebtEntity existing = new FcDebtEntity();
        existing.setId(1L);
        existing.setUserId(1L);
        existing.setIsActive(1);
        when(mapper.selectById(1L)).thenReturn(existing);
        ArgumentCaptor<FcDebtEntity> captor = ArgumentCaptor.forClass(FcDebtEntity.class);
        when(mapper.updateById(captor.capture())).thenReturn(1);
        doNothing().when(auditService).log(Mockito.eq(1L), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());

        service.softDelete(1L, 1L);

        assertEquals(0, captor.getValue().getIsActive());
    }

    private BigDecimal calcPayment(BigDecimal principal, BigDecimal apr, int months) {
        BigDecimal monthlyRate = apr.divide(new BigDecimal("12"), 10, RoundingMode.HALF_UP);
        if (monthlyRate.compareTo(BigDecimal.ZERO) == 0) {
            return principal.divide(new BigDecimal(months), 2, RoundingMode.HALF_UP);
        }
        double base = BigDecimal.ONE.add(monthlyRate).doubleValue();
        double pow = Math.pow(base, months);
        BigDecimal powBd = BigDecimal.valueOf(pow);
        BigDecimal numerator = principal.multiply(monthlyRate).multiply(powBd);
        BigDecimal denominator = powBd.subtract(BigDecimal.ONE);
        return numerator.divide(denominator, 2, RoundingMode.HALF_UP);
    }
}
