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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;

public class AdviceRuleSetServiceTest {

    @Test
    public void testEnableSwitchReloadsRegistry() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, FcAdviceRuleSetEntity.class);
        TableInfoHelper.initTableInfo(assistant, FcAdviceRuleParamEntity.class);
        FcAdviceRuleSetMapper ruleSetMapper = Mockito.mock(FcAdviceRuleSetMapper.class);
        FcAdviceRuleParamMapper paramMapper = Mockito.mock(FcAdviceRuleParamMapper.class);
        AdviceRuleRegistry registry = Mockito.mock(AdviceRuleRegistry.class);

        FcAdviceRuleSetEntity target = new FcAdviceRuleSetEntity();
        target.setId(1L);
        target.setCode("DEFAULT");
        target.setVersion(1);

        Mockito.when(ruleSetMapper.selectById(1L)).thenReturn(target);
        Mockito.when(ruleSetMapper.update(any(), any())).thenReturn(1);
        Mockito.when(ruleSetMapper.updateById(any())).thenReturn(1);

        AdviceRuleSetServiceImpl service = new AdviceRuleSetServiceImpl(ruleSetMapper, paramMapper, registry);
        service.enable(1L);

        ArgumentCaptor<FcAdviceRuleSetEntity> captor = ArgumentCaptor.forClass(FcAdviceRuleSetEntity.class);
        Mockito.verify(ruleSetMapper, times(1)).updateById(captor.capture());
        assertEquals(1, captor.getValue().getEnabled());
        Mockito.verify(registry, times(1)).reload();
    }
}
