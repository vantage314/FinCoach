package com.fincoach.core.healthv2.service;

import com.fincoach.core.healthv2.dto.HealthReportV2VO;
import com.fincoach.core.healthv2.entity.FcHealthReportEntity;

/**
 * 体检v2-体检报告服务接口
 */
public interface HealthReportV2Service {

    /**
     * 生成体检报告（M1 核心方法）
     * @param userId 用户ID
     * @return 报告 VO
     */
    HealthReportV2VO generate(Long userId);

    /**
     * 获取用户最新报告
     */
    HealthReportV2VO getLatest(Long userId);

    /**
     * 按 ID 获取报告
     */
    HealthReportV2VO getById(Long reportId);
}
