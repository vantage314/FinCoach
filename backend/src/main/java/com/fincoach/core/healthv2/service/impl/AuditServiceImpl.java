package com.fincoach.core.healthv2.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fincoach.core.healthv2.entity.FcAuditLogEntity;
import com.fincoach.core.healthv2.mapper.FcAuditLogMapper;
import com.fincoach.core.healthv2.service.AuditService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 审计日志服务实现（M1 最小实现）
 */
@Slf4j
@Service
public class AuditServiceImpl implements AuditService {

    @Autowired
    private FcAuditLogMapper auditLogMapper;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public void log(Long actorUserId, String action, String targetType, Long targetId, Object beforeObj, Object afterObj) {
        try {
            FcAuditLogEntity entity = new FcAuditLogEntity();
            entity.setActorUserId(actorUserId);
            entity.setAction(action);
            entity.setTargetType(targetType);
            entity.setTargetId(targetId);
            entity.setBeforeJson(beforeObj != null ? objectMapper.writeValueAsString(beforeObj) : null);
            entity.setAfterJson(afterObj != null ? objectMapper.writeValueAsString(afterObj) : null);
            entity.setCreatedAt(LocalDateTime.now());
            auditLogMapper.insert(entity);
            log.debug("[Audit] action={}, targetType={}, targetId={}, actor={}", action, targetType, targetId, actorUserId);
        } catch (Exception e) {
            // 审计日志写入失败不应影响主流程
            log.error("[Audit] 审计日志写入失败: action={}, targetType={}, targetId={}", action, targetType, targetId, e);
        }
    }
}
