package com.fincoach.core.healthv2.analyzer.market;

import com.fincoach.core.ticker.entity.FcTickerMappingEntity;
import com.fincoach.core.ticker.service.TickerMappingDbService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TickerMappingRegistryTest {

    private TickerMappingRegistry registry;

    @BeforeEach
    public void setup() {
        registry = new TickerMappingRegistry();
        registry.setDbEnabled(false);
        registry.init(); // Loads file
    }

    @Test
    public void testResolveAlias_Moutai() {
        TickerMappingRegistry.ResolutionResult res = registry.resolve("贵州茅台");
        assertEquals("600519.SS", res.getResolvedTicker());
        assertEquals("FILE_ALIAS", res.getSource());
        assertTrue(res.getWarnings().contains("TICKER_MAPPING_HIT_ALIAS"));
        assertTrue(res.getWarnings().contains(TickerMappingRegistry.TICKER_MAPPING_SOURCE_FILE));
    }

    @Test
    public void testResolveAlias_AAPL() {
        TickerMappingRegistry.ResolutionResult res = registry.resolve("AAPL");
        assertEquals("AAPL.US", res.getResolvedTicker());
        assertEquals("FILE_ALIAS", res.getSource());
        assertTrue(res.getWarnings().contains("TICKER_MAPPING_HIT_ALIAS"));
        assertTrue(res.getWarnings().contains(TickerMappingRegistry.TICKER_MAPPING_SOURCE_FILE));
    }

    @Test
    public void testResolveHeuristic_CN_600() {
        TickerMappingRegistry.ResolutionResult res = registry.resolve("600519");
        assertEquals("600519.SS", res.getResolvedTicker());
    }

    @Test
    public void testResolve_Unknown() {
        TickerMappingRegistry.ResolutionResult res = registry.resolve("Unknown Asset 123");
        assertNull(res.getResolvedTicker());
        assertEquals("NONE", res.getSource());
        assertTrue(res.getWarnings().contains("POSITION_TICKER_UNRESOLVED"));
    }

    @Test
    public void testResolveDbPriority() {
        TickerMappingDbService dbService = mock(TickerMappingDbService.class);
        FcTickerMappingEntity entity = new FcTickerMappingEntity();
        entity.setKeyword("AAPL");
        entity.setTicker("AAPL.US");
        entity.setPriority(100);
        entity.setEnabled(1);

        when(dbService.listEnabled()).thenReturn(List.of(entity));

        TickerMappingRegistry dbRegistry = new TickerMappingRegistry();
        dbRegistry.setTickerMappingDbService(dbService);
        dbRegistry.setDbEnabled(true);
        dbRegistry.init();

        TickerMappingRegistry.ResolutionResult res = dbRegistry.resolve("AAPL");
        assertEquals("AAPL.US", res.getResolvedTicker());
        assertEquals("DB", res.getSource());
        assertTrue(res.getWarnings().contains(TickerMappingRegistry.TICKER_MAPPING_SOURCE_DB));
    }

    @Test
    public void testResolveDbFallbackToFileOnError() {
        TickerMappingDbService dbService = mock(TickerMappingDbService.class);
        when(dbService.listEnabled()).thenThrow(new RuntimeException("db down"));

        TickerMappingRegistry dbRegistry = new TickerMappingRegistry();
        dbRegistry.setTickerMappingDbService(dbService);
        dbRegistry.setDbEnabled(true);
        dbRegistry.init();

        TickerMappingRegistry.ResolutionResult res = dbRegistry.resolve("茅台");
        assertEquals("600519.SS", res.getResolvedTicker());
        assertEquals("FILE_ALIAS", res.getSource());
        assertTrue(res.getWarnings().contains(TickerMappingRegistry.TICKER_MAPPING_DB_UNAVAILABLE_FALLBACK_FILE));
        assertTrue(res.getWarnings().contains(TickerMappingRegistry.TICKER_MAPPING_SOURCE_FILE));
        assertTrue(res.getWarnings().contains("TICKER_MAPPING_HIT_ALIAS"));
    }
}
