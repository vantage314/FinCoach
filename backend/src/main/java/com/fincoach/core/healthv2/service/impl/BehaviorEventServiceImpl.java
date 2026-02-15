package com.fincoach.core.healthv2.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fincoach.core.healthv2.entity.FcBehaviorEventEntity;
import com.fincoach.core.healthv2.mapper.FcBehaviorEventMapper;
import com.fincoach.core.healthv2.service.AuditService;
import com.fincoach.core.healthv2.service.BehaviorEventService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 行为事件服务实现
 */
@Slf4j
@Service
public class BehaviorEventServiceImpl implements BehaviorEventService {

    @Autowired
    private FcBehaviorEventMapper eventMapper;

    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private AuditService auditService;

    @Override
    public void recordEvent(Long userId, String eventType, BigDecimal amount, Map<String, Object> meta) {
        if (userId == null || eventType == null || eventType.isBlank()) {
            return;
        }
        FcBehaviorEventEntity entity = new FcBehaviorEventEntity();
        entity.setUserId(userId);
        entity.setEventType(eventType);
        entity.setAmount(amount);
        entity.setCreatedAt(LocalDateTime.now());
        if (meta != null && !meta.isEmpty()) {
            try {
                entity.setMetaJson(objectMapper.writeValueAsString(meta));
            } catch (Exception e) {
                log.warn("[BehaviorEvent] meta JSON 转换失败", e);
                entity.setMetaJson("{}");
            }
        }
        eventMapper.insert(entity);
        auditService.log(userId, "CREATE_BEHAVIOR_EVENT", "BEHAVIOR_EVENT", entity.getId(), null, entity);
        log.info("[BehaviorEvent] recorded: userId={}, type={}, amount={}", userId, eventType, amount);
    }

    @Override
    public List<FcBehaviorEventEntity> listRecent(Long userId, int days, int limit) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        return eventMapper.selectList(
                new LambdaQueryWrapper<FcBehaviorEventEntity>()
                        .eq(FcBehaviorEventEntity::getUserId, userId)
                        .ge(FcBehaviorEventEntity::getCreatedAt, since)
                        .orderByDesc(FcBehaviorEventEntity::getCreatedAt)
                        .last("LIMIT " + limit));
    }

    @Override
    public IPage<FcBehaviorEventEntity> listByUser(Long userId, int page, int size, String eventType) {
        LambdaQueryWrapper<FcBehaviorEventEntity> queryWrapper = new LambdaQueryWrapper<FcBehaviorEventEntity>()
                .eq(FcBehaviorEventEntity::getUserId, userId)
                .orderByDesc(FcBehaviorEventEntity::getCreatedAt);
        if (eventType != null && !eventType.isBlank()) {
            queryWrapper.eq(FcBehaviorEventEntity::getEventType, eventType);
        }
        return eventMapper.selectPage(
                new Page<>(page, size),
                queryWrapper);
    }

    @Override
    public Map<String, Integer> countByType(Long userId, int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        List<FcBehaviorEventEntity> events = eventMapper.selectList(
                new LambdaQueryWrapper<FcBehaviorEventEntity>()
                        .eq(FcBehaviorEventEntity::getUserId, userId)
                        .ge(FcBehaviorEventEntity::getCreatedAt, since));
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (FcBehaviorEventEntity e : events) {
            counts.merge(e.getEventType(), 1, Integer::sum);
        }
        return counts;
    }

    @Override
    public boolean hasEventInDays(Long userId, String eventType, int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        Long count = eventMapper.selectCount(
                new LambdaQueryWrapper<FcBehaviorEventEntity>()
                        .eq(FcBehaviorEventEntity::getUserId, userId)
                        .eq(FcBehaviorEventEntity::getEventType, eventType)
                        .ge(FcBehaviorEventEntity::getCreatedAt, since));
        return count != null && count > 0;
    }
}
