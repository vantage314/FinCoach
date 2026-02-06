package com.fincoach.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fincoach.core.repository.entity.TransactionRecord;
import com.fincoach.core.repository.mapper.TransactionRecordMapper;
import com.fincoach.core.service.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class TransactionServiceImpl implements TransactionService {

    @Autowired
    private TransactionRecordMapper transformRecordMapper;

    @Override
    public void record(Long userId, Long assetId, String assetName, String type, BigDecimal amount, String remark) {
        TransactionRecord record = new TransactionRecord();
        record.setUserId(userId);
        record.setRelatedAssetId(assetId);
        record.setAssetName(assetName);
        record.setTransType(type);
        record.setAmount(amount);
        record.setRemark(remark);
        record.setCreateTime(LocalDateTime.now());
        // balanceAfter depends on calculating current cash, simplified for now to null or calculated if needed
        // For MVP, leave balanceAfter null or handle specifically if requirements tighten
        
        transformRecordMapper.insert(record);
    }

    @Override
    public Page<TransactionRecord> getUserTransactions(Long userId, int page, int size) {
        Page<TransactionRecord> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<TransactionRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TransactionRecord::getUserId, userId)
               .orderByDesc(TransactionRecord::getCreateTime);
        return transformRecordMapper.selectPage(pageParam, wrapper);
    }
}
