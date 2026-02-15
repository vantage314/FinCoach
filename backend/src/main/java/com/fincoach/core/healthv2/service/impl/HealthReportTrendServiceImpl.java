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
        int n = points.size();
        trend.put("reportCount", n);

        List<Object> labels = new ArrayList<>();
        List<Integer> healthSeries = new ArrayList<>();
        List<Integer> riskSeries = new ArrayList<>();
        List<Integer> behaviorSeries = new ArrayList<>();
        List<Object> dtiSeries = new ArrayList<>();
        List<Object> emergencySeries = new ArrayList<>();
        List<Object> netWorthSeries = new ArrayList<>();
        List<Object> sharpeSeries = new ArrayList<>();
        List<Object> maxDdSeries = new ArrayList<>();

        for (HealthReportTrendPointVO p : points) {
            labels.add(p.getReportDate());
            healthSeries.add(p.getScores().get("health"));
            riskSeries.add(p.getScores().get("risk"));
            behaviorSeries.add(p.getScores().get("behavior"));

            Map<String, Object> m = p.getMetrics();
            dtiSeries.add(m.get("dti"));
            emergencySeries.add(m.get("emergencyMonths"));
            netWorthSeries.add(m.get("netWorth"));
            sharpeSeries.add(m.get("sharpe"));
            maxDdSeries.add(m.get("maxDrawdown"));
        }

        trend.put("labels", labels);
        trend.put("health", healthSeries);
        trend.put("risk", riskSeries);
        trend.put("behavior", behaviorSeries);
        trend.put("dti", dtiSeries);
        trend.put("emergencyMonths", emergencySeries);
        trend.put("netWorth", netWorthSeries);
        trend.put("sharpe", sharpeSeries);
        trend.put("maxDrawdown", maxDdSeries);

        HealthReportTrendPointVO first = n >= 1 ? points.get(0) : null;
        HealthReportTrendPointVO last = n >= 1 ? points.get(n - 1) : null;

        Map<String, Object> scoreDelta = new LinkedHashMap<>();
        scoreDelta.put("health", n >= 2 ? intDelta(first.getScores().get("health"), last.getScores().get("health")) : null);
        scoreDelta.put("risk", n >= 2 ? intDelta(first.getScores().get("risk"), last.getScores().get("risk")) : null);
        scoreDelta.put("behavior", n >= 2 ? intDelta(first.getScores().get("behavior"), last.getScores().get("behavior")) : null);

        Map<String, Object> metricDelta = new LinkedHashMap<>();
        metricDelta.put("dti", n >= 2 ? decimalDelta(first.getMetrics().get("dti"), last.getMetrics().get("dti")) : null);
        metricDelta.put("emergencyMonths", n >= 2 ? decimalDelta(first.getMetrics().get("emergencyMonths"), last.getMetrics().get("emergencyMonths")) : null);
        metricDelta.put("netWorth", n >= 2 ? decimalDelta(first.getMetrics().get("netWorth"), last.getMetrics().get("netWorth")) : null);
        metricDelta.put("sharpe", n >= 2 ? decimalDelta(first.getMetrics().get("sharpe"), last.getMetrics().get("sharpe")) : null);
        metricDelta.put("maxDrawdown", n >= 2 ? decimalDelta(first.getMetrics().get("maxDrawdown"), last.getMetrics().get("maxDrawdown")) : null);

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
