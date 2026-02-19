<template>
  <div class="diagnosis-container">
    <div class="score-card glass-panel" :class="getScoreClass(report?.score || 0)">
        <div class="score-content">
            <div class="score-circle">
                <span class="score-num">{{ report?.score || 0 }}</span>
                <span class="score-label">健康分</span>
            </div>
            <div class="score-info">
                <h2>{{ report?.level || '待诊断' }}</h2>
                <p v-if="report?.userType === 'NOVICE'">您的资产结构较为单一，建议开启定投计划。</p>
                <p v-else>您的资产配置超越了 {{ Math.min(99, (report?.score || 0) + 10) }}% 的用户。</p>
            </div>
        </div>
    </div>

    <div class="charts-row">
      <div class="chart-card glass-panel">
        <h3>资产结构透视</h3>
        <v-chart class="chart-instance" :option="radarOption" autoresize />
      </div>
      <div class="chart-card glass-panel">
        <h3>健康度趋势</h3>
        <v-chart class="chart-instance" :option="trendOption" autoresize />
      </div>
    </div>

    <div class="details-section glass-panel">
      <h3>📋 深度诊断报告</h3>
      <el-collapse v-model="activeNames" class="custom-collapse">
        <el-collapse-item name="1">
            <template #title>
                <div class="collapse-header">
                    <span>💧 流动性分析 (Liquidity)</span>
                    <el-tag :type="liquidityStatus" size="small">{{ liquidityRatio }}%</el-tag>
                </div>
            </template>
            <div class="diagnosis-item">
                <p>现金储备率：<strong>{{ liquidityRatio }}%</strong></p>
                <el-alert :title="liquiditySuggestion" :type="liquidityStatus" show-icon :closable="false" />
            </div>
        </el-collapse-item>
        
        <el-collapse-item name="2">
             <template #title>
                <div class="collapse-header">
                    <span>🛡️ 风险控制 (Risk Control)</span>
                    <el-tag type="warning" size="small">需关注</el-tag>
                </div>
            </template>
             <div class="diagnosis-item">
                <p>权益类资产占比：<strong>{{ report?.riskMatchScore ? '45%' : '0%' }}</strong></p>
                <el-alert title="当前风险敞口适中，建议增加债券配置以平滑波动。" type="warning" show-icon :closable="false" />
            </div>
        </el-collapse-item>
        
        <el-collapse-item name="3">
             <template #title>
                <div class="collapse-header">
                    <span>🚀 成长潜力 (Growth)</span>
                    <el-tag type="success" size="small">优秀</el-tag>
                </div>
            </template>
            <div class="diagnosis-item">
                <p>预期年化收益：<strong>7.5%</strong></p>
                <el-alert title="组合成长性良好，主要得益于科技ETF的配置。" type="success" show-icon :closable="false" />
            </div>
        </el-collapse-item>
      </el-collapse>
    </div>

    <div class="extended-section glass-panel">
      <h3>🔎 体检扩展模块</h3>
      <el-row :gutter="16">
        <el-col :xs="24" :md="12">
          <el-card class="module-card" shadow="never">
            <template #header>评分 v1</template>
            <div class="score-grid">
              <div class="score-item">
                <div class="label">风险</div>
                <div class="value">{{ scores?.riskScore?.value ?? '-' }}</div>
                <el-tag size="small">{{ scores?.riskScore?.level || 'N/A' }}</el-tag>
              </div>
              <div class="score-item">
                <div class="label">资产健康</div>
                <div class="value">{{ scores?.assetHealthScore?.value ?? '-' }}</div>
                <el-tag size="small">{{ scores?.assetHealthScore?.level || 'N/A' }}</el-tag>
              </div>
              <div class="score-item">
                <div class="label">行为</div>
                <div class="value">{{ scores?.behaviorScore?.value ?? '-' }}</div>
                <el-tag size="small">{{ scores?.behaviorScore?.level || 'N/A' }}</el-tag>
              </div>
            </div>
            <el-collapse v-model="scoreBreakdownActive" class="mini-collapse">
              <el-collapse-item name="risk">
                <template #title>风险拆解</template>
                <el-table :data="safeArray(scores?.riskScore?.breakdown)" size="small" border>
                  <el-table-column prop="code" label="Code" min-width="120" />
                  <el-table-column prop="weight" label="Weight" width="90" />
                  <el-table-column prop="rawValue" label="Raw" width="90" />
                  <el-table-column prop="scoreContribution" label="Score" width="90" />
                  <el-table-column prop="detail" label="Detail" min-width="160" />
                </el-table>
                <el-empty v-if="!safeArray(scores?.riskScore?.breakdown).length" description="暂无明细" />
              </el-collapse-item>
              <el-collapse-item name="asset">
                <template #title>资产健康拆解</template>
                <el-table :data="safeArray(scores?.assetHealthScore?.breakdown)" size="small" border>
                  <el-table-column prop="code" label="Code" min-width="120" />
                  <el-table-column prop="weight" label="Weight" width="90" />
                  <el-table-column prop="rawValue" label="Raw" width="90" />
                  <el-table-column prop="scoreContribution" label="Score" width="90" />
                  <el-table-column prop="detail" label="Detail" min-width="160" />
                </el-table>
                <el-empty v-if="!safeArray(scores?.assetHealthScore?.breakdown).length" description="暂无明细" />
              </el-collapse-item>
              <el-collapse-item name="behavior">
                <template #title>行为拆解</template>
                <el-table :data="safeArray(scores?.behaviorScore?.breakdown)" size="small" border>
                  <el-table-column prop="code" label="Code" min-width="120" />
                  <el-table-column prop="weight" label="Weight" width="90" />
                  <el-table-column prop="rawValue" label="Raw" width="90" />
                  <el-table-column prop="scoreContribution" label="Score" width="90" />
                  <el-table-column prop="detail" label="Detail" min-width="160" />
                </el-table>
                <el-empty v-if="!safeArray(scores?.behaviorScore?.breakdown).length" description="暂无明细" />
              </el-collapse-item>
            </el-collapse>
          </el-card>
        </el-col>

        <el-col :xs="24" :md="12">
          <el-card class="module-card" shadow="never">
            <template #header>相关性矩阵</template>
            <el-descriptions :column="2" border>
              <el-descriptions-item label="Assets">{{ correlationSummary?.assetsCount ?? '-' }}</el-descriptions-item>
              <el-descriptions-item label="SampleSize">{{ correlationSummary?.sampleSize ?? '-' }}</el-descriptions-item>
              <el-descriptions-item label="Warnings" :span="2">
                {{ safeArray(correlationSummary?.warnings).join(', ') || '暂无' }}
              </el-descriptions-item>
            </el-descriptions>
            <div v-if="matrixAssets.length && matrixRows.length" class="matrix-table">
              <table>
                <thead>
                  <tr>
                    <th></th>
                    <th v-for="asset in matrixAssets" :key="asset">{{ asset }}</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="(row, rowIndex) in matrixRows" :key="rowIndex">
                    <td class="row-label">{{ matrixAssets[rowIndex] || `#${rowIndex + 1}` }}</td>
                    <td v-for="(value, colIndex) in row" :key="colIndex">
                      {{ formatNum(value) }}
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
            <el-empty v-else description="未启用/样本不足" />
          </el-card>
        </el-col>

        <el-col :xs="24" :md="12">
          <el-card class="module-card" shadow="never">
            <template #header>再平衡建议 v1</template>
            <el-descriptions :column="2" border>
              <el-descriptions-item label="Threshold">{{ rebalanceAdvice?.threshold ?? '-' }}</el-descriptions-item>
              <el-descriptions-item label="Triggered">{{ rebalanceAdvice?.triggered ? '是' : '否' }}</el-descriptions-item>
              <el-descriptions-item label="Warnings" :span="2">
                {{ safeArray(rebalanceAdvice?.warnings).join(', ') || '暂无' }}
              </el-descriptions-item>
            </el-descriptions>
            <el-table :data="safeArray(rebalanceAdvice?.actions)" size="small" border>
              <el-table-column prop="asset" label="Asset" min-width="120" />
              <el-table-column prop="action" label="Action" width="100" />
              <el-table-column prop="suggestedWeightDelta" label="Delta" width="120" />
            </el-table>
            <el-empty v-if="!safeArray(rebalanceAdvice?.actions).length" description="暂无建议" />
          </el-card>
        </el-col>

        <el-col :xs="24" :md="12">
          <el-card class="module-card" shadow="never">
            <template #header>负债 + 现金流 v1</template>
            <el-descriptions :column="2" border>
              <el-descriptions-item label="DTI">{{ formatNum(debtCashflow?.dti) }}</el-descriptions-item>
              <el-descriptions-item label="SurplusRate">{{ formatNum(debtCashflow?.surplusRate) }}</el-descriptions-item>
              <el-descriptions-item label="EmergencyMonths">{{ formatNum(debtCashflow?.emergencyFundMonths) }}</el-descriptions-item>
              <el-descriptions-item label="StressLevel">{{ debtCashflow?.stressLevel ?? '-' }}</el-descriptions-item>
            </el-descriptions>
            <el-table :data="safeArray(debtCashflowAdvice?.list)" size="small" border>
              <el-table-column prop="title" label="Title" min-width="140" />
              <el-table-column prop="detail" label="Detail" min-width="200" />
              <el-table-column prop="priority" label="Priority" width="100" />
            </el-table>
            <el-empty v-if="!safeArray(debtCashflowAdvice?.list).length" description="暂无建议" />
            <div v-if="showDebtCashflowCta" class="cta-row">
              <el-button type="primary" plain @click="goDebt">完善债务信息</el-button>
              <el-button type="primary" plain @click="goCashflow">补充现金流</el-button>
            </div>
          </el-card>
        </el-col>

        <el-col :xs="24" :md="12">
          <el-card class="module-card" shadow="never">
            <template #header>债务优化 v1</template>
            <el-descriptions :column="1" border>
              <el-descriptions-item label="Strategy">{{ debtOptimizer?.strategy ?? '-' }}</el-descriptions-item>
              <el-descriptions-item label="Budget">{{ formatNum(debtOptimizer?.budgetForExtraPayment) }}</el-descriptions-item>
              <el-descriptions-item label="Tradeoff">
                {{ debtOptimizer?.tradeoffHint?.recommendation ?? '-' }}
              </el-descriptions-item>
            </el-descriptions>
            <el-table :data="safeArray(debtOptimizer?.plan).slice(0, 3)" size="small" border>
              <el-table-column prop="name" label="Name" min-width="140" />
              <el-table-column prop="priorityRank" label="Rank" width="80" />
              <el-table-column prop="recommendedExtraPayment" label="ExtraPay" width="120" />
              <el-table-column prop="estimatedMonthsToPayoff" label="Months" width="100" />
            </el-table>
            <el-empty v-if="!safeArray(debtOptimizer?.plan).length" description="暂无计划" />
          </el-card>
        </el-col>

        <el-col :xs="24" :md="12">
          <el-card class="module-card" shadow="never">
            <template #header>保险缺口 v1</template>
            <el-descriptions :column="2" border>
              <el-descriptions-item label="PremiumRatio">{{ formatNum(insuranceGap?.premiumRatio?.value) }}</el-descriptions-item>
              <el-descriptions-item label="Level">{{ insuranceGap?.premiumRatio?.level ?? '-' }}</el-descriptions-item>
              <el-descriptions-item label="Threshold">{{ formatNum(insuranceGap?.premiumRatio?.threshold) }}</el-descriptions-item>
              <el-descriptions-item label="TopGaps">{{ safeArray(insuranceGap?.topGaps).slice(0, 2).map((g) => g.type).join(', ') || '暂无' }}</el-descriptions-item>
            </el-descriptions>
            <el-table :data="safeArray(insuranceAdvice?.priorityList).slice(0, 5)" size="small" border>
              <el-table-column prop="type" label="Type" min-width="120" />
              <el-table-column prop="priorityRank" label="Rank" width="80" />
              <el-table-column prop="reason" label="Reason" min-width="180" />
            </el-table>
            <el-table :data="safeArray(insuranceAdvice?.list)" size="small" border>
              <el-table-column prop="title" label="Title" min-width="140" />
              <el-table-column prop="detail" label="Detail" min-width="200" />
              <el-table-column prop="priority" label="Priority" width="100" />
            </el-table>
            <el-empty v-if="!safeArray(insuranceAdvice?.list).length && !safeArray(insuranceAdvice?.priorityList).length" description="暂无建议" />
          </el-card>
        </el-col>

        <el-col :xs="24" :md="12">
          <el-card class="module-card" shadow="never">
            <template #header>预警 v1</template>
            <el-table :data="openAlerts" size="small" border>
              <el-table-column prop="code" label="Code" min-width="140" />
              <el-table-column prop="severity" label="Severity" width="110" />
              <el-table-column prop="title" label="Title" min-width="200" />
              <el-table-column prop="status" label="Status" width="100" />
              <el-table-column prop="createdAt" label="CreatedAt" min-width="160" />
            </el-table>
            <el-empty v-if="!openAlerts.length" description="暂无预警" />
          </el-card>
        </el-col>
      </el-row>
    </div>
    
    <div class="action-footer">
      <el-button type="primary" size="large" round class="action-btn" @click="$router.push('/plan')">
        前往生成优化方案
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useHealthStore } from '@/store/modules/health';
import { storeToRefs } from 'pinia';
import { use } from "echarts/core";
import { CanvasRenderer } from "echarts/renderers";
import { RadarChart, LineChart } from "echarts/charts";
import { TitleComponent, TooltipComponent, LegendComponent, GridComponent } from "echarts/components";
import VChart, { THEME_KEY } from "vue-echarts";

