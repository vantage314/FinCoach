package com.fincoach.core.healthv2.analyzer.portfolio;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fincoach.core.healthv2.common.HealthV2ConfigDefaults;
import com.fincoach.core.healthv2.entity.FcHealthReportEntity;
import com.fincoach.core.healthv2.mapper.FcHealthReportMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Builds approximate portfolio history from previous Health Reports.
 * Used when no direct market/transaction history is available.
 */
@Slf4j
@Component
public class PortfolioHistoryBuilder {

    @Autowired
    private FcHealthReportMapper reportMapper;

    @Autowired
    private ObjectMapper objectMapper;

    // Minimum data points to be useful
    private static final int MIN_POINTS = 2;
    // Max history points to look back
    private static final int MAX_HISTORY = 24;

    /**
     * Build input for PortfolioAnalyzer using recent report history + current data.
     *
     * @param userId Current user
     * @param currentNetWorth Current net worth (from current generation context)
     * @param currentAllocation Current asset allocation (from current generation context)
     * @return PortfolioInput with equity curve, returns series, and proxy asset returns
     */
    public PortfolioInput buildFromRecentReports(Long userId, BigDecimal currentNetWorth, Map<String, Object> currentAllocation) {
        PortfolioInput.PortfolioInputBuilder builder = PortfolioInput.builder();
        builder.rfAnnual(HealthV2ConfigDefaults.DEFAULT_RF_ANNUAL); // Default Rf

        List<String> warnings = new ArrayList<>();

        // 1. Fetch recent reports (time desc)
        List<FcHealthReportEntity> history = reportMapper.selectList(
                new LambdaQueryWrapper<FcHealthReportEntity>()
                        .eq(FcHealthReportEntity::getUserId, userId)
                        .orderByDesc(FcHealthReportEntity::getReportDate)
                        .last("LIMIT " + (MAX_HISTORY - 1)) // Reserve 1 for current
        );

        if (history.isEmpty()) {
            // No history, return nulls
            return builder.build();
        }

        // 2. Extract Data Points (Time ASC)
        // Structure: List of snapshots. 0=oldest, N=current
        List<Snapshot> snapshots = new ArrayList<>();

        // Add history (reverse to make it ASC)
        for (int i = history.size() - 1; i >= 0; i--) {
            FcHealthReportEntity report = history.get(i);
            Snapshot s = parseReport(report);
            if (s != null) {
                snapshots.add(s);
            }
        }

        // Add current
        Snapshot current = new Snapshot();
        current.netWorth = currentNetWorth != null ? currentNetWorth.doubleValue() : 0.0;
        current.allocation = convertAllocation(currentAllocation);
        snapshots.add(current);

        if (snapshots.size() < MIN_POINTS) {
            return builder.build();
        }

        // 3. Build Equity Curve
        List<Double> equityCurve = snapshots.stream()
                .map(s -> s.netWorth)
                .collect(Collectors.toList());
        builder.equityCurve(equityCurve);

        // 4. Build Returns Series (Total Portfolio)
        // r_t = (eq_t / eq_t-1) - 1
        List<Double> returnsSeries = new ArrayList<>();
        for (int i = 1; i < snapshots.size(); i++) {
            double prev = snapshots.get(i - 1).netWorth;
            double curr = snapshots.get(i).netWorth;
            if (prev <= 0) {
                // Cannot calculate return if prev is 0 or negative (edge case handling)
                returnsSeries.add(0.0);
            } else {
                returnsSeries.add((curr / prev) - 1.0);
            }
        }
        builder.returnsSeries(returnsSeries);

        // 5. Build Proxy Asset Returns for Correlation
        // Strategy: Track the change in "Asset Class Amount".
        // This is a proxy. Ideally we want Price Change, but we don't have prices.
        // We assume rebalancing isn't drastic between reports so Amount Change ~ Performance + Contribution.
        // It's an approximation.
        Map<String, List<Double>> assetReturns = new HashMap<>();
        Set<String> allKeys = new HashSet<>();
        for (Snapshot s : snapshots) {
            if (s.allocation != null) allKeys.addAll(s.allocation.keySet());
        }

        // For each asset type, build a return series
        for (String key : allKeys) {
            List<Double> series = new ArrayList<>();
            // We need returns, so size will be N-1
            boolean valid = true;
            for (int i = 1; i < snapshots.size(); i++) {
                Snapshot prevS = snapshots.get(i - 1);
                Snapshot currS = snapshots.get(i);
                
                // Implied amount = Total NetWorth * Allocation Ratio ?
                // NetWorth includes debt. We really want Total Assets.
                // But we parsed NetWorth.
                // Let's assume allocation is % of Total Assets.
                // We should ideally track TotalAssets in snapshot.
                // But parseReport extracts from metrics.portfolio.netWorth (as per prompt).
                // Actually prompt says "metricsJson -> portfolio.netWorth (or totalAssets)".
                // Let's stick to using NetWorth as body for now, or refine Parse to read TotalAssets.
                
                // Let's try to calculate an "Asset Amount" proxy: NetWorth * Ratio (very rough if debt high)
                // Refinement: parseReport should try to get totalAssets.
                
                Double prevAmt = getAssetAmount(prevS, key);
                Double currAmt = getAssetAmount(currS, key);
                
                if (prevAmt == null || currAmt == null || prevAmt <= 1.0) {
                     // Can't calc return, assume 0
                     series.add(0.0);
                } else {
                     series.add((currAmt / prevAmt) - 1.0);
                }
            }
            if (series.size() >= MIN_POINTS - 1) {
                assetReturns.put(key, series);
            }
        }
        builder.returnsByAssetKey(assetReturns);

        return builder.build();
    }

