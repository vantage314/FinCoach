package com.fincoach.core.healthv2.service.admin.impl;

import com.fincoach.core.healthv2.dto.admin.AdminDashboardStatsQueryDTO;
import com.fincoach.core.healthv2.mapper.admin.AdminDashboardStatsMapper;
import com.fincoach.core.healthv2.service.admin.AdminDashboardStatsService;
import com.fincoach.core.healthv2.vo.admin.AdminDashboardStatsVO;
import com.fincoach.core.healthv2.vo.admin.BucketVO;
import com.fincoach.core.healthv2.vo.admin.RecentAlertVO;
import com.fincoach.core.healthv2.vo.admin.RecentReportVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AdminDashboardStatsServiceImpl implements AdminDashboardStatsService {

    @Autowired
    private AdminDashboardStatsMapper statsMapper;

    @Override
    public AdminDashboardStatsVO getStats(AdminDashboardStatsQueryDTO query, Long actorUserId) {
        int days = (query != null && query.getWindowDays() != null) ? query.getWindowDays() : 30;
        int size = (query != null && query.getRecentSize() != null) ? query.getRecentSize() : 10;

        // Constraint check
        if (days < 7) days = 7;
        if (days > 365) days = 365;
        if (size < 1) size = 1;
        if (size > 50) size = 50;

        // 1. Overview
        long userCount = 0;
        long reportCount = 0;
        long alertCount = 0;
        try {
            userCount = statsMapper.countUsers();
            reportCount = statsMapper.countReports();
            alertCount = statsMapper.countAlertsInDays(days);
        } catch (Exception e) {
            log.error("[AdminStats] Failed to fetch counts", e);
        }

        // 2. Score Distribution
        List<BucketVO> healthBuckets = new ArrayList<>();
        List<BucketVO> riskBuckets = new ArrayList<>();
        try {
            List<Integer> healthScores = statsMapper.listHealthScoresInDays(days);
            List<Integer> riskScores = statsMapper.listRiskScoresInDays(days);
            healthBuckets = buildBuckets(healthScores);
            riskBuckets = buildBuckets(riskScores);
        } catch (Exception e) {
            log.error("[AdminStats] Failed to fetch scores", e);
            // safe fallback to empty buckets
            healthBuckets = buildBuckets(new ArrayList<>());
            riskBuckets = buildBuckets(new ArrayList<>());
        }

        // 3. Recent
        List<RecentReportVO> recentReports = new ArrayList<>();
        List<RecentAlertVO> recentAlerts = new ArrayList<>();
        try {
            recentReports = statsMapper.listRecentReports(size);
            recentAlerts = statsMapper.listRecentAlerts(days, size);
        } catch (Exception e) {
            log.error("[AdminStats] Failed to fetch recent items", e);
        }

        AdminDashboardStatsVO.Overview overview = AdminDashboardStatsVO.Overview.builder()
                .userCount(userCount)
                .reportCount(reportCount)
                .alertTriggerCount(alertCount)
                .notificationUnreadCount(0L) // Not required in specs
                .build();

        AdminDashboardStatsVO.ScoreDistribution distribution = AdminDashboardStatsVO.ScoreDistribution.builder()
                .healthScoreBuckets(healthBuckets)
                .riskScoreBuckets(riskBuckets)
                .build();

        AdminDashboardStatsVO.Recent recent = AdminDashboardStatsVO.Recent.builder()
                .recentReports(recentReports)
                .recentAlerts(recentAlerts)
                .build();

        AdminDashboardStatsVO.Meta meta = AdminDashboardStatsVO.Meta.builder()
                .generatedAt(LocalDateTime.now())
                .windowDays(days)
                .build();

        return AdminDashboardStatsVO.builder()
                .overview(overview)
                .scoreDistribution(distribution)
                .recent(recent)
                .meta(meta)
                .build();
    }

    private List<BucketVO> buildBuckets(List<Integer> scores) {
        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("0-39", 0L);
        counts.put("40-59", 0L);
        counts.put("60-79", 0L);
        counts.put("80-100", 0L);

        if (scores != null) {
            for (Integer score : scores) {
                if (score == null) continue;
                if (score >= 0 && score <= 39) {
                    counts.put("0-39", counts.get("0-39") + 1);
                } else if (score >= 40 && score <= 59) {
                    counts.put("40-59", counts.get("40-59") + 1);
                } else if (score >= 60 && score <= 79) {
                    counts.put("60-79", counts.get("60-79") + 1);
                } else if (score >= 80 && score <= 100) {
                    counts.put("80-100", counts.get("80-100") + 1);
                }
            }
        }

        return counts.entrySet().stream()
                .map(entry -> BucketVO.builder()
                        .label(entry.getKey())
                        .count(entry.getValue())
                        .build())
                .collect(Collectors.toList());
    }
}
