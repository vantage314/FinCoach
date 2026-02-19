package com.fincoach.core.healthv2.service;

import com.fincoach.core.healthv2.dto.CashflowMonthUpsertDTO;
import com.fincoach.core.healthv2.entity.FcCashflowMonthEntity;
import com.fincoach.core.healthv2.mapper.FcCashflowMonthMapper;
import com.fincoach.core.healthv2.service.impl.FcCashflowMonthServiceImpl;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

public class FcCashflowMonthServiceImplTest {

    @Test
    public void testUpsertComputesNetAndUpdatesExisting() {
        FcCashflowMonthMapper mapper = Mockito.mock(FcCashflowMonthMapper.class);
        AuditService auditService = Mockito.mock(AuditService.class);
        FcCashflowMonthServiceImpl service = new FcCashflowMonthServiceImpl();
        ReflectionTestUtils.setField(service, "cashflowMonthMapper", mapper);
        ReflectionTestUtils.setField(service, "auditService", auditService);

        FcCashflowMonthEntity existing = new FcCashflowMonthEntity();
        existing.setId(10L);
        existing.setUserId(1L);
        existing.setMonth("2026-01");
        when(mapper.selectOne(any())).thenReturn(null).thenReturn(existing);
        when(mapper.insert(any())).thenReturn(1);
        when(mapper.updateById(any())).thenReturn(1);
        doNothing().when(auditService).log(Mockito.eq(1L), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());

        CashflowMonthUpsertDTO dto1 = new CashflowMonthUpsertDTO();
        dto1.setMonth("2026-01");
        dto1.setIncome(new BigDecimal("10000"));
        dto1.setExpense(new BigDecimal("7000"));
        service.upsert(1L, dto1);

        CashflowMonthUpsertDTO dto2 = new CashflowMonthUpsertDTO();
        dto2.setMonth("2026-01");
        dto2.setIncome(new BigDecimal("12000"));
        dto2.setExpense(new BigDecimal("6500"));
        ArgumentCaptor<FcCashflowMonthEntity> captor = ArgumentCaptor.forClass(FcCashflowMonthEntity.class);
        when(mapper.updateById(captor.capture())).thenReturn(1);
        service.upsert(1L, dto2);

        assertEquals(new BigDecimal("5500"), captor.getValue().getNet());
    }

    @Test
    public void testListByRangeReturnsExpectedMonths() {
        FcCashflowMonthMapper mapper = Mockito.mock(FcCashflowMonthMapper.class);
        AuditService auditService = Mockito.mock(AuditService.class);
        FcCashflowMonthServiceImpl service = new FcCashflowMonthServiceImpl();
        ReflectionTestUtils.setField(service, "cashflowMonthMapper", mapper);
        ReflectionTestUtils.setField(service, "auditService", auditService);

        FcCashflowMonthEntity jan = entity("2026-01");
        FcCashflowMonthEntity feb = entity("2026-02");
        FcCashflowMonthEntity mar = entity("2026-03");
        FcCashflowMonthEntity apr = entity("2026-04");
        when(mapper.selectList(any())).thenReturn(List.of(apr, jan, mar, feb));

        List<FcCashflowMonthEntity> result = service.listByUserIdAndRange(1L, "2026-01", "2026-03");
        assertEquals(3, result.size());
        assertEquals("2026-01", result.get(0).getMonth());
        assertEquals("2026-02", result.get(1).getMonth());
        assertEquals("2026-03", result.get(2).getMonth());
    }

    private FcCashflowMonthEntity entity(String month) {
        FcCashflowMonthEntity entity = new FcCashflowMonthEntity();
        entity.setMonth(month);
        entity.setIncome(new BigDecimal("1"));
        entity.setExpense(new BigDecimal("1"));
        entity.setNet(new BigDecimal("0"));
        return entity;
    }
}
