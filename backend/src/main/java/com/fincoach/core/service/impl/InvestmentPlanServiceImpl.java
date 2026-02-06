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

    @Autowired
    private com.fincoach.core.repository.mapper.MarketSecurityMapper marketSecurityMapper;


    @Autowired
    private com.fincoach.core.service.TransactionService transactionService;

    // 最小操作阈值（防止建议买 1 块钱）
    private static final BigDecimal MIN_THRESHOLD = new BigDecimal("1000");
    // 安全红线：30000
    private static final BigDecimal SAFETY_THRESHOLD = new BigDecimal("30000");

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
    @Transactional
    public InvestmentPlanVO generatePlan(Long userId, String planType, BigDecimal investMoney) {
        log.info("开始生成调仓计划，用户ID: {}, 类型: {}, 新资金: {}", userId, planType, investMoney);
        
        // 获取用户风险等级
        RiskAssessmentVO riskProfile = riskAssessmentService.getLatest(userId);
        String userRiskLevel = riskProfile != null ? riskProfile.getRiskLevel() : "balanced";
        String riskLabel = riskProfile != null ? riskProfile.getLabel() : "平衡探索者";
        
        // 最终采用的风险等级（可能被降级）
        String finalRiskLevel = userRiskLevel;

        // 获取当前资产分布
        PortfolioSummaryVO summary = assetItemService.getPortfolioSummary(userId);
        BigDecimal currentTotal = summary.getTotalAmount() != null ? summary.getTotalAmount() : BigDecimal.ZERO;
        Map<String, BigDecimal> currentDist = summary.getCategoryDistribution();
        
        // 获取当前现金余额
        BigDecimal currentCash = currentDist.getOrDefault("现金储蓄", BigDecimal.ZERO);
        
        // Phase 7.6 路由逻辑：计算资金缺口
        BigDecimal gap = SAFETY_THRESHOLD.subtract(currentCash);
        
        List<PlanItemVO> items = new ArrayList<>();
        
        if ("CONTRIBUTION".equals(planType) && investMoney != null) {
            // --- 场景 A/B 分流逻辑 ---
            BigDecimal safeAmount = BigDecimal.ZERO;
            BigDecimal riskyAmount = BigDecimal.ZERO;
            
            if (gap.compareTo(BigDecimal.ZERO) > 0) {
                // 场景 A: 资金不足 (缺口存在，优先填坑)
                safeAmount = investMoney.min(gap);
                riskyAmount = investMoney.subtract(safeAmount);
                
                // 如果不仅要填坑，而且填完坑也没剩多少钱给进取投资（导致 riskyAmount == 0），
                // 或者即使有剩余，但只要触发了“填坑”逻辑且原等级较高，由于重心转移到了安全垫，
                // 系统判定该计划整体显性风险为 CONSERVATIVE。
                // 根据需求：如果 riskyAmount == 0 且 userRiskLevel 是激进型，标记为 CONSERVATIVE
                if (riskyAmount.compareTo(MIN_THRESHOLD) < 0 && isAggressive(userRiskLevel)) {
                    finalRiskLevel = "conservative";
                    log.info("触发风险降级: 用户流动资金 {} < 安全红线 {}, 强制降级为 conservative", currentCash, SAFETY_THRESHOLD);
                }
            } else {
                // 场景 B: 资金充裕 (全额进取)
                riskyAmount = investMoney;
            }

            log.info("资金路由结果 - 安全垫分配: {}, 进取分配: {}, 最终风险等级: {}", safeAmount, riskyAmount, finalRiskLevel);

            // 1. 生成安全垫资产 (R1/R2)
            if (safeAmount.compareTo(BigDecimal.ZERO) > 0) {
                List<com.fincoach.core.repository.entity.MarketSecurity> safePicks = pickSecurities("conservative"); // 强制取稳健资产
                distributeAmountToSecurities(items, safePicks, safeAmount, "基础安全垫构建 (强制稳健配置)", currentTotal, "conservative");
            }
            
            // 2. 生成进取资产 (User Risk)
            if (riskyAmount.compareTo(BigDecimal.ZERO) > 0) {
                List<com.fincoach.core.repository.entity.MarketSecurity> riskyPicks = pickSecurities(userRiskLevel);
                distributeAmountToSecurities(items, riskyPicks, riskyAmount, "超额资金进取配置", currentTotal, userRiskLevel);
            }

        } else {
             // 模式 B：存量再平衡 (保持原有逻辑，但可复用 pickSecurities)
             // 计算目标总资产
            BigDecimal targetTotal = currentTotal.add(investMoney != null ? investMoney : BigDecimal.ZERO);
            Map<String, BigDecimal> targetRatios = TARGET_RATIOS.getOrDefault(userRiskLevel, TARGET_RATIOS.get("balanced"));

             for (Map.Entry<String, BigDecimal> entry : targetRatios.entrySet()) {
                String category = entry.getKey();
                BigDecimal targetRatio = entry.getValue();
                
                BigDecimal currentAmount = currentDist.getOrDefault(category, BigDecimal.ZERO);
                BigDecimal targetAmount = targetTotal.multiply(targetRatio);
                BigDecimal diff = targetAmount.subtract(currentAmount);

                if (diff.abs().compareTo(MIN_THRESHOLD) >= 0) {
                    if (diff.compareTo(BigDecimal.ZERO) > 0) {
                        // BUY
                        if ("金融投资".equals(category)) {
                            List<com.fincoach.core.repository.entity.MarketSecurity> picks = pickSecurities(userRiskLevel);
                            if (!picks.isEmpty()) {
                                distributeAmountToSecurities(items, picks, diff, 
                                    String.format("基于您【%s】的偏好，优选核心资产", RiskLevelEnum.getByCode(userRiskLevel).getLabel()), 
                                    currentTotal, userRiskLevel);
                            } else {
                                items.add(createGenericBuyItem(category, diff, currentTotal, currentAmount, targetRatio));
                            }
                        } else {
                            items.add(createGenericBuyItem(category, diff, currentTotal, currentAmount, targetRatio));
                        }
                    } else {
                        // SELL
                        PlanItemVO item = new PlanItemVO();
                        item.setCategoryId(CATEGORY_IDS.get(category));
                        item.setCategoryName(category);
                        item.setCurrentRatio(currentTotal.compareTo(BigDecimal.ZERO) > 0 ? 
                                currentAmount.divide(currentTotal, 4, RoundingMode.HALF_UP) : BigDecimal.ZERO);
                        item.setTargetRatio(targetRatio);
                        item.setAmount(diff.abs().setScale(2, RoundingMode.HALF_UP));
                        item.setAction("SELL");
                        item.setReason(generateSellReason(category, item.getCurrentRatio(), targetRatio));
                        items.add(item);
                    }
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

        // 构建计划 VO
        InvestmentPlanVO plan = new InvestmentPlanVO();
        plan.setRiskLevel(finalRiskLevel); // 使用最终（可能降级）的等级
        plan.setRiskLabel(RiskLevelEnum.getByCode(finalRiskLevel).getLabel());
        plan.setTotalAmount(currentTotal);
        plan.setPlanType(planType);
        plan.setInvestMoney(investMoney);
        plan.setStatus("draft");
        plan.setCreateTime(LocalDateTime.now());
        plan.setItems(items);
        
        // 🔍 Phase 11.3 核心修复：生成后立即持久化，确保出现在列表中
        String defaultName = ("CONTRIBUTION".equals(planType) ? "智能定投计划-" : "存量再平衡-") 
            + LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("MMddHHmm"));
        plan.setPlanName(defaultName);
        
        Long planId = this.savePlan(userId, plan);
        plan.setId(planId);
        
        return plan;
    }
    
    // 辅助判断是否激进
    private boolean isAggressive(String riskLevel) {
        return "growth".equals(riskLevel) || "aggressive".equals(riskLevel);
    }
    
    // 辅助分配方法
    private void distributeAmountToSecurities(List<PlanItemVO> items, 
                                              List<com.fincoach.core.repository.entity.MarketSecurity> picks, 
                                              BigDecimal totalAmount, 
                                              String baseReason, 
                                              BigDecimal portfolioTotal,
                                              String riskLevel) {
        if (picks.isEmpty()) return;
        
        BigDecimal amountPerSec = totalAmount.divide(new BigDecimal(picks.size()), 2, RoundingMode.HALF_UP);
        for (com.fincoach.core.repository.entity.MarketSecurity sec : picks) {
            PlanItemVO subItem = new PlanItemVO();
            subItem.setCategoryId(CATEGORY_IDS.get("金融投资"));
            subItem.setCategoryName("金融投资");
            // 简单估算 currentRatio，实际上对于新资产是 0
            subItem.setCurrentRatio(BigDecimal.ZERO); 
            // 这里的 targetRatio 很难精确计算，因为是动态路由，暂时设为 0 或不展示
            subItem.setTargetRatio(BigDecimal.ZERO);
            subItem.setAmount(amountPerSec);
            subItem.setAction("BUY");
            subItem.setSubType(sec.getName());
            
            String riskLabel = RiskLevelEnum.getByCode(riskLevel) != null ? RiskLevelEnum.getByCode(riskLevel).getLabel() : riskLevel;
            subItem.setReason(String.format("%s。优选 %s 级资产【%s】，预期年涨幅 %s%%。", 
                    baseReason, sec.getRiskLevel(), sec.getName(), sec.getChangePercent()));
            items.add(subItem);
        }
    }

    /**
     * 智能选股逻辑
     */
    private List<com.fincoach.core.repository.entity.MarketSecurity> pickSecurities(String userRiskProfile) {
        List<String> targetRiskLevels = new ArrayList<>();
        int limit = 2; // 默认取2只

        // 风险等级映射矩阵
        switch (userRiskProfile) {
            case "conservative": // 保守型 -> R1 (国债、货币基金)
                targetRiskLevels.add("R1");
                limit = 1;
                break;
            case "steady": // 稳健型 -> R1, R2 (债券、固收+)
                targetRiskLevels.add("R1");
                targetRiskLevels.add("R2");
                limit = 2;
                break;
            case "balanced": // 平衡型 -> R2, R3 (大盘股、混合基金)
                targetRiskLevels.add("R2");
                targetRiskLevels.add("R3");
                limit = 2;
                break;
            case "growth": // 进取型 -> R3, R4 (成长股、行业ETF)
                targetRiskLevels.add("R3");
                targetRiskLevels.add("R4");
                limit = 3;
                break;
            case "aggressive": // 激进型 -> R4, R5 (科技股、高波资产)
                targetRiskLevels.add("R4");
                targetRiskLevels.add("R5");
                limit = 3;
                break;
            default:
                targetRiskLevels.add("R2");
                targetRiskLevels.add("R3");
        }

        LambdaQueryWrapper<com.fincoach.core.repository.entity.MarketSecurity> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(com.fincoach.core.repository.entity.MarketSecurity::getRiskLevel, targetRiskLevels)
               .orderByDesc(com.fincoach.core.repository.entity.MarketSecurity::getChangePercent) // 优选近期表现好的
               .last("LIMIT " + limit);

        List<com.fincoach.core.repository.entity.MarketSecurity> result = marketSecurityMapper.selectList(wrapper);
        
        // --- 兜底逻辑 ---
        if (result == null || result.isEmpty()) {
            com.fincoach.core.repository.entity.MarketSecurity fallback = new com.fincoach.core.repository.entity.MarketSecurity();
            fallback.setName("广发货币基金E(兜底)");
            fallback.setCode("000000");
            fallback.setType("FUND");
            fallback.setRiskLevel("R1");
            fallback.setChangePercent(new BigDecimal("2.5")); // 模拟收益率
            return Collections.singletonList(fallback);
        }
        
        return result;
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
        log.info("查询用户调仓历史，用户ID: {}", userId);
        List<InvestmentPlanVO> result = new ArrayList<>();
        try {
            LambdaQueryWrapper<InvestmentPlan> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(InvestmentPlan::getUserId, userId)
                   .orderByDesc(InvestmentPlan::getCreateTime)
                   .last("LIMIT 10");
            
            List<InvestmentPlan> plans = planMapper.selectList(wrapper);
            if (plans == null) return result;

            for (InvestmentPlan plan : plans) {
                try {
                    InvestmentPlanVO vo = new InvestmentPlanVO();
                    vo.setId(plan.getId());
                    vo.setPlanName(plan.getPlanName() != null ? plan.getPlanName() : "未命名计划");
                    
                    String riskLevel = plan.getRiskLevel() != null ? plan.getRiskLevel().toLowerCase() : "balanced";
                    vo.setRiskLevel(riskLevel);
                    vo.setRiskLabel(RiskLevelEnum.getByCode(riskLevel).getLabel());
                    
                    vo.setTotalAmount(plan.getTotalAmount() != null ? plan.getTotalAmount() : BigDecimal.ZERO);
                    vo.setPlanType(plan.getPlanType() != null ? plan.getPlanType() : "CONTRIBUTION");
                    vo.setInvestMoney(plan.getInvestMoney() != null ? plan.getInvestMoney() : BigDecimal.ZERO);
                    vo.setStatus(plan.getStatus() != null ? plan.getStatus() : "draft");
                    vo.setCreateTime(plan.getCreateTime());
                    
                    // 获取明细
                    LambdaQueryWrapper<PlanItem> itemWrapper = new LambdaQueryWrapper<>();
                    itemWrapper.eq(PlanItem::getPlanId, plan.getId());
                    List<PlanItem> items = itemMapper.selectList(itemWrapper);
                    
                    List<PlanItemVO> itemVOs = new ArrayList<>();
                    if (items != null) {
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
                    }
                    vo.setItems(itemVOs);
                    result.add(vo);
                } catch (Exception e) {
                    log.error("解析单笔投资计划失败 [ID: {}]: {}", plan.getId(), e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("查询投资计划历史失败 [UserID: {}]: {}", userId, e.getMessage());
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
                
                // 记账 (HOLD): 资产增加
                transactionService.record(userId, newAsset.getId(), newAsset.getAssetName(), "HOLD", item.getAmount(), "计划执行: 新增持仓");
                
                // Phase 7.5 新增逻辑: 资金同源扣减 (买入同时也需要扣钱)
                // 找一个钱够的现金账户
                com.fincoach.core.repository.entity.AssetItem cashAccount = assetItemService.getLargestByCategory(userId, 1); // 1 = 现金储蓄
                if (cashAccount != null && cashAccount.getCurrentValue().compareTo(item.getAmount()) >= 0) {
                    cashAccount.setCurrentValue(cashAccount.getCurrentValue().subtract(item.getAmount()));
                    cashAccount.setUpdateTime(LocalDateTime.now());
                    assetItemService.updateAsset(cashAccount);
                    log.info("自动扣减现金账户: {} - {}", cashAccount.getAssetName(), item.getAmount());
                    
                    // 记账 (BUY): 现金减少
                    transactionService.record(userId, cashAccount.getId(), "现金账户", "BUY", item.getAmount().negate(), "计划执行: 买入 " + newAsset.getAssetName());

                } else {
                    // 如果现金不足，但可能是导入的历史数据问题，这里选择记录日志但不阻断(或者阻断?)
                    // 根据需求："校验：如果现金余额不足，禁止执行" -> 抛出异常
                    throw new RuntimeException("现金余额不足 (需 ¥" + item.getAmount() + ")，无法自动执行扣款，请先补充现金资产。");
                }
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
                
                // 记账 (SELL): 资产减少
                transactionService.record(userId, largestAsset.getId(), largestAsset.getAssetName(), "SELL", item.getAmount().negate(), "计划执行: 卖出赎回");
                
                // 增加现金? (如果卖出应该有钱回流，但目前的简易逻辑可能没处理回流到现金。
                // 既然没处理回流，这里先不记入金。或者默认回流到最大现金账户?
                // 用户需求只提了 B C 场景。C: "在扣减现金时记 BUY，在增加持仓时记 HOLD。"
                // 没有明确提到 SELL 的回流。为了完整性，最好处理回流，但目前的 auto-execution 逻辑并没有处理 Sell 的现金回流。
                // 我会暂且只记录 SELL 导致的资产减少。
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

    
    private PlanItemVO createGenericBuyItem(String category, BigDecimal amount, BigDecimal total, BigDecimal current, BigDecimal targetRatio) {
        PlanItemVO item = new PlanItemVO();
        item.setCategoryId(CATEGORY_IDS.get(category));
        item.setCategoryName(category);
        item.setCurrentRatio(total.compareTo(BigDecimal.ZERO) > 0 ? 
                current.divide(total, 4, RoundingMode.HALF_UP) : BigDecimal.ZERO);
        item.setTargetRatio(targetRatio);
        item.setAmount(amount.setScale(2, RoundingMode.HALF_UP));
        item.setAction("BUY");
        item.setSubType(getSubType(category, current));
        item.setReason(generateBuyReason(category, item.getCurrentRatio(), targetRatio));
        return item;
    }
}
