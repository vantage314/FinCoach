import request from '@/api/request';

// 1. 获取新闻列表 (对应 MarketController.getNewsList)
export const getNewsList = (params: { limit?: number }) => {
    return request.get('/market/news', { params });
};

// 2. 获取新闻详情 (暂未实现，预留)
export const getNewsDetail = (id: number) => {
    return request.get(`/market/news/${id}`);
};

// 3. 获取公司 F10 资料
export const getCompanyProfile = (code: string) => {
    return request.get(`/market/company/profile/${code}`);
};

// 4. 获取自选股列表
export const getWatchlist = () => {
    return request.get('/invest/watchlist'); // 保持原样，假设在 InvestController
};

// 5. 切换自选状态
export const toggleWatchlist = (code: string) => {
    return request.post('/invest/watchlist/toggle', { code });
};

// 6. 获取公司财务摘要 (统一走市场智能接口)
export const getFinancialReports = (code: string) => {
    return request.get(`/market/finance/${code}`);
};

// 7. 获取公司公告列表 (统一走市场智能接口)
export const getCompanyNotices = (code: string) => {
    return request.get(`/market/notices/${code}`);
};
