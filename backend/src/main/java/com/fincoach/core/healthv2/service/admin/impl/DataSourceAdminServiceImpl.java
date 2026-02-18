package com.fincoach.core.healthv2.service.admin.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fincoach.core.healthv2.dto.admin.AdminDataSourceImportResultDTO;
import com.fincoach.core.healthv2.dto.admin.AdminDataSourceStatusDTO;
import com.fincoach.core.healthv2.dto.admin.AdminJobActionResultDTO;
import com.fincoach.core.healthv2.dto.admin.AdminJobStatusDTO;
import com.fincoach.core.healthv2.entity.FcPortfolioPriceSnapshotEntity;
import com.fincoach.core.healthv2.mapper.FcPortfolioPriceSnapshotMapper;
import com.fincoach.core.healthv2.service.admin.CrawlerRunRequest;
import com.fincoach.core.healthv2.service.admin.CrawlerRunResult;
import com.fincoach.core.healthv2.service.admin.CrawlerRunner;
import com.fincoach.core.healthv2.service.admin.DataSourceAdminService;
import com.fincoach.core.repository.entity.FcJobStatusEntity;
import com.fincoach.core.repository.entity.FcSystemConfigEntity;
import com.fincoach.core.repository.mapper.FcJobStatusMapper;
import com.fincoach.core.repository.mapper.FcSystemConfigMapper;
import com.fincoach.core.ticker.entity.FcTickerMappingEntity;
import com.fincoach.core.ticker.mapper.FcTickerMappingMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
@Service
public class DataSourceAdminServiceImpl implements DataSourceAdminService {

    private static final String CFG_KEY_MODE = "DATA_SOURCE_MODE";
    private static final String DEFAULT_MODE = "DEMO_DB";
    private static final String JOB_NAME = "PY_MARKET_CRAWLER";
    private static final String STATUS_RUNNING = "RUNNING";
    private static final String STATUS_STOPPED = "STOPPED";
    private static final int DEMO_DAYS = 20;
    private static final List<DemoMapping> DEMO_MAPPINGS = List.of(
            new DemoMapping("STOCK", "SPY.US", "US", 100),
            new DemoMapping("BOND", "AGG.US", "US", 90),
            new DemoMapping("CASH", "CASH", "CASH", 80)
    );

    private final FcSystemConfigMapper systemConfigMapper;
    private final FcJobStatusMapper jobStatusMapper;
    private final FcPortfolioPriceSnapshotMapper snapshotMapper;
    private final FcTickerMappingMapper tickerMappingMapper;
    private final CrawlerRunner crawlerRunner;
    private final ExecutorService crawlerExecutor = Executors.newSingleThreadExecutor();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private volatile Future<?> crawlerFuture;
    private volatile AtomicBoolean crawlerStopSignal;

    public DataSourceAdminServiceImpl(FcSystemConfigMapper systemConfigMapper,
                                      FcJobStatusMapper jobStatusMapper,
                                      FcPortfolioPriceSnapshotMapper snapshotMapper,
                                      FcTickerMappingMapper tickerMappingMapper,
                                      CrawlerRunner crawlerRunner) {
        this.systemConfigMapper = systemConfigMapper;
        this.jobStatusMapper = jobStatusMapper;
        this.snapshotMapper = snapshotMapper;
        this.tickerMappingMapper = tickerMappingMapper;
        this.crawlerRunner = crawlerRunner;
    }

    @Override
    public AdminDataSourceStatusDTO getStatus(Long actorUserId) {
        FcSystemConfigEntity config = ensureModeConfig(actorUserId);
        FcJobStatusEntity job = ensureJobStatus();

        AdminDataSourceStatusDTO dto = new AdminDataSourceStatusDTO();
        dto.setMode(config != null && config.getCfgValue() != null ? config.getCfgValue() : DEFAULT_MODE);
        dto.setJob(toJobDto(job));
        return dto;
    }

