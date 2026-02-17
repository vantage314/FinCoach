package com.fincoach.core.healthv2.analyzer.portfolio;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class PortfolioHistoryFacadeSnapshotTest {

    @Test
    public void testPreferCacheHit() {
        PortfolioHistoryBuilderSnapshot snapshotBuilder = Mockito.mock(PortfolioHistoryBuilderSnapshot.class);
        PortfolioHistoryBuilderMarket marketBuilder = Mockito.mock(PortfolioHistoryBuilderMarket.class);
        PortfolioHistoryBuilder reportBuilder = Mockito.mock(PortfolioHistoryBuilder.class);

        PortfolioInput cached = new PortfolioInput();
        cached.setReturnsSeries(List.of(0.01, 0.02));
        cached.setWarnings(new ArrayList<>(List.of(PortfolioHistoryBuilderSnapshot.WARN_CACHE_HIT)));

        when(snapshotBuilder.buildFromSnapshots(1L, 90)).thenReturn(cached);

        PortfolioHistoryFacade facade = new PortfolioHistoryFacade();
        facade.setSnapshotBuilder(snapshotBuilder);
        facade.setMarketBuilder(marketBuilder);
        facade.setReportBuilder(reportBuilder);
        facade.setPreferCache(true);
        facade.setLookbackDays(90);
        facade.setSnapshotEnabled(false);

        PortfolioHistoryFacade.FacadeResult result = facade.build(1L, BigDecimal.ONE, Collections.emptyMap(), Collections.emptyMap());

        assertEquals("SNAPSHOT_CACHE", result.source);
        assertNotNull(result.input);
        assertTrue(result.input.getWarnings().contains(PortfolioHistoryFacade.WARN_SNAPSHOT_PREFER_CACHE_ENABLED));
        assertTrue(result.input.getWarnings().contains(PortfolioHistoryBuilderSnapshot.WARN_CACHE_HIT));
        verifyNoInteractions(marketBuilder);
        verifyNoInteractions(reportBuilder);
    }

    @Test
    public void testPreferCacheMissFallbackToMarket() {
        PortfolioHistoryBuilderSnapshot snapshotBuilder = Mockito.mock(PortfolioHistoryBuilderSnapshot.class);
        PortfolioHistoryBuilderMarket marketBuilder = Mockito.mock(PortfolioHistoryBuilderMarket.class);
        PortfolioHistoryBuilder reportBuilder = Mockito.mock(PortfolioHistoryBuilder.class);

        PortfolioInput cached = new PortfolioInput();
        cached.setReturnsSeries(List.of(0.01));
        cached.setWarnings(new ArrayList<>(List.of(PortfolioHistoryBuilderSnapshot.WARN_CACHE_MISS)));

        PortfolioInput market = new PortfolioInput();
        market.setReturnsSeries(List.of(0.01, 0.02));
        market.setWarnings(new ArrayList<>());

        when(snapshotBuilder.buildFromSnapshots(1L, 90)).thenReturn(cached);
        when(marketBuilder.buildFromMarketData(Mockito.eq(1L), Mockito.any())).thenReturn(market);

        PortfolioHistoryFacade facade = new PortfolioHistoryFacade();
        facade.setSnapshotBuilder(snapshotBuilder);
        facade.setMarketBuilder(marketBuilder);
        facade.setReportBuilder(reportBuilder);
        facade.setPreferCache(true);
        facade.setLookbackDays(90);
        facade.setSnapshotEnabled(true);

        PortfolioHistoryFacade.FacadeResult result = facade.build(1L, BigDecimal.ONE, Collections.emptyMap(), Collections.emptyMap());

        assertEquals("MARKET_DATA_DAILY_CLOSE", result.source);
        assertNotNull(result.input);
        assertTrue(result.input.getWarnings().contains(PortfolioHistoryBuilderSnapshot.WARN_CACHE_MISS));
        assertTrue(result.input.getWarnings().contains(PortfolioHistoryFacade.WARN_SNAPSHOT_PREFER_CACHE_ENABLED));
    }

    @Test
    public void testSnapshotDbUnavailableFallbackToMarket() {
        PortfolioHistoryBuilderSnapshot snapshotBuilder = Mockito.mock(PortfolioHistoryBuilderSnapshot.class);
        PortfolioHistoryBuilderMarket marketBuilder = Mockito.mock(PortfolioHistoryBuilderMarket.class);
        PortfolioHistoryBuilder reportBuilder = Mockito.mock(PortfolioHistoryBuilder.class);

        PortfolioInput market = new PortfolioInput();
        market.setReturnsSeries(List.of(0.01, 0.02));
        market.setWarnings(new ArrayList<>());

        when(snapshotBuilder.buildFromSnapshots(1L, 90)).thenThrow(new RuntimeException("db down"));
        when(marketBuilder.buildFromMarketData(Mockito.eq(1L), Mockito.any())).thenReturn(market);

        PortfolioHistoryFacade facade = new PortfolioHistoryFacade();
        facade.setSnapshotBuilder(snapshotBuilder);
        facade.setMarketBuilder(marketBuilder);
        facade.setReportBuilder(reportBuilder);
        facade.setPreferCache(true);
        facade.setLookbackDays(90);
        facade.setSnapshotEnabled(true);

        PortfolioHistoryFacade.FacadeResult result = facade.build(1L, BigDecimal.ONE, Collections.emptyMap(), Collections.emptyMap());

        assertEquals("MARKET_DATA_DAILY_CLOSE", result.source);
        assertNotNull(result.input);
        assertTrue(result.input.getWarnings().contains(PortfolioHistoryBuilderSnapshot.WARN_DB_UNAVAILABLE));
    }
}