use([CanvasRenderer, RadarChart, LineChart, TitleComponent, TooltipComponent, LegendComponent, GridComponent]);

const healthStore = useHealthStore();
const { report } = storeToRefs(healthStore);
const router = useRouter();

const activeNames = ref(['1']);
const scoreBreakdownActive = ref<string[]>([]);

onMounted(() => {
  healthStore.fetchHealthReport();
});

// Mock 计算
const liquidityRatio = computed(() => report.value ? Math.round((report.value.liquidityScore / 20) * 100) : 0);
const liquidityStatus = computed(() => liquidityRatio.value >= 60 ? 'success' : 'warning');
const liquiditySuggestion = computed(() => 
    liquidityRatio.value >= 60 
    ? "现金储备充足，足以应对突发情况。" 
    : "现金储备不足，建议预留 3-6 个月生活费作为应急金。"
);

const getScoreClass = (score: number) => {
    if (score >= 80) return 'excellent';
    if (score >= 60) return 'good';
    return 'risk';
};

const scores = computed(() => report.value?.metrics?.scores || (report.value as any)?.scores || {});
const portfolio = computed(() => report.value?.portfolio || (report.value as any)?.metrics?.portfolio || {});
const correlationSummary = computed(() => (portfolio.value as any)?.correlationMatrixSummary);
const matrixAssets = computed(() => (portfolio.value as any)?.correlationMatrix?.assets || []);
const matrixRows = computed(() => (portfolio.value as any)?.correlationMatrix?.matrix || []);
const rebalanceAdvice = computed(() => (portfolio.value as any)?.rebalanceAdviceV1);
const debtCashflow = computed(() => report.value?.metrics?.debtCashflowV1);
const adviceV2 = computed(() => report.value?.adviceV2 || (report.value as any)?.advice?.adviceV2 || {});
const debtCashflowAdvice = computed(() => (adviceV2.value as any)?.debtCashflowAdviceV1);
const debtOptimizer = computed(() => (adviceV2.value as any)?.debtOptimizerV1);
const insuranceGap = computed(() => report.value?.metrics?.insuranceGapV1);
const insuranceAdvice = computed(() => (adviceV2.value as any)?.insuranceAdviceV1);
const adviceWarnings = computed(() => safeArray((adviceV2.value as any)?.meta?.warnings));
const showDebtCashflowCta = computed(() => {
  return adviceWarnings.value.includes('CASHFLOW_INSUFFICIENT_DATA')
    || adviceWarnings.value.includes('EMERGENCY_FUND_UNKNOWN')
    || adviceWarnings.value.includes('DTI_INSUFFICIENT_INCOME');
});
const alertsV1 = computed(() => report.value?.metrics?.alertsV1);
const openAlerts = computed(() => {
  return safeArray(alertsV1.value?.openAlerts ?? alertsV1.value?.alerts);
});

