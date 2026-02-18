package com.fincoach.core.healthv2.service.admin.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fincoach.core.healthv2.dto.admin.AdminDataSourceImportResultDTO;
import com.fincoach.core.healthv2.dto.admin.AdminDataSourceStatusDTO;
import com.fincoach.core.healthv2.dto.admin.AdminJobActionResultDTO;
import com.fincoach.core.healthv2.dto.admin.AdminJobStatusDTO;
import com.fincoach.core.healthv2.dto.admin.AdminRealtimeHealthDTO;
import com.fincoach.core.healthv2.entity.FcPortfolioPriceSnapshotEntity;
import com.fincoach.core.healthv2.mapper.FcPortfolioPriceSnapshotMapper;
import com.fincoach.core.healthv2.service.admin.CrawlerRunRequest;
import com.fincoach.core.healthv2.service.admin.CrawlerRunResult;
import com.fincoach.core.healthv2.service.admin.CrawlerRunner;
import com.fincoach.core.healthv2.service.admin.DataSourceAdminService;
import com.fincoach.core.healthv2.util.LogLimiter;
import com.fincoach.core.repository.entity.FcJobStatusEntity;
import com.fincoach.core.repository.entity.FcSystemConfigEntity;
import com.fincoach.core.repository.mapper.FcJobStatusMapper;
import com.fincoach.core.repository.mapper.FcSystemConfigMapper;
import com.fincoach.core.ticker.entity.FcTickerMappingEntity;
import com.fincoach.core.ticker.mapper.FcTickerMappingMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
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
    private static final String CFG_KEY_CRAWLER_MODE = "CRAWLER_MODE";
    private static final String CFG_KEY_CRAWLER_INTERVAL_SECONDS = "CRAWLER_INTERVAL_SECONDS";
    private static final String CFG_KEY_CRAWLER_MAX_BATCHES = "CRAWLER_MAX_BATCHES";
    private static final String DEFAULT_MODE = "DEMO_DB";
    private static final String DEFAULT_CRAWLER_MODE = "DAEMON";
    private static final int DEFAULT_CRAWLER_INTERVAL_SECONDS = 10;
    private static final int DEFAULT_CRAWLER_MAX_BATCHES = 0;
    private static final String JOB_NAME = "PY_MARKET_CRAWLER";
    private static final String STATUS_RUNNING = "RUNNING";
    private static final String STATUS_STOPPED = "STOPPED";
    private static final String STATUS_STOPPING = "STOPPING";
    private static final String STATUS_FAILED = "FAILED";
    private static final int EVENT_RING_SIZE = 20;
    private static final String LEGACY_DEMO_KEY = "DEMO_DB";
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

    @Value("${crawler.staleThresholdSeconds:30}")
    private long staleThresholdSeconds = 30;

    @Value("${crawler.logMaxChars:4000}")
    private int logMaxChars = 4000;

    @Value("${crawler.selfHealEnabled:true}")
    private boolean selfHealEnabled = true;

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
        CrawlerConfig crawlerConfig = loadCrawlerConfig(actorUserId);

        AdminDataSourceStatusDTO dto = new AdminDataSourceStatusDTO();
        dto.setMode(config != null && config.getCfgValue() != null ? config.getCfgValue() : DEFAULT_MODE);
        dto.setCrawlerMode(crawlerConfig.mode);
        dto.setCrawlerIntervalSeconds(crawlerConfig.intervalSeconds);
        dto.setCrawlerMaxBatches(crawlerConfig.maxBatches);
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
        LocalDateTime now = LocalDateTime.now();
        cleanupLegacyDemoData(now);
        int insertedMappings = ensureTickerMappings();

        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(DEMO_DAYS - 1);

        int inserted = 0;
        int skipped = 0;
        BigDecimal base = new BigDecimal("100000.00");

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
    @Transactional
    public AdminJobActionResultDTO startRealtime(Long actorUserId) {
        ensureJobStatus();
        FcJobStatusEntity job = lockJobStatus();
        LocalDateTime now = LocalDateTime.now();
        String status = normalizeStatus(job != null ? job.getStatus() : null);
        boolean stale = isStale(job, now);
        if (STATUS_RUNNING.equals(status) && !stale) {
            AdminJobActionResultDTO dto = new AdminJobActionResultDTO();
            dto.setJobName(JOB_NAME);
            dto.setStatus(STATUS_RUNNING);
            dto.setMessage("already running");
            return dto;
        }
        if (STATUS_STOPPING.equals(status)) {
            AdminJobActionResultDTO dto = new AdminJobActionResultDTO();
            dto.setJobName(JOB_NAME);
            dto.setStatus(STATUS_STOPPING);
            dto.setMessage("stop in progress");
            return dto;
        }
        if (STATUS_RUNNING.equals(status) && stale) {
            String line = formatLogLine(now, "WARN", "STALE detected, auto-restarting");
            String merged = LogLimiter.appendAndTruncate(job != null ? job.getLastLog() : null, line, logMaxChars);
            UpdateWrapper<FcJobStatusEntity> staleUpdate = new UpdateWrapper<>();
            staleUpdate.eq("job_name", JOB_NAME)
                    .set("status", STATUS_STOPPED)
                    .set("last_end_at", now)
                    .set("last_log", merged)
                    .set("updated_at", now);
            jobStatusMapper.update(null, staleUpdate);
            recordEvent(job, now, "STALE_RESTART", "stale detected, auto-restarting", 1, 0, 1, false);
        }
        CrawlerConfig crawlerConfig = loadCrawlerConfig(actorUserId);
        AtomicBoolean stopSignal = new AtomicBoolean(false);
        crawlerStopSignal = stopSignal;
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

        crawlerFuture = crawlerExecutor.submit(() -> runCrawlerLoop(actorUserId, crawlerConfig, stopSignal));

        log.info("event=PY_CRAWLER_START userId={} job={}", actorUserId, JOB_NAME);
        recordEvent(job, now, "START", "start requested", 0, 0, 0, false);
        AdminJobActionResultDTO dto = new AdminJobActionResultDTO();
        dto.setJobName(JOB_NAME);
        dto.setStatus(STATUS_RUNNING);
        dto.setMessage("start requested");
        return dto;
    }

    @Override
    @Transactional
    public AdminJobActionResultDTO stopRealtime(Long actorUserId) {
        ensureJobStatus();
        FcJobStatusEntity job = lockJobStatus();
        String status = normalizeStatus(job != null ? job.getStatus() : null);
        if (STATUS_STOPPED.equals(status)) {
            AdminJobActionResultDTO dto = new AdminJobActionResultDTO();
            dto.setJobName(JOB_NAME);
            dto.setStatus(STATUS_STOPPED);
            dto.setMessage("already stopped");
            return dto;
        }
        if (STATUS_STOPPING.equals(status)) {
            AdminJobActionResultDTO dto = new AdminJobActionResultDTO();
            dto.setJobName(JOB_NAME);
            dto.setStatus(STATUS_STOPPING);
            dto.setMessage("stop in progress");
            return dto;
        }
        AtomicBoolean signal = crawlerStopSignal;
        if (signal != null) {
            signal.set(true);
        }
        LocalDateTime now = LocalDateTime.now();
        boolean hasRunningFuture = crawlerFuture != null && !crawlerFuture.isDone();
        if (hasRunningFuture) {
            crawlerFuture.cancel(true);
            UpdateWrapper<FcJobStatusEntity> uw = new UpdateWrapper<>();
            uw.eq("job_name", JOB_NAME)
                    .set("status", STATUS_STOPPING)
                    .set("updated_at", now);
            jobStatusMapper.update(null, uw);
            recordEvent(job, now, "STOP_REQUEST", "stop requested", 0, 0, 0, false);
        } else {
            UpdateWrapper<FcJobStatusEntity> uw = new UpdateWrapper<>();
            uw.eq("job_name", JOB_NAME)
                    .set("status", STATUS_STOPPED)
                    .set("last_end_at", now)
                    .set("updated_at", now);
            jobStatusMapper.update(null, uw);
            recordEvent(job, now, "STOPPED", "stopped", 0, 0, 0, false);
        }

        log.info("event=PY_CRAWLER_STOP userId={} job={}", actorUserId, JOB_NAME);
        AdminJobActionResultDTO dto = new AdminJobActionResultDTO();
        dto.setJobName(JOB_NAME);
        dto.setStatus(hasRunningFuture ? STATUS_STOPPING : STATUS_STOPPED);
        dto.setMessage("stop requested");
        return dto;
    }

    @Override
    public AdminRealtimeHealthDTO getRealtimeHealth(Long actorUserId) {
        FcJobStatusEntity job = ensureJobStatus();
        LocalDateTime now = LocalDateTime.now();
        AdminRealtimeHealthDTO dto = new AdminRealtimeHealthDTO();
        if (job == null) {
            dto.setStatus(STATUS_STOPPED);
            dto.setStale(false);
            return dto;
        }
        dto.setStatus(normalizeStatus(job.getStatus()));
        dto.setLastHeartbeatAt(formatTime(job.getLastHeartbeatAt()));
        dto.setSecondsSinceHeartbeat(secondsSinceHeartbeat(job, now));
        dto.setLastStartAt(formatTime(job.getLastStartAt()));
        dto.setLastEndAt(formatTime(job.getLastEndAt()));
        dto.setStale(isStale(job, now));
        return dto;
    }

    @Override
    @Transactional
    public AdminJobActionResultDTO recoverRealtime(Long actorUserId) {
        ensureJobStatus();
        FcJobStatusEntity job = lockJobStatus();
        String status = normalizeStatus(job != null ? job.getStatus() : null);
        if (STATUS_STOPPING.equals(status)) {
            AdminJobActionResultDTO dto = new AdminJobActionResultDTO();
            dto.setJobName(JOB_NAME);
            dto.setStatus(STATUS_STOPPING);
            dto.setMessage("stop in progress");
            return dto;
        }
        recordEvent(job, LocalDateTime.now(), "MANUAL_RECOVER", "manual recover requested", 0, 1, 0, false);
        return startRealtime(actorUserId);
    }

    private boolean isCrawlerRunning() {
        return crawlerFuture != null && !crawlerFuture.isDone();
    }

    private void runCrawlerLoop(Long actorUserId, CrawlerConfig crawlerConfig, AtomicBoolean stopSignal) {
        long userId = actorUserId != null ? actorUserId : 1L;
        String scriptPath = resolveScriptPath();
        int batches = 0;
        try {
            while (!stopSignal.get()) {
                batches++;
                CrawlerRunResult result = runCrawlerBatch(userId, scriptPath, stopSignal);
                updateJobHeartbeat(LocalDateTime.now());
                if (!result.isSuccess()) {
                    if (stopSignal.get()) {
                        break;
                    }
                    updateJobFailed(result.getError());
                    return;
                }
                if (crawlerConfig.shouldStopAfterBatch(batches)) {
                    break;
                }
                if (crawlerConfig.intervalSeconds > 0) {
                    try {
                        Thread.sleep(crawlerConfig.intervalSeconds * 1000L);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
            updateJobStopped();
        } catch (Exception e) {
            updateJobFailed(e.getMessage());
        }
    }

    private CrawlerRunResult runCrawlerBatch(long userId, String scriptPath, AtomicBoolean stopSignal) {
        List<String> logLines = new ArrayList<>();
        Consumer<String> lineConsumer = (line) -> {
            if (line == null || line.isBlank()) return;
            appendLogLine(logLines, line);
            String aggregated = String.join("\n", logLines);
            updateJobLog(aggregated, LocalDateTime.now());
            handleCrawlerLine(line, userId);
        };
        CrawlerRunResult result = crawlerRunner.run(new CrawlerRunRequest(scriptPath), lineConsumer, stopSignal);
        log.info("event=PY_CRAWLER_BATCH userId={} success={} lines={}",
                userId, result.isSuccess(), result.getLines());
        return result;
    }

    private void updateJobLog(String logValue, LocalDateTime now) {
        String merged = LogLimiter.appendAndTruncate(null, logValue, logMaxChars);
        UpdateWrapper<FcJobStatusEntity> uw = new UpdateWrapper<>();
        uw.eq("job_name", JOB_NAME)
                .set("last_log", merged)
                .set("last_heartbeat_at", now)
                .set("updated_at", now);
        jobStatusMapper.update(null, uw);
    }

    private void updateJobHeartbeat(LocalDateTime now) {
        UpdateWrapper<FcJobStatusEntity> uw = new UpdateWrapper<>();
        uw.eq("job_name", JOB_NAME)
                .set("last_heartbeat_at", now)
                .set("updated_at", now);
        jobStatusMapper.update(null, uw);
    }

    private void updateJobStopped() {
        LocalDateTime now = LocalDateTime.now();
        UpdateWrapper<FcJobStatusEntity> uw = new UpdateWrapper<>();
        uw.eq("job_name", JOB_NAME)
                .set("status", STATUS_STOPPED)
                .set("last_end_at", now)
                .set("updated_at", now);
        jobStatusMapper.update(null, uw);
        FcJobStatusEntity job = jobStatusMapper.selectOne(
                new LambdaQueryWrapper<FcJobStatusEntity>()
                        .eq(FcJobStatusEntity::getJobName, JOB_NAME)
                        .last("LIMIT 1"));
        recordEvent(job, now, "STOPPED", "stopped", 0, 0, 0, false);
    }

    private void updateJobFailed(String error) {
        LocalDateTime now = LocalDateTime.now();
        UpdateWrapper<FcJobStatusEntity> uw = new UpdateWrapper<>();
        uw.eq("job_name", JOB_NAME)
                .set("status", STATUS_FAILED)
                .set("last_end_at", now)
                .set("last_error", trimError(error))
                .set("last_error_at", now)
                .set("updated_at", now);
        jobStatusMapper.update(null, uw);
        FcJobStatusEntity job = jobStatusMapper.selectOne(
                new LambdaQueryWrapper<FcJobStatusEntity>()
                        .eq(FcJobStatusEntity::getJobName, JOB_NAME)
                        .last("LIMIT 1"));
        recordEvent(job, now, "FAILED", trimError(error), 0, 0, 0, true);
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

    private CrawlerConfig loadCrawlerConfig(Long actorUserId) {
        String modeRaw = ensureConfigValue(CFG_KEY_CRAWLER_MODE, DEFAULT_CRAWLER_MODE, actorUserId);
        String mode = normalizeCrawlerMode(modeRaw);
        if (mode == null) {
            mode = DEFAULT_CRAWLER_MODE;
        }
        String intervalRaw = ensureConfigValue(CFG_KEY_CRAWLER_INTERVAL_SECONDS,
                String.valueOf(DEFAULT_CRAWLER_INTERVAL_SECONDS), actorUserId);
        String maxBatchesRaw = ensureConfigValue(CFG_KEY_CRAWLER_MAX_BATCHES,
                String.valueOf(DEFAULT_CRAWLER_MAX_BATCHES), actorUserId);

        int intervalSeconds = parseInt(intervalRaw, DEFAULT_CRAWLER_INTERVAL_SECONDS);
        if (intervalSeconds <= 0) {
            intervalSeconds = DEFAULT_CRAWLER_INTERVAL_SECONDS;
        }
        int maxBatches = parseInt(maxBatchesRaw, DEFAULT_CRAWLER_MAX_BATCHES);
        if (maxBatches < 0) {
            maxBatches = DEFAULT_CRAWLER_MAX_BATCHES;
        }
        if ("RUN_ONCE".equals(mode)) {
            maxBatches = 1;
        }
        return new CrawlerConfig(mode, intervalSeconds, maxBatches);
    }

    private String normalizeCrawlerMode(String mode) {
        if (mode == null) return null;
        String normalized = mode.trim().toUpperCase();
        if ("RUN_ONCE".equals(normalized) || "DAEMON".equals(normalized)) {
            return normalized;
        }
        return null;
    }

    private int parseInt(String value, int defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception ignored) {
            return defaultValue;
        }
    }

    private String ensureConfigValue(String key, String defaultValue, Long actorUserId) {
        FcSystemConfigEntity existing = systemConfigMapper.selectOne(
                new LambdaQueryWrapper<FcSystemConfigEntity>()
                        .eq(FcSystemConfigEntity::getCfgKey, key)
                        .last("LIMIT 1"));
        if (existing != null && existing.getCfgValue() != null && !existing.getCfgValue().isBlank()) {
            return existing.getCfgValue();
        }
        LocalDateTime now = LocalDateTime.now();
        if (existing == null) {
            FcSystemConfigEntity created = new FcSystemConfigEntity();
            created.setCfgKey(key);
            created.setCfgValue(defaultValue);
            created.setUpdatedBy(actorUserId);
            created.setUpdatedAt(now);
            systemConfigMapper.insert(created);
        } else {
            UpdateWrapper<FcSystemConfigEntity> uw = new UpdateWrapper<>();
            uw.eq("cfg_key", key)
                    .set("cfg_value", defaultValue)
                    .set("updated_by", actorUserId)
                    .set("updated_at", now);
            systemConfigMapper.update(null, uw);
        }
        return defaultValue;
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
        LocalDateTime now = LocalDateTime.now();
        if (entity == null) {
            AdminJobStatusDTO dto = new AdminJobStatusDTO();
            dto.setJobName(JOB_NAME);
            dto.setStatus(STATUS_STOPPED);
            dto.setStale(false);
            return dto;
        }
        AdminJobStatusDTO dto = new AdminJobStatusDTO();
        dto.setJobName(entity.getJobName());
        String status = normalizeStatus(entity.getStatus());
        dto.setStatus(status);
        dto.setLastStartAt(formatTime(entity.getLastStartAt()));
        dto.setLastHeartbeatAt(formatTime(entity.getLastHeartbeatAt()));
        dto.setLastEndAt(formatTime(entity.getLastEndAt()));
        dto.setLastError(trimError(entity.getLastError()));
        dto.setLastLog(trimLog(entity.getLastLog()));
        dto.setUpdatedAt(formatTime(entity.getUpdatedAt()));
        Long seconds = secondsSinceHeartbeat(entity, now);
        dto.setSecondsSinceHeartbeat(seconds);
        dto.setStale(isStale(entity, now));
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
        return LogLimiter.truncate(logValue, logMaxChars);
    }

    private String formatLogLine(LocalDateTime now, String level, String message) {
        String normalized = level == null ? "INFO" : level.toUpperCase();
        String content = message == null ? "" : message;
        return now + " [" + normalized + "] " + content;
    }

    private String mergeLog(String existing, String line) {
        if (line == null || line.isBlank()) {
            return existing;
        }
        if (existing == null || existing.isBlank()) {
            return line;
        }
        return existing + "\n" + line;
    }

    private void recordEvent(FcJobStatusEntity job, LocalDateTime now, String type, String message,
                             int staleInc, int recoverInc, int restartInc, boolean setLastErrorAt) {
        List<CrawlerEvent> events = parseEvents(job != null ? job.getRecentEventsJson() : null);
        events.add(new CrawlerEvent(now.toString(), type, message));
        if (events.size() > EVENT_RING_SIZE) {
            events = events.subList(events.size() - EVENT_RING_SIZE, events.size());
        }
        String eventsJson = writeEvents(events);
        String logLine = formatLogLine(now, type, message);
        String mergedLog = LogLimiter.appendAndTruncate(job != null ? job.getLastLog() : null, logLine, logMaxChars);
        UpdateWrapper<FcJobStatusEntity> uw = new UpdateWrapper<>();
        uw.eq("job_name", JOB_NAME)
                .set("recent_events_json", eventsJson)
                .set("last_log", mergedLog)
                .set("updated_at", now);
        if (staleInc != 0) {
            uw.set("stale_count", safeCount(job != null ? job.getStaleCount() : null) + staleInc);
        }
        if (recoverInc != 0) {
            uw.set("recover_count", safeCount(job != null ? job.getRecoverCount() : null) + recoverInc);
        }
        if (restartInc != 0) {
            uw.set("restart_count", safeCount(job != null ? job.getRestartCount() : null) + restartInc);
        }
        if (setLastErrorAt) {
            uw.set("last_error_at", now);
        }
        jobStatusMapper.update(null, uw);
    }

    private int safeCount(Integer value) {
        return value == null ? 0 : value;
    }

    private List<CrawlerEvent> parseEvents(String json) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<CrawlerEvent>>() {});
        } catch (Exception ignored) {
            return new ArrayList<>();
        }
    }

    private String writeEvents(List<CrawlerEvent> events) {
        try {
            return objectMapper.writeValueAsString(events);
        } catch (Exception ignored) {
            return "[]";
        }
    }

    private FcJobStatusEntity lockJobStatus() {
        FcJobStatusEntity locked = jobStatusMapper.selectForUpdate(JOB_NAME);
        if (locked != null) {
            return locked;
        }
        ensureJobStatus();
        return jobStatusMapper.selectForUpdate(JOB_NAME);
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return STATUS_STOPPED;
        }
        String normalized = status.trim().toUpperCase();
        if (STATUS_RUNNING.equals(normalized)
                || STATUS_STOPPED.equals(normalized)
                || STATUS_STOPPING.equals(normalized)
                || STATUS_FAILED.equals(normalized)) {
            return normalized;
        }
        return STATUS_STOPPED;
    }

    private boolean isStale(FcJobStatusEntity entity, LocalDateTime now) {
        if (entity == null) {
            return false;
        }
        String status = normalizeStatus(entity.getStatus());
        if (!STATUS_RUNNING.equals(status)) {
            return false;
        }
        LocalDateTime heartbeat = entity.getLastHeartbeatAt();
        if (heartbeat == null) {
            return true;
        }
        long seconds = Duration.between(heartbeat, now).getSeconds();
        return seconds > staleThresholdSeconds;
    }

    private Long secondsSinceHeartbeat(FcJobStatusEntity entity, LocalDateTime now) {
        if (entity == null || entity.getLastHeartbeatAt() == null) {
            return null;
        }
        return Duration.between(entity.getLastHeartbeatAt(), now).getSeconds();
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

    private void cleanupLegacyDemoData(LocalDateTime now) {
        snapshotMapper.delete(new QueryWrapper<FcPortfolioPriceSnapshotEntity>()
                .eq("source", LEGACY_DEMO_KEY));
        UpdateWrapper<FcTickerMappingEntity> uw = new UpdateWrapper<>();
        uw.eq("keyword", LEGACY_DEMO_KEY)
                .set("enabled", 0)
                .set("updated_at", now);
        tickerMappingMapper.update(null, uw);
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

    private static class CrawlerConfig {
        private final String mode;
        private final int intervalSeconds;
        private final int maxBatches;

        private CrawlerConfig(String mode, int intervalSeconds, int maxBatches) {
            this.mode = mode;
            this.intervalSeconds = intervalSeconds;
            this.maxBatches = maxBatches;
        }

        private boolean shouldStopAfterBatch(int batches) {
            if ("RUN_ONCE".equals(mode)) {
                return true;
            }
            if (maxBatches > 0) {
                return batches >= maxBatches;
            }
            return false;
        }
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

    private static class CrawlerEvent {
        private String ts;
        private String type;
        private String msg;

        private CrawlerEvent(String ts, String type, String msg) {
            this.ts = ts;
            this.type = type;
            this.msg = msg;
        }

        public String getTs() {
            return ts;
        }

        public void setTs(String ts) {
            this.ts = ts;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getMsg() {
            return msg;
        }

        public void setMsg(String msg) {
            this.msg = msg;
        }
    }
}
