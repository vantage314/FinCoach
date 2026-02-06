import { defineStore } from 'pinia';
import { ref } from 'vue';
import {
    generatePlan,
    savePlan,
    getHistory,
    executePlan,
    type InvestmentPlanVO
} from '@/api/plan';
import { ElMessage } from 'element-plus';

export const useInvestmentPlanStore = defineStore('investmentPlan', () => {
    const plans = ref<InvestmentPlanVO[]>([]);
    const loading = ref(false);
    const currentDraft = ref<InvestmentPlanVO | null>(null);

    // 获取历史计划
    const fetchHistory = async () => {
        loading.value = true;
        try {
            const { data } = await getHistory();
            if (data.code === 200) {
                plans.value = data.data;
            }
        } catch (error) {
            console.error('Fetch history failed:', error);
        } finally {
            loading.value = false;
        }
    };

    // 生成计划草稿
    const generateDraft = async (amount: number, mode: 'CONTRIBUTION' | 'REBALANCE') => {
        loading.value = true;
        try {
            const { data } = await generatePlan(mode, amount);
            if (data.code === 200) {
                currentDraft.value = data.data;
                return data.data;
            }
        } catch (error) {
            console.error('Generate draft failed:', error);
            ElMessage.error('生成建议失败，请稍后重试');
            return null;
        } finally {
            loading.value = false;
        }
    };

    // 保存计划
    const saveCurrentPlan = async (plan: InvestmentPlanVO) => {
        loading.value = true;
        try {
            const { data } = await savePlan(plan);
            if (data.code === 200) {
                ElMessage.success('计划已保存');
                await fetchHistory(); // 刷新列表
                return true;
            }
        } catch (error) {
            console.error('Save plan failed:', error);
            ElMessage.error('保存失败');
            return false;
        } finally {
            loading.value = false;
        }
    };

    // 执行计划
    const executePlanById = async (planId: number) => {
        loading.value = true;
        try {
            const { data } = await executePlan(planId);
            if (data.code === 200) {
                ElMessage.success('执行成功！资产已自动更新');
                await fetchHistory(); // 刷新列表状态
                return true;
            }
        } catch (error) {
            console.error('Execution failed:', error);
            ElMessage.error(error.message || '执行失败');
            return false;
        } finally {
            loading.value = false;
        }
    };

    return {
        plans,
        loading,
        currentDraft,
        fetchHistory,
        generateDraft,
        saveCurrentPlan,
        executePlanById
    };
});
