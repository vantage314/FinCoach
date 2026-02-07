import request from '@/utils/request';

// 1. 获取市场证券列表 (标准命名)
export const getMarketSecurities = (params?: any) => {
    return request.get('/market/securities', { params });
};

// 🔥 修复点：添加别名导出，兼容旧代码调用的 'getSecurities'
export const getSecurities = getMarketSecurities;

// 2. 获取单个证券详情 (为 Phase 12.3 做准备)
export const getSecurityDetail = (code: string) => {
    return request.get(`/market/detail/${code}`);
};
