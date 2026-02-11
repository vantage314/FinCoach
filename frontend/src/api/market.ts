import request from '@/utils/request';

// 类型定义
export interface Security {
    id: number;
    name: string;
    code: string;
    type: string;
    currentPrice: number;
    changePercent: number;
    riskLevel: string;
    sector: string;
    description?: string;
    marketCap?: number;
    peRatio?: number;
    volume?: number;
    high52w?: number;
    low52w?: number;
    // 🔥 新增详细行情字段
    openPrice?: number;
    highPrice?: number;
    lowPrice?: number;
    turnover?: number;
}

export interface MarketNews {
    id: number;
    title: string;
    source: string;
    publishTime: string;
    content: string;
}

export interface MarketIndex {
    name: string;
    code: string;
    value: number;
    changePercent: number;
    icon: string;
}

// K线数据项
export interface KLineItem {
    day: string;
    open: string;
    high: string;
    low: string;
    close: string;
    volume: string;
}

// 1. 获取市场证券列表 (标准命名)
export const getMarketSecurities = (params?: any) => {
    return request.get('/market/securities', { params });
};

// 🔥 修复点：添加别名导出，兼容旧代码调用的 'getSecurities'
export const getSecurities = getMarketSecurities;

// 2. 获取单个证券详情
export const getSecurityDetail = (code: string) => {
    return request.get(`/market/detail/${code}`);
};

// 3. 🔥 获取K线数据 (后端代理新浪接口)
export const getKLineData = (code: string, type: string = 'day') => {
    return request.get('/market/kline', { params: { code, type } });
};

// 4. 获取智能财务摘要
export const getMarketFinance = (code: string) => {
    return request.get(`/market/finance/${code}`);
};

// 5. 获取智能公告列表
export const getMarketNotices = (code: string) => {
    return request.get(`/market/notices/${code}`);
};

