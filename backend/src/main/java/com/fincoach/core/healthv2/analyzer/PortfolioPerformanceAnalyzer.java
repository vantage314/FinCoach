package com.fincoach.core.healthv2.analyzer;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fincoach.core.healthv2.entity.FcAssetClassParamEntity;
import com.fincoach.core.healthv2.entity.FcAssetCorrParamEntity;
import com.fincoach.core.healthv2.entity.FcHealthReportEntity;
import com.fincoach.core.healthv2.mapper.FcAssetClassParamMapper;
import com.fincoach.core.healthv2.mapper.FcAssetCorrParamMapper;
import com.fincoach.core.healthv2.mapper.FcHealthReportMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * Portfolio Performance Analyzer (M2)
 * 
 * 双路径策略：
 * - HISTORY：基于用户历史报告序列计算 sharpe / maxDrawdown
 * - PARAM：基于 fc_asset_class_param + fc_asset_corr_param 估算
 * 历史点 < historyMinPoints 时走 PARAM 路径
 */
@Slf4j
@Component
public class PortfolioPerformanceAnalyzer {

    @Autowired
    private FcHealthReportMapper reportMapper;
    @Autowired
    private FcAssetClassParamMapper classParamMapper;
    @Autowired
    private FcAssetCorrParamMapper corrParamMapper;
    @Autowired
    private ObjectMapper objectMapper;

    @Value("${healthv2.riskFreeRateAnnual:0.01}")
    private double riskFreeRate;

    @Value("${healthv2.historyMinPoints:6}")
    private int historyMinPoints;

    /**
     * 计算组合绩效指标
     *
     * @param userId     用户ID
     * @param allocation 当前资产类别占比 Map (type -> ratio)
     * @return performance map (sharpe, maxDrawdown, corrMatrix, method)
     */
    public Map<String, Object> computePerformance(Long userId, Map<String, Object> allocation) {
        Map<String, Object> result = new LinkedHashMap<>();

        // 拉取历史报告序列
        List<FcHealthReportEntity> history = reportMapper.selectList(
                new LambdaQueryWrapper<FcHealthReportEntity>()
                        .eq(FcHealthReportEntity::getUserId, userId)
                        .orderByAsc(FcHealthReportEntity::getReportDate));

        if (history.size() >= historyMinPoints) {
            return computeFromHistory(history, allocation);
        } else {
            return computeFromParams(allocation, history.size());
        }
    }

    // ============================= HISTORY 路径 =============================

    private Map<String, Object> computeFromHistory(List<FcHealthReportEntity> history,
                                                    Map<String, Object> allocation) {
        Map<String, Object> result = new LinkedHashMap<>();
        log.info("[M2-Analyzer] 走 HISTORY 路径, 历史报告数={}", history.size());

        // 提取 proxy 序列：每份报告的 totalAssets
        List<BigDecimal> proxyValues = new ArrayList<>();
        for (FcHealthReportEntity r : history) {
            BigDecimal totalAssets = extractTotalAssets(r);
            if (totalAssets != null && totalAssets.compareTo(BigDecimal.ZERO) > 0) {
                proxyValues.add(totalAssets);
            }
        }

        if (proxyValues.size() < 2) {
            result.put("sharpe", null);
            result.put("maxDrawdown", null);
            result.put("corrMatrix", null);
            result.put("method", "HISTORY");
            result.put("reason", "有效净值序列不足");
            return result;
        }

        // 计算 returns
        List<Double> returns = new ArrayList<>();
        for (int i = 1; i < proxyValues.size(); i++) {
            double prev = proxyValues.get(i - 1).doubleValue();
            double curr = proxyValues.get(i).doubleValue();
            if (prev > 0) {
                returns.add((curr - prev) / prev);
            }
        }

        // Sharpe = (mean(returns) - rf_period) / std(returns)
        // rf_period ≈ riskFreeRate / 12 (假设每月一次报告)
        double rfPeriod = riskFreeRate / 12.0;
        double meanReturn = returns.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double stdReturn = calcStd(returns, meanReturn);

        Double sharpe = null;
        if (stdReturn > 1e-9) {
            sharpe = Math.round(((meanReturn - rfPeriod) / stdReturn) * 100.0) / 100.0;
        }

        // MaxDrawdown
        double maxDrawdown = calcMaxDrawdown(proxyValues);

        result.put("sharpe", sharpe);
        result.put("maxDrawdown", Math.round(maxDrawdown * 10000.0) / 10000.0);

        // corrMatrix 即使走 HISTORY，也用参数表（M2 简化：历史序列无法按类别拆分）
        result.put("corrMatrix", buildCorrMatrix(allocation));
        result.put("method", "HISTORY");
        return result;
    }

    // ============================= PARAM 路径 =============================

