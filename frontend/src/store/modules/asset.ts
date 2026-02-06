import { defineStore } from 'pinia';
import { getAssetList, getAssetSummary, deleteAssets, type AssetItem, type PortfolioSummary } from '@/api/asset';

export const useAssetStore = defineStore('asset', {
    state: () => ({
        assets: [] as AssetItem[],
        summary: {
            totalAmount: 0,
            categoryDistribution: {},
            investmentLimit: 0
        } as PortfolioSummary,
        loading: false,
        total: 0
    }),

    actions: {
        async getList(params: any = {}) {
            this.loading = true;
            try {
                const res: any = await getAssetList(params);
                if (res.code === 200) {
                    // 🔍 核心修复：兼容分页结构 { records: [...], total: 10 }
                    const rawData = res.data;

                    if (rawData && Array.isArray(rawData.records)) {
                        this.assets = rawData.records;
                        this.total = Number(rawData.total || 0);
                    } else if (Array.isArray(rawData)) {
                        this.assets = rawData;
                        this.total = rawData.length;
                    } else {
                        this.assets = [];
                        this.total = 0;
                    }
                }
            } catch (error) {
                console.error('获取资产列表失败:', error);
            } finally {
                this.loading = false;
            }
        },

        async getSummary() {
            try {
                const res: any = await getAssetSummary();
                if (res.code === 200) {
                    this.summary = res.data;
                }
            } catch (error) {
                console.error('获取资产统计失败:', error);
            }
        }
    }
});
