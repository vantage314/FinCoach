package com.fincoach.core.healthv2.service.admin;

import com.fincoach.core.healthv2.mapper.FcPortfolioPriceSnapshotMapper;
import com.fincoach.core.healthv2.dto.admin.AdminJobActionResultDTO;
import com.fincoach.core.healthv2.service.admin.impl.DataSourceAdminServiceImpl;
import com.fincoach.core.repository.entity.FcSystemConfigEntity;
import com.fincoach.core.repository.mapper.FcJobStatusMapper;
import com.fincoach.core.repository.mapper.FcSystemConfigMapper;
import com.fincoach.core.ticker.mapper.FcTickerMappingMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RunnerDaemonModeTest {

    @Test
    public void daemonModeKeepsRunningUntilStop() throws Exception {
        FcSystemConfigMapper configMapper = Mockito.mock(FcSystemConfigMapper.class);
        FcJobStatusMapper jobStatusMapper = Mockito.mock(FcJobStatusMapper.class);
        FcPortfolioPriceSnapshotMapper snapshotMapper = Mockito.mock(FcPortfolioPriceSnapshotMapper.class);
        FcTickerMappingMapper tickerMappingMapper = Mockito.mock(FcTickerMappingMapper.class);

        FcSystemConfigEntity modeConfig = new FcSystemConfigEntity();
        modeConfig.setCfgKey("CRAWLER_MODE");
        modeConfig.setCfgValue("DAEMON");
        FcSystemConfigEntity intervalConfig = new FcSystemConfigEntity();
        intervalConfig.setCfgKey("CRAWLER_INTERVAL_SECONDS");
        intervalConfig.setCfgValue("1");
        FcSystemConfigEntity maxBatchConfig = new FcSystemConfigEntity();
        maxBatchConfig.setCfgKey("CRAWLER_MAX_BATCHES");
        maxBatchConfig.setCfgValue("0");

        Mockito.when(configMapper.selectOne(Mockito.any())).thenReturn(modeConfig, intervalConfig, maxBatchConfig);
        Mockito.when(configMapper.insert(Mockito.any())).thenReturn(1);
        Mockito.when(configMapper.update(Mockito.isNull(), Mockito.any())).thenReturn(1);

        Mockito.when(jobStatusMapper.selectOne(Mockito.any())).thenReturn(null);
        Mockito.when(jobStatusMapper.insert(Mockito.any())).thenReturn(1);

        Mockito.when(snapshotMapper.selectOne(Mockito.any())).thenReturn(null);
        Mockito.when(snapshotMapper.insert(Mockito.any())).thenReturn(1);

        Mockito.when(jobStatusMapper.update(Mockito.isNull(), Mockito.any())).thenReturn(1);

        CountDownLatch latch = new CountDownLatch(2);
        CrawlerRunner runner = new CrawlerRunner() {
            @Override
            public CrawlerRunResult run(CrawlerRunRequest request, Consumer<String> lineConsumer, AtomicBoolean stopSignal) {
                lineConsumer.accept("{\"assetKey\":\"SPY.US\",\"date\":\"2026-02-18\",\"price\":123.45}");
                latch.countDown();
                CrawlerRunResult result = new CrawlerRunResult();
                result.setSuccess(true);
                result.setLines(1);
                return result;
            }
        };

        DataSourceAdminServiceImpl service = new DataSourceAdminServiceImpl(
                configMapper, jobStatusMapper, snapshotMapper, tickerMappingMapper, runner);

        AdminJobActionResultDTO startResult = service.startRealtime(1L);
        assertEquals("RUNNING", startResult.getStatus());
        assertTrue(latch.await(3, TimeUnit.SECONDS));
        AdminJobActionResultDTO stopResult = service.stopRealtime(1L);
        assertEquals("STOPPED", stopResult.getStatus());
    }
}