    @Override
    public AdminDataSourceStatusDTO switchMode(String mode, Long actorUserId) {
        String normalized = normalizeMode(mode);
        if (normalized == null) {
            throw new IllegalArgumentException("invalid mode");
        }
        FcSystemConfigEntity config = ensureModeConfig(actorUserId);
        String previous = config != null ? config.getCfgValue() : DEFAULT_MODE;

        LocalDateTime now = LocalDateTime.now();
        UpdateWrapper<FcSystemConfigEntity> uw = new UpdateWrapper<>();
        uw.eq("cfg_key", CFG_KEY_MODE)
                .set("cfg_value", normalized)
                .set("updated_by", actorUserId)
                .set("updated_at", now);
        int updated = systemConfigMapper.update(null, uw);
        if (updated == 0) {
            FcSystemConfigEntity created = ensureModeConfig(actorUserId);
            if (created != null) {
                created.setCfgValue(normalized);
                created.setUpdatedBy(actorUserId);
                created.setUpdatedAt(now);
                systemConfigMapper.updateById(created);
            }
        }

        log.info("event=DATA_SOURCE_MODE_SWITCH userId={} previous={} mode={}", actorUserId, previous, normalized);
        return getStatus(actorUserId);
    }

    @Override
    public AdminDataSourceImportResultDTO importDemoData(Long actorUserId) {
        long userId = actorUserId != null ? actorUserId : 1L;
        int insertedMappings = ensureTickerMappings();

        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(DEMO_DAYS - 1);

        int inserted = 0;
        int skipped = 0;
        BigDecimal base = new BigDecimal("100000.00");
        LocalDateTime now = LocalDateTime.now();

        int assetSize = DEMO_MAPPINGS.size();
        for (int i = 0; i < DEMO_DAYS; i++) {
            LocalDate date = start.plusDays(i);
            BigDecimal value = base.add(BigDecimal.valueOf(i * 520L))
                    .add(BigDecimal.valueOf((i % 5) * 230L));

            String assetKey = DEMO_MAPPINGS.get(i % assetSize).assetKey;
            FcPortfolioPriceSnapshotEntity existing = findSnapshot(userId, date);
            if (existing != null) {
                existing.setEquity(value);
                existing.setBaseCurrency("USD");
                existing.setDataSource(assetKey);
                existing.setUpdatedAt(now);
                snapshotMapper.updateById(existing);
                skipped++;
                continue;
            }

            FcPortfolioPriceSnapshotEntity entity = new FcPortfolioPriceSnapshotEntity();
            entity.setUserId(userId);
            entity.setAsOfDate(date);
            entity.setEquity(value);
            entity.setBaseCurrency("USD");
            entity.setDataSource(assetKey);
            entity.setCreatedAt(now);
            entity.setUpdatedAt(now);
            snapshotMapper.insert(entity);
            inserted++;
        }

        AdminDataSourceImportResultDTO dto = new AdminDataSourceImportResultDTO();
        dto.setInsertedSnapshots(inserted);
        dto.setSkippedSnapshots(skipped);
        dto.setInsertedMappings(insertedMappings);
        dto.setMessage("demo import completed");

        log.info("event=DEMO_DATA_IMPORT userId={} insertedSnapshots={} skippedSnapshots={} insertedMappings={}",
                actorUserId, inserted, skipped, insertedMappings);
        return dto;
    }

    @Override
    public AdminJobActionResultDTO startRealtime(Long actorUserId) {
        ensureJobStatus();
        if (isCrawlerRunning()) {
            AdminJobActionResultDTO dto = new AdminJobActionResultDTO();
            dto.setJobName(JOB_NAME);
            dto.setStatus(STATUS_RUNNING);
            dto.setMessage("already running");
            return dto;
        }
        AtomicBoolean stopSignal = new AtomicBoolean(false);
        crawlerStopSignal = stopSignal;
        LocalDateTime now = LocalDateTime.now();
        UpdateWrapper<FcJobStatusEntity> uw = new UpdateWrapper<>();
        uw.eq("job_name", JOB_NAME)
                .set("status", STATUS_RUNNING)
                .set("last_start_at", now)
                .set("last_heartbeat_at", now)
                .set("last_end_at", null)
                .set("last_error", null)
                .set("last_log", null)
                .set("updated_at", now);
        jobStatusMapper.update(null, uw);

        crawlerFuture = crawlerExecutor.submit(() -> runCrawler(actorUserId, stopSignal));

        log.info("event=PY_CRAWLER_START userId={} job={}", actorUserId, JOB_NAME);
        AdminJobActionResultDTO dto = new AdminJobActionResultDTO();
        dto.setJobName(JOB_NAME);
        dto.setStatus(STATUS_RUNNING);
        dto.setMessage("start requested");
        return dto;
    }

