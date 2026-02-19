package com.fincoach.core.healthv2.service;

import com.fincoach.core.healthv2.dto.DebtUpsertDTO;
import com.fincoach.core.healthv2.entity.FcDebtEntity;

import java.util.List;

public interface FcDebtService {

    FcDebtEntity upsert(Long userId, DebtUpsertDTO dto);

    List<FcDebtEntity> listActiveByUserId(Long userId);

    void softDelete(Long userId, Long id);
}
