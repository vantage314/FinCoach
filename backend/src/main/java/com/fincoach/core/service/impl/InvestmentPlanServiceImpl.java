package com.fincoach.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fincoach.core.common.RiskLevelEnum;
import com.fincoach.core.controller.vo.InvestmentPlanVO;
import com.fincoach.core.controller.vo.InvestmentPlanVO.PlanItemVO;
import com.fincoach.core.controller.vo.PortfolioSummaryVO;
import com.fincoach.core.controller.vo.RiskAssessmentVO;
import com.fincoach.core.repository.entity.InvestmentPlan;
import com.fincoach.core.repository.entity.PlanItem;
import com.fincoach.core.repository.mapper.InvestmentPlanMapper;
import com.fincoach.core.repository.mapper.PlanItemMapper;
import com.fincoach.core.service.AssetItemService;
import com.fincoach.core.service.InvestmentPlanService;
import com.fincoach.core.service.RiskAssessmentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 调仓计划服务实现
 * 核心算法：基于目标偏离度的再平衡
 */
@Slf4j
@Service
public class InvestmentPlanServiceImpl implements InvestmentPlanService {

    @Autowired
    private InvestmentPlanMapper planMapper;

    @Autowired
    private PlanItemMapper itemMapper;

    @Autowired
    private AssetItemService assetItemService;

    @Autowired
    private RiskAssessmentService riskAssessmentService;

    // 最小操作阈值（防止建议买 1 块钱）
    private static final BigDecimal MIN_THRESHOLD = new BigDecimal("1000");

    // 各风险等级的目标配置
    private static final Map<String, Map<String, BigDecimal>> TARGET_RATIOS = new HashMap<>();
    
    static {
        // 保守型
        Map<String, BigDecimal> conservative = new HashMap<>();
        conservative.put("现金储蓄", new BigDecimal("0.30"));
        conservative.put("金融投资", new BigDecimal("0.10"));
        conservative.put("固定资产", new BigDecimal("0.60"));
        TARGET_RATIOS.put("conservative", conservative);
        
        // 稳健型
        Map<String, BigDecimal> steady = new HashMap<>();
        steady.put("现金储蓄", new BigDecimal("0.20"));
        steady.put("金融投资", new BigDecimal("0.25"));
        steady.put("固定资产", new BigDecimal("0.55"));
        TARGET_RATIOS.put("steady", steady);
        
        // 平衡型
        Map<String, BigDecimal> balanced = new HashMap<>();
        balanced.put("现金储蓄", new BigDecimal("0.15"));
        balanced.put("金融投资", new BigDecimal("0.40"));
        balanced.put("固定资产", new BigDecimal("0.45"));
        TARGET_RATIOS.put("balanced", balanced);
        
        // 进取型
        Map<String, BigDecimal> growth = new HashMap<>();
        growth.put("现金储蓄", new BigDecimal("0.10"));
        growth.put("金融投资", new BigDecimal("0.60"));
        growth.put("固定资产", new BigDecimal("0.30"));
        TARGET_RATIOS.put("growth", growth);
        
        // 激进型
        Map<String, BigDecimal> aggressive = new HashMap<>();
        aggressive.put("现金储蓄", new BigDecimal("0.05"));
        aggressive.put("金融投资", new BigDecimal("0.80"));
        aggressive.put("固定资产", new BigDecimal("0.15"));
        TARGET_RATIOS.put("aggressive", aggressive);
    }

    // 分类 ID 映射
    private static final Map<String, Integer> CATEGORY_IDS = new HashMap<>();
    static {
        CATEGORY_IDS.put("现金储蓄", 1);
        CATEGORY_IDS.put("金融投资", 2);
        CATEGORY_IDS.put("固定资产", 3);
    }

