package com.fincoach.core.service;

import com.fincoach.core.controller.vo.HealthReportVO;

/**
 * 资产健康度检测服务
 */
public interface HealthCheckService {
    
    /**
     * 执行资产健康度体检
     * @param userId 用户ID
     * @return 健康度报告
     */
    HealthReportVO checkHealth(Long userId);
}
