package com.fincoach.core.healthv2.analyzer.market;

import com.fincoach.core.ticker.entity.FcTickerMappingEntity;
import com.fincoach.core.ticker.service.TickerMappingDbService;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TickerMappingRegistryReloadTest {

    private static class StubDbService implements TickerMappingDbService {
        private List<FcTickerMappingEntity> list = new ArrayList<>();

        @Override
        public Optional<FcTickerMappingEntity> findBest(String keyword) {
            return Optional.empty();
        }

        @Override
        public List<FcTickerMappingEntity> listEnabled() {
            return list;
        }
    }

    @Test
    public void testReloadUsesDbMappings() {
        StubDbService dbService = new StubDbService();
        TickerMappingRegistry registry = new TickerMappingRegistry();
        registry.setTickerMappingDbService(dbService);
        registry.setDbEnabled(true);
        registry.init();

        TickerMappingRegistry.ResolutionResult before = registry.resolve("AAPL");
        assertEquals("FILE_ALIAS", before.getSource());

        FcTickerMappingEntity entity = new FcTickerMappingEntity();
        entity.setKeyword("AAPL");
        entity.setTicker("AAPL.US");
        entity.setPriority(100);
        entity.setEnabled(1);
        dbService.list = List.of(entity);

        registry.reload();

        TickerMappingRegistry.ResolutionResult after = registry.resolve("AAPL");
        assertEquals("DB", after.getSource());
        assertEquals("AAPL.US", after.getResolvedTicker());
        assertTrue(after.getWarnings().contains(TickerMappingRegistry.TICKER_MAPPING_SOURCE_DB));
    }
}
