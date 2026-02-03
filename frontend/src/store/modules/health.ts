import { defineStore } from 'pinia';
import { ref } from 'vue';
import { checkHealth, type HealthReport } from '@/api/health';

export const useHealthStore = defineStore('health', () => {
    const report = ref<HealthReport | null>(null);
    const loading = ref(false);
    const error = ref<string | null>(null);

    /**
     * 获取健康度报告
     */
    const fetchHealthReport = async () => {
        loading.value = true;
        error.value = null;

        try {
            const res: any = await checkHealth();
            if (res.code === 200) {
                report.value = res.data;
            } else {
                error.value = res.message || '获取健康度报告失败';
            }
        } catch (e: any) {
            error.value = e.message || '网络错误';
        } finally {
            loading.value = false;
        }
    };

    /**
     * 清除报告（用于退出登录）
     */
    const clearReport = () => {
        report.value = null;
        error.value = null;
    };

    return {
        report,
        loading,
        error,
        fetchHealthReport,
        clearReport
    };
});