    @Override
    public InvestmentPlanVO generatePlan(Long userId, String planType, BigDecimal investMoney) {
        log.info("开始生成调仓计划，用户ID: {}, 类型: {}, 新资金: {}", userId, planType, investMoney);
        
        // 获取用户风险等级
        RiskAssessmentVO riskProfile = riskAssessmentService.getLatest(userId);
        String riskLevel = riskProfile != null ? riskProfile.getRiskLevel() : "balanced";
        String riskLabel = riskProfile != null ? riskProfile.getLabel() : "平衡探索者";
        
        // 获取当前资产分布
        PortfolioSummaryVO summary = assetItemService.getPortfolioSummary(userId);
        BigDecimal currentTotal = summary.getTotalAmount() != null ? summary.getTotalAmount() : BigDecimal.ZERO;
        Map<String, BigDecimal> currentDist = summary.getCategoryDistribution();
        
        // 计算目标总资产 (当前总资产 + 新增资金)
        BigDecimal targetTotal = currentTotal.add(investMoney != null ? investMoney : BigDecimal.ZERO);
        
        // 获取目标配置比例
        Map<String, BigDecimal> targetRatios = TARGET_RATIOS.getOrDefault(riskLevel, TARGET_RATIOS.get("balanced"));
        
        // 构建计划 VO
        InvestmentPlanVO plan = new InvestmentPlanVO();
        plan.setRiskLevel(riskLevel);
        plan.setRiskLabel(riskLabel);
        plan.setTotalAmount(currentTotal);
        plan.setPlanType(planType);
        plan.setInvestMoney(investMoney);
        plan.setStatus("draft");
        plan.setCreateTime(LocalDateTime.now());
        
        List<PlanItemVO> items = new ArrayList<>();
        
        // 如果是增量模式且没有总资产，则只按比例分配新资金
        boolean isContribution = "CONTRIBUTION".equals(planType);
        
        // 计算每个分类的目标金额和缺口
        for (Map.Entry<String, BigDecimal> entry : targetRatios.entrySet()) {
            String category = entry.getKey();
            BigDecimal targetRatio = entry.getValue();
            
            BigDecimal currentAmount = currentDist.getOrDefault(category, BigDecimal.ZERO);
            BigDecimal targetAmount = targetTotal.multiply(targetRatio);
            BigDecimal diff = targetAmount.subtract(currentAmount);
            
            // 模式 A：增量模式 (只买不卖，除非严重超标)
            if (isContribution) {
                // 在增量模式下，我们主要关注如何分配 investMoney
                // 如果 diff > 0，说明该分类缺钱，我们可以从 investMoney 中划拨
                if (diff.compareTo(BigDecimal.ZERO) > 0) {
                    PlanItemVO item = new PlanItemVO();
                    item.setCategoryId(CATEGORY_IDS.get(category));
                    item.setCategoryName(category);
                    item.setCurrentRatio(currentTotal.compareTo(BigDecimal.ZERO) > 0 ? 
                            currentAmount.divide(currentTotal, 4, RoundingMode.HALF_UP) : BigDecimal.ZERO);
                    item.setTargetRatio(targetRatio);
                    
                    // 实际分配金额不能超过 diff，也不能超过剩余的 investMoney (简化逻辑：按缺口比例分配)
                    // 这里采用简化逻辑：直接显示目标需要补足的差额
                    item.setAmount(diff.setScale(2, RoundingMode.HALF_UP));
                    item.setAction("BUY");
                    item.setSubType(getSubType(category, currentAmount));
                    item.setReason(String.format("根据您的风险偏好，%s类资产尚有缺口，建议优先补足。", category));
                    items.add(item);
                }
            } else {
                // 模式 B：存量再平衡 (原有逻辑)
                if (diff.abs().compareTo(MIN_THRESHOLD) >= 0) {
                    PlanItemVO item = new PlanItemVO();
                    item.setCategoryId(CATEGORY_IDS.get(category));
                    item.setCategoryName(category);
                    item.setCurrentRatio(currentTotal.compareTo(BigDecimal.ZERO) > 0 ? 
                            currentAmount.divide(currentTotal, 4, RoundingMode.HALF_UP) : BigDecimal.ZERO);
                    item.setTargetRatio(targetRatio);
                    item.setAmount(diff.abs().setScale(2, RoundingMode.HALF_UP));
                    
                    if (diff.compareTo(BigDecimal.ZERO) > 0) {
                        item.setAction("BUY");
                        item.setSubType(getSubType(category, currentAmount));
                        item.setReason(generateBuyReason(category, item.getCurrentRatio(), targetRatio));
                    } else {
                        item.setAction("SELL");
                        item.setReason(generateSellReason(category, item.getCurrentRatio(), targetRatio));
                    }
                    items.add(item);
                }
            }
        }
        
        // 排序：先卖后买
        items.sort((a, b) -> {
            if ("SELL".equals(a.getAction()) && "BUY".equals(b.getAction())) return -1;
            if ("BUY".equals(a.getAction()) && "SELL".equals(b.getAction())) return 1;
            return 0;
        });
        
        if (items.isEmpty()) {
            PlanItemVO item = new PlanItemVO();
            item.setAction("INFO");
            item.setCategoryName("系统提示");
            item.setReason("✅ 您的资产配置已经非常接近目标，暂无调仓建议！");
            items.add(item);
        }
        
        plan.setItems(items);
        return plan;
    }

