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
    private AssetItemMapper assetItemMapper;

    @Override
    public void addAsset(Long userId, AssetItemDTO dto) {
        log.info("开始录入资产，用户ID: {}, 资产名称: {}, 分类ID: {}", userId, dto.getAssetName(), dto.getCategoryId());
        
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
}
