<template>
  <div class="investor-panel glass-card">
    <div class="panel-header">
      <div class="header-left">
        <h3 class="title">您当前处于：{{ personaTag || '财富增值期' }}</h3>
        <p class="subtitle">您的基础保障已充足，现在专注于资产配置的再平衡与收益优化。</p>
      </div>
      <div class="header-right">
        <el-tag type="primary" size="large" effect="dark">🚀 进阶与优化</el-tag>
      </div>
    </div>

    <div class="chart-area" ref="radarChartRef"></div>
    
    <div class="metrics-grid">
         <div class="metric-item">
             <span class="label">流动性</span>
             <span class="value high">优</span>
         </div>
         <div class="metric-item">
             <span class="label">收益性</span>
             <span class="value medium">良</span>
         </div>
         <div class="metric-item">
             <span class="label">风险度</span>
             <span class="value medium">适中</span>
         </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { defineProps, onMounted, ref, watch } from 'vue';
import * as echarts from 'echarts';

const props = defineProps<{
  personaTag: string;
}>();

const radarChartRef = ref<HTMLElement | null>(null);
let myChart: echarts.ECharts | null = null;

const initChart = () => {
    if (!radarChartRef.value) return;
    
    myChart = echarts.init(radarChartRef.value);
    
    const option = {
        radar: {
            indicator: [
                { name: '流动性', max: 100 },
                { name: '收益性', max: 100 },
                { name: '风险度', max: 100 },
                { name: '分散度', max: 100 },
                { name: '成长性', max: 100 }
            ],
            center: ['50%', '50%'],
            radius: '70%',
            splitArea: {
                areaStyle: {
                    color: ['rgba(255, 255, 255, 0.01)', 'rgba(255, 255, 255, 0.05)']
                }
            },
            axisName: {
                color: '#94a3b8'
            }
        },
        series: [
            {
                name: '模型对比',
                type: 'radar',
                data: [
                    {
                        value: [80, 50, 40, 60, 50],
                        name: '当前配置',
                        itemStyle: { color: '#06b6d4' },
                        areaStyle: { opacity: 0.3 }
                    },
                    {
                        value: [60, 80, 70, 80, 85],
                        name: '理想模型 (进取型)',
                        itemStyle: { color: '#f59e0b' },
                        areaStyle: { opacity: 0.1 },
                        lineStyle: { type: 'dashed' }
                    }
                ]
            }
        ]
    };
    
    myChart.setOption(option);
};

onMounted(() => {
    initChart();
});

watch(() => props.personaTag, () => {
    if(myChart) {
        myChart.resize();
    }
});
</script>

<style lang="scss" scoped>
@use "@/theme/variables.scss" as *;

.glass-card {
    background: rgba(255, 255, 255, 0.05);
    backdrop-filter: blur(10px);
    border: 1px solid rgba(255, 255, 255, 0.1);
    border-radius: 16px;
    padding: 24px;
    margin-bottom: 24px;
}

.panel-header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    margin-bottom: 12px;

    .title {
        font-size: 20px;
        font-weight: 700;
        color: white;
        margin-bottom: 8px;
    }
    .subtitle {
        color: #94a3b8;
        font-size: 14px;
    }
}

.chart-area {
    width: 100%;
    height: 300px;
}

.metrics-grid {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 12px;
    margin-top: 12px;
    
    .metric-item {
        background: rgba(255,255,255,0.05);
        padding: 12px;
        border-radius: 8px;
        text-align: center;
        
        .label {
            display: block;
            font-size: 12px;
            color: #64748b;
            margin-bottom: 4px;
        }
        .value {
            font-weight: 700;
            color: white;
            
            &.high { color: #10b981; }
            &.medium { color: #f59e0b; }
        }
    }
}
</style>
