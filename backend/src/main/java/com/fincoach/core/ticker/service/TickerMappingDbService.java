package com.fincoach.core.ticker.service;

import com.fincoach.core.ticker.entity.FcTickerMappingEntity;

import java.util.List;
import java.util.Optional;

public interface TickerMappingDbService {
    Optional<FcTickerMappingEntity> findBest(String keyword);
    List<FcTickerMappingEntity> listEnabled();
}
