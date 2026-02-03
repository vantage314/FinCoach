import request from './request';

export interface RiskAssessmentResult {
    id: number;
    totalScore: number;
    riskLevel: string;
    label: string;
    description: string;
    equityLimit: number;
    createTime: string;
    // 知行合一诊断
    actualRatio: number;
    idealRatio: number;
    gap: number;
    diagnosis: string;
}

/**
 * 提交风险测评
 */
export const submitAssessment = (answers: Record<string, number>) => {
    return request.post<RiskAssessmentResult>('/risk/assess', { answers });
};

/**
 * 获取最新测评结果
 */
export const getLatestResult = () => {
    return request.get<RiskAssessmentResult>('/risk/latest');
};