const safeArray = <T>(value: T[] | undefined | null): T[] => {
  return Array.isArray(value) ? value : [];
};

const formatNum = (value: any) => {
  if (value === null || value === undefined || Number.isNaN(Number(value))) return '-';
  return Number(value).toFixed(2);
};

const goDebt = () => {
  router.push('/app/debt');
};

const goCashflow = () => {
  router.push('/app/cashflow');
};

// 雷达图配置
const radarOption = computed(() => {
    const score = report.value?.score || 0;
    // Mock dimensions based on score
    const liquidity = report.value?.liquidityScore ? (report.value.liquidityScore / 20 * 100) : 60;
    const diversity = report.value?.diversityScore ? (report.value.diversityScore / 20 * 100) : 50;
    
    return {
        tooltip: {},
        radar: {
            indicator: [
                { name: '流动性', max: 100 },
                { name: '分散度', max: 100 },
                { name: '收益性', max: 100 },
                { name: '安全性', max: 100 },
                { name: '抗通胀', max: 100 }
            ],
            radius: '65%',
            splitNumber: 5,
            axisName: {
                color: '#94a3b8'
            },
            splitArea: {
                areaStyle: {
                    color: ['rgba(255, 255, 255, 0.02)', 'rgba(255, 255, 255, 0.05)']
                }
            },
            splitLine: {
                lineStyle: {
                    color: 'rgba(255, 255, 255, 0.1)'
                }
            }
        },
        series: [{
            type: 'radar',
            data: [
                {
                    value: [liquidity, diversity, score > 60 ? 75 : 50, score > 70 ? 80 : 60, 60],
                    name: '我的资产',
                    areaStyle: { color: 'rgba(79, 70, 229, 0.4)' },
                    itemStyle: { color: '#4f46e5' }
                },
                {
                    value: [60, 60, 60, 60, 60],
                    name: '健康基准',
                    lineStyle: { type: 'dashed', color: '#10b981' },
                    itemStyle: { color: '#10b981' }
                }
            ]
        }]
    };
});

