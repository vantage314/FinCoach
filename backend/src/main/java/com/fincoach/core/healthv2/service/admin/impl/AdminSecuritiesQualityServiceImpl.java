package com.fincoach.core.healthv2.service.admin.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fincoach.core.healthv2.dto.admin.AdminSecuritiesQualityAnomalyDTO;
import com.fincoach.core.healthv2.dto.admin.AdminSecuritiesQualityCoverageDTO;
import com.fincoach.core.healthv2.dto.admin.AdminSecuritiesQualityDTO;
import com.fincoach.core.healthv2.dto.admin.AdminSecuritiesQualityMissingMappingDTO;
import com.fincoach.core.healthv2.entity.FcPortfolioPriceSnapshotEntity;
import com.fincoach.core.healthv2.mapper.FcPortfolioPriceSnapshotMapper;
import com.fincoach.core.healthv2.service.admin.AdminSecuritiesQualityService;
import com.fincoach.core.repository.entity.FcSystemConfigEntity;
import com.fincoach.core.repository.mapper.FcSystemConfigMapper;
import com.fincoach.core.ticker.entity.FcTickerMappingEntity;
import com.fincoach.core.ticker.mapper.FcTickerMappingMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AdminSecuritiesQualityServiceImpl implements AdminSecuritiesQualityService {

    private static final String CFG_SNAPSHOT_MIN_DAYS = "SNAPSHOT_MIN_DAYS";
    private static final String CFG_SNAPSHOT_MAX_LAG_DAYS = "SNAPSHOT_MAX_LAG_DAYS";
    private static final String CFG_ANOMALY_CHANGE_PCT = "ANOMALY_CHANGE_PCT";

    private static final long DEFAULT_MIN_DAYS = 20;
    private static final long DEFAULT_MAX_LAG_DAYS = 3;
    private static final double DEFAULT_ANOMALY_PCT = 0.2;
    private static final int MAX_ANOMALIES = 50;

    private final FcTickerMappingMapper mappingMapper;
    private final FcPortfolioPriceSnapshotMapper snapshotMapper;
    private final FcSystemConfigMapper systemConfigMapper;

    public AdminSecuritiesQualityServiceImpl(FcTickerMappingMapper mappingMapper,
                                             FcPortfolioPriceSnapshotMapper snapshotMapper,
                                             FcSystemConfigMapper systemConfigMapper) {
        this.mappingMapper = mappingMapper;
        this.snapshotMapper = snapshotMapper;
        this.systemConfigMapper = systemConfigMapper;
    }

    @Override
    public AdminSecuritiesQualityDTO evaluate() {
        long minDays = readLongConfig(CFG_SNAPSHOT_MIN_DAYS, DEFAULT_MIN_DAYS);
        long maxLagDays = readLongConfig(CFG_SNAPSHOT_MAX_LAG_DAYS, DEFAULT_MAX_LAG_DAYS);
        double anomalyThreshold = readDoubleConfig(CFG_ANOMALY_CHANGE_PCT, DEFAULT_ANOMALY_PCT);

        List<FcPortfolioPriceSnapshotEntity> snapshots = snapshotMapper.selectList(
                new QueryWrapper<FcPortfolioPriceSnapshotEntity>()
                        .orderByAsc("source", "snap_date"));

        Set<String> assetKeys = new LinkedHashSet<>();
        Set<LocalDate> dates = new LinkedHashSet<>();
        Map<String, List<FcPortfolioPriceSnapshotEntity>> byAsset = new LinkedHashMap<>();

        for (FcPortfolioPriceSnapshotEntity snap : snapshots) {
            if (snap == null) continue;
            String rawKey = snap.getDataSource();
            String key = normalizeKey(rawKey);
            if (key.isBlank()) continue;
            assetKeys.add(key);
            if (snap.getAsOfDate() != null) {
                dates.add(snap.getAsOfDate());
            }
            byAsset.computeIfAbsent(key, k -> new ArrayList<>()).add(snap);
        }

        LocalDate latestDate = dates.stream().max(Comparator.naturalOrder()).orElse(null);
        long lagDays = latestDate == null ? 0 : ChronoUnit.DAYS.between(latestDate, LocalDate.now());
        if (lagDays < 0) {
            lagDays = 0;
        }

        AdminSecuritiesQualityCoverageDTO coverage = new AdminSecuritiesQualityCoverageDTO();
        coverage.setAssetsCount(assetKeys.size());
        coverage.setDaysCovered(dates.size());
        coverage.setLatestDate(latestDate == null ? null : latestDate.toString());
        coverage.setLagDays(lagDays);
        coverage.setIssues(buildCoverageIssues(assetKeys, dates, latestDate, lagDays, minDays, maxLagDays));

        List<AdminSecuritiesQualityMissingMappingDTO> missingMappings = buildMissingMappings(assetKeys);
        List<AdminSecuritiesQualityAnomalyDTO> anomalies = detectAnomalies(byAsset, anomalyThreshold);

        AdminSecuritiesQualityDTO dto = new AdminSecuritiesQualityDTO();
        dto.setMissingMappings(missingMappings);
        dto.setSnapshotCoverage(coverage);
        dto.setAnomalies(anomalies);
        dto.setRecommendations(buildRecommendations(missingMappings, coverage, anomalies));
        return dto;
    }

    private List<AdminSecuritiesQualityMissingMappingDTO> buildMissingMappings(Set<String> assetKeys) {
        List<AdminSecuritiesQualityMissingMappingDTO> missing = new ArrayList<>();
        if (assetKeys == null || assetKeys.isEmpty()) {
            return missing;
        }

        List<FcTickerMappingEntity> mappings = mappingMapper.selectList(
                new QueryWrapper<FcTickerMappingEntity>()
                        .select("keyword", "enabled")
                        .in("keyword", assetKeys)
                        .eq("enabled", 1));
        Set<String> mapped = mappings.stream()
                .map(FcTickerMappingEntity::getKeyword)
                .filter(v -> v != null && !v.isBlank())
                .map(this::normalizeKey)
                .collect(Collectors.toSet());

        for (String key : assetKeys) {
            if (!mapped.contains(key)) {
                AdminSecuritiesQualityMissingMappingDTO item = new AdminSecuritiesQualityMissingMappingDTO();
                item.setAssetKey(key);
                item.setReason("缺少 ticker 映射");
                missing.add(item);
            }
        }
        return missing;
    }

    private List<AdminSecuritiesQualityAnomalyDTO> detectAnomalies(Map<String, List<FcPortfolioPriceSnapshotEntity>> byAsset,
                                                                   double threshold) {
        List<AdminSecuritiesQualityAnomalyDTO> anomalies = new ArrayList<>();
        if (byAsset == null || byAsset.isEmpty()) {
            return anomalies;
        }
        double absThreshold = Math.abs(threshold);

        for (Map.Entry<String, List<FcPortfolioPriceSnapshotEntity>> entry : byAsset.entrySet()) {
            List<FcPortfolioPriceSnapshotEntity> series = entry.getValue();
            if (series == null || series.size() < 2) continue;
            series.sort(Comparator.comparing(FcPortfolioPriceSnapshotEntity::getAsOfDate,
                    Comparator.nullsLast(Comparator.naturalOrder())));
            for (int i = 1; i < series.size(); i++) {
                FcPortfolioPriceSnapshotEntity prev = series.get(i - 1);
                FcPortfolioPriceSnapshotEntity curr = series.get(i);
                if (prev == null || curr == null) continue;
                if (prev.getAsOfDate() == null || curr.getAsOfDate() == null) continue;
                BigDecimal prevValue = prev.getEquity();
                BigDecimal currValue = curr.getEquity();
                if (prevValue == null || currValue == null) continue;
                if (prevValue.compareTo(BigDecimal.ZERO) == 0) continue;

                double changePct = currValue.subtract(prevValue)
                        .divide(prevValue, 6, RoundingMode.HALF_UP)
                        .doubleValue();
                if (Math.abs(changePct) >= absThreshold) {
                    AdminSecuritiesQualityAnomalyDTO anomaly = new AdminSecuritiesQualityAnomalyDTO();
                    anomaly.setAssetKey(entry.getKey());
                    anomaly.setDate(curr.getAsOfDate() == null ? null : curr.getAsOfDate().toString());
                    anomaly.setChangePct(changePct);
                    anomaly.setReason(String.format("单日波动超过阈值 %.2f%%", absThreshold * 100));
                    anomalies.add(anomaly);
                    if (anomalies.size() >= MAX_ANOMALIES) {
                        return anomalies;
                    }
                }
            }
        }
        return anomalies;
    }

    private List<String> buildCoverageIssues(Set<String> assetKeys,
                                             Set<LocalDate> dates,
                                             LocalDate latestDate,
                                             long lagDays,
                                             long minDays,
                                             long maxLagDays) {
        List<String> issues = new ArrayList<>();
        if (dates == null || dates.isEmpty()) {
            issues.add("没有快照数据");
        }
        if (assetKeys == null || assetKeys.isEmpty()) {
            issues.add("没有可用资产标识");
        }
        long covered = dates == null ? 0 : dates.size();
        if (covered > 0 && covered < minDays) {
            issues.add(String.format("覆盖天数不足（当前 %d 天，要求 ≥ %d 天）", covered, minDays));
        }
        if (latestDate != null && lagDays > maxLagDays) {
            issues.add(String.format("最新快照滞后 %d 天（要求 ≤ %d 天）", lagDays, maxLagDays));
        }
        return issues;
    }

    private List<String> buildRecommendations(List<AdminSecuritiesQualityMissingMappingDTO> missingMappings,
                                              AdminSecuritiesQualityCoverageDTO coverage,
                                              List<AdminSecuritiesQualityAnomalyDTO> anomalies) {
        List<String> recs = new ArrayList<>();
        if (missingMappings != null && !missingMappings.isEmpty()) {
            recs.add("补齐 ticker mapping（可前往 证券映射 页面）");
        }
        if (coverage != null) {
            boolean hasNoSnapshots = coverage.getIssues() != null && coverage.getIssues().stream().anyMatch(v -> v.contains("没有快照数据"));
            boolean hasCoverageGap = coverage.getIssues() != null && coverage.getIssues().stream().anyMatch(v -> v.contains("覆盖天数不足"));
            boolean hasLag = coverage.getIssues() != null && coverage.getIssues().stream().anyMatch(v -> v.contains("滞后"));
            if (hasNoSnapshots || hasCoverageGap) {
                recs.add("导入演示数据或启动抓取以补齐行情快照");
            }
            if (hasLag) {
                recs.add("检查数据源刷新任务，确保快照每日更新");
            }
        }
        if (anomalies != null && !anomalies.isEmpty()) {
            recs.add("复核异常波动资产，确认行情来源");
        }
        if (recs.isEmpty()) {
            recs.add("数据质量正常，可继续观察");
        }
        return recs;
    }

    private long readLongConfig(String key, long defaultValue) {
        FcSystemConfigEntity config = systemConfigMapper.selectOne(
                new QueryWrapper<FcSystemConfigEntity>().eq("cfg_key", key).last("LIMIT 1"));
        if (config == null || config.getCfgValue() == null || config.getCfgValue().isBlank()) {
            return defaultValue;
        }
        try {
            return Long.parseLong(config.getCfgValue().trim());
        } catch (Exception ignored) {
            return defaultValue;
        }
    }

    private double readDoubleConfig(String key, double defaultValue) {
        FcSystemConfigEntity config = systemConfigMapper.selectOne(
                new QueryWrapper<FcSystemConfigEntity>().eq("cfg_key", key).last("LIMIT 1"));
        if (config == null || config.getCfgValue() == null || config.getCfgValue().isBlank()) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(config.getCfgValue().trim());
        } catch (Exception ignored) {
            return defaultValue;
        }
    }

    private String normalizeKey(String value) {
        if (value == null) return "";
        return value.trim().toUpperCase();
    }
}
