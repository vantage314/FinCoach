package com.fincoach.core.healthv2.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fincoach.core.healthv2.dto.CreateCashflowDTO;
import com.fincoach.core.healthv2.entity.FcCashflowEntity;
import com.fincoach.core.healthv2.mapper.FcCashflowMapper;
import com.fincoach.core.healthv2.service.AuditService;
import com.fincoach.core.healthv2.service.FcCashflowService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 体检v2-现金流服务实现
 * upsert 语义：同 user_id + month 存在则更新，否则新增
 */
@Slf4j
@Service
public class FcCashflowServiceImpl implements FcCashflowService {

    @Autowired
    private FcCashflowMapper cashflowMapper;

    @Autowired
    private AuditService auditService;

    @Override
    public FcCashflowEntity upsert(Long userId, CreateCashflowDTO dto) {
        log.info("[HealthV2-Cashflow] upsert, userId={}, month={}", userId, dto.getMonth());

        // 检查是否已存在
        LambdaQueryWrapper<FcCashflowEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FcCashflowEntity::getUserId, userId)
               .eq(FcCashflowEntity::getMonth, dto.getMonth());
        FcCashflowEntity existing = cashflowMapper.selectOne(wrapper);

        if (existing != null) {
            // 更新
            FcCashflowEntity before = new FcCashflowEntity();
            before.setId(existing.getId());
            before.setIncome(existing.getIncome());
            before.setFixedExpense(existing.getFixedExpense());

            existing.setIncome(dto.getIncome());
            existing.setFixedExpense(dto.getFixedExpense());
            existing.setVariableExpense(dto.getVariableExpense());
            existing.setMonthlyDebtPayment(dto.getMonthlyDebtPayment() != null ? dto.getMonthlyDebtPayment() : BigDecimal.ZERO);
            existing.setNotes(dto.getNotes());
            existing.setUpdateTime(LocalDateTime.now());

            cashflowMapper.updateById(existing);
            auditService.log(userId, "UPDATE", "FC_CASHFLOW", existing.getId(), before, existing);
            return existing;
        } else {
            // 新增
            FcCashflowEntity entity = new FcCashflowEntity();
            entity.setUserId(userId);
            entity.setMonth(dto.getMonth());
            entity.setIncome(dto.getIncome());
            entity.setFixedExpense(dto.getFixedExpense());
            entity.setVariableExpense(dto.getVariableExpense());
            entity.setMonthlyDebtPayment(dto.getMonthlyDebtPayment() != null ? dto.getMonthlyDebtPayment() : BigDecimal.ZERO);
            entity.setNotes(dto.getNotes());
            entity.setCreateTime(LocalDateTime.now());
            entity.setUpdateTime(LocalDateTime.now());

            cashflowMapper.insert(entity);
            auditService.log(userId, "CREATE", "FC_CASHFLOW", entity.getId(), null, entity);
            return entity;
        }
    }

    @Override
    public List<FcCashflowEntity> listByUserId(Long userId) {
        LambdaQueryWrapper<FcCashflowEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FcCashflowEntity::getUserId, userId)
               .orderByDesc(FcCashflowEntity::getMonth);
        return cashflowMapper.selectList(wrapper);
    }
}
