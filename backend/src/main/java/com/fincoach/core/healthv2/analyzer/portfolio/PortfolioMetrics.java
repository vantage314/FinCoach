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
public class PortfolioMetrics {
    /**
     * Sharpe Ratio
     */
    private Double sharpe;

    /**
     * Max Drawdown (0..1)
     */
    private Double maxDrawdown;

    /**
     * Correlation Matrix
     * Key1 -> Key2 -> Correlation Coefficient
     */
    private Map<String, Map<String, Double>> correlation;

    /**
     * Warnings during calculation (e.g. "INSUFFICIENT_DATA")
     */
    private List<String> warnings;
}
