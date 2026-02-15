package com.fincoach.core.healthv2.analyzer.portfolio;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioInput {
    /**
     * Annual risk-free rate (e.g., 0.02 for 2%)
     * Can be null, analyzer will use default or 0
     */
    private Double rfAnnual;

    /**
     * Historical returns series (e.g., daily or monthly returns)
     * Ordered by time asc
     */
    private List<Double> returnsSeries;

    /**
     * Historical equity curve (net value)
     * Ordered by time asc, used for MaxDrawdown
     */
    private List<Double> equityCurve;

    /**
     * Returns series by asset key (e.g., "STOCK", "GOLD")
     * Used for Correlation Matrix
     */
    private Map<String, List<Double>> returnsByAssetKey;
}
