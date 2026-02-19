package com.fincoach.core.healthv2.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fincoach.core.healthv2.entity.FcAlertEntity;
import com.fincoach.core.healthv2.entity.FcNotificationEntity;
import com.fincoach.core.healthv2.mapper.FcAlertMapper;
import com.fincoach.core.healthv2.mapper.FcNotificationMapper;
import com.fincoach.core.healthv2.service.AlertService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class AlertServiceImpl implements AlertService {

    @Autowired
    private FcAlertMapper alertMapper;

    @Autowired
    private FcNotificationMapper notificationMapper;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    @Transactional
    public FcAlertEntity raiseAlert(Long userId,
                                    String alertType,
                                    String severity,
                                    String title,
                                    String message,
                                    String source,
                                    Map<String, Object> meta) {
        String normalizedSeverity = normalizeSeverity(severity);
        String dedupeKey = buildDedupeKey(userId, alertType);
        LocalDateTime now = LocalDateTime.now();
        FcAlertEntity existing = alertMapper.selectOne(
                new LambdaQueryWrapper<FcAlertEntity>()
                        .eq(FcAlertEntity::getDedupeKey, dedupeKey)
                        .eq(FcAlertEntity::getStatus, "OPEN")
                        .last("LIMIT 1"));

        if (existing == null) {
            FcAlertEntity created = new FcAlertEntity();
            created.setUserId(userId);
            created.setAlertType(alertType);
            created.setSeverity(normalizedSeverity);
            created.setTitle(safe(title));
            created.setMessage(safe(message));
            created.setStatus("OPEN");
            created.setFirstSeenAt(now);
            created.setLastSeenAt(now);
            created.setHitCount(1);
            created.setDedupeKey(dedupeKey);
            created.setSource(source == null ? "UNKNOWN" : source);
            created.setMetaJson(serialize(meta));
            alertMapper.insert(created);
            createNotificationIfNeeded(created, true, false);
            return created;
        }

        boolean escalated = severityRank(normalizedSeverity) > severityRank(existing.getSeverity());
        existing.setSeverity(normalizedSeverity);
        existing.setTitle(safe(title));
        existing.setMessage(safe(message));
        existing.setLastSeenAt(now);
        existing.setHitCount(existing.getHitCount() == null ? 1 : existing.getHitCount() + 1);
        existing.setMetaJson(serialize(meta));
        alertMapper.updateById(existing);
        createNotificationIfNeeded(existing, false, escalated);
        return existing;
    }

    @Override
    public boolean ackAlert(Long alertId) {
        return updateStatus(alertId, "ACKED");
    }

    @Override
    public boolean resolveAlert(Long alertId) {
        return updateStatus(alertId, "RESOLVED");
    }

    @Override
    public List<FcAlertEntity> listUserAlerts(Long userId, String status) {
        if (userId == null) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<FcAlertEntity> wrapper = new LambdaQueryWrapper<FcAlertEntity>()
                .eq(FcAlertEntity::getUserId, userId);
        if (status != null && !status.isBlank()) {
            wrapper.eq(FcAlertEntity::getStatus, status);
        }
        wrapper.orderByDesc(FcAlertEntity::getLastSeenAt);
        List<FcAlertEntity> list = alertMapper.selectList(wrapper);
        return list == null ? Collections.emptyList() : list;
    }

    @Override
    public List<FcAlertEntity> listAdminAlerts(String status, String alertType, String severity, Long userId) {
        LambdaQueryWrapper<FcAlertEntity> wrapper = new LambdaQueryWrapper<>();
        if (userId != null) {
            wrapper.eq(FcAlertEntity::getUserId, userId);
        }
        if (alertType != null && !alertType.isBlank()) {
            wrapper.eq(FcAlertEntity::getAlertType, alertType);
        }
        if (severity != null && !severity.isBlank()) {
            wrapper.eq(FcAlertEntity::getSeverity, severity);
        }
        if (status != null && !status.isBlank()) {
            wrapper.eq(FcAlertEntity::getStatus, status);
        }
        wrapper.orderByDesc(FcAlertEntity::getLastSeenAt);
        List<FcAlertEntity> list = alertMapper.selectList(wrapper);
        return list == null ? Collections.emptyList() : list;
    }

    private boolean updateStatus(Long alertId, String status) {
        if (alertId == null) {
            return false;
        }
        FcAlertEntity existing = alertMapper.selectById(alertId);
        if (existing == null) {
            return false;
        }
        existing.setStatus(status);
        alertMapper.updateById(existing);
        return true;
    }

    private void createNotificationIfNeeded(FcAlertEntity alert, boolean isNew, boolean escalated) {
        if (alert == null || alert.getUserId() == null) {
            return;
        }
        if (!isNew && !escalated) {
            return;
        }
        FcNotificationEntity notification = new FcNotificationEntity();
        notification.setUserId(alert.getUserId());
        notification.setTitle(alert.getTitle());
        notification.setBody(alert.getMessage());
        notification.setIsRead(0);
        notification.setCreatedAt(LocalDateTime.now());
        notification.setAlertId(alert.getId());
        notificationMapper.insert(notification);
    }

    private String serialize(Map<String, Object> meta) {
        if (meta == null || meta.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(meta);
        } catch (Exception e) {
            log.warn("[AlertService] serialize meta failed", e);
            return null;
        }
    }

    private String normalizeSeverity(String severity) {
        if (severity == null || severity.isBlank()) {
            return "INFO";
        }
        String upper = severity.trim().toUpperCase();
        if (!"INFO".equals(upper) && !"WARN".equals(upper) && !"DANGER".equals(upper)) {
            return "INFO";
        }
        return upper;
    }

    private int severityRank(String severity) {
        if (severity == null) return 0;
        return switch (severity) {
            case "WARN" -> 1;
            case "DANGER" -> 2;
            default -> 0;
        };
    }

    private String buildDedupeKey(Long userId, String alertType) {
        String userPart = userId == null ? "SYSTEM" : String.valueOf(userId);
        return userPart + ":" + (alertType == null ? "UNKNOWN" : alertType);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
