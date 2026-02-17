package com.fincoach.core.ticker.service.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fincoach.core.healthv2.analyzer.market.TickerMappingRegistry;
import com.fincoach.core.ticker.dto.admin.AdminTickerMappingDTO;
import com.fincoach.core.ticker.dto.admin.AdminTickerMappingQueryDTO;
import com.fincoach.core.ticker.dto.admin.AdminTickerMappingSaveDTO;
import com.fincoach.core.ticker.entity.FcTickerMappingEntity;
import com.fincoach.core.ticker.mapper.FcTickerMappingMapper;
import com.fincoach.core.ticker.service.admin.impl.AdminTickerMappingServiceImpl;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

public class AdminTickerMappingServiceTest {

    @Test
    public void testPageAndSaveEnableDisableReload() {
        FcTickerMappingMapper mapper = Mockito.mock(FcTickerMappingMapper.class);
        TickerMappingRegistry registry = Mockito.mock(TickerMappingRegistry.class);
        when(registry.normalizeInputKey("AAPL")).thenReturn("AAPL");
        when(registry.normalizeInputKey("AAPL.US")).thenReturn("AAPL.US");

        AdminTickerMappingServiceImpl service = new AdminTickerMappingServiceImpl(mapper, registry);

        // page
        FcTickerMappingEntity entity = new FcTickerMappingEntity();
        entity.setId(1L);
        entity.setKeyword("AAPL");
        entity.setTicker("AAPL.US");
        Page<FcTickerMappingEntity> page = new Page<>(1, 20);
        page.setRecords(List.of(entity));
        when(mapper.selectPage(any(Page.class), any())).thenReturn(page);

        AdminTickerMappingQueryDTO query = new AdminTickerMappingQueryDTO();
        IPage<AdminTickerMappingDTO> result = service.page(query);
        assertEquals(1, result.getRecords().size());
        assertEquals("AAPL", result.getRecords().get(0).getKeyword());

        // save
        AdminTickerMappingSaveDTO saveDTO = new AdminTickerMappingSaveDTO();
        saveDTO.setKeyword("AAPL");
        saveDTO.setTicker("AAPL.US");

        doAnswer(invocation -> {
            FcTickerMappingEntity arg = invocation.getArgument(0);
            arg.setId(100L);
            return 1;
        }).when(mapper).insert(any(FcTickerMappingEntity.class));

        Long id = service.save(saveDTO);
        assertNotNull(id);

        // enable/disable
        FcTickerMappingEntity existing = new FcTickerMappingEntity();
        existing.setId(100L);
        when(mapper.selectById(100L)).thenReturn(existing);
        service.enable(100L);
        service.disable(100L);

        // reload
        service.reload();
        Mockito.verify(registry, Mockito.times(1)).reload();

        ArgumentCaptor<FcTickerMappingEntity> captor = ArgumentCaptor.forClass(FcTickerMappingEntity.class);
        Mockito.verify(mapper, Mockito.atLeastOnce()).updateById(captor.capture());
    }
}
