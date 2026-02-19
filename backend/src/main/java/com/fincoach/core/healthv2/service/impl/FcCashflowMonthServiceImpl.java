package com.fincoach.core.healthv2.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fincoach.core.healthv2.dto.CashflowMonthUpsertDTO;
import com.fincoach.core.healthv2.entity.FcCashflowMonthEntity;
import com.fincoach.core.healthv2.mapper.FcCashflowMonthMapper;
import com.fincoach.core.healthv2.service.AuditService;
import com.fincoach.core.healthv2.service.FcCashflowMonthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class FcCashflowMonthServiceImpl implements FcCashflowMonthService {

    @Autowired
    private FcCashflowMonthMapper cashflowMonthMapper;

    @Autowired
    private AuditService auditService;

    @Override
    public FcCashflowMonthEntity upsert(Long userId, CashflowMonthUpsertDTO dto) {
        log.info("[HealthV2-CashflowMonth] upsert, userId={}, month={}", userId, dto.getMonth());

        LambdaQueryWrapper<FcCashflowMonthEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FcCashflowMonthEntity::getUserId, userId)
               .eq(FcCashflowMonthEntity::getMonth, dto.getMonth());
        FcCashflowMonthEntity existing = cashflowMonthMapper.selectOne(wrapper);

        BigDecimal income = dto.getIncome();
        BigDecimal expense = dto.getExpense();
        BigDecimal net = income.subtract(expense);

        if (existing != null) {
            FcCashflowMonthEntity before = new FcCashflowMonthEntity();
            before.setId(existing.getId());
            before.setIncome(existing.getIncome());
            before.setExpense(existing.getExpense());

            existing.setIncome(income);
            existing.setExpense(expense);
            existing.setNet(net);
            existing.setUpdatedAt(LocalDateTime.now());
            cashflowMonthMapper.updateById(existing);
            auditService.log(userId, "UPDATE", "FC_CASHFLOW_MONTH", existing.getId(), before, existing);
            return existing;
        }

        FcCashflowMonthEntity entity = new FcCashflowMonthEntity();
        entity.setUserId(userId);
        entity.setMonth(dto.getMonth());
        entity.setIncome(income);
        entity.setExpense(expense);
        entity.setNet(net);
        entity.setUpdatedAt(LocalDateTime.now());
        cashflowMonthMapper.insert(entity);
        auditService.log(userId, "CREATE", "FC_CASHFLOW_MONTH", entity.getId(), null, entity);
        return entity;
    }

    @Override
    public List<FcCashflowMonthEntity> listByUserIdAndRange(Long userId, String from, String to) {
        LambdaQueryWrapper<FcCashflowMonthEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FcCashflowMonthEntity::getUserId, userId);
        List<FcCashflowMonthEntity> list = cashflowMonthMapper.selectList(wrapper);
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }
        YearMonth fromYm = parseMonth(from);
        YearMonth toYm = parseMonth(to);
        return list.stream()
                .filter(item -> inRange(parseMonth(item.getMonth()), fromYm, toYm))
                .sorted(Comparator.comparing(FcCashflowMonthEntity::getMonth))
                .collect(Collectors.toList());
    }

    private YearMonth parseMonth(String month) {
        if (!StringUtils.hasText(month)) {
            return null;
        }
        return YearMonth.parse(month.trim());
    }

    private boolean inRange(YearMonth target, YearMonth from, YearMonth to) {
        if (target == null) {
            return false;
        }
        boolean afterFrom = from == null || !target.isBefore(from);
        boolean beforeTo = to == null || !target.isAfter(to);
        return afterFrom && beforeTo;
    }
}
