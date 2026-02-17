package com.fincoach.core.ticker.service.admin.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fincoach.core.healthv2.analyzer.market.TickerMappingRegistry;
import com.fincoach.core.ticker.dto.admin.AdminTickerMappingDTO;
import com.fincoach.core.ticker.dto.admin.AdminTickerMappingQueryDTO;
import com.fincoach.core.ticker.dto.admin.AdminTickerMappingSaveDTO;
import com.fincoach.core.ticker.entity.FcTickerMappingEntity;
import com.fincoach.core.ticker.mapper.FcTickerMappingMapper;
import com.fincoach.core.ticker.service.admin.AdminTickerMappingService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AdminTickerMappingServiceImpl implements AdminTickerMappingService {

    private final FcTickerMappingMapper mapper;
    private final TickerMappingRegistry registry;

    public AdminTickerMappingServiceImpl(FcTickerMappingMapper mapper, TickerMappingRegistry registry) {
        this.mapper = mapper;
        this.registry = registry;
    }

    @Override
    public IPage<AdminTickerMappingDTO> page(AdminTickerMappingQueryDTO query) {
        int page = query != null && query.getPage() != null ? query.getPage() : 1;
        int size = query != null && query.getSize() != null ? query.getSize() : 20;

        QueryWrapper<FcTickerMappingEntity> qw = new QueryWrapper<>();
        if (query != null) {
            if (query.getKeyword() != null && !query.getKeyword().trim().isEmpty()) {
                qw.like("keyword", query.getKeyword().trim());
            }
            if (query.getTicker() != null && !query.getTicker().trim().isEmpty()) {
                qw.like("ticker", query.getTicker().trim());
            }
            if (query.getEnabled() != null) {
                qw.eq("enabled", query.getEnabled());
            }
        }
        qw.orderByDesc("priority", "updated_at");

        IPage<FcTickerMappingEntity> result = mapper.selectPage(new Page<>(page, size), qw);
        return result.convert(this::toDto);
    }

    @Override
    public Long save(AdminTickerMappingSaveDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("请求体不能为空");
        }
        String keyword = dto.getKeyword() != null ? dto.getKeyword().trim() : "";
        String ticker = dto.getTicker() != null ? dto.getTicker().trim() : "";
        if (keyword.isEmpty()) {
            throw new IllegalArgumentException("keyword 不能为空");
        }
        if (ticker.isEmpty()) {
            throw new IllegalArgumentException("ticker 不能为空");
        }

        String normalizedKeyword = registry.normalizeInputKey(keyword);
        String normalizedTicker = registry.normalizeInputKey(ticker);

        LocalDateTime now = LocalDateTime.now();
        FcTickerMappingEntity entity;
        if (dto.getId() != null) {
            entity = mapper.selectById(dto.getId());
            if (entity == null) {
                throw new IllegalArgumentException("记录不存在");
            }
            entity.setId(dto.getId());
        } else {
            entity = new FcTickerMappingEntity();
            entity.setCreatedAt(now);
        }

        entity.setKeyword(normalizedKeyword);
        entity.setTicker(normalizedTicker);
        entity.setMarket(dto.getMarket());
        entity.setPriority(dto.getPriority() != null ? dto.getPriority() : 0);
        entity.setEnabled(dto.getEnabled() != null ? dto.getEnabled() : 1);
        entity.setUpdatedAt(now);

        if (dto.getId() == null) {
            mapper.insert(entity);
        } else {
            mapper.updateById(entity);
        }
        return entity.getId();
    }

    @Override
    public void enable(Long id) {
        updateEnabled(id, 1);
    }

    @Override
    public void disable(Long id) {
        updateEnabled(id, 0);
    }

    @Override
    public void reload() {
        registry.reload();
    }

    private void updateEnabled(Long id, int enabled) {
        if (id == null) {
            throw new IllegalArgumentException("id 不能为空");
        }
        FcTickerMappingEntity entity = mapper.selectById(id);
        if (entity == null) {
            throw new IllegalArgumentException("记录不存在");
        }
        entity.setEnabled(enabled);
        entity.setUpdatedAt(LocalDateTime.now());
        mapper.updateById(entity);
    }

    private AdminTickerMappingDTO toDto(FcTickerMappingEntity entity) {
        AdminTickerMappingDTO dto = new AdminTickerMappingDTO();
        dto.setId(entity.getId());
        dto.setKeyword(entity.getKeyword());
        dto.setTicker(entity.getTicker());
        dto.setMarket(entity.getMarket());
        dto.setPriority(entity.getPriority());
        dto.setEnabled(entity.getEnabled());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }
}
