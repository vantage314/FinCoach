package com.fincoach.core.healthv2.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fincoach.core.healthv2.dto.HealthReportTrendPointVO;
import com.fincoach.core.healthv2.dto.HealthReportTrendVO;
import com.fincoach.core.healthv2.entity.FcBehaviorEventEntity;
import com.fincoach.core.healthv2.entity.FcHealthReportEntity;
import com.fincoach.core.healthv2.mapper.FcBehaviorEventMapper;
import com.fincoach.core.healthv2.mapper.FcHealthReportMapper;
import com.fincoach.core.healthv2.service.HealthReportTrendService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class HealthReportTrendServiceImpl implements HealthReportTrendService {

    @Autowired
    private FcHealthReportMapper reportMapper;
    @Autowired
    private FcBehaviorEventMapper behaviorEventMapper;
    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public HealthReportTrendVO getTrend(Long userId, int points) {
        List<FcHealthReportEntity> rows = reportMapper.selectList(
                new LambdaQueryWrapper<FcHealthReportEntity>()
                        .eq(FcHealthReportEntity::getUserId, userId)
                        .orderByDesc(FcHealthReportEntity::getReportDate)
                        .last("LIMIT " + points)
        );

        List<HealthReportTrendPointVO> reportPoints = new ArrayList<>();
        for (FcHealthReportEntity row : rows) {
            HealthReportTrendPointVO point = new HealthReportTrendPointVO();
            point.setReportId(row.getId());
            point.setReportDate(row.getReportDate());

            Map<String, Integer> scores = new LinkedHashMap<>();
            scores.put("health", row.getHealthScore());
            scores.put("risk", row.getRiskScore());
            scores.put("behavior", row.getBehaviorScore());
            point.setScores(scores);

            point.setMetrics(extractMetrics(row.getMetricsJson()));
            reportPoints.add(point);
        }
        Collections.reverse(reportPoints);

        HealthReportTrendVO vo = new HealthReportTrendVO();
        vo.setPoints(points);
        vo.setReports(reportPoints);
        vo.setTrend(buildTrend(reportPoints));
        vo.setExecution(buildExecution(userId));
        return vo;
    }

    private Map<String, Object> extractMetrics(String metricsJson) {
        Map<String, Object> metricValues = new LinkedHashMap<>();
        metricValues.put("dti", null);
        metricValues.put("emergencyMonths", null);
        metricValues.put("netWorth", null);
        metricValues.put("sharpe", null);
        metricValues.put("maxDrawdown", null);

        if (metricsJson == null || metricsJson.isBlank()) {
            return metricValues;
        }

        try {
            Map<String, Object> metrics = objectMapper.readValue(metricsJson, new TypeReference<Map<String, Object>>() {});
            metricValues.put("dti", getNested(metrics, "cashflow", "dti"));
            metricValues.put("emergencyMonths", getNested(metrics, "cashflow", "emergencyMonths"));
            metricValues.put("netWorth", getNested(metrics, "portfolio", "netWorth"));
            metricValues.put("sharpe", getNested(metrics, "portfolio", "performance", "sharpe"));
            metricValues.put("maxDrawdown", getNested(metrics, "portfolio", "performance", "maxDrawdown"));
        } catch (Exception e) {
            log.warn("[Trend] metrics JSON 解析失败，按 null 兜底");
        }
        return metricValues;
    }

    @SuppressWarnings("unchecked")
    private Object getNested(Map<String, Object> source, String... path) {
        Object current = source;
        for (String key : path) {
            if (!(current instanceof Map<?, ?> map)) {
                return null;
            }
            current = ((Map<String, Object>) map).get(key);
            if (current == null) {
                return null;
            }
        }
        return current;
    }

    private Map<String, Object> buildTrend(List<HealthReportTrendPointVO> points) {
        Map<String, Object> trend = new LinkedHashMap<>();
        trend.put("reportCount", points.size());
        if (points.size() < 2) {
            trend.put("scoreDelta", Collections.emptyMap());
            trend.put("metricDelta", Collections.emptyMap());
            return trend;
        }

        HealthReportTrendPointVO first = points.get(0);
        HealthReportTrendPointVO last = points.get(points.size() - 1);

        Map<String, Object> scoreDelta = new LinkedHashMap<>();
        scoreDelta.put("health", intDelta(first.getScores().get("health"), last.getScores().get("health")));
        scoreDelta.put("risk", intDelta(first.getScores().get("risk"), last.getScores().get("risk")));
        scoreDelta.put("behavior", intDelta(first.getScores().get("behavior"), last.getScores().get("behavior")));

        Map<String, Object> metricDelta = new LinkedHashMap<>();
        metricDelta.put("dti", decimalDelta(first.getMetrics().get("dti"), last.getMetrics().get("dti")));
        metricDelta.put("emergencyMonths", decimalDelta(first.getMetrics().get("emergencyMonths"), last.getMetrics().get("emergencyMonths")));
        metricDelta.put("netWorth", decimalDelta(first.getMetrics().get("netWorth"), last.getMetrics().get("netWorth")));
        metricDelta.put("sharpe", decimalDelta(first.getMetrics().get("sharpe"), last.getMetrics().get("sharpe")));
        metricDelta.put("maxDrawdown", decimalDelta(first.getMetrics().get("maxDrawdown"), last.getMetrics().get("maxDrawdown")));

        trend.put("scoreDelta", scoreDelta);
        trend.put("metricDelta", metricDelta);
        return trend;
    }

    private Integer intDelta(Integer from, Integer to) {
        if (from == null || to == null) {
            return null;
        }
        return to - from;
    }

    private BigDecimal decimalDelta(Object from, Object to) {
        BigDecimal fromVal = toBigDecimal(from);
        BigDecimal toVal = toBigDecimal(to);
        if (fromVal == null || toVal == null) {
            return null;
        }
        return toVal.subtract(fromVal);
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        try {
            return new BigDecimal(value.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private Map<String, Object> buildExecution(Long userId) {
        LocalDateTime since = LocalDateTime.now().minusDays(30);

        Long confirmCount = behaviorEventMapper.selectCount(
                new LambdaQueryWrapper<FcBehaviorEventEntity>()
                        .eq(FcBehaviorEventEntity::getUserId, userId)
                        .eq(FcBehaviorEventEntity::getEventType, "REBALANCE_CONFIRM")
                        .ge(FcBehaviorEventEntity::getCreatedAt, since)
        );

        FcBehaviorEventEntity latest = behaviorEventMapper.selectOne(
                new LambdaQueryWrapper<FcBehaviorEventEntity>()
                        .eq(FcBehaviorEventEntity::getUserId, userId)
                        .eq(FcBehaviorEventEntity::getEventType, "REBALANCE_CONFIRM")
                        .ge(FcBehaviorEventEntity::getCreatedAt, since)
                        .orderByDesc(FcBehaviorEventEntity::getCreatedAt)
                        .last("LIMIT 1")
        );

        Map<String, Object> execution = new LinkedHashMap<>();
        execution.put("rebalanceConfirmCount30d", confirmCount == null ? 0L : confirmCount);
        execution.put("lastRebalanceConfirmAt", latest == null ? null : latest.getCreatedAt());
        return execution;
    }
}
