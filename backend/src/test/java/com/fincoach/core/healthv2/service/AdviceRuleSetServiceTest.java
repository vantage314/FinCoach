package com.fincoach.core.healthv2.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fincoach.core.healthv2.advice.rules.AdviceRuleRegistry;
import com.fincoach.core.healthv2.entity.FcAdviceRuleParamEntity;
import com.fincoach.core.healthv2.entity.FcAdviceRuleSetEntity;
import com.fincoach.core.healthv2.mapper.FcAdviceRuleParamMapper;
import com.fincoach.core.healthv2.mapper.FcAdviceRuleSetMapper;
import com.fincoach.core.healthv2.service.impl.AdviceRuleSetServiceImpl;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;

public class AdviceRuleSetServiceTest {

    @Test
    public void testEnableDisablesPreviousEnabled() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, FcAdviceRuleSetEntity.class);
        TableInfoHelper.initTableInfo(assistant, FcAdviceRuleParamEntity.class);
        FcAdviceRuleSetMapper ruleSetMapper = Mockito.mock(FcAdviceRuleSetMapper.class);
        FcAdviceRuleParamMapper paramMapper = Mockito.mock(FcAdviceRuleParamMapper.class);
        AdviceRuleRegistry registry = Mockito.mock(AdviceRuleRegistry.class);

        FcAdviceRuleSetEntity enabled = new FcAdviceRuleSetEntity();
        enabled.setId(1L);
        enabled.setCode("DEFAULT");
        enabled.setVersion(1);
        enabled.setEnabled(1);

        FcAdviceRuleSetEntity target = new FcAdviceRuleSetEntity();
        target.setId(2L);
        target.setCode("DEFAULT");
        target.setVersion(2);
        target.setEnabled(0);

        Mockito.when(ruleSetMapper.selectList(any())).thenReturn(List.of(enabled));
        Mockito.when(ruleSetMapper.selectById(2L)).thenReturn(target);
        Mockito.when(ruleSetMapper.update(any(), any())).thenReturn(1);
        Mockito.when(ruleSetMapper.updateById(any())).thenReturn(1);

        AdviceRuleSetServiceImpl service = new AdviceRuleSetServiceImpl(ruleSetMapper, paramMapper, registry);
        service.enable(2L);

        ArgumentCaptor<FcAdviceRuleSetEntity> captor = ArgumentCaptor.forClass(FcAdviceRuleSetEntity.class);
        Mockito.verify(ruleSetMapper, times(1)).updateById(captor.capture());
        assertEquals(1, captor.getValue().getEnabled());
        Mockito.verify(ruleSetMapper, times(1)).update(any(), any());
        Mockito.verify(registry, times(1)).reload();
    }

    @Test
    public void testEnableAutoHealsMultipleEnabled() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, FcAdviceRuleSetEntity.class);
        TableInfoHelper.initTableInfo(assistant, FcAdviceRuleParamEntity.class);
        FcAdviceRuleSetMapper ruleSetMapper = Mockito.mock(FcAdviceRuleSetMapper.class);
        FcAdviceRuleParamMapper paramMapper = Mockito.mock(FcAdviceRuleParamMapper.class);
        AdviceRuleRegistry registry = Mockito.mock(AdviceRuleRegistry.class);

        FcAdviceRuleSetEntity older = new FcAdviceRuleSetEntity();
        older.setId(1L);
        older.setCode("DEFAULT");
        older.setVersion(1);
        older.setEnabled(1);

        FcAdviceRuleSetEntity latest = new FcAdviceRuleSetEntity();
        latest.setId(2L);
        latest.setCode("DEFAULT");
        latest.setVersion(2);
        latest.setEnabled(1);

        Mockito.when(ruleSetMapper.selectList(any())).thenReturn(List.of(older, latest));
        Mockito.when(ruleSetMapper.selectById(2L)).thenReturn(latest);
        Mockito.when(ruleSetMapper.update(any(), any())).thenReturn(1);
        Mockito.when(ruleSetMapper.updateById(any())).thenReturn(1);

        AdviceRuleSetServiceImpl service = new AdviceRuleSetServiceImpl(ruleSetMapper, paramMapper, registry);
        service.enable(2L);

        Mockito.verify(ruleSetMapper, times(2)).update(any(), any());
        Mockito.verify(registry, times(1)).reload();
    }
}
