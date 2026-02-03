package com.fincoach.core.service;

import com.fincoach.core.controller.dto.RiskAssessmentDTO;
import com.fincoach.core.controller.vo.RiskAssessmentVO;

/**
 * 风险测评服务接口
 */
public interface RiskAssessmentService {
    
    /**
     * 提交风险测评
     * @param userId 用户ID
     * @param dto 答卷数据
     * @return 测评结果
     */
    RiskAssessmentVO assess(Long userId, RiskAssessmentDTO dto);
    
    /**
     * 获取用户最新测评结果
     * @param userId 用户ID
     * @return 测评结果，不存在则返回 null
     */
    RiskAssessmentVO getLatest(Long userId);
}
