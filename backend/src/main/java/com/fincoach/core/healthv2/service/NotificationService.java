package com.fincoach.core.healthv2.service;

import com.fincoach.core.healthv2.entity.FcNotificationEntity;

import java.util.List;

/**
 * 体检v2-通知服务接口
 */
public interface NotificationService {

    /**
     * 创建通知
     */
    void create(Long userId, String type, String title, String content, String payloadJson);

    /**
     * 查询通知列表（支持按已读/未读筛选）
     */
    List<FcNotificationEntity> list(Long userId, Integer isRead, int size);

    /**
     * 标记单条通知已读
     */
    void markRead(Long userId, Long notificationId);

    /**
     * 标记全部已读
     */
    void markAllRead(Long userId);

    /**
     * 获取未读数
     */
    long countUnread(Long userId);
}