    @Override
    @Transactional
    public Long savePlan(Long userId, InvestmentPlanVO planVO) {
        log.info("保存调仓计划，用户ID: {}, 计划名称: {}", userId, planVO.getPlanName());
        
        InvestmentPlan plan = new InvestmentPlan();
        plan.setUserId(userId);
        plan.setPlanName(planVO.getPlanName());
        plan.setRiskLevel(planVO.getRiskLevel());
        plan.setTotalAmount(planVO.getTotalAmount());
        plan.setPlanType(planVO.getPlanType());
        plan.setInvestMoney(planVO.getInvestMoney());
        plan.setStatus("saved");
        plan.setCreateTime(LocalDateTime.now());
        planMapper.insert(plan);
        
        for (PlanItemVO itemVO : planVO.getItems()) {
            if ("INFO".equals(itemVO.getAction())) continue;
            
            PlanItem item = new PlanItem();
            item.setPlanId(plan.getId());
            item.setAction(itemVO.getAction());
            item.setCategoryId(itemVO.getCategoryId());
            item.setCategoryName(itemVO.getCategoryName());
            item.setSubType(itemVO.getSubType());
            item.setAmount(itemVO.getAmount());
            item.setCurrentRatio(itemVO.getCurrentRatio());
            item.setTargetRatio(itemVO.getTargetRatio());
            item.setReason(itemVO.getReason());
            item.setCreateTime(LocalDateTime.now());
            itemMapper.insert(item);
        }
        
        return plan.getId();
    }

    @Override
    public List<InvestmentPlanVO> getHistory(Long userId) {
        LambdaQueryWrapper<InvestmentPlan> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InvestmentPlan::getUserId, userId)
               .orderByDesc(InvestmentPlan::getCreateTime)
               .last("LIMIT 10");
        
        List<InvestmentPlan> plans = planMapper.selectList(wrapper);
        List<InvestmentPlanVO> result = new ArrayList<>();
        
