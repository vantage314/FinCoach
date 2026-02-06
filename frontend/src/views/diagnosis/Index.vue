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
    
    <div class="action-footer">
      <el-button type="primary" size="large" round class="action-btn" @click="$router.push('/plan')">
        前往生成优化方案
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
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

const activeNames = ref(['1']);

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
</style>
