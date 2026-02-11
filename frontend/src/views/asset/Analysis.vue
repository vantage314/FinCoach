<template>
  <div class="analysis-container">
    <div class="metrics-row">
      <div class="metric-card total">
        <div class="label">总资产估值 (元)</div>
        <div class="value">¥ {{ formatMoney(data.totalAsset) }}</div>
        <div class="sub">今日盈亏: <span class="plus">+{{ formatMoney(data.dayProfit) }}</span></div>
      </div>
      <div class="metric-card profit">
        <div class="label">累计持仓盈亏 (元)</div>
        <div class="value" :class="data.totalProfit >= 0 ? 'plus' : 'minus'">
          {{ data.totalProfit >= 0 ? '+' : '' }}{{ formatMoney(data.totalProfit) }}
        </div>
        <div class="sub">跑赢通胀: <span class="plus">是</span></div>
      </div>
      <div class="metric-card health" :class="getHealthClass(data.healthScore)">
        <div class="score-circle">
          <span class="score">{{ data.healthScore }}</span>
          <span class="text">分</span>
        </div>
        <div class="health-info">
          <div class="level">{{ data.healthLevel }}</div>
          <div class="desc">点击查看体检报告 ></div>
        </div>
      </div>
    </div>

    <div class="charts-row">
      <div class="chart-card">
        <h3>资产配置分布</h3>
        <div ref="pieChartRef" class="echart-box"></div>
      </div>
      
      <div class="report-card">
        <h3>🩺 智能体检报告</h3>
        <div class="suggestion-list">
          <div v-if="data.suggestions.length === 0" class="empty-tip">暂无建议，资产状况良好</div>
          <div v-for="(item, index) in data.suggestions" :key="index" class="suggestion-item">
            <el-icon v-if="item.includes('⚠️')" color="#f56c6c"><Warning /></el-icon>
            <el-icon v-else-if="item.includes('✅')" color="#67c23a"><CircleCheck /></el-icon>
            <el-icon v-else color="#e6a23c"><InfoFilled /></el-icon>
            <span class="text">{{ item }}</span>
          </div>
        </div>
        <el-button type="primary" plain class="re-check-btn" @click="loadData">重新体检</el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue';
import * as echarts from 'echarts';
import request from '@/api/request';
import { Warning, CircleCheck, InfoFilled } from '@element-plus/icons-vue';

const data = ref<any>({
  totalAsset: 0,
  totalProfit: 0,
  dayProfit: 0,
  healthScore: 0,
  healthLevel: '--',
  suggestions: [],
  typeDistribution: {}
});

const pieChartRef = ref<HTMLElement | null>(null);
let myChart: echarts.ECharts | null = null;

const loadData = async () => {
  try {
    const res: any = await request.get('/asset/analysis');
    if (res.code === 200) {
      data.value = res.data;
      initCharts(res.data.typeDistribution);
    }
  } catch (error) {
    console.error("加载资产分析失败", error);
  }
};

const initCharts = (distMap: any) => {
  if (!pieChartRef.value) return;
  if (myChart) myChart.dispose();
  
  myChart = echarts.init(pieChartRef.value);
  
  const pieData = Object.keys(distMap).map(key => ({
    name: formatType(key),
    value: distMap[key]
  }));

  myChart.setOption({
    backgroundColor: 'transparent',
    tooltip: { trigger: 'item' },
    legend: { bottom: '5%', left: 'center', textStyle: { color: '#909399' } },
    series: [
      {
        name: '资产分布',
        type: 'pie',
        radius: ['40%', '70%'],
        avoidLabelOverlap: false,
        itemStyle: { borderRadius: 10, borderColor: '#1d212b', borderWidth: 2 },
        label: { show: false },
        emphasis: { label: { show: true, fontSize: 16, fontWeight: 'bold', color: '#fff' } },
        data: pieData
      }
    ]
  });
};

const formatMoney = (val: number) => Number(val).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
const formatType = (key: string) => {
  const map: any = { STOCK: '股票', FUND: '基金', BOND: '债券', CASH: '现金', FIXED: '固收' };
  return map[key] || key;
};
const getHealthClass = (score: number) => {
  if (score >= 90) return 'bg-success';
  if (score >= 60) return 'bg-warning';
  return 'bg-danger';
};

onMounted(() => {
  loadData();
  window.addEventListener('resize', () => myChart?.resize());
});
onUnmounted(() => {
  window.removeEventListener('resize', () => myChart?.resize());
});
</script>

<style scoped>
/* 全局容器：深色背景 */
.analysis-container { padding: 20px; background: #14161a; min-height: 100vh; color: #fff; }

/* 1. 指标卡片 */
.metrics-row { display: grid; grid-template-columns: repeat(3, 1fr); gap: 20px; margin-bottom: 20px; }
.metric-card { 
  background: #1d212b;
  padding: 20px; 
  border-radius: 12px; 
  border: 1px solid #2c3038; 
  position: relative; 
  overflow: hidden; 
}

.metric-card .label { font-size: 14px; color: #909399; margin-bottom: 8px; }
.metric-card .value { font-size: 28px; font-weight: bold; color: #fff; font-family: monospace; }
.metric-card .sub { font-size: 12px; color: #909399; margin-top: 8px; }
.plus { color: #f56c6c; } .minus { color: #67c23a; }

/* 健康分卡片样式 (渐变色) */
.metric-card.health { display: flex; align-items: center; gap: 20px; color: #fff; border: none; }
.metric-card.bg-success { background: linear-gradient(135deg, #67c23a 0%, #409eff 100%); }
.metric-card.bg-warning { background: linear-gradient(135deg, #e6a23c 0%, #f56c6c 100%); }
.metric-card.bg-danger { background: linear-gradient(135deg, #f56c6c 0%, #909399 100%); }

.score-circle { width: 60px; height: 60px; background: rgba(255,255,255,0.2); border-radius: 50%; display: flex; align-items: center; justify-content: center; flex-direction: column; line-height: 1; }
.score-circle .score { font-size: 24px; font-weight: bold; }
.score-circle .text { font-size: 12px; }
.health-info .level { font-size: 20px; font-weight: bold; margin-bottom: 4px; }
.health-info .desc { font-size: 12px; opacity: 0.8; cursor: pointer; text-decoration: underline; }

/* 2. 图表与报告区 */
.charts-row { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; }
.chart-card, .report-card { 
  background: #1d212b;
  padding: 20px; 
  border-radius: 12px; 
  border: 1px solid #2c3038;
  min-height: 400px; 
}
h3 { margin-top: 0; margin-bottom: 20px; font-size: 16px; border-left: 4px solid #409eff; padding-left: 10px; }

.echart-box { height: 350px; }

.suggestion-list { margin-top: 20px; display: flex; flex-direction: column; gap: 15px; }
.suggestion-item { 
  display: flex; gap: 10px; align-items: flex-start; 
  background: #2b303c;
  padding: 10px; 
  border-radius: 8px; 
  border: 1px solid #363636; 
}
.suggestion-item .text { font-size: 14px; color: #dcdfe6; line-height: 1.5; }
.empty-tip { color: #909399; text-align: center; margin-top: 20px; }
.re-check-btn { margin-top: 20px; width: 100%; }
</style>
