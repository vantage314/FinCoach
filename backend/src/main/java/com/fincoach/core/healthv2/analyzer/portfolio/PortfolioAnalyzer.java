package com.fincoach.core.healthv2.analyzer.portfolio;

public interface PortfolioAnalyzer {
    /**
     * Analyze portfolio metrics based on input data.
     * Guaranteed to return a non-null PortfolioMetrics object,
     * though fields inside might be null if data is insufficient.
     *
     * @param input Data for analysis
     * @return Calculated metrics
     */
    PortfolioMetrics analyze(PortfolioInput input);
}
