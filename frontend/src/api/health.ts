import request from './request';

export interface HealthSuggestion {
    type: 'success' | 'warning' | 'info';
    message: string;
}

export interface HealthReport {
    score: number;
    level: string;
    liquidityScore: number;
    riskMatchScore: number;
    protectionScore: number;
    diversityScore: number;
    suggestions: HealthSuggestion[];
}

/**
 * 执行资产健康度体检
 */
export const checkHealth = () => {
    return request.get<HealthReport>('/health/check');
};
