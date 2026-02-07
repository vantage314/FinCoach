import request from '@/utils/request';

// 1. 获取新闻列表
export const getNewsList = (params: { limit?: number }) => {
    return request.get('/invest/news/list', { params });
};

// 2. 获取新闻详情
export const getNewsDetail = (id: number) => {
    return request.get(`/invest/news/${id}`);
};

// 3. 获取公司 F10 资料
export const getCompanyProfile = (code: string) => {
    return request.get(`/invest/profile/${code}`);
};

// 4. 获取自选股列表
export const getWatchlist = () => {
    return request.get('/invest/watchlist');
};

// 5. 切换自选状态
export const toggleWatchlist = (code: string) => {
    return request.post('/invest/watchlist/toggle', { code });
};

// 6. 获取公司财务报表
export const getFinancialReports = (code: string) => {
    return request.get(`/invest/finance/${code}`);
};

// 7. 获取公司公告列表
export const getCompanyNotices = (code: string) => {
    return request.get(`/invest/notice/${code}`);
};
