import request from './request';

export const fetchMarketDebugLatest = (includeMatrix = true) => {
    return request.get('/admin/portfolio/market-debug/latest', {
        params: { includeMatrix },
    });
};
