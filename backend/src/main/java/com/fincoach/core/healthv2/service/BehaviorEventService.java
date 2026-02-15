package com.fincoach.core.healthv2.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fincoach.core.healthv2.entity.FcBehaviorEventEntity;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 体检v2-行为事件服务接口
 */
public interface BehaviorEventService {

    /**
     * 记录行为事件
     */
    void recordEvent(Long userId, String eventType, BigDecimal amount, Map<String, Object> meta);

    /**
     * 查询用户近N天事件
     */
    List<FcBehaviorEventEntity> listRecent(Long userId, int days, int limit);

    /**
     * 分页查询用户事件（按时间倒序）
     */
    IPage<FcBehaviorEventEntity> listByUser(Long userId, int page, int size, String eventType);

    /**
     * 统计用户近N天各事件类型数量
     */
    Map<String, Integer> countByType(Long userId, int days);

    /**
     * 判断近N天内是否有指定事件
     */
    boolean hasEventInDays(Long userId, String eventType, int days);

    /**
     * 兼容旧调用
     */
    default void record(Long userId, String eventType, BigDecimal amount, Map<String, Object> meta) {
        recordEvent(userId, eventType, amount, meta);
    }

    /**
     * 兼容旧调用
     */
    default IPage<FcBehaviorEventEntity> page(Long userId, int page, int size) {
        return listByUser(userId, page, size, null);
    }
}
