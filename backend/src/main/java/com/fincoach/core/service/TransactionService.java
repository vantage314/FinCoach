package com.fincoach.core.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fincoach.core.repository.entity.TransactionRecord;

import java.math.BigDecimal;

public interface TransactionService {
    void record(Long userId, Long assetId, String assetName, String type, BigDecimal amount, String remark);
    Page<TransactionRecord> getUserTransactions(Long userId, int page, int size);
}