    @Override
    public AdminJobActionResultDTO stopRealtime(Long actorUserId) {
        ensureJobStatus();
        AtomicBoolean signal = crawlerStopSignal;
        if (signal != null) {
            signal.set(true);
        }
        if (crawlerFuture != null && !crawlerFuture.isDone()) {
            crawlerFuture.cancel(true);
        }
        LocalDateTime now = LocalDateTime.now();
        UpdateWrapper<FcJobStatusEntity> uw = new UpdateWrapper<>();
        uw.eq("job_name", JOB_NAME)
                .set("status", STATUS_STOPPED)
                .set("last_end_at", now)
                .set("updated_at", now);
        jobStatusMapper.update(null, uw);

        log.info("event=PY_CRAWLER_STOP userId={} job={}", actorUserId, JOB_NAME);
        AdminJobActionResultDTO dto = new AdminJobActionResultDTO();
        dto.setJobName(JOB_NAME);
        dto.setStatus(STATUS_STOPPED);
        dto.setMessage("stop requested");
        return dto;
    }

    private boolean isCrawlerRunning() {
        return crawlerFuture != null && !crawlerFuture.isDone();
    }

    private void runCrawler(Long actorUserId, AtomicBoolean stopSignal) {
        long userId = actorUserId != null ? actorUserId : 1L;
        String scriptPath = resolveScriptPath();
        List<String> logLines = new ArrayList<>();
        Consumer<String> lineConsumer = (line) -> {
            if (line == null || line.isBlank()) return;
            appendLogLine(logLines, line);
            String aggregated = String.join("\n", logLines);
            updateJobLog(aggregated, LocalDateTime.now());
            handleCrawlerLine(line, userId);
        };

        CrawlerRunResult result = crawlerRunner.run(new CrawlerRunRequest(scriptPath), lineConsumer, stopSignal);
        LocalDateTime finishedAt = LocalDateTime.now();
        UpdateWrapper<FcJobStatusEntity> uw = new UpdateWrapper<>();
        uw.eq("job_name", JOB_NAME)
                .set("status", STATUS_STOPPED)
                .set("last_end_at", finishedAt)
                .set("updated_at", finishedAt);
        if (!result.isSuccess()) {
            uw.set("last_error", trimError(result.getError()));
        }
        jobStatusMapper.update(null, uw);
        log.info("event=PY_CRAWLER_FINISH userId={} success={} lines={}",
                actorUserId, result.isSuccess(), result.getLines());
    }

    private void updateJobLog(String logValue, LocalDateTime now) {
        UpdateWrapper<FcJobStatusEntity> uw = new UpdateWrapper<>();
        uw.eq("job_name", JOB_NAME)
                .set("last_log", trimLog(logValue))
                .set("last_heartbeat_at", now)
                .set("updated_at", now);
        jobStatusMapper.update(null, uw);
    }

    private void appendLogLine(List<String> lines, String line) {
        if (lines.size() >= 50) {
            lines.remove(0);
        }
        lines.add(line);
    }

    private void handleCrawlerLine(String line, long userId) {
        try {
            CrawlerPriceLine parsed = objectMapper.readValue(line, CrawlerPriceLine.class);
            if (parsed == null || parsed.getAssetKey() == null || parsed.getAssetKey().isBlank()) {
                return;
            }
            LocalDate date = parsed.getDate() != null ? LocalDate.parse(parsed.getDate()) : null;
            if (date == null || parsed.getPrice() == null) {
                return;
            }
            upsertSnapshot(userId, date, parsed.getAssetKey().trim(), parsed.getPrice());
        } catch (Exception e) {
            log.warn("Crawler line parse failed: {}", e.getMessage());
        }
    }

