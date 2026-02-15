package com.fincoach.core.healthv2.analyzer.portfolio;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper for statistical calculations.
 * Null-safe: returns null/0/NaN-handling as appropriate for the metrics logic.
 */
public class MathStatsHelper {

    public static Double mean(List<Double> values) {
        if (values == null || values.isEmpty()) return null;
        double sum = 0.0;
        int count = 0;
        for (Double v : values) {
            if (v != null) {
                sum += v;
                count++;
            }
        }
        return count == 0 ? null : sum / count;
    }

    public static Double stdDevSample(List<Double> values) {
        if (values == null || values.size() < 2) return null;
        Double mu = mean(values);
        if (mu == null) return null;

        double sumSqDiff = 0.0;
        int count = 0;
        for (Double v : values) {
            if (v != null) {
                sumSqDiff += Math.pow(v - mu, 2);
                count++;
            }
        }
        if (count < 2) return null;
        return Math.sqrt(sumSqDiff / (count - 1));
    }

    // Pearson Correlation
    public static Double correlation(List<Double> x, List<Double> y) {
        if (x == null || y == null) return null;
        // Truncate to min length
        int len = Math.min(x.size(), y.size());
        if (len < 2) return null;

        List<Double> xSub = x.subList(0, len);
        List<Double> ySub = y.subList(0, len);

        Double meanX = mean(xSub);
        Double meanY = mean(ySub);
        if (meanX == null || meanY == null) return null;

        double sumXY = 0.0;
        double sumX2 = 0.0;
        double sumY2 = 0.0;

        for (int i = 0; i < len; i++) {
            Double xi = xSub.get(i);
            Double yi = ySub.get(i);
            if (xi == null || yi == null) continue; // skip missing pairs? strict: return null? Let's skip.
            // Actually simplier logic:
            double diffX = xi - meanX;
            double diffY = yi - meanY;
            sumXY += diffX * diffY;
            sumX2 += diffX * diffX;
            sumY2 += diffY * diffY;
        }

        if (sumX2 == 0 || sumY2 == 0) return null; // Variance is 0

        double r = sumXY / Math.sqrt(sumX2 * sumY2);
        
        if (Double.isNaN(r) || Double.isInfinite(r)) return null;
        // Clamp -1 to 1 just in case of float precision
        return Math.max(-1.0, Math.min(1.0, r));
    }
}
