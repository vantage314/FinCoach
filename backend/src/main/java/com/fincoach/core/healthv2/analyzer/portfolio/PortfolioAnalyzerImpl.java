package com.fincoach.core.healthv2.analyzer.portfolio;

import org.springframework.stereotype.Component;

import com.fincoach.core.healthv2.debug.PortfolioDebugContextHolder;
import com.fincoach.core.healthv2.debug.PortfolioMarketDebugSnapshot;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;

@Component
public class PortfolioAnalyzerImpl implements PortfolioAnalyzer {

    private static final int MIN_CORR_POINTS = 20;

    @Override
    public PortfolioMetrics analyze(PortfolioInput input) {
        if (input == null) {
            input = new PortfolioInput(null, null, null, null, null, null);
        }

        PortfolioMetrics metrics = new PortfolioMetrics();
        List<String> w = new ArrayList<>();
        if (input.getWarnings() != null) {
            w.addAll(input.getWarnings());
        }
        metrics.setWarnings(w);
        
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
        Map<String, NavigableMap<LocalDate, Double>> assetReturnsByDate = input.getReturnsByAssetDate();
        if (assetReturnsByDate == null || assetReturnsByDate.size() < 2) {
            metrics.setCorrelation(null);
            return;
        }

        ReturnSeriesAligner aligner = new ReturnSeriesAligner(MIN_CORR_POINTS);
        ReturnSeriesAligner.AlignedSeriesResult aligned = aligner.align(assetReturnsByDate);
        mergeWarnings(metrics, aligned.getWarnings());
        updateCorrelationDebug(aligned, false);

        if (aligned.getEffectivePoints() < MIN_CORR_POINTS || aligned.getReturns().length < 2) {
            addWarning(metrics, ReturnSeriesAligner.WARN_INSUFFICIENT);
            metrics.setCorrelation(null);
            updateCorrelationDebug(aligned, false);
            return;
        }

        List<String> symbols = aligned.getSymbols();
        double[][] returns = aligned.getReturns();
        Map<String, Map<String, Double>> matrix = new LinkedHashMap<>();

        for (int i = 0; i < symbols.size(); i++) {
            String symA = symbols.get(i);
            Map<String, Double> row = new LinkedHashMap<>();
            matrix.put(symA, row);
            row.put(symA, 1.0);
        }

        for (int i = 0; i < symbols.size(); i++) {
            for (int j = i + 1; j < symbols.size(); j++) {
                double corr = pearson(returns[i], returns[j], metrics);
                matrix.get(symbols.get(i)).put(symbols.get(j), corr);
                matrix.get(symbols.get(j)).put(symbols.get(i), corr);
            }
        }

        metrics.setCorrelation(matrix);
        updateCorrelationDebug(aligned, true);
    }

    private double pearson(double[] x, double[] y, PortfolioMetrics metrics) {
        int len = Math.min(x.length, y.length);
        if (len < 2) {
            addWarning(metrics, ReturnSeriesAligner.WARN_INSUFFICIENT);
            return 0.0;
        }

        double sumX = 0.0;
        double sumY = 0.0;
        for (int i = 0; i < len; i++) {
            sumX += x[i];
            sumY += y[i];
        }
        double meanX = sumX / len;
        double meanY = sumY / len;

        double sumXY = 0.0;
        double sumX2 = 0.0;
        double sumY2 = 0.0;
        for (int i = 0; i < len; i++) {
            double dx = x[i] - meanX;
            double dy = y[i] - meanY;
            sumXY += dx * dy;
            sumX2 += dx * dx;
            sumY2 += dy * dy;
        }

        if (sumX2 == 0.0 || sumY2 == 0.0) {
            return 0.0;
        }

        double r = sumXY / Math.sqrt(sumX2 * sumY2);
        if (Double.isNaN(r) || Double.isInfinite(r)) {
            return 0.0;
        }
        return Math.max(-1.0, Math.min(1.0, r));
    }

    private void mergeWarnings(PortfolioMetrics metrics, List<String> warnings) {
        if (warnings == null || warnings.isEmpty()) return;
        for (String w : warnings) {
            addWarning(metrics, w);
        }
    }

    private void addWarning(PortfolioMetrics metrics, String warning) {
        if (metrics == null || warning == null) return;
        if (metrics.getWarnings() == null) {
            metrics.setWarnings(new ArrayList<>());
        }
        if (!metrics.getWarnings().contains(warning)) {
            metrics.getWarnings().add(warning);
        }
    }

    private void updateCorrelationDebug(ReturnSeriesAligner.AlignedSeriesResult aligned, boolean matrixEmitted) {
        PortfolioMarketDebugSnapshot snapshot = PortfolioDebugContextHolder.get();
        if (snapshot == null) return;
        PortfolioMarketDebugSnapshot.CorrelationDebug debug = new PortfolioMarketDebugSnapshot.CorrelationDebug();
        debug.setEffectivePoints(aligned != null ? aligned.getEffectivePoints() : 0);
        debug.setMinPoints(MIN_CORR_POINTS);
        debug.setMatrixEmitted(matrixEmitted);
        String mode = "NONE";
        List<String> warnings = aligned != null ? aligned.getWarnings() : null;
        if (warnings != null) {
            if (warnings.contains(ReturnSeriesAligner.WARN_INTERSECTION)) {
                mode = "INTERSECTION";
            } else if (warnings.contains(ReturnSeriesAligner.WARN_RELAXED)) {
                mode = "RELAXED";
            }
        }
        debug.setAlignedMode(mode);
        snapshot.setCorrelation(debug);
    }
}