// 趋势图配置 (Mock)
const trendOption = computed(() => ({
    tooltip: { trigger: 'axis' },
    grid: { top: '15%', bottom: '10%', left: '10%', right: '5%' },
    xAxis: {
        type: 'category',
        data: ['9月', '10月', '11月', '12月', '1月', '2月'],
        axisLine: { lineStyle: { color: '#475569' } },
        axisLabel: { color: '#94a3b8' }
    },
    yAxis: {
        type: 'value',
        interval: 20,
        splitLine: { lineStyle: { color: 'rgba(255,255,255,0.05)' } },
        axisLabel: { color: '#94a3b8' }
    },
    series: [{
        data: [65, 68, 70, 72, 69, 75],
        type: 'line',
        smooth: true,
        showSymbol: false,
        lineStyle: { color: '#F56C6C', width: 3 },
        areaStyle: {
            color: {
                type: 'linear',
                x: 0, y: 0, x2: 0, y2: 1,
                colorStops: [
                    { offset: 0, color: 'rgba(245, 108, 108, 0.5)' },
                    { offset: 1, color: 'rgba(245, 108, 108, 0)' }
                ]
            }
        }
    }]
}));
</script>

<style lang="scss" scoped>
@use "@/theme/variables.scss" as *;

.diagnosis-container {
  min-height: 100vh;
  padding: 24px;
  background: linear-gradient(135deg, #0f172a, #1e293b);
  color: white;
  padding-bottom: 80px;
}

.glass-panel {
  background: rgba(30, 41, 59, 0.6);
  backdrop-filter: blur(12px);
  border-radius: 16px;
  padding: 24px;
  border: 1px solid rgba(255, 255, 255, 0.05);
  margin-bottom: 24px;
}

.score-card {
    display: flex;
    justify-content: center;
    padding: 40px 24px;
    background: radial-gradient(circle at center, rgba(79, 70, 229, 0.1), rgba(30, 41, 59, 0.6));
    
    &.excellent { border-top: 2px solid $secondary-color; }
    &.good { border-top: 2px solid $primary-color; }
    &.risk { border-top: 2px solid $error-color; }
    
    .score-content {
        text-align: center;
    }
    
    .score-circle {
        position: relative;
        display: inline-flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        width: 140px;
        height: 140px;
        border-radius: 50%;
        background: rgba(255, 255, 255, 0.05);
        border: 4px solid rgba(255, 255, 255, 0.1);
        margin-bottom: 16px;
        box-shadow: 0 0 20px rgba(0,0,0,0.2);
        
        .score-num {
            font-size: 56px;
            font-weight: 800;
            line-height: 1;
            background: linear-gradient(to bottom, #fff, #94a3b8);
            -webkit-background-clip: text;
            -webkit-text-fill-color: transparent;
        }
        
        .score-label {
            font-size: 13px;
            color: $text-secondary;
        }
    }
    
    .score-info {
        h2 { margin: 0 0 8px; font-size: 24px; }
        p { margin: 0; color: $text-secondary; font-size: 14px; }
    }
}

.charts-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  margin-bottom: 24px;

  @media (max-width: 768px) {
    grid-template-columns: 1fr;
  }
}

.chart-card {
    display: flex;
    flex-direction: column;
    min-height: 350px;
    
    h3 {
        margin: 0 0 16px;
        color: $text-primary;
        font-size: 18px;
        font-weight: 600;
    }
}

.chart-instance {
    flex: 1;
    width: 100%;
    min-height: 300px;
}

.details-section {
    h3 {
        margin: 0 0 20px;
        font-size: 18px;
        color: $text-primary;
    }
}

.custom-collapse {
    border: none;
    --el-collapse-header-bg-color: transparent;
    --el-collapse-content-bg-color: transparent;
    --el-collapse-border-color: rgba(255,255,255,0.05);
    
    :deep(.el-collapse-item__header) {
        color: $text-light;
        font-size: 15px;
        padding: 12px 0;
        height: auto;
    }
    
    :deep(.el-collapse-item__wrap) {
        border-bottom: none;
    }
    
    .collapse-header {
        flex: 1;
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding-right: 12px;
    }
}

.diagnosis-item {
    p { color: $text-secondary; margin-bottom: 12px; }
    strong { color: $text-primary; }
}

.action-footer {
    display: flex;
    justify-content: center;
    margin-top: 32px;
    
    .action-btn {
        width: 200px;
        font-weight: 600;
        box-shadow: 0 4px 15px rgba($primary-color, 0.4);
    }
}

.extended-section {
  margin-bottom: 24px;
}

.module-card {
  background: rgba(15, 23, 42, 0.6);
  border: 1px solid rgba(255, 255, 255, 0.08);
  margin-bottom: 16px;
}

.score-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  margin-bottom: 12px;
}

.score-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.score-item .label {
  font-size: 12px;
  color: $text-secondary;
}

.score-item .value {
  font-size: 20px;
  font-weight: 700;
}

.mini-collapse {
  margin-top: 8px;
}

.matrix-table {
  overflow-x: auto;
  margin-top: 12px;
}

.matrix-table table {
  width: 100%;
  border-collapse: collapse;
  font-size: 12px;
}

.cta-row {
  margin-top: 12px;
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.matrix-table th,
.matrix-table td {
  border: 1px solid rgba(255, 255, 255, 0.08);
  padding: 6px 8px;
  text-align: center;
}

.matrix-table th {
  color: #94a3b8;
  font-weight: 600;
}

.row-label {
  text-align: left;
  color: #e2e8f0;
  font-weight: 600;
}

@media (max-width: 768px) {
  .score-grid {
    grid-template-columns: 1fr;
  }
}
</style>
