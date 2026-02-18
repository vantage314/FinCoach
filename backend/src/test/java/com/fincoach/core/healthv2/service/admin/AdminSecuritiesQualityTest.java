package com.fincoach.core.healthv2.service.admin;

import com.fincoach.core.healthv2.dto.admin.AdminSecuritiesQualityDTO;
import com.fincoach.core.healthv2.entity.FcPortfolioPriceSnapshotEntity;
import com.fincoach.core.healthv2.mapper.FcPortfolioPriceSnapshotMapper;
import com.fincoach.core.healthv2.service.admin.impl.AdminSecuritiesQualityServiceImpl;
import com.fincoach.core.repository.mapper.FcSystemConfigMapper;
import com.fincoach.core.ticker.mapper.FcTickerMappingMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class AdminSecuritiesQualityTest {

    @Test
    public void testMissingMappingAndSnapshotIssues() {
        FcTickerMappingMapper mappingMapper = Mockito.mock(FcTickerMappingMapper.class);
        FcPortfolioPriceSnapshotMapper snapshotMapper = Mockito.mock(FcPortfolioPriceSnapshotMapper.class);
        FcSystemConfigMapper systemConfigMapper = Mockito.mock(FcSystemConfigMapper.class);

        when(systemConfigMapper.selectOne(any())).thenReturn(null);
        when(mappingMapper.selectList(any())).thenReturn(List.of());

        LocalDate d1 = LocalDate.now().minusDays(5);
        LocalDate d2 = LocalDate.now().minusDays(4);
        FcPortfolioPriceSnapshotEntity s1 = new FcPortfolioPriceSnapshotEntity();
        s1.setDataSource("DEMO_DB");
        s1.setAsOfDate(d1);
        s1.setEquity(new BigDecimal("100000"));
        FcPortfolioPriceSnapshotEntity s2 = new FcPortfolioPriceSnapshotEntity();
        s2.setDataSource("DEMO_DB");
        s2.setAsOfDate(d2);
        s2.setEquity(new BigDecimal("100100"));
        when(snapshotMapper.selectList(any())).thenReturn(List.of(s1, s2));

        AdminSecuritiesQualityServiceImpl service = new AdminSecuritiesQualityServiceImpl(
                mappingMapper, snapshotMapper, systemConfigMapper);

        AdminSecuritiesQualityDTO dto = service.evaluate();
        assertNotNull(dto);
        assertNotNull(dto.getMissingMappings());
        assertFalse(dto.getMissingMappings().isEmpty());
        assertTrue(dto.getMissingMappings().stream().anyMatch(m -> "DEMO_DB".equals(m.getAssetKey())));

        assertNotNull(dto.getSnapshotCoverage());
        assertNotNull(dto.getSnapshotCoverage().getIssues());
        assertTrue(dto.getSnapshotCoverage().getIssues().stream().anyMatch(i -> i.contains("覆盖天数不足")));
        assertTrue(dto.getSnapshotCoverage().getIssues().stream().anyMatch(i -> i.contains("滞后")));
    }
}
