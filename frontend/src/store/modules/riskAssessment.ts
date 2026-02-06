import { defineStore } from 'pinia';
import { submitAssessment, getLatestResult, type RiskAssessmentResult } from '@/api/risk';
import { useUserStore } from './user';

export const useRiskAssessmentStore = defineStore('riskAssessment', {
    state: () => ({
        currentStep: 0,
        answers: {} as Record<string, number>,
        assessmentResult: null as RiskAssessmentResult | null,
        riskLevel: '' // 缓存的风险等级
    }),

    actions: {
        // 重置测评状态 (重新测评前调用)
        resetAssessment() {
            this.currentStep = 0;
            this.answers = {};
            this.assessmentResult = null;
        },

        // 提交测评
        async submitAssessment(answers: Record<string, number>) {
            const res = await submitAssessment(answers);
            if (res.data.code === 200 && res.data.data) {
                this.assessmentResult = res.data.data;
                this.riskLevel = res.data.data.riskLevel;

                // 联动更新 UserStore (如果有)
                // const userStore = useUserStore();
                // if (userStore.userInfo) {
                //    userStore.userInfo.riskLevel = this.riskLevel;
                // }
            }
            return res;
        },

        // 获取最新结果 (强制刷新)
        async fetchLatestResult() {
            try {
                const res = await getLatestResult();
                if (res.data.code === 200 && res.data.data) {
                    this.assessmentResult = res.data.data;
                    this.riskLevel = res.data.data.riskLevel;
                    return res.data.data;
                }
            } catch (error) {
                console.error('Fetch risk result failed', error);
            }
            return null;
        }
    }
});