    private void upsertSnapshot(long userId, LocalDate date, String assetKey, BigDecimal price) {
        LocalDateTime now = LocalDateTime.now();
        FcPortfolioPriceSnapshotEntity existing = findSnapshot(userId, date);
        if (existing != null) {
            existing.setEquity(price);
            existing.setBaseCurrency("USD");
            existing.setDataSource(assetKey);
            existing.setUpdatedAt(now);
            snapshotMapper.updateById(existing);
            return;
        }
        FcPortfolioPriceSnapshotEntity entity = new FcPortfolioPriceSnapshotEntity();
        entity.setUserId(userId);
        entity.setAsOfDate(date);
        entity.setEquity(price);
        entity.setBaseCurrency("USD");
        entity.setDataSource(assetKey);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        snapshotMapper.insert(entity);
    }

    private String resolveScriptPath() {
        Path base = Path.of(System.getProperty("user.dir"));
        Path direct = base.resolve("scripts").resolve("python").resolve("crawler.py");
        if (Files.exists(direct)) {
            return direct.toAbsolutePath().toString();
        }
        Path fromBackend = base.resolve("..").resolve("scripts").resolve("python").resolve("crawler.py").normalize();
        if (Files.exists(fromBackend)) {
            return fromBackend.toAbsolutePath().toString();
        }
        Path ps1 = base.resolve("scripts").resolve("python").resolve("run_crawler.ps1");
        if (Files.exists(ps1)) {
            return ps1.toAbsolutePath().toString();
        }
        Path ps1FromBackend = base.resolve("..").resolve("scripts").resolve("python").resolve("run_crawler.ps1").normalize();
        return ps1FromBackend.toAbsolutePath().toString();
    }

    private String normalizeMode(String mode) {
        if (mode == null) return null;
        String normalized = mode.trim().toUpperCase();
        if (DEFAULT_MODE.equals(normalized) || "REALTIME".equals(normalized)) {
            return normalized;
        }
        return null;
    }

    private FcSystemConfigEntity ensureModeConfig(Long actorUserId) {
        FcSystemConfigEntity existing = systemConfigMapper.selectOne(
                new LambdaQueryWrapper<FcSystemConfigEntity>()
                        .eq(FcSystemConfigEntity::getCfgKey, CFG_KEY_MODE)
                        .last("LIMIT 1"));
        if (existing != null) {
            if (existing.getCfgValue() == null || existing.getCfgValue().isBlank()) {
                existing.setCfgValue(DEFAULT_MODE);
                existing.setUpdatedBy(actorUserId);
                existing.setUpdatedAt(LocalDateTime.now());
                systemConfigMapper.updateById(existing);
            }
            return existing;
        }
        FcSystemConfigEntity created = new FcSystemConfigEntity();
        created.setCfgKey(CFG_KEY_MODE);
        created.setCfgValue(DEFAULT_MODE);
        created.setUpdatedBy(actorUserId);
        created.setUpdatedAt(LocalDateTime.now());
        systemConfigMapper.insert(created);
        return created;
    }

    private FcJobStatusEntity ensureJobStatus() {
        FcJobStatusEntity existing = jobStatusMapper.selectOne(
                new LambdaQueryWrapper<FcJobStatusEntity>()
                        .eq(FcJobStatusEntity::getJobName, JOB_NAME)
                        .last("LIMIT 1"));
        if (existing != null) {
            if (existing.getStatus() == null) {
                existing.setStatus(STATUS_STOPPED);
                existing.setUpdatedAt(LocalDateTime.now());
                jobStatusMapper.updateById(existing);
            }
            return existing;
        }
        FcJobStatusEntity created = new FcJobStatusEntity();
        created.setJobName(JOB_NAME);
        created.setStatus(STATUS_STOPPED);
        created.setUpdatedAt(LocalDateTime.now());
        jobStatusMapper.insert(created);
        return created;
    }