    private Double getAssetAmount(Snapshot s, String key) {
        if (s.allocation == null || !s.allocation.containsKey(key)) return 0.0;
        return s.allocation.get(key) * s.totalAssets; // Using TotalAssets
    }

    private Snapshot parseReport(FcHealthReportEntity report) {
        try {
            if (report.getMetricsJson() == null) return null;
            Map<String, Object> metrics = objectMapper.readValue(report.getMetricsJson(), new TypeReference<Map<String, Object>>() {});
            @SuppressWarnings("unchecked")
            Map<String, Object> portfolio = (Map<String, Object>) metrics.get("portfolio");
            if (portfolio == null) return null;

            Snapshot s = new Snapshot();
            
            // NetWorth
            Object nwObj = portfolio.get("netWorth");
            if (nwObj instanceof Number) s.netWorth = ((Number) nwObj).doubleValue();
            else s.netWorth = 0.0;
            
            // TotalAssets (Better for allocation proxy)
            Object taObj = portfolio.get("totalAssets");
            if (taObj instanceof Number) s.totalAssets = ((Number) taObj).doubleValue();
            else s.totalAssets = s.netWorth; // Fallback

            // Allocation
            @SuppressWarnings("unchecked")
            Map<String, Object> allocMap = (Map<String, Object>) portfolio.get("allocation");
            s.allocation = convertAllocation(allocMap);
            
            return s;
        } catch (Exception e) {
            log.warn("[PortfolioHistory] Failed to parse report {}: {}", report.getId(), e.getMessage());
            return null;
        }
    }

    private Map<String, Double> convertAllocation(Map<String, Object> raw) {
        if (raw == null) return new HashMap<>();
        Map<String, Double> res = new HashMap<>();
        for (Map.Entry<String, Object> e : raw.entrySet()) {
            if (e.getValue() instanceof Number) {
                res.put(e.getKey(), ((Number) e.getValue()).doubleValue());
            }
        }
        return res;
    }

    private static class Snapshot {
        double netWorth;
        double totalAssets;
        Map<String, Double> allocation;
    }
}
