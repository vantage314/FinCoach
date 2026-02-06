<template>
  <div class="investor-panel">
    <!-- 顶部概览卡片 -->
    <div class="metrics-row">
        <div class="metric-card glass-panel">
            <div class="label">总资产估值</div>
            <div class="value">¥{{ totalAssets.toLocaleString() }}</div>
            <div class="trend positive">
                <el-icon><Top /></el-icon> 2.1%
            </div>
        </div>
        <div class="metric-card glass-panel">
            <div class="label">昨日盈亏 (预估)</div>
            <div class="value" :class="dailyPnL >= 0 ? 'up' : 'down'">
                {{ dailyPnL >= 0 ? '+' : '' }}¥{{ dailyPnL.toLocaleString() }}
            </div>
            <div class="trend">
                跑赢沪深300
            </div>
        </div>
        <div class="metric-card glass-panel">
            <div class="label">持有收益率</div>
            <div class="value up">+5.82%</div>
            <div class="trend">
                年化 12.4%
            </div>
        </div>
    </div>
    
    <!-- 趋势图 -->
    <div class="trend-chart-card glass-panel">
        <h3>近 7 日资产趋势</h3>
        <v-chart class="chart-instance" :option="chartOption" autoresize />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import { Top } from '@element-plus/icons-vue';
import { use } from "echarts/core";
import { CanvasRenderer } from "echarts/renderers";
import { LineChart } from "echarts/charts";
import { GridComponent, TooltipComponent } from "echarts/components";
import VChart from "vue-echarts";

use([CanvasRenderer, LineChart, GridComponent, TooltipComponent]);

const props = defineProps<{
    totalAssets: number
}>();

// Mock data
const dailyPnL = 1258;

// 生成 Mock 趋势数据
const generateTrend = () => {
    const data = [];
    let base = props.totalAssets || 100000;
    if (base === 0) base = 50000; // default for empty
    
    // 生成过去 6 天 + 今天
    for (let i = 0; i < 7; i++) {
        const date = new Date();
        date.setDate(date.getDate() - (6 - i));
        const dateStr = `${date.getMonth() + 1}-${date.getDate()}`;
        
        // 随机波动 -1% ~ +1.5%
        const volatility = (Math.random() - 0.4) * 0.03; 
        const val = Math.round(base * (1 + volatility * (i - 3))); // 简单模拟
        data.push({ date: dateStr, value: val });
    }
    // 强制最后一个点等于当前
    if (props.totalAssets > 0) {
        data[6].value = props.totalAssets;
    }
    return data;
};

const trendData = computed(() => generateTrend());

const chartOption = computed(() => ({
    tooltip: {
        trigger: 'axis',
        formatter: '{b}: ¥{c}'
    },
    grid: { left: 10, right: 10, top: 10, bottom: 0, containLabel: true },
    xAxis: {
        type: 'category',
        data: trendData.value.map(i => i.date),
        axisLine: { show: false },
        axisTick: { show: false },
        axisLabel: { color: '#94a3b8' }
    },
    yAxis: {
        type: 'value',
        splitLine: { lineStyle: { color: 'rgba(255,255,255,0.05)' } },
        axisLabel: { show: false }
    },
    series: [{
        data: trendData.value.map(i => i.value),
        type: 'line',
        smooth: true,
        showSymbol: false,
        lineStyle: { width: 3, color: '#4f46e5' },
        areaStyle: {
            color: {
                type: 'linear',
                x: 0, y: 0, x2: 0, y2: 1,
                colorStops: [
                    { offset: 0, color: 'rgba(79, 70, 229, 0.3)' },
                    { offset: 1, color: 'rgba(79, 70, 229, 0)' }
                ]
            }
        }
    }]
}));
</script>

<style lang="scss" scoped>
@use "@/theme/variables.scss" as *;

.metrics-row {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 16px;
    margin-bottom: 24px;
    
    @media (max-width: 768px) {
        grid-template-columns: 1fr;
    }
}

.glass-panel {
  background: rgba(30, 41, 59, 0.6);
  backdrop-filter: blur(12px);
  border-radius: 12px;
  padding: 20px;
  border: 1px solid rgba(255, 255, 255, 0.05);
}

.metric-card {
    .label {
        font-size: 13px;
        color: $text-secondary;
        margin-bottom: 8px;
    }
    .value {
        font-size: 24px;
        font-weight: 700;
        color: $text-primary;
        font-family: 'Roboto Mono';
        margin-bottom: 8px;
        
        &.up { color: $finance-up; }
        &.down { color: $finance-down; }
    }
    .trend {
        font-size: 12px;
        display: flex;
        align-items: center;
        gap: 4px;
        color: $text-dim;
        
        &.positive { color: $finance-up; }
    }
}

.trend-chart-card {
    h3 {
        margin: 0 0 16px;
        font-size: 16px;
        color: $text-primary;
        font-weight: 500;
    }
}

.chart-instance {
    height: 240px;
    width: 100%;
}
</style>
