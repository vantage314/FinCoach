import { defineStore } from 'pinia';
import { ref } from 'vue';
import { checkHealth, getHealthReportV2Latest, generateHealthReportV2, type HealthReport, type HealthSuggestion } from '@/api/health';

export const useHealthStore = defineStore('health', () => {
    const report = ref<HealthReport | null>(null);
    const loading = ref(false);
    const error = ref<string | null>(null);

    const normalizeSuggestions = (input: unknown): HealthSuggestion[] => {
        if (!Array.isArray(input)) return [];
        return input.map((item) => {
            if (item && typeof item === 'object') {
                const type = (item as any).type;
                const message = (item as any).message;
                if (typeof type === 'string' && typeof message === 'string') {
                    const safeType = ['success', 'warning', 'info'].includes(type) ? type : 'info';
                    return { type: safeType as HealthSuggestion['type'], message };
                }
            }
            if (typeof item === 'string') {
                const trimmed = item.trim();
                const cleaned = trimmed.replace(/^@\{/, '').replace(/\}$/, '');
                const parts = cleaned.split(/;\s*/);
                const typePart = parts.find((part) => part.startsWith('type='));
                const messagePart = parts.find((part) => part.startsWith('message='));
                const rawType = typePart ? typePart.slice(5) : 'info';
                const rawMessage = messagePart ? messagePart.slice(8) : trimmed;
                const safeType = ['success', 'warning', 'info'].includes(rawType) ? rawType : 'info';
                return { type: safeType as HealthSuggestion['type'], message: rawMessage };
            }
            return { type: 'info', message: String(item) };
        });
    };

    const mapV2Report = (data: any): HealthReport => {
        const metrics = data?.metrics || {};
        const adviceV2 = data?.advice?.adviceV2 || data?.adviceV2 || {};
        const portfolio = metrics?.portfolio || data?.portfolio;
        const score = data?.healthScore ?? data?.score ?? 0;
        const level =
            metrics?.scores?.assetHealthScore?.level ||
            metrics?.scores?.riskScore?.level ||
            metrics?.scores?.behaviorScore?.level ||
            data?.level ||
            'N/A';

        return {
            score,
            level,
            liquidityScore: data?.liquidityScore ?? 0,
            riskMatchScore: data?.riskScore ?? data?.riskMatchScore ?? 0,
            protectionScore: data?.protectionScore ?? 0,
            diversityScore: data?.diversityScore ?? 0,
            suggestions: normalizeSuggestions(data?.suggestions),
            metrics,
            portfolio,
            adviceV2
        };
    };

    /**
     * 获取健康度报告
     */
    const fetchHealthReport = async () => {
        loading.value = true;
        error.value = null;

        try {
            try {
                const res: any = await getHealthReportV2Latest();
                if (res.code === 200) {
                    report.value = mapV2Report(res.data);
                    return;
                }
            } catch {
                // fall through to generate / legacy
            }

            try {
                const res: any = await generateHealthReportV2();
                if (res.code === 200) {
                    report.value = mapV2Report(res.data);
                    return;
                }
            } catch {
                // fall through to legacy
            }

            const res: any = await checkHealth();
            if (res.code === 200) {
                report.value = {
                    ...res.data,
                    suggestions: normalizeSuggestions(res.data?.suggestions)
                };
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
