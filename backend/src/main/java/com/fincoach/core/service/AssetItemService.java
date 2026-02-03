package com.fincoach.core.service;

import com.fincoach.core.controller.dto.AssetItemDTO;
import com.fincoach.core.controller.dto.AssetQueryDTO;
import com.fincoach.core.controller.vo.PortfolioSummaryVO;
import com.fincoach.core.repository.entity.AssetItem;

import java.util.List;

public interface AssetItemService {
    /**
     * 添加资产
     */
    void addAsset(Long userId, AssetItemDTO dto);

    /**
     * 获取用户资产列表（支持条件过滤）
     */
    List<AssetItem> getUserAssets(Long userId, AssetQueryDTO query);

    /**
     * 获取资产组合统计
     */
    PortfolioSummaryVO getPortfolioSummary(Long userId);

    /**
     * 批量删除资产（带越权防御）
     */
    /**
     * 更新资产信息
     */
    void updateAsset(AssetItem asset);

    /**
     * 获取指定分类下金额最大的资产
     */
    AssetItem getLargestByCategory(Long userId, Integer categoryId);

    /**
     * 内部使用的实体添加方法
     */
    void internalAddAsset(AssetItem item);
}
