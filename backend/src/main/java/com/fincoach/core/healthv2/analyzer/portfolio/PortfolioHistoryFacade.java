package com.fincoach.core.healthv2.analyzer.portfolio;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class PortfolioHistoryFacade {

    @Autowired
    private PortfolioHistoryBuilderMarket marketBuilder;
    
    @Autowired
    private PortfolioHistoryBuilder reportBuilder;

    /**
     * Build input trying Market Data first, then fallback to Report History.
     */
    public FacadeResult build(Long userId, BigDecimal netWorth, Map<String, Object> allocation, Map<String, Object> positions) {
        PortfolioInput input = null;
        String source = "UNKNOWN";
        
        // 1. Try Market Data
        try {
            input = marketBuilder.buildFromMarketData(userId, positions);
            if (input != null && input.getReturnsSeries() != null && input.getReturnsSeries().size() >= 2) {
                source = "MARKET_DATA_DAILY_CLOSE";
            } else {
                input = null; // Insufficient data
            }
        } catch (Exception e) {
            log.warn("[PortfolioFacade] Market builder failed: {}", e.getMessage());
            input = null;
        }
        
        // 2. Fallback to Report History
        if (input == null) {
            try {
                input = reportBuilder.buildFromRecentReports(userId, netWorth, allocation);
                source = "REPORT_NET_WORTH_APPROX";
                if (input.getWarnings() != null) {
                    input.getWarnings().add("MARKET_DATA_FALLBACK_TO_REPORT_APPROX");
                }
            } catch (Exception e) {
                log.error("[PortfolioFacade] Report builder failed: {}", e.getMessage());
            }
        }
        
        return new FacadeResult(input, source);
    }
    
    public static class FacadeResult {
        public PortfolioInput input;
        public String source;
        
        public FacadeResult(PortfolioInput input, String source) {
            this.input = input;
            this.source = source;
        }
    }
}