    private Map<String, Object> computeFromParams(Map<String, Object> allocation, int historyCount) {
        Map<String, Object> result = new LinkedHashMap<>();
        log.info("[M2-Analyzer] 走 PARAM 路径, 历史报告数={} < {}", historyCount, historyMinPoints);

        if (allocation == null || allocation.isEmpty()) {
            result.put("sharpe", null);
            result.put("maxDrawdown", null);
            result.put("corrMatrix", null);
            result.put("method", "PARAM");
            result.put("reason", "无资产配置数据");
            return result;
        }

        // 加载参数表
        List<FcAssetClassParamEntity> allParams = classParamMapper.selectList(null);
        Map<String, FcAssetClassParamEntity> paramMap = new HashMap<>();
        for (FcAssetClassParamEntity p : allParams) {
            paramMap.put(p.getAssetType(), p);
        }

        // 提取当前用户的资产类型和权重
        List<String> types = new ArrayList<>();
        List<Double> weights = new ArrayList<>();
        for (Map.Entry<String, Object> e : allocation.entrySet()) {
            types.add(e.getKey());
            weights.add(toBigDecimal(e.getValue()).doubleValue());
        }

        // 组合预期收益 = Σ(wi * ri)
        double portfolioReturn = 0;
        for (int i = 0; i < types.size(); i++) {
            FcAssetClassParamEntity p = paramMap.get(types.get(i));
            double er = (p != null) ? p.getExpectedReturn().doubleValue() : 0.03;
            portfolioReturn += weights.get(i) * er;
        }

        // 组合波动率 = sqrt(Σ wi*wj*σi*σj*ρij)
        List<FcAssetCorrParamEntity> allCorrs = corrParamMapper.selectList(null);
        Map<String, Double> corrMap = new HashMap<>();
        for (FcAssetCorrParamEntity c : allCorrs) {
            corrMap.put(c.getTypeA() + "|" + c.getTypeB(), c.getCorr().doubleValue());
        }

        double portfolioVar = 0;
        for (int i = 0; i < types.size(); i++) {
            for (int j = 0; j < types.size(); j++) {
                double wi = weights.get(i);
                double wj = weights.get(j);
                double volI = getVolatility(paramMap, types.get(i));
                double volJ = getVolatility(paramMap, types.get(j));
                double corr = getCorrValue(corrMap, types.get(i), types.get(j));
                portfolioVar += wi * wj * volI * volJ * corr;
            }
        }
        double portfolioVol = Math.sqrt(Math.max(0, portfolioVar));

        // Sharpe (param) = (portfolioReturn - rf) / portfolioVol
        Double sharpe = null;
        if (portfolioVol > 1e-9) {
            sharpe = Math.round(((portfolioReturn - riskFreeRate) / portfolioVol) * 100.0) / 100.0;
        }

        // MaxDrawdown 估算：使用 Cornish-Fisher 简化 ≈ 2.5 * portfolioVol (年化)
        double maxDrawdown = Math.min(1.0, 2.5 * portfolioVol);
        maxDrawdown = Math.round(maxDrawdown * 10000.0) / 10000.0;

        result.put("sharpe", sharpe);
        result.put("maxDrawdown", maxDrawdown);
        result.put("corrMatrix", buildCorrMatrix(allocation));
        result.put("method", "PARAM");
        return result;
    }

    // ============================= 工具方法 =============================

    /**
     * 构建 corrMatrix 结构
     */
    private Map<String, Object> buildCorrMatrix(Map<String, Object> allocation) {
        if (allocation == null || allocation.isEmpty()) {
            return null;
        }

        List<String> types = new ArrayList<>(allocation.keySet());

        List<FcAssetCorrParamEntity> allCorrs = corrParamMapper.selectList(null);
        Map<String, Double> corrMap = new HashMap<>();
        for (FcAssetCorrParamEntity c : allCorrs) {
            corrMap.put(c.getTypeA() + "|" + c.getTypeB(), c.getCorr().doubleValue());
        }

        List<List<Double>> matrix = new ArrayList<>();
        for (String ti : types) {
            List<Double> row = new ArrayList<>();
            for (String tj : types) {
                row.add(getCorrValue(corrMap, ti, tj));
            }
            matrix.add(row);
        }

        Map<String, Object> corr = new LinkedHashMap<>();
        corr.put("types", types);
        corr.put("matrix", matrix);
        return corr;
    }

    private BigDecimal extractTotalAssets(FcHealthReportEntity report) {
        try {
            if (report.getMetricsJson() == null) return null;
            Map<String, Object> metrics = objectMapper.readValue(report.getMetricsJson(),
                    new TypeReference<Map<String, Object>>() {});
            // M1 patch 格式: metrics.portfolio.totalAssets
            Object portfolio = metrics.get("portfolio");
            if (portfolio instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> portMap = (Map<String, Object>) portfolio;
                return toBigDecimal(portMap.get("totalAssets"));
            }
            // fallback 旧 M1 扁平: metrics.totalAssets
            return toBigDecimal(metrics.get("totalAssets"));
        } catch (Exception e) {
            log.warn("[M2-Analyzer] 解析 metricsJson 失败, reportId={}", report.getId());
            return null;
        }
    }

    private double getVolatility(Map<String, FcAssetClassParamEntity> paramMap, String type) {
        FcAssetClassParamEntity p = paramMap.get(type);
        return (p != null) ? p.getVolatility().doubleValue() : 0.10;
    }

    private double getCorrValue(Map<String, Double> corrMap, String a, String b) {
        if (a.equals(b)) return 1.0;
        Double v = corrMap.get(a + "|" + b);
        if (v != null) return v;
        v = corrMap.get(b + "|" + a);
        return (v != null) ? v : 0.0;
    }

    private double calcStd(List<Double> values, double mean) {
        if (values.size() < 2) return 0;
        double sumSq = 0;
        for (double v : values) {
            sumSq += (v - mean) * (v - mean);
        }
        return Math.sqrt(sumSq / (values.size() - 1));
    }

    private double calcMaxDrawdown(List<BigDecimal> values) {
        double maxDD = 0;
        double peak = values.get(0).doubleValue();
        for (BigDecimal v : values) {
            double val = v.doubleValue();
            if (val > peak) peak = val;
            double dd = (peak - val) / peak;
            if (dd > maxDD) maxDD = dd;
        }
        return maxDD;
    }

    private BigDecimal toBigDecimal(Object obj) {
        if (obj == null) return BigDecimal.ZERO;
        if (obj instanceof BigDecimal) return (BigDecimal) obj;
        if (obj instanceof Number) return BigDecimal.valueOf(((Number) obj).doubleValue());
        try {
            return new BigDecimal(obj.toString());
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }
}