    private AdminJobStatusDTO toJobDto(FcJobStatusEntity entity) {
        if (entity == null) {
            AdminJobStatusDTO dto = new AdminJobStatusDTO();
            dto.setJobName(JOB_NAME);
            dto.setStatus(STATUS_STOPPED);
            return dto;
        }
        AdminJobStatusDTO dto = new AdminJobStatusDTO();
        dto.setJobName(entity.getJobName());
        dto.setStatus(entity.getStatus() == null ? STATUS_STOPPED : entity.getStatus());
        dto.setLastStartAt(formatTime(entity.getLastStartAt()));
        dto.setLastHeartbeatAt(formatTime(entity.getLastHeartbeatAt()));
        dto.setLastEndAt(formatTime(entity.getLastEndAt()));
        dto.setLastError(trimError(entity.getLastError()));
        dto.setLastLog(trimLog(entity.getLastLog()));
        dto.setUpdatedAt(formatTime(entity.getUpdatedAt()));
        return dto;
    }

    private String formatTime(LocalDateTime time) {
        return time == null ? null : time.toString();
    }

    private String trimError(String error) {
        if (error == null) return null;
        if (error.length() <= 500) return error;
        return error.substring(0, 500);
    }

    private String trimLog(String logValue) {
        if (logValue == null) return null;
        if (logValue.length() <= 2000) return logValue;
        return logValue.substring(logValue.length() - 2000);
    }

    private int ensureTickerMappings() {
        LocalDateTime now = LocalDateTime.now();
        List<String> keys = DEMO_MAPPINGS.stream().map(mapping -> mapping.assetKey).toList();
        List<FcTickerMappingEntity> existing = tickerMappingMapper.selectList(
                new QueryWrapper<FcTickerMappingEntity>().in("keyword", keys));
        List<String> existingKeys = existing.stream()
                .map(FcTickerMappingEntity::getKeyword)
                .filter(v -> v != null && !v.isBlank())
                .toList();
        int inserted = 0;
        for (DemoMapping mapping : DEMO_MAPPINGS) {
            if (existingKeys.contains(mapping.assetKey)) {
                continue;
            }
            FcTickerMappingEntity entry = buildMapping(
                    mapping.assetKey, mapping.ticker, mapping.market, mapping.priority, now);
            tickerMappingMapper.insert(entry);
            inserted++;
        }
        return inserted;
    }

    private FcTickerMappingEntity buildMapping(String keyword, String ticker, String market, int priority, LocalDateTime now) {
        FcTickerMappingEntity entity = new FcTickerMappingEntity();
        entity.setKeyword(keyword);
        entity.setTicker(ticker);
        entity.setMarket(market);
        entity.setPriority(priority);
        entity.setEnabled(1);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        return entity;
    }

    private FcPortfolioPriceSnapshotEntity findSnapshot(long userId, LocalDate date) {
        return snapshotMapper.selectOne(new QueryWrapper<FcPortfolioPriceSnapshotEntity>()
                .eq("user_id", userId)
                .eq("snap_date", date)
                .last("LIMIT 1"));
    }

    private static class CrawlerPriceLine {
        private String assetKey;
        private String date;
        private BigDecimal price;

        public String getAssetKey() {
            return assetKey;
        }

        public void setAssetKey(String assetKey) {
            this.assetKey = assetKey;
        }

        public String getDate() {
            return date;
        }

        public void setDate(String date) {
            this.date = date;
        }

        public BigDecimal getPrice() {
            return price;
        }

        public void setPrice(BigDecimal price) {
            this.price = price;
        }
    }

    private static class DemoMapping {
        private final String assetKey;
        private final String ticker;
        private final String market;
        private final int priority;

        private DemoMapping(String assetKey, String ticker, String market, int priority) {
            this.assetKey = assetKey;
            this.ticker = ticker;
            this.market = market;
            this.priority = priority;
        }
    }
}
