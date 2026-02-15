package com.fincoach.core.healthv2.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fincoach.core.healthv2.entity.FcNotificationEntity;
import com.fincoach.core.healthv2.mapper.FcNotificationMapper;
import com.fincoach.core.healthv2.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

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
    public IPage<FcNotificationEntity> list(Long userId, Integer isRead, int page, int size) {
        LambdaQueryWrapper<FcNotificationEntity> qw = new LambdaQueryWrapper<FcNotificationEntity>()
                .eq(FcNotificationEntity::getUserId, userId)
                .orderByDesc(FcNotificationEntity::getCreatedAt);
        if (isRead != null) {
            qw.eq(FcNotificationEntity::getIsRead, isRead);
        }
        return notificationMapper.selectPage(new Page<>(page, size), qw);
    }

    @Override
    public FcNotificationEntity getById(Long notificationId) {
        return notificationMapper.selectById(notificationId);
    }

    @Override
    public boolean markRead(Long userId, Long notificationId) {
        int rows = notificationMapper.update(null,
                new LambdaUpdateWrapper<FcNotificationEntity>()
                        .eq(FcNotificationEntity::getId, notificationId)
                        .eq(FcNotificationEntity::getUserId, userId)
                        .set(FcNotificationEntity::getIsRead, 1));
        return rows > 0;
    }

    @Override
    public int markAllRead(Long userId) {
        return notificationMapper.update(null,
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
