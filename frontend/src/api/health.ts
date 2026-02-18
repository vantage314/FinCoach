import request from './request';

export interface HealthSuggestion {
    type: 'success' | 'warning' | 'info';
    message: string;
}

export interface ScoreBreakdownItem {
    code?: string;
    weight?: number;
    rawValue?: number;
    scoreContribution?: number;
    detail?: string;
}

export interface ScoreMetric {
    value?: number;
    level?: string;
    breakdown?: ScoreBreakdownItem[];
}

export interface ScoresV1 {
    riskScore?: ScoreMetric;
    assetHealthScore?: ScoreMetric;
    behaviorScore?: ScoreMetric;
}

export interface CorrelationMatrixSummary {
    assetsCount?: number;
    sampleSize?: number;
    warnings?: string[];
}

export interface CorrelationMatrix {
    assets?: string[];
    matrix?: number[][];
}

export interface RebalanceAction {
    asset?: string;
    action?: string;
    suggestedWeightDelta?: number;
}

export interface RebalanceAdviceV1 {
    threshold?: number;
    triggered?: boolean;
    warnings?: string[];
    actions?: RebalanceAction[];
}

export interface DebtCashflowV1 {
    dti?: number;
    surplusRate?: number;
    emergencyFundMonths?: number;
    stressLevel?: string;
    warnings?: string[];
}

export interface DebtCashflowAdviceItem {
    title?: string;
    detail?: string;
    priority?: string;
}

export interface DebtCashflowAdviceV1 {
    list?: DebtCashflowAdviceItem[];
}

export interface DebtOptimizerPlanItem {
    name?: string;
    priorityRank?: number;
    recommendedExtraPayment?: number;
    estimatedMonthsToPayoff?: number;
}

export interface DebtOptimizerV1 {
    strategy?: string;
    budgetForExtraPayment?: number;
    tradeoffHint?: { recommendation?: string };
    plan?: DebtOptimizerPlanItem[];
}

export interface InsurancePremiumRatio {
    value?: number;
    level?: string;
    threshold?: number;
}

export interface InsuranceGapItem {
    type?: string;
    gap?: number;
    priorityRank?: number;
    reason?: string;
}

export interface InsuranceGapV1 {
    premiumRatio?: InsurancePremiumRatio;
    topGaps?: InsuranceGapItem[];
}

export interface InsuranceAdviceItem {
    title?: string;
    detail?: string;
    priority?: string;
}

export interface InsuranceAdviceV1 {
    priorityList?: InsuranceGapItem[];
    list?: InsuranceAdviceItem[];
}

export interface AlertItem {
    code?: string;
    severity?: string;
    title?: string;
    status?: string;
    createdAt?: string;
}

export interface AlertsV1 {
    openAlerts?: AlertItem[];
    alerts?: AlertItem[];
}

export interface HealthReport {
    score: number;
    level: string;
    liquidityScore: number;
    riskMatchScore: number;
    protectionScore: number;
    diversityScore: number;
    suggestions: HealthSuggestion[];
    metrics?: {
        scores?: ScoresV1;
        debtCashflowV1?: DebtCashflowV1;
        insuranceGapV1?: InsuranceGapV1;
        alertsV1?: AlertsV1;
    };
    portfolio?: {
        correlationMatrixSummary?: CorrelationMatrixSummary;
        correlationMatrix?: CorrelationMatrix;
        rebalanceAdviceV1?: RebalanceAdviceV1;
    };
    adviceV2?: {
        debtCashflowAdviceV1?: DebtCashflowAdviceV1;
        debtOptimizerV1?: DebtOptimizerV1;
        insuranceAdviceV1?: InsuranceAdviceV1;
    };
}

/**
 * 执行资产健康度体检
 */
export const checkHealth = () => {
    return request.get<HealthReport>('/health/check');
};
