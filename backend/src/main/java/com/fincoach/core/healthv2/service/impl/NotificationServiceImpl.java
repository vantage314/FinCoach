package com.fincoach.core.healthv2.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fincoach.core.healthv2.entity.FcNotificationEntity;
import com.fincoach.core.healthv2.mapper.FcNotificationMapper;
import com.fincoach.core.healthv2.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 通知服务实现
 */
@Slf4j
@Service
public class NotificationServiceImpl implements NotificationService {

    @Autowired
    private FcNotificationMapper notificationMapper;

    @Override
    public void create(Long userId, String type, String title, String content, String payloadJson) {
        FcNotificationEntity entity = new FcNotificationEntity();
        entity.setUserId(userId);
        entity.setType(type);
        entity.setTitle(title);
        entity.setContent(content);
        entity.setPayloadJson(payloadJson);
        entity.setIsRead(0);
        entity.setCreatedAt(LocalDateTime.now());
        notificationMapper.insert(entity);
        log.info("[Notification] created: userId={}, type={}, title={}", userId, type, title);
    }

    @Override
    public List<FcNotificationEntity> list(Long userId, Integer isRead, int size) {
        LambdaQueryWrapper<FcNotificationEntity> qw = new LambdaQueryWrapper<FcNotificationEntity>()
                .eq(FcNotificationEntity::getUserId, userId)
                .orderByDesc(FcNotificationEntity::getCreatedAt)
                .last("LIMIT " + size);
        if (isRead != null) {
            qw.eq(FcNotificationEntity::getIsRead, isRead);
        }
        return notificationMapper.selectList(qw);
    }

    @Override
    public void markRead(Long userId, Long notificationId) {
        notificationMapper.update(null,
                new LambdaUpdateWrapper<FcNotificationEntity>()
                        .eq(FcNotificationEntity::getId, notificationId)
                        .eq(FcNotificationEntity::getUserId, userId)
                        .set(FcNotificationEntity::getIsRead, 1));
    }

    @Override
    public void markAllRead(Long userId) {
        notificationMapper.update(null,
                new LambdaUpdateWrapper<FcNotificationEntity>()
                        .eq(FcNotificationEntity::getUserId, userId)
                        .eq(FcNotificationEntity::getIsRead, 0)
                        .set(FcNotificationEntity::getIsRead, 1));
    }

    @Override
    public long countUnread(Long userId) {
        Long count = notificationMapper.selectCount(
                new LambdaQueryWrapper<FcNotificationEntity>()
                        .eq(FcNotificationEntity::getUserId, userId)
                        .eq(FcNotificationEntity::getIsRead, 0));
        return count != null ? count : 0;
    }
}
