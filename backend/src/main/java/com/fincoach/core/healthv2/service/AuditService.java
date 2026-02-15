package com.fincoach.core.healthv2.service;

/**
 * 审计日志服务接口
 */
public interface AuditService {

    /**
     * 记录审计日志
     * @param actorUserId 操作用户ID
     * @param action      操作类型: CREATE/UPDATE/DELETE/GENERATE_REPORT
     * @param targetType  目标类型: FC_ASSET/FC_LIABILITY/...
     * @param targetId    目标ID
     * @param beforeObj   变更前对象（序列化为JSON）
     * @param afterObj    变更后对象（序列化为JSON）
     */
    void log(Long actorUserId, String action, String targetType, Long targetId, Object beforeObj, Object afterObj);
}
