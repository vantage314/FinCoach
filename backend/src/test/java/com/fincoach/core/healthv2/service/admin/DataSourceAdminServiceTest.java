package com.fincoach.core.healthv2.service.admin;

import com.fincoach.core.healthv2.dto.admin.AdminDataSourceImportResultDTO;
import com.fincoach.core.healthv2.dto.admin.AdminDataSourceStatusDTO;
import com.fincoach.core.healthv2.service.admin.CrawlerRunRequest;
import com.fincoach.core.healthv2.service.admin.CrawlerRunResult;
import com.fincoach.core.healthv2.service.admin.CrawlerRunner;
import com.fincoach.core.healthv2.service.admin.impl.DataSourceAdminServiceImpl;
import com.fincoach.core.healthv2.mapper.FcPortfolioPriceSnapshotMapper;
import com.fincoach.core.repository.entity.FcSystemConfigEntity;
import com.fincoach.core.repository.mapper.FcJobStatusMapper;
import com.fincoach.core.repository.mapper.FcSystemConfigMapper;
import com.fincoach.core.ticker.mapper.FcTickerMappingMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
                configMapper, jobStatusMapper, snapshotMapper, tickerMappingMapper, new NoopRunner());

        AdminDataSourceStatusDTO status = service.getStatus(1L);
        assertEquals("DEMO_DB", status.getMode());
        assertNotNull(status.getJob());
        assertEquals("STOPPED", status.getJob().getStatus());

        Mockito.verify(configMapper, times(4)).insert(Mockito.any());
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

        Mockito.when(configMapper.selectOne(Mockito.any())).thenReturn(existing, updated, null, null, null);
        Mockito.when(configMapper.update(Mockito.isNull(), Mockito.any())).thenReturn(1);
        Mockito.when(jobStatusMapper.selectOne(Mockito.any())).thenReturn(null);

        DataSourceAdminServiceImpl service = new DataSourceAdminServiceImpl(
                configMapper, jobStatusMapper, snapshotMapper, tickerMappingMapper, new NoopRunner());

        AdminDataSourceStatusDTO status = service.switchMode("REALTIME", 1L);
        assertEquals("REALTIME", status.getMode());
    }

    @Test
    public void testImportDemoData() {
        FcSystemConfigMapper configMapper = Mockito.mock(FcSystemConfigMapper.class);
        FcJobStatusMapper jobStatusMapper = Mockito.mock(FcJobStatusMapper.class);
        FcPortfolioPriceSnapshotMapper snapshotMapper = Mockito.mock(FcPortfolioPriceSnapshotMapper.class);
        FcTickerMappingMapper tickerMappingMapper = Mockito.mock(FcTickerMappingMapper.class);

        Mockito.when(tickerMappingMapper.selectList(Mockito.any())).thenReturn(List.of());
        Mockito.when(tickerMappingMapper.insert(Mockito.any())).thenReturn(1);
        Mockito.when(tickerMappingMapper.update(Mockito.isNull(), Mockito.any())).thenReturn(1);
        Mockito.when(snapshotMapper.selectOne(Mockito.any())).thenReturn(null);
        Mockito.when(snapshotMapper.insert(Mockito.any())).thenReturn(1);
        Mockito.when(snapshotMapper.delete(Mockito.any())).thenReturn(1);

        DataSourceAdminServiceImpl service = new DataSourceAdminServiceImpl(
                configMapper, jobStatusMapper, snapshotMapper, tickerMappingMapper, new NoopRunner());

        AdminDataSourceImportResultDTO result = service.importDemoData(1L);
        assertEquals(20, result.getInsertedSnapshots());
        assertEquals(0, result.getSkippedSnapshots());
        assertEquals(3, result.getInsertedMappings());

        Mockito.verify(snapshotMapper, times(20)).insert(Mockito.any());
        Mockito.verify(tickerMappingMapper, times(3)).insert(Mockito.any());
    }

    @Test
    public void testRealtimeRunnerWritesSnapshots() throws Exception {
        FcSystemConfigMapper configMapper = Mockito.mock(FcSystemConfigMapper.class);
        FcJobStatusMapper jobStatusMapper = Mockito.mock(FcJobStatusMapper.class);
        FcPortfolioPriceSnapshotMapper snapshotMapper = Mockito.mock(FcPortfolioPriceSnapshotMapper.class);
        FcTickerMappingMapper tickerMappingMapper = Mockito.mock(FcTickerMappingMapper.class);

        Mockito.when(jobStatusMapper.selectOne(Mockito.any())).thenReturn(null);
        FcSystemConfigEntity modeConfig = new FcSystemConfigEntity();
        modeConfig.setCfgKey("CRAWLER_MODE");
        modeConfig.setCfgValue("RUN_ONCE");
        FcSystemConfigEntity intervalConfig = new FcSystemConfigEntity();
        intervalConfig.setCfgKey("CRAWLER_INTERVAL_SECONDS");
        intervalConfig.setCfgValue("1");
        FcSystemConfigEntity maxBatchConfig = new FcSystemConfigEntity();
        maxBatchConfig.setCfgKey("CRAWLER_MAX_BATCHES");
        maxBatchConfig.setCfgValue("1");
        Mockito.when(configMapper.selectOne(Mockito.any())).thenReturn(modeConfig, intervalConfig, maxBatchConfig);
        Mockito.when(configMapper.insert(Mockito.any())).thenReturn(1);
        Mockito.when(snapshotMapper.selectOne(Mockito.any())).thenReturn(null);
        Mockito.when(snapshotMapper.insert(Mockito.any())).thenReturn(1);

        CountDownLatch latch = new CountDownLatch(1);
        FakeRunner runner = new FakeRunner(List.of(
                "{\"assetKey\":\"SPY.US\",\"date\":\"2026-02-18\",\"price\":123.45}"
        ), latch);

        DataSourceAdminServiceImpl service = new DataSourceAdminServiceImpl(
                configMapper, jobStatusMapper, snapshotMapper, tickerMappingMapper, runner);

        service.startRealtime(1L);
        assertTrue(latch.await(2, TimeUnit.SECONDS));

        Mockito.verify(snapshotMapper, times(1)).insert(Mockito.any());
    }

    private static class NoopRunner implements CrawlerRunner {
        @Override
        public CrawlerRunResult run(CrawlerRunRequest request, Consumer<String> lineConsumer, AtomicBoolean stopSignal) {
            CrawlerRunResult result = new CrawlerRunResult();
            result.setSuccess(true);
            result.setLines(0);
            return result;
        }
    }

    private static class FakeRunner implements CrawlerRunner {
        private final List<String> lines;
        private final CountDownLatch latch;

        private FakeRunner(List<String> lines, CountDownLatch latch) {
            this.lines = lines;
            this.latch = latch;
        }

        @Override
        public CrawlerRunResult run(CrawlerRunRequest request, Consumer<String> lineConsumer, AtomicBoolean stopSignal) {
            for (String line : lines) {
                lineConsumer.accept(line);
            }
            latch.countDown();
            CrawlerRunResult result = new CrawlerRunResult();
            result.setSuccess(true);
            result.setLines(lines.size());
            return result;
        }
    }
}
