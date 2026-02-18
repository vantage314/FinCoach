package com.fincoach.core.healthv2.service.admin;

import com.fincoach.core.healthv2.dto.admin.AdminDataSourceImportResultDTO;
import com.fincoach.core.healthv2.dto.admin.AdminDataSourceStatusDTO;
import com.fincoach.core.healthv2.service.admin.impl.DataSourceAdminServiceImpl;
import com.fincoach.core.healthv2.mapper.FcPortfolioPriceSnapshotMapper;
import com.fincoach.core.repository.entity.FcSystemConfigEntity;
import com.fincoach.core.repository.mapper.FcJobStatusMapper;
import com.fincoach.core.repository.mapper.FcSystemConfigMapper;
import com.fincoach.core.ticker.mapper.FcTickerMappingMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.times;

public class DataSourceAdminServiceTest {

    @Test
    public void testGetStatusDefault() {
        FcSystemConfigMapper configMapper = Mockito.mock(FcSystemConfigMapper.class);
        FcJobStatusMapper jobStatusMapper = Mockito.mock(FcJobStatusMapper.class);
        FcPortfolioPriceSnapshotMapper snapshotMapper = Mockito.mock(FcPortfolioPriceSnapshotMapper.class);
        FcTickerMappingMapper tickerMappingMapper = Mockito.mock(FcTickerMappingMapper.class);

        Mockito.when(configMapper.selectOne(Mockito.any())).thenReturn(null);
        Mockito.when(jobStatusMapper.selectOne(Mockito.any())).thenReturn(null);

        DataSourceAdminServiceImpl service = new DataSourceAdminServiceImpl(
                configMapper, jobStatusMapper, snapshotMapper, tickerMappingMapper);

        AdminDataSourceStatusDTO status = service.getStatus(1L);
        assertEquals("DEMO_DB", status.getMode());
        assertNotNull(status.getJob());
        assertEquals("STOPPED", status.getJob().getStatus());

        Mockito.verify(configMapper, times(1)).insert(Mockito.any());
        Mockito.verify(jobStatusMapper, times(1)).insert(Mockito.any());
    }

    @Test
    public void testSwitchMode() {
        FcSystemConfigMapper configMapper = Mockito.mock(FcSystemConfigMapper.class);
        FcJobStatusMapper jobStatusMapper = Mockito.mock(FcJobStatusMapper.class);
        FcPortfolioPriceSnapshotMapper snapshotMapper = Mockito.mock(FcPortfolioPriceSnapshotMapper.class);
        FcTickerMappingMapper tickerMappingMapper = Mockito.mock(FcTickerMappingMapper.class);

        FcSystemConfigEntity existing = new FcSystemConfigEntity();
        existing.setCfgKey("DATA_SOURCE_MODE");
        existing.setCfgValue("DEMO_DB");

        FcSystemConfigEntity updated = new FcSystemConfigEntity();
        updated.setCfgKey("DATA_SOURCE_MODE");
        updated.setCfgValue("REALTIME");

        Mockito.when(configMapper.selectOne(Mockito.any())).thenReturn(existing, updated);
        Mockito.when(configMapper.update(Mockito.isNull(), Mockito.any())).thenReturn(1);
        Mockito.when(jobStatusMapper.selectOne(Mockito.any())).thenReturn(null);

        DataSourceAdminServiceImpl service = new DataSourceAdminServiceImpl(
                configMapper, jobStatusMapper, snapshotMapper, tickerMappingMapper);

        AdminDataSourceStatusDTO status = service.switchMode("REALTIME", 1L);
        assertEquals("REALTIME", status.getMode());
    }

    @Test
    public void testImportDemoData() {
        FcSystemConfigMapper configMapper = Mockito.mock(FcSystemConfigMapper.class);
        FcJobStatusMapper jobStatusMapper = Mockito.mock(FcJobStatusMapper.class);
        FcPortfolioPriceSnapshotMapper snapshotMapper = Mockito.mock(FcPortfolioPriceSnapshotMapper.class);
        FcTickerMappingMapper tickerMappingMapper = Mockito.mock(FcTickerMappingMapper.class);

        Mockito.when(tickerMappingMapper.selectCount(Mockito.any())).thenReturn(0L);
        Mockito.when(tickerMappingMapper.insert(Mockito.any())).thenReturn(1);
        Mockito.when(snapshotMapper.selectCount(Mockito.any())).thenReturn(0L);
        Mockito.when(snapshotMapper.insert(Mockito.any())).thenReturn(1);

        DataSourceAdminServiceImpl service = new DataSourceAdminServiceImpl(
                configMapper, jobStatusMapper, snapshotMapper, tickerMappingMapper);

        AdminDataSourceImportResultDTO result = service.importDemoData(1L);
        assertEquals(20, result.getInsertedSnapshots());
        assertEquals(0, result.getSkippedSnapshots());
        assertEquals(3, result.getInsertedMappings());

        Mockito.verify(snapshotMapper, times(20)).insert(Mockito.any());
        Mockito.verify(tickerMappingMapper, times(3)).insert(Mockito.any());
    }
}
