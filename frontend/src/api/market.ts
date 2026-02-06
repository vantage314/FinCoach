import request from './request';

// 证券类型
export interface Security {
    id: number;
    name: string;
    code: string;
    type: 'stock' | 'fund' | 'bond';
    currentPrice: number;
    changePercent: number;
    riskLevel: 'R1' | 'R2' | 'R3' | 'R4' | 'R5';
    sector?: string;
    description?: string;
    // 扩展字段
    marketCap?: string;   // 市值
    peRatio?: number;     // 市盈率
    volume?: string;      // 成交量
    high52w?: number;     // 52周最高
    low52w?: number;      // 52周最低
}

// 市场新闻
export interface MarketNews {
    id: number;
    title: string;
    source: string;
    publishTime: string;
    content?: string;
}

// 市场指数
export interface MarketIndex {
    name: string;
    code: string;
    value: number;
    changePercent: number;
    icon?: string;
}

// 获取证券列表
export const getSecurities = (params?: { page?: number; size?: number; type?: string; keyword?: string }) => {
    return request.get('/market/securities', { params });
};

// 获取证券详情
export const getSecurityDetail = (id: number) => {
    return request.get(`/market/securities/${id}`);
};

// 获取市场新闻
export const getMarketNews = () => {
    return request.get('/market/news');
};

// 获取市场指数
export const getMarketIndices = () => {
    return request.get('/market/indices');
};
