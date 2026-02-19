package com.fincoach.core.healthv2.service.admin;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fincoach.core.healthv2.service.admin.impl.DataSourceAdminServiceImpl;
import com.fincoach.core.healthv2.scheduler.CrawlerSelfHealScheduler;
import com.fincoach.core.healthv2.mapper.FcPortfolioPriceSnapshotMapper;
import com.fincoach.core.repository.entity.FcJobStatusEntity;
import com.fincoach.core.repository.entity.FcSystemConfigEntity;
import com.fincoach.core.repository.mapper.FcJobStatusMapper;
import com.fincoach.core.repository.mapper.FcSystemConfigMapper;
import com.fincoach.core.ticker.mapper.FcTickerMappingMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CrawlerObservabilityTest {

    @Test
    public void staleRestartCapsEventRingAndIncrementsCounters() throws Exception {
        FcSystemConfigMapper configMapper = Mockito.mock(FcSystemConfigMapper.class);
        FcJobStatusMapper jobStatusMapper = Mockito.mock(FcJobStatusMapper.class);
        FcPortfolioPriceSnapshotMapper snapshotMapper = Mockito.mock(FcPortfolioPriceSnapshotMapper.class);
        FcTickerMappingMapper tickerMappingMapper = Mockito.mock(FcTickerMappingMapper.class);

        FcSystemConfigEntity dataModeConfig = new FcSystemConfigEntity();
        dataModeConfig.setCfgKey("DATA_SOURCE_MODE");
        dataModeConfig.setCfgValue("REALTIME");
        FcSystemConfigEntity modeConfig = new FcSystemConfigEntity();
        modeConfig.setCfgKey("CRAWLER_MODE");
        modeConfig.setCfgValue("RUN_ONCE");
        FcSystemConfigEntity intervalConfig = new FcSystemConfigEntity();
        intervalConfig.setCfgKey("CRAWLER_INTERVAL_SECONDS");
        intervalConfig.setCfgValue("1");
        FcSystemConfigEntity maxBatchConfig = new FcSystemConfigEntity();
        maxBatchConfig.setCfgKey("CRAWLER_MAX_BATCHES");
        maxBatchConfig.setCfgValue("1");
        Mockito.when(configMapper.selectOne(Mockito.any())).thenReturn(
                dataModeConfig,
                modeConfig, intervalConfig, maxBatchConfig,
                modeConfig, intervalConfig, maxBatchConfig
        );

        String json = buildEventsJson(20);

        FcJobStatusEntity job = new FcJobStatusEntity();
        job.setJobName("PY_MARKET_CRAWLER");
        job.setStatus("RUNNING");
        job.setLastHeartbeatAt(LocalDateTime.now().minusSeconds(120));
        job.setRecentEventsJson(json);
        job.setLastLog("old");
        job.setStaleCount(0);
        job.setRestartCount(0);

        Mockito.when(jobStatusMapper.selectOne(Mockito.any())).thenReturn(job);
        Mockito.when(jobStatusMapper.selectForUpdate(Mockito.any())).thenReturn(job);
        Mockito.when(jobStatusMapper.update(Mockito.isNull(), Mockito.any())).thenReturn(1);

        CrawlerRunner runner = Mockito.mock(CrawlerRunner.class);
        Mockito.when(runner.run(Mockito.any(), Mockito.any(), Mockito.any())).thenAnswer(invocation -> {
            CrawlerRunResult result = new CrawlerRunResult();
            result.setSuccess(true);
            result.setLines(0);
            return result;
        });

        DataSourceAdminServiceImpl service = new DataSourceAdminServiceImpl(
                configMapper, jobStatusMapper, snapshotMapper, tickerMappingMapper, runner);

        configureEventMapper(service);

        CrawlerSelfHealScheduler scheduler = new CrawlerSelfHealScheduler(service);
        scheduler.checkAndHeal();

        ArgumentCaptor<UpdateWrapper> captor = ArgumentCaptor.forClass(UpdateWrapper.class);
        Mockito.verify(jobStatusMapper, Mockito.atLeastOnce()).update(Mockito.isNull(), captor.capture());
        List<UpdateWrapper> updates = captor.getAllValues().stream()
                .filter(uw -> String.valueOf(uw.getSqlSet()).contains("recent_events_json"))
                .toList();
        assertTrue(updates.size() > 0);
        UpdateWrapper update = updates.stream()
                .filter(uw -> {
                    String jsonValue = extractJson(uw);
                    if (jsonValue == null) return false;
                    try {
                        List<?> parsed = new ObjectMapper().readValue(jsonValue, List.class);
                        String lastType = String.valueOf(((Map<?, ?>) parsed.get(parsed.size() - 1)).get("type"));
                        return "STALE_RESTART".equals(lastType);
                    } catch (Exception e) {
                        return false;
                    }
                })
                .findFirst()
                .orElse(updates.get(0));
        String eventJson = extractJson(update);
        assertNotNull(eventJson);
        List<?> parsed = new ObjectMapper().readValue(eventJson, List.class);
        assertEquals(20, parsed.size());
        String lastType = String.valueOf(((Map<?, ?>) parsed.get(parsed.size() - 1)).get("type"));
        assertEquals("STALE_RESTART", lastType);
        assertTrue(String.valueOf(update.getSqlSet()).contains("stale_count"));
        assertTrue(String.valueOf(update.getSqlSet()).contains("restart_count"));
    }

    @Test
    public void manualRecoverRecordsEventAndCounter() throws Exception {
        FcSystemConfigMapper configMapper = Mockito.mock(FcSystemConfigMapper.class);
        FcJobStatusMapper jobStatusMapper = Mockito.mock(FcJobStatusMapper.class);
        FcPortfolioPriceSnapshotMapper snapshotMapper = Mockito.mock(FcPortfolioPriceSnapshotMapper.class);
        FcTickerMappingMapper tickerMappingMapper = Mockito.mock(FcTickerMappingMapper.class);

        FcSystemConfigEntity dataModeConfig = new FcSystemConfigEntity();
        dataModeConfig.setCfgKey("DATA_SOURCE_MODE");
        dataModeConfig.setCfgValue("REALTIME");
        FcSystemConfigEntity modeConfig = new FcSystemConfigEntity();
        modeConfig.setCfgKey("CRAWLER_MODE");
        modeConfig.setCfgValue("RUN_ONCE");
        FcSystemConfigEntity intervalConfig = new FcSystemConfigEntity();
        intervalConfig.setCfgKey("CRAWLER_INTERVAL_SECONDS");
        intervalConfig.setCfgValue("1");
        FcSystemConfigEntity maxBatchConfig = new FcSystemConfigEntity();
        maxBatchConfig.setCfgKey("CRAWLER_MAX_BATCHES");
        maxBatchConfig.setCfgValue("1");
        Mockito.when(configMapper.selectOne(Mockito.any())).thenReturn(
                dataModeConfig,
                modeConfig, intervalConfig, maxBatchConfig,
                modeConfig, intervalConfig, maxBatchConfig
        );

        FcJobStatusEntity job = new FcJobStatusEntity();
        job.setJobName("PY_MARKET_CRAWLER");
        job.setStatus("STOPPED");
        job.setRecoverCount(0);

        Mockito.when(jobStatusMapper.selectOne(Mockito.any())).thenReturn(job);
        Mockito.when(jobStatusMapper.selectForUpdate(Mockito.any())).thenReturn(job);
        Mockito.when(jobStatusMapper.update(Mockito.isNull(), Mockito.any())).thenReturn(1);

        CrawlerRunner runner = Mockito.mock(CrawlerRunner.class);
        Mockito.when(runner.run(Mockito.any(), Mockito.any(), Mockito.any())).thenAnswer(invocation -> {
            CrawlerRunResult result = new CrawlerRunResult();
            result.setSuccess(true);
            result.setLines(0);
            return result;
        });

        DataSourceAdminServiceImpl service = new DataSourceAdminServiceImpl(
                configMapper, jobStatusMapper, snapshotMapper, tickerMappingMapper, runner);

        configureEventMapper(service);
        service.recoverRealtime(1L);

        ArgumentCaptor<UpdateWrapper> captor = ArgumentCaptor.forClass(UpdateWrapper.class);
        Mockito.verify(jobStatusMapper, Mockito.atLeastOnce()).update(Mockito.isNull(), captor.capture());
        UpdateWrapper update = captor.getAllValues().stream()
                .filter(uw -> String.valueOf(uw.getSqlSet()).contains("recent_events_json"))
                .findFirst()
                .orElse(null);
        assertNotNull(update);
        String eventJson = extractJson(update);
        assertNotNull(eventJson);
        List<?> parsed = new ObjectMapper().readValue(eventJson, List.class);
        String lastType = String.valueOf(((Map<?, ?>) parsed.get(parsed.size() - 1)).get("type"));
        assertEquals("MANUAL_RECOVER", lastType);
        assertTrue(String.valueOf(update.getSqlSet()).contains("recover_count"));
    }

    private String extractJson(UpdateWrapper wrapper) {
        if (wrapper == null) {
            return null;
        }
        for (Object value : wrapper.getParamNameValuePairs().values()) {
            if (value instanceof String) {
                String text = (String) value;
                if (text.startsWith("[") && text.contains("\"type\"")) {
                    return text;
                }
            }
        }
        return null;
    }

    private String buildEventsJson(int count) throws Exception {
        List<Map<String, String>> events = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            events.add(Map.of("ts", "t" + i, "type", "EVT", "msg", "m" + i));
        }
        return new ObjectMapper().writeValueAsString(events);
    }

    private void configureEventMapper(DataSourceAdminServiceImpl service) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        Class<?> eventClass = Class.forName(
                "com.fincoach.core.healthv2.service.admin.impl.DataSourceAdminServiceImpl$CrawlerEvent");
        Constructor<?> ctor = eventClass.getDeclaredConstructor(String.class, String.class, String.class);
        ctor.setAccessible(true);
        SimpleModule module = new SimpleModule();
        module.addDeserializer((Class) eventClass, new JsonDeserializer<>() {
            @Override
            public Object deserialize(JsonParser p, DeserializationContext ctxt) {
                try {
                    Map<?, ?> node = p.readValueAs(Map.class);
                    String ts = String.valueOf(node.get("ts"));
                    String type = String.valueOf(node.get("type"));
                    String msg = String.valueOf(node.get("msg"));
                    return ctor.newInstance(ts, type, msg);
                } catch (Exception e) {
                    return null;
                }
            }
        });
        mapper.registerModule(module);
        Field field = DataSourceAdminServiceImpl.class.getDeclaredField("objectMapper");
        field.setAccessible(true);
        field.set(service, mapper);
    }
}
