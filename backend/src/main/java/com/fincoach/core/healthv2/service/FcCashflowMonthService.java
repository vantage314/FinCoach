package com.fincoach.core.healthv2.service;

import com.fincoach.core.healthv2.dto.CashflowMonthUpsertDTO;
import com.fincoach.core.healthv2.entity.FcCashflowMonthEntity;

import java.util.List;

public interface FcCashflowMonthService {

    FcCashflowMonthEntity upsert(Long userId, CashflowMonthUpsertDTO dto);

    List<FcCashflowMonthEntity> listByUserIdAndRange(Long userId, String from, String to);
}