        for (InvestmentPlan plan : plans) {
            InvestmentPlanVO vo = new InvestmentPlanVO();
            vo.setId(plan.getId());
            vo.setPlanName(plan.getPlanName());
            vo.setRiskLevel(plan.getRiskLevel());
            vo.setRiskLabel(RiskLevelEnum.getByCode(plan.getRiskLevel()).getLabel());
            vo.setTotalAmount(plan.getTotalAmount());
            vo.setPlanType(plan.getPlanType());
            vo.setInvestMoney(plan.getInvestMoney());
            vo.setStatus(plan.getStatus());
            vo.setCreateTime(plan.getCreateTime());
            
            LambdaQueryWrapper<PlanItem> itemWrapper = new LambdaQueryWrapper<>();
            itemWrapper.eq(PlanItem::getPlanId, plan.getId());
            List<PlanItem> items = itemMapper.selectList(itemWrapper);
            
            List<PlanItemVO> itemVOs = new ArrayList<>();
            for (PlanItem item : items) {
                PlanItemVO itemVO = new PlanItemVO();
                itemVO.setId(item.getId());
                itemVO.setAction(item.getAction());
                itemVO.setCategoryId(item.getCategoryId());
                itemVO.setCategoryName(item.getCategoryName());
                itemVO.setSubType(item.getSubType());
                itemVO.setAmount(item.getAmount());
                itemVO.setCurrentRatio(item.getCurrentRatio());
                itemVO.setTargetRatio(item.getTargetRatio());
                itemVO.setReason(item.getReason());
                itemVOs.add(itemVO);
            }
            vo.setItems(itemVOs);
            result.add(vo);
        }
        return result;
    }

    @Override
    @Transactional
    public void executePlan(Long userId, Long planId) {
        log.info("一键执行调仓计划，用户ID: {}, 计划ID: {}", userId, planId);
        InvestmentPlan plan = planMapper.selectById(planId);
        if (plan == null || !plan.getUserId().equals(userId)) {
            throw new RuntimeException("计划不存在或无权操作");
        }
        if ("executed".equals(plan.getStatus())) {
            throw new RuntimeException("计划已执行，请勿重复操作");
        }

        LambdaQueryWrapper<PlanItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PlanItem::getPlanId, planId);
        List<PlanItem> items = itemMapper.selectList(wrapper);

        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("MMdd");
        String dateSuffix = LocalDateTime.now().format(formatter);

        for (PlanItem item : items) {
            if ("INFO".equals(item.getAction())) continue;

            if ("BUY".equals(item.getAction())) {
                // 买入逻辑：创建新记录
                com.fincoach.core.repository.entity.AssetItem newAsset = new com.fincoach.core.repository.entity.AssetItem();
                newAsset.setUserId(userId);
                newAsset.setCategoryId(item.getCategoryId());
                
                // 命名规则: 新入库-[子类/大类]-[日期]
                String assetLabel = item.getSubType() != null ? item.getSubType() : item.getCategoryName();
                newAsset.setAssetName("新入库-" + assetLabel + "-" + dateSuffix);
                
                newAsset.setCurrentValue(item.getAmount());
                newAsset.setHoldingCost(item.getAmount()); // 初始成本设为买入金额
                newAsset.setSubType(item.getSubType());
                newAsset.setUpdateTime(LocalDateTime.now());
                assetItemService.internalAddAsset(newAsset);
            } else if ("SELL".equals(item.getAction())) {
                // 卖出逻辑：查找该分类下金额最大的资产进行扣减
                com.fincoach.core.repository.entity.AssetItem largestAsset = assetItemService.getLargestByCategory(userId, item.getCategoryId());
                
                if (largestAsset == null) {
                    log.warn("用户没有该分类资产，跳过卖出建议: {}", item.getCategoryName());
                    continue;
                }
                
                BigDecimal newAmount = largestAsset.getCurrentValue().subtract(item.getAmount());
                if (newAmount.compareTo(BigDecimal.ZERO) < 0) {
                    // 如果最大的一笔都不够扣，强制提示异常
                    throw new RuntimeException(String.format("资产余额不足以执行卖出建议：[%s] 差额 ¥%s，请手动调整。", 
                        largestAsset.getAssetName(), item.getAmount().subtract(largestAsset.getCurrentValue())));
                }
                
                largestAsset.setCurrentValue(newAmount);
                largestAsset.setUpdateTime(LocalDateTime.now());
                assetItemService.updateAsset(largestAsset);
            }
        }

        plan.setStatus("executed");
        planMapper.updateById(plan);
        log.info("调仓计划执行成功且数据已同步");
    }

    @Override
    @Transactional
    public void markExecuted(Long userId, Long planId) {
        InvestmentPlan plan = planMapper.selectById(planId);
        if (plan != null && plan.getUserId().equals(userId)) {
            plan.setStatus("executed");
            planMapper.updateById(plan);
        }
    }

    private String getSubType(String category, BigDecimal currentAmount) {
        if ("金融投资".equals(category)) {
            if (currentAmount.compareTo(BigDecimal.ZERO) <= 0) {
                return "宽基指数基金（建议新开仓）";
            } else {
                return "对现有持仓进行加仓";
            }
        }
        return null;
    }

    private String generateBuyReason(String category, BigDecimal current, BigDecimal target) {
        int currentPct = current.multiply(new BigDecimal("100")).intValue();
        int targetPct = target.multiply(new BigDecimal("100")).intValue();
        
        if ("金融投资".equals(category)) {
            return String.format("当前权益占比 %d%%，低于目标 %d%%，需补足仓位以追求长期增值。", currentPct, targetPct);
        } else if ("现金储蓄".equals(category)) {
            return String.format("当前现金占比 %d%%，低于目标 %d%%，建议增加流动资金以应对突发支出。", currentPct, targetPct);
        } else {
            return String.format("当前占比 %d%%，低于目标 %d%%，建议适当增加配置。", currentPct, targetPct);
        }
    }

    private String generateSellReason(String category, BigDecimal current, BigDecimal target) {
        int currentPct = current.multiply(new BigDecimal("100")).intValue();
        int targetPct = target.multiply(new BigDecimal("100")).intValue();
        
        if ("现金储蓄".equals(category)) {
            return String.format("当前现金占比 %d%%，高于目标 %d%%，资金闲置过多，建议转出部分用于投资。", currentPct, targetPct);
        } else if ("金融投资".equals(category)) {
            return String.format("当前权益占比 %d%%，超过目标 %d%%，建议落袋为安，适度降低风险敞口。", currentPct, targetPct);
        } else {
            return String.format("当前占比 %d%%，高于目标 %d%%，建议适当减持。", currentPct, targetPct);
        }
    }
}
}
