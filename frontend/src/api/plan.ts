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

// 生成投资计划 [关键修复]
export const generateInvestmentPlan = (data: { planType: string; investMoney: number }) => {
    return request.post<any, { code: number; data: InvestmentPlanVO }>('/plan/generate', data);
};

// 执行投资计划 [关键修复]
export const executePlan = (planId: number) => {
    return request.post<any, { code: number; msg: string }>(`/plan/execute?planId=${planId}`);
};

// 获取投资计划列表
export const getPlanList = (params?: any) => {
    return request.get<any, { code: number; data: InvestmentPlanVO[] }>('/plan/list', { params });
};

// 获取计划详情
export const getPlanDetail = (planId: number) => {
    return request.get<any, { code: number; data: InvestmentPlanVO }>(`/plan/detail/${planId}`);
};

// 兼容旧代码的别名导出（如果需要）
export const generatePlan = generateInvestmentPlan;
export const savePlan = (plan: InvestmentPlanVO) => {
    return request.post<any, { code: number; data: number }>('/plan/save', plan);
};
