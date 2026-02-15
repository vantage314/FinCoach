package com.fincoach.core.healthv2.analyzer.portfolio;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class PortfolioAnalyzerImpl implements PortfolioAnalyzer {

    @Override
    public PortfolioMetrics analyze(PortfolioInput input) {
        if (input == null) {
            input = new PortfolioInput(null, null, null, null);
        }

        PortfolioMetrics metrics = new PortfolioMetrics();
        metrics.setWarnings(new ArrayList<>());
        
        try {
            calculateSharpe(input, metrics);
        } catch (Exception e) {
            metrics.getWarnings().add("CALC_ERROR_SHARPE: " + e.getMessage());
        }

        try {
            calculateMaxDrawdown(input, metrics);
        } catch (Exception e) {
            metrics.getWarnings().add("CALC_ERROR_MDD: " + e.getMessage());
        }

        try {
            calculateCorrelation(input, metrics);
        } catch (Exception e) {
            metrics.getWarnings().add("CALC_ERROR_CORR: " + e.getMessage());
        }

        return metrics;
    }

    private void calculateSharpe(PortfolioInput input, PortfolioMetrics metrics) {
        if (input.getReturnsSeries() == null || input.getReturnsSeries().size() < 2) {
            metrics.getWarnings().add("INSUFFICIENT_RETURNS_SERIES");
            metrics.setSharpe(null);
            return;
        }

        Double rfAnnual = input.getRfAnnual() != null ? input.getRfAnnual() : 0.0;
        // Assume returnsSeries are monthly or we normalize? 
        // Standard Sharpe: (Rp - Rf) / Sigma_p
        // Simplest assumption: input returns are periodic (e.g. monthly). 
        // We need mean and stdDev.
        
        Double meanReturn = MathStatsHelper.mean(input.getReturnsSeries());
        Double stdDev = MathStatsHelper.stdDevSample(input.getReturnsSeries());

        if (meanReturn == null || stdDev == null || stdDev == 0) {
            metrics.setSharpe(null);
            metrics.getWarnings().add("SHARPE_CALC_FAILED_STD_DEV_ZERO_OR_NULL");
            return;
        }

        // Adjust Rf to period? 
        // Let's assume input returns are monthly for now, rfAnnual is annual.
        // rfPeriod = rfAnnual / 12 (approx)
        double rfPeriod = rfAnnual / 12.0;

        double sharpe = (meanReturn - rfPeriod) / stdDev;
        
        // Annualize Sharpe? Commonly Sharpe is annualized: Sharpe_annual = Sharpe_period * sqrt(periods_per_year)
        // For minimal viable, let's store raw or annualized. 
        // Let's annualize it assuming monthly data: * sqrt(12)
        // But user requirement says: "Sharpe = (mean(r) - rf_month) / std(r)"
        // It doesn't explicitly ask for annualization of the final ratio, but usually Sharpe is quoted annualized.
        // Let's stick to the prompt's implied formula: "(mean(r) - rf_month) / std(r)"
        // If the prompt didn't say normalize, we'll output the raw ratio.
        // Wait, prompt said: "Sharpe = (mean(r) - rf) / std(r)" (implied periodic)
        // And "rf_month = rfAnnual / 12"
        
        // Filter Infinity/NaN
        if (Double.isNaN(sharpe) || Double.isInfinite(sharpe)) {
            metrics.setSharpe(null);
        } else {
            metrics.setSharpe(sharpe);
        }
    }

    private void calculateMaxDrawdown(PortfolioInput input, PortfolioMetrics metrics) {
        List<Double> equityCurve = input.getEquityCurve();
        if (equityCurve == null || equityCurve.size() < 2) {
            metrics.getWarnings().add("INSUFFICIENT_EQUITY_CURVE");
            metrics.setMaxDrawdown(null);
            return;
        }

        double maxPeak = -Double.MAX_VALUE;
        double maxDrawdown = 0.0;

        for (Double val : equityCurve) {
            if (val == null) continue;
            if (val > maxPeak) {
                maxPeak = val;
            }
            double drawdown = (maxPeak - val) / maxPeak;
            if (drawdown > maxDrawdown) {
                maxDrawdown = drawdown;
            }
        }
        
        // Ensure 0..1 range validation? 
        // If val was negative or erratic, calculation holds.
        // Assuming equity curve > 0 usually.
        
        metrics.setMaxDrawdown(maxDrawdown);
    }

    private void calculateCorrelation(PortfolioInput input, PortfolioMetrics metrics) {
        Map<String, List<Double>> assetReturns = input.getReturnsByAssetKey();
        if (assetReturns == null || assetReturns.size() < 2) {
            // Not enough assets to corelate or null
            metrics.setCorrelation(null); 
            // Warning? Maybe just info. "INSUFFICIENT_ASSETS_FOR_CORR"
            return;
        }

        Map<String, Map<String, Double>> matrix = new HashMap<>();
        List<String> keys = new ArrayList<>(assetReturns.keySet());
        
        for (int i = 0; i < keys.size(); i++) {
            String keyA = keys.get(i);
            matrix.putIfAbsent(keyA, new HashMap<>());
            matrix.get(keyA).put(keyA, 1.0); // Self correlation

            for (int j = i + 1; j < keys.size(); j++) {
                String keyB = keys.get(j);
                List<Double> seriesA = assetReturns.get(keyA);
                List<Double> seriesB = assetReturns.get(keyB);
                
                Double corr = MathStatsHelper.correlation(seriesA, seriesB);
                if (corr != null) {
                    matrix.get(keyA).put(keyB, corr);
                    matrix.putIfAbsent(keyB, new HashMap<>());
                    matrix.get(keyB).put(keyA, corr);
                }
            }
        }
        metrics.setCorrelation(matrix);
    }
}
