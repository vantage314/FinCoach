package com.fincoach.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fincoach.core.common.AssetCategoryEnum;
import com.fincoach.core.controller.dto.AssetItemDTO;
import com.fincoach.core.controller.dto.AssetQueryDTO;
import com.fincoach.core.controller.vo.PortfolioSummaryVO;
import com.fincoach.core.repository.entity.AssetItem;
import com.fincoach.core.repository.mapper.AssetItemMapper;
import com.fincoach.core.service.AssetItemService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class AssetItemServiceImpl implements AssetItemService {

    @Autowired
    private com.fincoach.core.service.TransactionService transactionService;

    @Autowired
    private AssetItemMapper assetItemMapper;

    @Override
    public void addAsset(Long userId, AssetItemDTO dto) {
        log.info("开始录入资产，用户ID: {}, 资产名称: {}, 分类ID: {}", userId, dto.getAssetName(), dto.getCategoryId());

        if (dto.getCategoryId() == null) {
            throw new IllegalArgumentException("categoryId is required");
        }
        
        // 金融投资类必须选择子类型
        if (dto.getCategoryId() == 2 && (dto.getSubType() == null || dto.getSubType().isBlank())) {
            throw new IllegalArgumentException("金融投资类必须选择子类型（股票/基金/债券）");
        }
        
        AssetItem item = new AssetItem();
        item.setUserId(userId);
        item.setCategoryId(dto.getCategoryId());
        item.setAssetName(dto.getAssetName());
        item.setCurrentValue(dto.getCurrentValue());
        item.setHoldingCost(dto.getHoldingCost());
        item.setAssetCode(dto.getAssetCode());
        item.setSubType(dto.getSubType());
        item.setUpdateTime(LocalDateTime.now());
        
        int rows = assetItemMapper.insert(item);
        log.info("资产录入成功，影响行数: {}, 资产ID: {}", rows, item.getId());

        // 记录流水 (DEPOSIT)
        try {
            transactionService.record(userId, item.getId(), item.getAssetName(), "DEPOSIT", item.getCurrentValue(), "手动录入资产");
        } catch (Exception e) {
            log.error("记账失败", e);
        }
    }

    @Override
    public List<AssetItem> getUserAssets(Long userId, AssetQueryDTO query) {
        LambdaQueryWrapper<AssetItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AssetItem::getUserId, userId);
        
        // 条件查询：分类ID
        if (query != null && query.getCategoryId() != null) {
            wrapper.eq(AssetItem::getCategoryId, query.getCategoryId());
        }
        
        // 条件查询：资产名称模糊匹配
        if (query != null && StringUtils.hasText(query.getAssetName())) {
            wrapper.like(AssetItem::getAssetName, query.getAssetName());
        }
        
        wrapper.orderByDesc(AssetItem::getUpdateTime);
        return assetItemMapper.selectList(wrapper);
    }

    @Override
    public PortfolioSummaryVO getPortfolioSummary(Long userId) {
        List<AssetItem> assets = getUserAssets(userId, null);
        
        PortfolioSummaryVO vo = new PortfolioSummaryVO();
        
        // 计算总金额
        BigDecimal total = assets.stream()
                .map(AssetItem::getCurrentValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        vo.setTotalAmount(total);
        
        // 计算各分类占比
        Map<String, BigDecimal> distribution = new HashMap<>();
        for (AssetCategoryEnum category : AssetCategoryEnum.values()) {
            BigDecimal categoryTotal = assets.stream()
                    .filter(a -> a.getCategoryId().equals(category.getId()))
                    .map(AssetItem::getCurrentValue)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            BigDecimal percentage = total.compareTo(BigDecimal.ZERO) > 0
                    ? categoryTotal.multiply(new BigDecimal("100")).divide(total, 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            distribution.put(category.getName(), percentage);
        }
        vo.setCategoryDistribution(distribution);
        
        // 计算投资红线 (总额的20%)
        vo.setInvestmentLimit(total.multiply(new BigDecimal("0.20")).setScale(2, RoundingMode.HALF_UP));
        
        // --- Phase 8: 小白保护指标计算 ---
        BigDecimal cash = distribution.getOrDefault("现金储蓄", BigDecimal.ZERO); // 占比
        // 为了计算 Gap，我们需要现金的绝对值
        BigDecimal cashAmount = assets.stream()
                .filter(a -> a.getCategoryId() == 1) // 1=CASH
                .map(AssetItem::getCurrentValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal threshold = new BigDecimal("30000");
        BigDecimal gap = threshold.subtract(cashAmount);
        
        vo.setSafetyThreshold(threshold);
        vo.setLiquidityGap(gap.max(BigDecimal.ZERO)); // 缺口不为负
        
        // 进度计算: min(cash / 30000, 1) * 100
        double progress = 0.0;
        if (threshold.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal p = cashAmount.divide(threshold, 4, RoundingMode.HALF_UP);
            progress = Math.min(p.doubleValue(), 1.0) * 100;
        }
        vo.setSafetyProgress(progress);
        
        // 画像标签
        if (gap.compareTo(BigDecimal.ZERO) > 0) {
            vo.setPersonaTag("🌱 蓄力期"); // 还是新手，需要存钱
        } else {
            vo.setPersonaTag("🌳 增值期"); // 钱够了，可以去浪
        }

        log.info("资产统计完成，用户ID: {}, 总金额: {}, 缺口: {}, 标签: {}", userId, total, gap, vo.getPersonaTag());
        return vo;
    }

    @Override
    public int deleteAssets(Long userId, List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        
        log.info("开始删除资产，用户ID: {}, 资产ID列表: {}", userId, ids);
        
        // 越权防御：只能删除属于当前用户的资产
        LambdaQueryWrapper<AssetItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(AssetItem::getId, ids)
               .eq(AssetItem::getUserId, userId);
        
        // 记录流水 (DELETE) - 先查再删
        try {
            List<AssetItem> assetsToDelete = assetItemMapper.selectList(wrapper);
            for (AssetItem asset : assetsToDelete) {
                transactionService.record(userId, asset.getId(), asset.getAssetName(), "DELETE", asset.getCurrentValue().negate(), "删除资产");
            }
        } catch (Exception e) {
            log.warn("记账失败 (删除操作)", e);
        }

        int rows = assetItemMapper.delete(wrapper);
        log.info("资产删除完成，实际删除行数: {}", rows);
        return rows;
    }

    @Override
    public void updateAsset(AssetItem asset) {
        assetItemMapper.updateById(asset);
    }

    @Override
    public AssetItem getLargestByCategory(Long userId, Integer categoryId) {
        LambdaQueryWrapper<AssetItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AssetItem::getUserId, userId)
               .eq(AssetItem::getCategoryId, categoryId)
               .orderByDesc(AssetItem::getCurrentValue)
               .last("LIMIT 1");
        return assetItemMapper.selectOne(wrapper);
    }

    @Override
    public void internalAddAsset(AssetItem item) {
        assetItemMapper.insert(item);
    }

    // ======================== Phase 14: 资产分析与健康体检 ========================
    
    @Autowired
    private com.fincoach.core.repository.mapper.MarketSecurityMapper marketSecurityMapper;

    @Override
    public com.fincoach.core.controller.vo.AssetAnalysisVO analyze(Long userId) {
        log.info("[AssetAnalysis] 开始分析用户资产, userId: {}", userId);
        
        List<AssetItem> assets = getUserAssets(userId, null);
        
        BigDecimal totalValue = BigDecimal.ZERO;
        BigDecimal totalCost = BigDecimal.ZERO;
        Map<String, BigDecimal> typeMap = new HashMap<>(); // STOCK, FUND, BOND, CASH...
        
        List<com.fincoach.core.controller.vo.AssetAnalysisVO.AssetItemVO> holdings = new java.util.ArrayList<>();

        // 1. 遍历资产，计算实时价值
        for (AssetItem asset : assets) {
            BigDecimal currentValue = asset.getCurrentValue(); // 默认为静态金额(如现金)
            String assetType = getAssetTypeString(asset.getCategoryId(), asset.getSubType());
            
            // 如果是证券类资产，尝试去行情表查最新价
            if (asset.getStockCode() != null && asset.getQuantity() != null 
                    && asset.getQuantity().compareTo(BigDecimal.ZERO) > 0) {
                var security = marketSecurityMapper.selectOne(
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.fincoach.core.repository.entity.MarketSecurity>()
                        .eq("code", asset.getStockCode())
                );
                if (security != null) {
                    // 市值 = 持仓量 * 实时价
                    BigDecimal marketPrice = security.getCurrentPrice();
                    currentValue = asset.getQuantity().multiply(marketPrice);
                    
                    // 累加成本 (用于算盈亏)
                    if (asset.getCostPrice() != null) {
                        totalCost = totalCost.add(asset.getQuantity().multiply(asset.getCostPrice()));
                    }
                }
            } else {
                // 非证券类资产：成本 = 当前值
                totalCost = totalCost.add(currentValue != null ? currentValue : BigDecimal.ZERO);
            }

            // 累加总资产
            if (currentValue != null) {
                totalValue = totalValue.add(currentValue);
            }
            
            // 累加分类分布
            typeMap.put(assetType, typeMap.getOrDefault(assetType, BigDecimal.ZERO).add(currentValue != null ? currentValue : BigDecimal.ZERO));

            // 记录持仓明细用于排序
            if (currentValue != null && currentValue.compareTo(BigDecimal.ZERO) > 0) {
                var item = new com.fincoach.core.controller.vo.AssetAnalysisVO.AssetItemVO();
                item.setName(asset.getAssetName());
                item.setCode(asset.getStockCode());
                item.setValue(currentValue);
                holdings.add(item);
            }
        }

        // 2. 组装基础数据
        var vo = new com.fincoach.core.controller.vo.AssetAnalysisVO();
        vo.setTotalAsset(totalValue);
        vo.setTotalProfit(totalValue.subtract(totalCost)); // 总盈亏
        vo.setDayProfit(totalValue.multiply(new BigDecimal("0.012"))); // 模拟今日盈亏 +1.2%
        vo.setTypeDistribution(typeMap);

        // 计算持仓占比
        for (var item : holdings) {
            if (totalValue.compareTo(BigDecimal.ZERO) > 0) {
                item.setPercent(item.getValue().divide(totalValue, 4, RoundingMode.HALF_UP).multiply(new BigDecimal(100)));
            }
        }
        // 排序取前5
        holdings.sort((a, b) -> b.getValue().compareTo(a.getValue()));
        if (holdings.size() > 5) holdings = holdings.subList(0, 5);
        vo.setTopHoldings(holdings);

        // 3. 智能体检算法
        performHealthCheck(vo, totalValue, typeMap);

        log.info("[AssetAnalysis] 分析完成, 总资产: {}, 盈亏: {}, 健康分: {}", 
                vo.getTotalAsset(), vo.getTotalProfit(), vo.getHealthScore());
        return vo;
    }
    
    /**
     * 获取资产类型字符串
     */
    private String getAssetTypeString(Integer categoryId, String subType) {
        if (categoryId == null) return "OTHER";
        switch (categoryId) {
            case 1: return "CASH";
            case 2: // 金融投资
                if (subType != null) {
                    if (subType.contains("股票")) return "STOCK";
                    if (subType.contains("基金")) return "FUND";
                    if (subType.contains("债券")) return "BOND";
                }
                return "INVESTMENT";
            case 3: return "FIXED"; // 固定资产
            default: return "OTHER";
        }
    }

    /**
     * 健康体检算法
     */
    private void performHealthCheck(com.fincoach.core.controller.vo.AssetAnalysisVO vo, 
                                    BigDecimal total, Map<String, BigDecimal> typeMap) {
        int score = 100;
        List<String> suggestions = new java.util.ArrayList<>();

        if (total.compareTo(BigDecimal.ZERO) == 0) {
            vo.setHealthScore(0);
            vo.setHealthLevel("空仓");
            suggestions.add("您还没有录入任何资产，请先记一笔吧！");
            vo.setSuggestions(suggestions);
            return;
        }

        // 计算比例
        BigDecimal cashRatio = typeMap.getOrDefault("CASH", BigDecimal.ZERO).divide(total, 4, RoundingMode.HALF_UP);
        BigDecimal equityValue = typeMap.getOrDefault("STOCK", BigDecimal.ZERO)
                .add(typeMap.getOrDefault("FUND", BigDecimal.ZERO));
        BigDecimal stockRatio = equityValue.divide(total, 4, RoundingMode.HALF_UP);

        // 规则 1: 流动性危机 (现金 < 5%)
        if (cashRatio.compareTo(new BigDecimal("0.05")) < 0) {
            score -= 20;
            suggestions.add("⚠️ 流动性告急：您的现金类资产不足 5%，建议预留 3-6 个月的生活费作为备用金。");
        }

        // 规则 2: 风险敞口过大 (权益 > 80%)
        if (stockRatio.compareTo(new BigDecimal("0.80")) > 0) {
            score -= 15;
            suggestions.add("⚠️ 激进投资：您的股票/基金占比超过 80%，市场波动可能导致资产大幅缩水，建议配置债券或黄金对冲。");
        } else if (stockRatio.compareTo(new BigDecimal("0.20")) < 0 && stockRatio.compareTo(BigDecimal.ZERO) > 0) {
            // 规则 3: 过于保守
            score -= 5;
            suggestions.add("💡 过于保守：您的权益类资产较低，可能跑不赢通胀，建议适当关注指数基金。");
        } else if (stockRatio.compareTo(BigDecimal.ZERO) > 0) {
            suggestions.add("✅ 均衡配置：您的股债配比处于健康区间。");
        }

        // 规则 4: 持仓集中度
        if (vo.getTopHoldings() != null && !vo.getTopHoldings().isEmpty() 
                && vo.getTopHoldings().get(0).getPercent() != null
                && vo.getTopHoldings().get(0).getPercent().compareTo(new BigDecimal("60")) > 0) {
            score -= 10;
            suggestions.add("⚠️ 集中度过高：单一资产占比超过 60%，请注意个股黑天鹅风险。");
        }

        vo.setHealthScore(score);
        if (score >= 90) vo.setHealthLevel("✨ 完美资产");
        else if (score >= 75) vo.setHealthLevel("👍 健康资产");
        else if (score >= 60) vo.setHealthLevel("😷 亚健康");
        else vo.setHealthLevel("🚑 高危资产");
        
        vo.setSuggestions(suggestions);
    }
}
