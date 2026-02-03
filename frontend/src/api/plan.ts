import request from './request';

export interface PlanItemVO {
    id?: number;
    action: 'BUY' | 'SELL' | 'INFO';
    categoryId?: number;
    categoryName: string;
    subType?: string;
    amount?: number;
    currentRatio?: number;
    targetRatio?: number;
    reason: string;
}

export interface InvestmentPlanVO {
    id?: number;
    riskLevel: string;
    riskLabel: string;
    totalAmount: number;
    status: string;
    createTime?: string;
    items: PlanItemVO[];
}


/**
 * 保存调仓计划
 */
export const savePlan = (plan: InvestmentPlanVO) => {
    return request.post<number>('/plan/save', plan);
};

/**
 * 生成调仓计划（预览）
 * @param planType CONTRIBUTION | REBALANCE
 * @param investMoney 投入金额（仅限 CONTRIBUTION）
 */
export const generatePlan = (planType: 'CONTRIBUTION' | 'REBALANCE' = 'REBALANCE', investMoney?: number) => {
    return request.post<InvestmentPlanVO>(`/plan/generate?planType=${planType}${investMoney ? `&investMoney=${investMoney}` : ''}`);
};

/**
 * 获取历史计划
 */
export const getPlanHistory = () => {
    return request.get<InvestmentPlanVO[]>('/plan/history');
};

/**
 * 执行调仓计划（一键执行）
 */
export const executePlan = (planId: number) => {
    return request.post<void>(`/plan/execute/${planId}`);
};
