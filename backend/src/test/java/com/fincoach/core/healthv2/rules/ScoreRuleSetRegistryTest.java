package com.fincoach.core.healthv2.rules;

import com.fincoach.core.healthv2.entity.FcScoreRuleParamEntity;
import com.fincoach.core.healthv2.entity.FcScoreRuleSetEntity;
import com.fincoach.core.healthv2.mapper.FcScoreRuleParamMapper;
import com.fincoach.core.healthv2.mapper.FcScoreRuleSetMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

public class ScoreRuleSetRegistryTest {

    @Test
    public void testFallbackWhenEmpty() {
        FcScoreRuleSetMapper setMapper = Mockito.mock(FcScoreRuleSetMapper.class);
        FcScoreRuleParamMapper paramMapper = Mockito.mock(FcScoreRuleParamMapper.class);
        when(setMapper.selectActive()).thenReturn(null);

        ScoreRuleSetRegistry registry = new ScoreRuleSetRegistry(setMapper, paramMapper);
        registry.reload();
        ScoreRuleSnapshot snapshot = registry.get();

        assertEquals(ScoreRuleSnapshot.SOURCE_FALLBACK_DEFAULT, snapshot.getSource());
        assertEquals(ScoreRuleDefaults.DEFAULT_CODE, snapshot.getCode());
        assertTrue(snapshot.getWarnings().contains(ScoreRuleSetRegistry.WARN_FALLBACK_DEFAULT));
        assertFalse(snapshot.getMissingParams().isEmpty());
    }

    @Test
    public void testActiveLoadAndMissingParamWarning() {
        FcScoreRuleSetMapper setMapper = Mockito.mock(FcScoreRuleSetMapper.class);
        FcScoreRuleParamMapper paramMapper = Mockito.mock(FcScoreRuleParamMapper.class);

        FcScoreRuleSetEntity active = new FcScoreRuleSetEntity();
        active.setId(1L);
        active.setCode("DEFAULT");
        active.setVersion(2);
        when(setMapper.selectActive()).thenReturn(active);

        FcScoreRuleParamEntity sharpe = new FcScoreRuleParamEntity();
        sharpe.setRuleSetId(1L);
        sharpe.setParamKey(ScoreRuleDefaults.W_SHARPE);
        sharpe.setParamValue("0.60");
        sharpe.setValueType("DECIMAL");
        when(paramMapper.selectList(Mockito.any())).thenReturn(List.of(sharpe));

        ScoreRuleSetRegistry registry = new ScoreRuleSetRegistry(setMapper, paramMapper);
        registry.reload();
        ScoreRuleSnapshot snapshot = registry.get();

        assertEquals(ScoreRuleSnapshot.SOURCE_DB_ACTIVE, snapshot.getSource());
        assertEquals(2, snapshot.getVersion());
        assertTrue(snapshot.getWarnings().stream().anyMatch(w -> w.startsWith(ScoreRuleSetRegistry.WARN_PARAM_MISSING_PREFIX)));
        assertEquals("0.60", snapshot.getParams().get(ScoreRuleDefaults.W_SHARPE).getRawValue());
    }

    @Test
    public void testReloadChangesEffect() {
        FcScoreRuleSetMapper setMapper = Mockito.mock(FcScoreRuleSetMapper.class);
        FcScoreRuleParamMapper paramMapper = Mockito.mock(FcScoreRuleParamMapper.class);

        FcScoreRuleSetEntity v1 = new FcScoreRuleSetEntity();
        v1.setId(1L);
        v1.setCode("DEFAULT");
        v1.setVersion(1);

        FcScoreRuleSetEntity v2 = new FcScoreRuleSetEntity();
        v2.setId(2L);
        v2.setCode("DEFAULT");
        v2.setVersion(2);

        when(setMapper.selectActive()).thenReturn(v1, v2);
        when(paramMapper.selectList(Mockito.any())).thenReturn(List.of());

        ScoreRuleSetRegistry registry = new ScoreRuleSetRegistry(setMapper, paramMapper);
        registry.reload();
        assertEquals(1, registry.get().getVersion());

        registry.reload();
        assertEquals(2, registry.get().getVersion());
    }
}
