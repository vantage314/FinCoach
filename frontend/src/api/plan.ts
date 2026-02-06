import request from './request';

export interface PlanItemVO {
    id?: number;
    action: string;
    categoryId: number;
    categoryName: string;
    subType?: string;
    amount: number;
    currentRatio: number;
    targetRatio: number;
    reason: string;
}

export interface InvestmentPlanVO {
    id?: number;
    planName?: string;
    riskLevel: string;
    riskLabel: string;
    totalAmount: number;
    planType: string;
    investMoney: number;
    status: string;
    createTime?: string;
    items: PlanItemVO[];
}

// 生成调仓计划
export const generatePlan = (planType: string, investMoney?: number) => {
    return request.post<any, { code: number; data: InvestmentPlanVO }>('/plan/generate', null, {
        params: { planType, investMoney }
    });
};

// 保存调仓计划
export const savePlan = (plan: InvestmentPlanVO) => {
    return request.post<any, { code: number; data: number }>('/plan/save', plan);
};

// 获取历史计划
export const getHistory = () => {
    return request.get<any, { code: number; data: InvestmentPlanVO[] }>('/plan/history');
};

// 一键执行调仓计划
export const executePlan = (planId: number) => {
    return request.post<any, { code: number; msg: string }>('/plan/execute/' + planId);
};
