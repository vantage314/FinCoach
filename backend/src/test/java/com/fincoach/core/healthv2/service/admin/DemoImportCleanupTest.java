package com.fincoach.core.healthv2.service.admin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.fincoach.core.healthv2.dto.admin.AdminSecuritiesQualityDTO;
import com.fincoach.core.healthv2.entity.FcPortfolioPriceSnapshotEntity;
import com.fincoach.core.healthv2.mapper.FcPortfolioPriceSnapshotMapper;
import com.fincoach.core.healthv2.service.admin.impl.AdminSecuritiesQualityServiceImpl;
import com.fincoach.core.healthv2.service.admin.impl.DataSourceAdminServiceImpl;
import com.fincoach.core.repository.mapper.FcJobStatusMapper;
import com.fincoach.core.repository.mapper.FcSystemConfigMapper;
import com.fincoach.core.ticker.mapper.FcTickerMappingMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DemoImportCleanupTest {

    @Test
    public void cleanupLegacyDemoKeysBeforeImport() {
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

        service.importDemoData(1L);

        ArgumentCaptor<QueryWrapper<FcPortfolioPriceSnapshotEntity>> deleteCaptor = ArgumentCaptor.forClass(QueryWrapper.class);
        Mockito.verify(snapshotMapper).delete(deleteCaptor.capture());
        String deleteParams = String.valueOf(deleteCaptor.getValue().getParamNameValuePairs());
        String deleteSql = String.valueOf(deleteCaptor.getValue().getSqlSegment());
        assertTrue(deleteParams.contains("DEMO_DB") || deleteSql.contains("source"));

        ArgumentCaptor<UpdateWrapper> updateCaptor = ArgumentCaptor.forClass(UpdateWrapper.class);
        Mockito.verify(tickerMappingMapper).update(Mockito.isNull(), updateCaptor.capture());
        String updateParams = String.valueOf(updateCaptor.getValue().getParamNameValuePairs());
        String updateSql = String.valueOf(updateCaptor.getValue().getSqlSegment());
        assertTrue(updateParams.contains("DEMO_DB") || updateSql.contains("keyword"));

        FcTickerMappingMapper mappingMapper = Mockito.mock(FcTickerMappingMapper.class);
        FcPortfolioPriceSnapshotMapper qualitySnapshotMapper = Mockito.mock(FcPortfolioPriceSnapshotMapper.class);
        FcSystemConfigMapper qualityConfigMapper = Mockito.mock(FcSystemConfigMapper.class);

        Mockito.when(mappingMapper.selectList(Mockito.any())).thenReturn(List.of());
        Mockito.when(qualityConfigMapper.selectOne(Mockito.any())).thenReturn(null);

        FcPortfolioPriceSnapshotEntity legacy = new FcPortfolioPriceSnapshotEntity();
        legacy.setDataSource("DEMO_DB");
        legacy.setAsOfDate(LocalDate.now());
        legacy.setEquity(new BigDecimal("100000"));
        Mockito.when(qualitySnapshotMapper.selectList(Mockito.any())).thenReturn(List.of(legacy));

        AdminSecuritiesQualityServiceImpl qualityService = new AdminSecuritiesQualityServiceImpl(
                mappingMapper, qualitySnapshotMapper, qualityConfigMapper);
        AdminSecuritiesQualityDTO dto = qualityService.evaluate();
        assertTrue(dto.getLegacyDemoKeyIssues().contains("DEMO_DB"));
        assertFalse(dto.getMissingMappings().stream().anyMatch(m -> "DEMO_DB".equals(m.getAssetKey())));
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
}
