package com.fincoach.core.ticker.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fincoach.core.ticker.entity.FcTickerMappingEntity;
import com.fincoach.core.ticker.mapper.FcTickerMappingMapper;
import com.fincoach.core.ticker.service.TickerMappingDbService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class TickerMappingDbServiceImpl implements TickerMappingDbService {

    private final FcTickerMappingMapper mapper;

    public TickerMappingDbServiceImpl(FcTickerMappingMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Optional<FcTickerMappingEntity> findBest(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return Optional.empty();
        }

        QueryWrapper<FcTickerMappingEntity> qw = new QueryWrapper<>();
        qw.eq("enabled", 1)
          .eq("keyword", keyword)
          .orderByDesc("priority", "updated_at")
          .last("LIMIT 1");

        return Optional.ofNullable(mapper.selectOne(qw));
    }

    @Override
    public List<FcTickerMappingEntity> listEnabled() {
        QueryWrapper<FcTickerMappingEntity> qw = new QueryWrapper<>();
        qw.eq("enabled", 1)
          .orderByDesc("priority", "updated_at");
        return mapper.selectList(qw);
    }
}
