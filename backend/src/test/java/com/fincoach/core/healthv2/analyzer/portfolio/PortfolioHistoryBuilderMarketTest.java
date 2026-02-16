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
    public void testBuild_HappyPath() {
        // Arrange
        MarketDataProvider mockProvider = Mockito.mock(MarketDataProvider.class);
        PortfolioHistoryBuilderMarket builder = new PortfolioHistoryBuilderMarket(mockProvider);
        
        // Setup data: 1 Asset, 3 days, increasing price
        Map<LocalDate, BigDecimal> prices = new TreeMap<>();
        prices.put(LocalDate.of(2023,1,1), new BigDecimal("100"));
        prices.put(LocalDate.of(2023,1,2), new BigDecimal("110")); // +10%
        prices.put(LocalDate.of(2023,1,3), new BigDecimal("121")); // +10%
        
        Mockito.when(mockProvider.getDailySeries(ArgumentMatchers.eq("AAPL"), ArgumentMatchers.any(), ArgumentMatchers.any()))
               .thenReturn(prices);
               
        Map<String, Object> positions = new HashMap<>();
        positions.put("AAPL", 10.0); // 10 shares
        
        // Act
        PortfolioInput input = builder.buildFromMarketData(1L, positions);
        
        // Assert
        assertNotNull(input);
        assertEquals(3, input.getEquityCurve().size());
        assertEquals(1000.0, input.getEquityCurve().get(0), 0.01);
        assertEquals(1100.0, input.getEquityCurve().get(1), 0.01);
        assertEquals(1210.0, input.getEquityCurve().get(2), 0.01);
        
        assertEquals(2, input.getReturnsSeries().size());
        assertEquals(0.10, input.getReturnsSeries().get(0), 0.0001);
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
        
        Mockito.when(mockProvider.getDailySeries(ArgumentMatchers.eq("AAPL"), ArgumentMatchers.any(), ArgumentMatchers.any()))
               .thenReturn(prices);
               
        Map<String, Object> positions = new HashMap<>();
        positions.put("AAPL", 1.0);
        
        // Act
        PortfolioInput input = builder.buildFromMarketData(1L, positions);
        
        // Assert
        assertNotNull(input);
        assertEquals(2, input.getEquityCurve().size()); // Only valid points
        assertEquals(1, input.getReturnsSeries().size()); // 1 return
        
        // Asset returns check: Logic fills gap with 0.0 for asset return series if alignment issue? 
        // Logic says: if alignment issue, add 0.0. But here validDates only has 2 dates.
        // Step 4 logic: returnsSeries length = equityCurve size - 1.
        
        // Asset Returns logic: iterates validDates (size 2). 
        // Loop range: i=1 to size-1 (index 1).
        // dPrev = index 0 (2023-01-01), dCurr = index 1 (2023-01-03).
        // prices has both. So return is calc correctly (120/100 - 1 = 0.2).
        // No gap warning expected for asset return itself because we skipped the gap day entirely in validDates.
        
        assertEquals(0.20, input.getReturnsSeries().get(0), 0.0001);
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
