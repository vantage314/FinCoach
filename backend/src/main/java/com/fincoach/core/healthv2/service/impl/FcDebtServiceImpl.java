package com.fincoach.core.healthv2.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fincoach.core.healthv2.dto.DebtUpsertDTO;
import com.fincoach.core.healthv2.entity.FcDebtEntity;
import com.fincoach.core.healthv2.mapper.FcDebtMapper;
import com.fincoach.core.healthv2.service.AuditService;
import com.fincoach.core.healthv2.service.FcDebtService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
public class FcDebtServiceImpl implements FcDebtService {

    private static final int ACTIVE = 1;

    @Autowired
    private FcDebtMapper debtMapper;

    @Autowired
    private AuditService auditService;

    @Override
    public FcDebtEntity upsert(Long userId, DebtUpsertDTO dto) {
        FcDebtEntity existing = null;
        if (dto.getId() != null) {
            existing = debtMapper.selectById(dto.getId());
            if (existing == null || !existing.getUserId().equals(userId)) {
                throw new IllegalArgumentException("债务不存在或无权操作");
            }
        }

        FcDebtEntity entity = existing == null ? new FcDebtEntity() : existing;
        FcDebtEntity before = null;
        if (existing != null) {
            before = new FcDebtEntity();
            before.setId(existing.getId());
            before.setDebtType(existing.getDebtType());
            before.setRemainingBalance(existing.getRemainingBalance());
            before.setApr(existing.getApr());
        }

        if (existing == null) {
            entity.setUserId(userId);
            entity.setCreatedAt(LocalDateTime.now());
        }

        entity.setDebtType(normalizeDebtType(dto.getDebtType()));
        BigDecimal normalizedApr = normalizeApr(dto.getApr());
        entity.setApr(normalizedApr);
        entity.setPrincipal(dto.getPrincipal());
        entity.setTermMonths(dto.getTermMonths());
        entity.setRemainingBalance(dto.getRemainingBalance());
        entity.setStartDate(parseDate(dto.getStartDate()));
        entity.setEndDate(parseDate(dto.getEndDate()));
        entity.setIsActive(ACTIVE);

        BigDecimal monthlyPayment = dto.getMonthlyPayment();
        if (monthlyPayment == null && isInstallmentDebt(entity.getDebtType())) {
            BigDecimal principal = choosePrincipal(entity);
            monthlyPayment = calcMonthlyPayment(principal, normalizedApr, dto.getTermMonths());
        }
        entity.setMonthlyPayment(monthlyPayment);
        entity.setUpdatedAt(LocalDateTime.now());

        if (existing == null) {
            debtMapper.insert(entity);
            auditService.log(userId, "CREATE", "FC_DEBT", entity.getId(), null, entity);
        } else {
            debtMapper.updateById(entity);
            auditService.log(userId, "UPDATE", "FC_DEBT", entity.getId(), before, entity);
        }
        return entity;
    }

    @Override
    public List<FcDebtEntity> listActiveByUserId(Long userId) {
        LambdaQueryWrapper<FcDebtEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FcDebtEntity::getUserId, userId)
               .eq(FcDebtEntity::getIsActive, ACTIVE);
        List<FcDebtEntity> list = debtMapper.selectList(wrapper);
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }
        List<FcDebtEntity> activeList = list.stream()
                .filter(item -> item.getIsActive() == null || item.getIsActive() == ACTIVE)
                .collect(java.util.stream.Collectors.toList());
        activeList.sort(Comparator.comparing((FcDebtEntity e) -> e.getApr() == null ? BigDecimal.ZERO : e.getApr())
                .reversed());
        return activeList;
    }

    @Override
    public void softDelete(Long userId, Long id) {
        FcDebtEntity existing = debtMapper.selectById(id);
        if (existing == null || !existing.getUserId().equals(userId)) {
            throw new IllegalArgumentException("债务不存在或无权操作");
        }
        existing.setIsActive(0);
        existing.setUpdatedAt(LocalDateTime.now());
        debtMapper.updateById(existing);
        auditService.log(userId, "DELETE", "FC_DEBT", existing.getId(), existing, null);
    }

    private String normalizeDebtType(String debtType) {
        if (!StringUtils.hasText(debtType)) {
            return null;
        }
        return debtType.trim().toUpperCase();
    }

    private boolean isInstallmentDebt(String debtType) {
        if (!StringUtils.hasText(debtType)) {
            return false;
        }
        String normalized = debtType.trim().toUpperCase();
        return !"CREDITCARD".equals(normalized) && !"CREDIT_CARD".equals(normalized) && !"OTHER".equals(normalized);
    }

    /**
     * 利率归一化：落库统一为小数制 (0~1)
     */
    private BigDecimal normalizeApr(BigDecimal apr) {
        if (apr == null) {
            return null;
        }
        if (apr.compareTo(BigDecimal.ONE) > 0) {
            BigDecimal normalized = apr.divide(new BigDecimal("100"), 8, RoundingMode.HALF_UP);
            log.warn("[HealthV2-Debt] apr 以百分制输入({})，已自动归一为小数制({})", apr, normalized);
            return normalized;
        }
        return apr;
    }

    private BigDecimal choosePrincipal(FcDebtEntity entity) {
        if (entity.getRemainingBalance() != null) {
            return entity.getRemainingBalance();
        }
        return entity.getPrincipal();
    }

    private BigDecimal calcMonthlyPayment(BigDecimal principal, BigDecimal apr, Integer termMonths) {
        if (principal == null || termMonths == null || termMonths <= 0) {
            return null;
        }
        BigDecimal rate = apr == null ? BigDecimal.ZERO : apr;
        BigDecimal monthlyRate = rate.divide(new BigDecimal("12"), 10, RoundingMode.HALF_UP);
        if (monthlyRate.compareTo(BigDecimal.ZERO) == 0) {
            return principal.divide(new BigDecimal(termMonths), 2, RoundingMode.HALF_UP);
        }
        double base = BigDecimal.ONE.add(monthlyRate).doubleValue();
        double pow = Math.pow(base, termMonths);
        BigDecimal powBd = BigDecimal.valueOf(pow);
        BigDecimal numerator = principal.multiply(monthlyRate).multiply(powBd);
        BigDecimal denominator = powBd.subtract(BigDecimal.ONE);
        if (denominator.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return numerator.divide(denominator, 2, RoundingMode.HALF_UP);
    }

    private LocalDate parseDate(String input) {
        if (!StringUtils.hasText(input)) {
            return null;
        }
        return LocalDate.parse(input.trim());
    }
}
