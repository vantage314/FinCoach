import request from './request';

export interface AssetCategory {
    id: number;
    name: string;
    formType: 'SIMPLE' | 'INVEST' | 'PROPERTY';
    iconSlug: string;
}

export interface AssetItem {
    id: number;
    userId: number;
    categoryId: number;
    subType: string | null;
    assetName: string;
    currentValue: number;
    holdingCost: number | null;
    assetCode: string | null;
    updateTime: string;
}

export interface AssetItemDTO {
    categoryId: number;
    subType?: string;
    assetName: string;
    currentValue: number;
    holdingCost?: number;
    assetCode?: string;
}

export interface PortfolioSummary {
    totalAmount: number;
    categoryDistribution: Record<string, number>;
    investmentLimit: number;

    // Phase 8: 小白保护机制
    safetyThreshold?: number;
    liquidityGap?: number;
    safetyProgress?: number;
    personaTag?: string;
}

/**
 * 获取资产分类列表
 */
export const getCategories = () => {
    return request.get<AssetCategory[]>('/asset/categories');
};

/**
 * 录入新资产
 */
export const addAsset = (data: AssetItemDTO) => {
    return request.post('/asset/add', data);
};

/**
 * 获取用户资产列表
 */
export const getAssetList = (params?: { categoryId?: number; assetName?: string }) => {
    return request.get<AssetItem[]>('/asset/list', { params });
};

/**
 * 获取资产组合统计
 */
export const getAssetSummary = () => {
    return request.get<PortfolioSummary>('/asset/summary');
};

/**
 * 批量删除资产
 */
export const deleteAssets = (ids: number[]) => {
    return request.delete('/asset', { data: ids });
};
