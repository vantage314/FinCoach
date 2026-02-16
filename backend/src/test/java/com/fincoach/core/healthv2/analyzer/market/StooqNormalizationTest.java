package com.fincoach.core.healthv2.analyzer.market;

import com.fincoach.core.healthv2.analyzer.market.impl.StooqMarketDataProvider;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class StooqNormalizationTest {

    @Test
    public void testGetDailySeries_ParsesCsv() {
        // Arrange
        RestTemplate mockRest = Mockito.mock(RestTemplate.class);
        String csvResponse = "Date,Open,High,Low,Close,Volume\n" +
                "2023-01-01,100,105,95,101.50,1000\n" +
                "2023-01-02,101,106,96,102.00,1200\n";
        
        Mockito.when(mockRest.getForObject(ArgumentMatchers.anyString(), ArgumentMatchers.eq(String.class)))
               .thenReturn(csvResponse);
        
        StooqMarketDataProvider provider = new StooqMarketDataProvider(mockRest);
        
        // Act
        LocalDate start = LocalDate.of(2023, 1, 1);
        LocalDate end = LocalDate.of(2023, 1, 2);
        Map<LocalDate, BigDecimal> result = provider.getDailySeries("AAPL", start, end);
        
        // Assert
        assertEquals(2, result.size());
        assertEquals(new BigDecimal("101.50"), result.get(LocalDate.of(2023, 1, 1)));
        assertEquals(new BigDecimal("102.00"), result.get(LocalDate.of(2023, 1, 2)));
    }
    
    @Test
    public void testGetDailySeries_HandlesEmptyResponse() {
        RestTemplate mockRest = Mockito.mock(RestTemplate.class);
        Mockito.when(mockRest.getForObject(ArgumentMatchers.anyString(), ArgumentMatchers.eq(String.class)))
               .thenReturn("");
               
        StooqMarketDataProvider provider = new StooqMarketDataProvider(mockRest);
        
        Map<LocalDate, BigDecimal> result = provider.getDailySeries("AAPL", LocalDate.now(), LocalDate.now());
        assertTrue(result.isEmpty());
    }
}
