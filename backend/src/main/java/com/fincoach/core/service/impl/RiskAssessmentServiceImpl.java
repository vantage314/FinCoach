package com.fincoach.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fincoach.core.common.RiskLevelEnum;
import com.fincoach.core.controller.dto.RiskAssessmentDTO;
import com.fincoach.core.controller.vo.PortfolioSummaryVO;
import com.fincoach.core.controller.vo.RiskAssessmentVO;
import com.fincoach.core.repository.entity.RiskAssessment;
import com.fincoach.core.repository.mapper.RiskAssessmentMapper;
import com.fincoach.core.service.AssetItemService;
import com.fincoach.core.service.RiskAssessmentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 风险测评服务实现
 * 核心逻辑：一票否决 + 分值映射 + 知行合一诊断
 */
@Slf4j
@Service
public class RiskAssessmentServiceImpl implements RiskAssessmentService {

    @Autowired
    private RiskAssessmentMapper riskAssessmentMapper;

    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private AssetItemService assetItemService;

    // 一票否决题目的 key
    private static final String KILLER_QUESTION_KEY = "q3";
    // 一票否决的分值阈值（选择"6个月内"时分值为0）
    private static final int KILLER_SCORE_THRESHOLD = 0;

    @Override
    public RiskAssessmentVO assess(Long userId, RiskAssessmentDTO dto) {
        Map<String, Integer> answers = dto.getAnswers();
        
        log.info("开始风险测评，用户ID: {}, 答卷: {}", userId, answers);
        
        int totalScore;
        RiskLevelEnum riskLevel;
        
        // 一票否决机制：检查 Q3（资金闲置时间）
        Integer killerAnswer = answers.get(KILLER_QUESTION_KEY);
        if (killerAnswer != null && killerAnswer <= KILLER_SCORE_THRESHOLD) {
            log.info("触发一票否决机制，用户ID: {}, Q3答案: {}", userId, killerAnswer);
            totalScore = 0;
            riskLevel = RiskLevelEnum.CONSERVATIVE;
        } else {
            // 常规计算：累加所有分数
            totalScore = answers.values().stream()
                    .mapToInt(Integer::intValue)
                    .sum();
            riskLevel = RiskLevelEnum.getByScore(totalScore);
        }
        
        log.info("测评计算完成，用户ID: {}, 总分: {}, 等级: {}", userId, totalScore, riskLevel.getCode());
        
        // 构建实体并落库
        RiskAssessment entity = new RiskAssessment();
        entity.setUserId(userId);
        entity.setTotalScore(totalScore);
        entity.setRiskLevel(riskLevel.getCode());
        entity.setCreateTime(LocalDateTime.now());
        
        try {
            entity.setAssessmentJson(objectMapper.writeValueAsString(answers));
        } catch (Exception e) {
            log.warn("序列化答卷失败", e);
            entity.setAssessmentJson("{}");
        }
        
        riskAssessmentMapper.insert(entity);
        log.info("测评结果已保存，记录ID: {}", entity.getId());
        
        return buildVO(entity, riskLevel, userId);
    }

    @Override
    public RiskAssessmentVO getLatest(Long userId) {
        LambdaQueryWrapper<RiskAssessment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RiskAssessment::getUserId, userId)
               .orderByDesc(RiskAssessment::getCreateTime)
               .last("LIMIT 1");
        
        RiskAssessment entity = riskAssessmentMapper.selectOne(wrapper);
        if (entity == null) {
            return null;
        }
        
        RiskLevelEnum riskLevel = RiskLevelEnum.getByCode(entity.getRiskLevel());
        return buildVO(entity, riskLevel, userId);
    }
    
    private RiskAssessmentVO buildVO(RiskAssessment entity, RiskLevelEnum riskLevel, Long userId) {
        RiskAssessmentVO vo = new RiskAssessmentVO();
        vo.setId(entity.getId());
        vo.setTotalScore(entity.getTotalScore());
        vo.setRiskLevel(riskLevel.getCode());
        vo.setLabel(riskLevel.getLabel());
        vo.setDescription(riskLevel.getDescription());
        vo.setEquityLimit(riskLevel.getEquityLimit());
        vo.setCreateTime(entity.getCreateTime());
        
        // ===== 知行合一诊断 =====
        calculateDiagnosis(vo, riskLevel, userId);
        
        return vo;
    }
    
    /**
     * 计算"知行合一"诊断
     */
    private void calculateDiagnosis(RiskAssessmentVO vo, RiskLevelEnum riskLevel, Long userId) {
        BigDecimal idealRatio = riskLevel.getEquityLimit();
        vo.setIdealRatio(idealRatio);
        
        // 获取用户当前持仓
        PortfolioSummaryVO summary = assetItemService.getPortfolioSummary(userId);
        BigDecimal totalAmount = summary.getTotalAmount();
        
        // 容错：总资产为 0
        if (totalAmount == null || totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            vo.setActualRatio(BigDecimal.ZERO);
            vo.setGap(idealRatio.negate());
            vo.setDiagnosis("📋 检测到您尚未录入资产，请先完善资产档案后再查看配置建议。");
            return;
        }
        
        // 计算权益占比 (categoryId=2 为金融投资/权益类)
        Map<String, BigDecimal> distribution = summary.getCategoryDistribution();
        BigDecimal equityAmount = distribution.getOrDefault("金融投资", BigDecimal.ZERO);
        
        BigDecimal actualRatio = equityAmount.divide(totalAmount, 4, RoundingMode.HALF_UP);
        vo.setActualRatio(actualRatio);
        
        BigDecimal gap = actualRatio.subtract(idealRatio);
        vo.setGap(gap);
        
        // 生成诊断文案
        String diagnosis;
        double gapValue = gap.doubleValue();
        
        if (gapValue < -0.2) {
            diagnosis = "⚠️ 您的配置过于保守，建议适当增加权益类投资（如股票、基金），避免资产长期跑输通胀。";
        } else if (gapValue > 0.1) {
            diagnosis = "🔴 您的风险敞口已超标！建议落袋为安，适度增加固收类配置（如债券、银行理财）以降低波动。";
        } else {
            diagnosis = "✅ 恭喜！您的知行合一做得很好，当前资产配置与风险偏好完美匹配，请继续保持！";
        }
        
        vo.setDiagnosis(diagnosis);
        log.info("诊断完成，用户ID: {}, 实际占比: {}, 理想占比: {}, 偏差: {}", 
                userId, actualRatio, idealRatio, gap);
    }
}
