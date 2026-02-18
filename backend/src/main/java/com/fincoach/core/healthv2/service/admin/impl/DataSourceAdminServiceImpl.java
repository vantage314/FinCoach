package com.fincoach.core.healthv2.service.admin.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fincoach.core.healthv2.dto.admin.AdminDataSourceImportResultDTO;
import com.fincoach.core.healthv2.dto.admin.AdminDataSourceStatusDTO;
import com.fincoach.core.healthv2.dto.admin.AdminJobActionResultDTO;
import com.fincoach.core.healthv2.dto.admin.AdminJobStatusDTO;
import com.fincoach.core.healthv2.entity.FcPortfolioPriceSnapshotEntity;
import com.fincoach.core.healthv2.mapper.FcPortfolioPriceSnapshotMapper;
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
import java.util.List;

@Slf4j
@Service
public class DataSourceAdminServiceImpl implements DataSourceAdminService {

    private static final String CFG_KEY_MODE = "DATA_SOURCE_MODE";
    private static final String DEFAULT_MODE = "DEMO_DB";
    private static final String JOB_NAME = "PY_MARKET_CRAWLER";
    private static final String STATUS_RUNNING = "RUNNING";
    private static final String STATUS_STOPPED = "STOPPED";
    private static final int DEMO_DAYS = 20;

    private final FcSystemConfigMapper systemConfigMapper;
    private final FcJobStatusMapper jobStatusMapper;
    private final FcPortfolioPriceSnapshotMapper snapshotMapper;
    private final FcTickerMappingMapper tickerMappingMapper;

    public DataSourceAdminServiceImpl(FcSystemConfigMapper systemConfigMapper,
                                      FcJobStatusMapper jobStatusMapper,
                                      FcPortfolioPriceSnapshotMapper snapshotMapper,
                                      FcTickerMappingMapper tickerMappingMapper) {
        this.systemConfigMapper = systemConfigMapper;
        this.jobStatusMapper = jobStatusMapper;
        this.snapshotMapper = snapshotMapper;
        this.tickerMappingMapper = tickerMappingMapper;
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

        for (int i = 0; i < DEMO_DAYS; i++) {
            LocalDate date = start.plusDays(i);
            BigDecimal value = base.add(BigDecimal.valueOf(i * 520L))
                    .add(BigDecimal.valueOf((i % 5) * 230L));

            if (snapshotExists(userId, date)) {
                skipped++;
                continue;
            }

            FcPortfolioPriceSnapshotEntity entity = new FcPortfolioPriceSnapshotEntity();
            entity.setUserId(userId);
            entity.setAsOfDate(date);
            entity.setEquity(value);
            entity.setBaseCurrency("USD");
            entity.setDataSource(DEFAULT_MODE);
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
        LocalDateTime now = LocalDateTime.now();
        UpdateWrapper<FcJobStatusEntity> uw = new UpdateWrapper<>();
        uw.eq("job_name", JOB_NAME)
                .set("status", STATUS_RUNNING)
                .set("last_start_at", now)
                .set("last_heartbeat_at", now)
                .set("last_end_at", null)
                .set("last_error", null)
                .set("updated_at", now);
        jobStatusMapper.update(null, uw);

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

    private int ensureTickerMappings() {
        Long count = tickerMappingMapper.selectCount(new QueryWrapper<>());
        if (count != null && count > 0) {
            return 0;
        }
        LocalDateTime now = LocalDateTime.now();
        List<FcTickerMappingEntity> entries = List.of(
                buildMapping("STOCK", "SPY.US", "US", 100, now),
                buildMapping("BOND", "AGG.US", "US", 90, now),
                buildMapping("CASH", "CASH", "CASH", 80, now)
        );
        int inserted = 0;
        for (FcTickerMappingEntity entry : entries) {
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

    private boolean snapshotExists(long userId, LocalDate date) {
        Long count = snapshotMapper.selectCount(new QueryWrapper<FcPortfolioPriceSnapshotEntity>()
                .eq("user_id", userId)
                .eq("snap_date", date));
        return count != null && count > 0;
    }
}
