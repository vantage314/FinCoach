package com.fincoach.core.healthv2.repository;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fincoach.core.healthv2.entity.FcPortfolioPriceSnapshotEntity;
import com.fincoach.core.healthv2.mapper.FcPortfolioPriceSnapshotMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class PortfolioPriceSnapshotRepository {

    private static final Logger log = LoggerFactory.getLogger(PortfolioPriceSnapshotRepository.class);

    private final FcPortfolioPriceSnapshotMapper mapper;
    private final AtomicBoolean dbUnavailable = new AtomicBoolean(false);

    public PortfolioPriceSnapshotRepository(FcPortfolioPriceSnapshotMapper mapper) {
        this.mapper = mapper;
    }

    public List<FcPortfolioPriceSnapshotEntity> findByUserIdAndDateRange(long userId, LocalDate from, LocalDate to) {
        dbUnavailable.set(false);
        try {
            QueryWrapper<FcPortfolioPriceSnapshotEntity> qw = new QueryWrapper<>();
            qw.eq("user_id", userId)
              .ge("snap_date", from)
              .le("snap_date", to)
              .orderByAsc("snap_date");
            return mapper.selectList(qw);
        } catch (Exception e) {
            dbUnavailable.set(true);
            log.warn("[SnapshotRepo] DB unavailable: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    public boolean consumeDbUnavailable() {
        return dbUnavailable.getAndSet(false);
    }
}
