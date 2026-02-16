package com.fincoach.core.healthv2.analyzer.portfolio;

import com.fincoach.core.healthv2.analyzer.market.MarketDataProvider;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.*;

public class PortfolioHistoryBuilderMarketTest {

    @Test
    public void testBuild_HappyPath_WithNormalization() {
        // Arrange
        MarketDataProvider mockProvider = Mockito.mock(MarketDataProvider.class);
        PortfolioHistoryBuilderMarket builder = new PortfolioHistoryBuilderMarket(mockProvider);
        
        // Setup data: 1 Asset, 3 days
        Map<LocalDate, BigDecimal> prices = new TreeMap<>();
        prices.put(LocalDate.of(2023,1,1), new BigDecimal("100"));
        prices.put(LocalDate.of(2023,1,2), new BigDecimal("110"));
        prices.put(LocalDate.of(2023,1,3), new BigDecimal("121"));
        
        // EXPECT "AAPL.US" because "AAPL" gets normalized
        Mockito.when(mockProvider.getDailySeries(ArgumentMatchers.eq("AAPL.US"), ArgumentMatchers.any(), ArgumentMatchers.any()))
               .thenReturn(prices);
               
        Map<String, Object> positions = new HashMap<>();
        positions.put("AAPL", 10.0); // "AAPL" -> Resolved "AAPL" -> Normalized "AAPL.US"
        
        // Act
        PortfolioInput input = builder.buildFromMarketData(1L, positions);
        
        // Assert
        assertNotNull(input);
        assertEquals(3, input.getEquityCurve().size());
        
        // Check warnings for normalization
        boolean hasNormWarning = input.getWarnings().stream().anyMatch(w -> w.contains("MARKET_DATA_SYMBOL_NORMALIZED") && w.contains("AAPL.US"));
        assertTrue(hasNormWarning, "Should warn about normalization");
    }
    
    @Test
    public void testBuild_Resolution_ChineseName() {
         MarketDataProvider mockProvider = Mockito.mock(MarketDataProvider.class);
         PortfolioHistoryBuilderMarket builder = new PortfolioHistoryBuilderMarket(mockProvider);
         
         Map<LocalDate, BigDecimal> prices = new TreeMap<>();
         prices.put(LocalDate.of(2023,1,1), new BigDecimal("1000"));
         prices.put(LocalDate.of(2023,1,2), new BigDecimal("1010"));
         
         // "贵州茅台" -> "600519.SS"
         Mockito.when(mockProvider.getDailySeries(ArgumentMatchers.eq("600519.SS"), ArgumentMatchers.any(), ArgumentMatchers.any()))
                .thenReturn(prices);
                
         Map<String, Object> positions = new HashMap<>();
         positions.put("贵州茅台", 100);
         
         PortfolioInput input = builder.buildFromMarketData(1L, positions);
         
         assertNotNull(input);
         assertEquals(2, input.getEquityCurve().size());
    }

    @Test
    public void testBuild_Gaps() {
        // Arrange
        MarketDataProvider mockProvider = Mockito.mock(MarketDataProvider.class);
        PortfolioHistoryBuilderMarket builder = new PortfolioHistoryBuilderMarket(mockProvider);
        
        // Setup data: Missing middle day
        Map<LocalDate, BigDecimal> prices = new TreeMap<>();
        prices.put(LocalDate.of(2023,1,1), new BigDecimal("100"));
        // Gap on 2023-01-02
        prices.put(LocalDate.of(2023,1,3), new BigDecimal("120")); 
        
        Mockito.when(mockProvider.getDailySeries(ArgumentMatchers.eq("AAPL.US"), ArgumentMatchers.any(), ArgumentMatchers.any()))
               .thenReturn(prices);
               
        Map<String, Object> positions = new HashMap<>();
        positions.put("AAPL", 1.0);
        
        // Act
        PortfolioInput input = builder.buildFromMarketData(1L, positions);
        
        // Assert
        assertNotNull(input);
        assertEquals(2, input.getEquityCurve().size()); 
        assertEquals(1, input.getReturnsSeries().size()); 
        
        // Check warnings
        // Norm warning present
        assertTrue(input.getWarnings().stream().anyMatch(w -> w.contains("MARKET_DATA_SYMBOL_NORMALIZED")));
    }
    
    @Test
    public void testBuild_InsufficientPoints_ReturnsNull() {
        MarketDataProvider mockProvider = Mockito.mock(MarketDataProvider.class);
        PortfolioHistoryBuilderMarket builder = new PortfolioHistoryBuilderMarket(mockProvider);
        
        Map<LocalDate, BigDecimal> prices = new TreeMap<>();
        prices.put(LocalDate.of(2023,1,1), new BigDecimal("100"));
        // Only 1 point
        
        Mockito.when(mockProvider.getDailySeries(ArgumentMatchers.anyString(), ArgumentMatchers.any(), ArgumentMatchers.any()))
               .thenReturn(prices);
               
        Map<String, Object> positions = new HashMap<>();
        positions.put("AAPL", 1.0);
        
        PortfolioInput input = builder.buildFromMarketData(1L, positions);
        
        assertNull(input);
    }
}
