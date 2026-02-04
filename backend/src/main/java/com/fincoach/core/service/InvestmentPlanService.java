package com.fincoach.core.service;

import com.fincoach.core.controller.vo.InvestmentPlanVO;

import java.math.BigDecimal;
import java.util.List;

/**
 * 调仓计划服务
 */
public interface InvestmentPlanService {
    
    /**
     * 生成调仓计划（预览，不保存）
     * @param userId 用户ID
     * @param planType 计划类型: CONTRIBUTION (新增资金), REBALANCE (存量调整)
     * @param investMoney 本次投入的新资金额度
     */
    InvestmentPlanVO generatePlan(Long userId, String planType, BigDecimal investMoney);
    
    /**
     * 保存调仓计划
     */
    Long savePlan(Long userId, InvestmentPlanVO plan);
    
    /**
     * 获取历史计划列表
     */
    List<InvestmentPlanVO> getHistory(Long userId);
    
    /**
     * 执行调仓计划
     * 将建议转化为真实的资产记录
     */
    void executePlan(Long userId, Long planId);

    /**
     * 标记计划为已执行状态
     */
    void markExecuted(Long userId, Long planId);
}
