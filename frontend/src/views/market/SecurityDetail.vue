<template>
  <div class="security-detail">
    <!-- 返回按钮 -->
    <div class="back-bar">
      <el-button :icon="ArrowLeft" link @click="router.back()">
        返回市场中心
      </el-button>
    </div>

    <!-- 头部信息 -->
    <header class="detail-header">
      <div class="security-main">
        <h1 class="security-name">{{ security?.name }}</h1>
        <span class="security-code">{{ security?.code }}</span>
        <el-tag :type="getRiskTagType(security?.riskLevel)" class="risk-tag">
          {{ security?.riskLevel }}
        </el-tag>
      </div>
      <div class="price-info">
        <span class="current-price" :class="getChangeClass(security?.changePercent || 0)">
          {{ formatPrice(security?.currentPrice || 0) }}
        </span>
        <span class="change-value" :class="getChangeClass(security?.changePercent || 0)">
          {{ formatChange(security?.changePercent || 0) }}
        </span>
      </div>
    </header>

    <!-- 图表区 -->
    <section class="chart-section">
      <div class="chart-header">
        <h2 class="section-title">近30日走势</h2>
        <div class="chart-tabs">
          <span 
            class="chart-tab" 
            :class="{ active: kLineType === 'day' }"
            @click="kLineType = 'day'"
          >日K</span>
          <span 
            class="chart-tab" 
            :class="{ active: kLineType === 'week' }"
            @click="kLineType = 'week'"
          >周K</span>
          <span 
            class="chart-tab" 
            :class="{ active: kLineType === 'month' }"
            @click="kLineType = 'month'"
          >月K</span>
        </div>
      </div>
      <div ref="chartRef" class="chart-container"></div>
    </section>

    <!-- 信息区 -->
    <section class="info-section">
      <h2 class="section-title">基本信息</h2>
      <div class="info-grid">
        <div class="info-item">
          <span class="info-label">市值</span>
          <span class="info-value">{{ security?.marketCap || '--' }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">市盈率</span>
          <span class="info-value">{{ security?.peRatio ? security.peRatio.toFixed(2) : '--' }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">所属板块</span>
          <span class="info-value">{{ security?.sector }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">成交量</span>
          <span class="info-value">{{ security?.volume || '--' }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">52周范围</span>
          <span class="info-value" style="font-size: 14px">
            {{ security?.low52w }} ~ {{ security?.high52w }}
          </span>
        </div>
        <div class="info-item">
          <span class="info-label">风险等级</span>
          <span class="info-value">{{ security?.riskLevel }}</span>
        </div>
      </div>
      <div class="description">
        <h3 class="desc-title">资产简介</h3>
        <p class="desc-text">{{ security?.description }}</p>
      </div>
    </section>

    <!-- 底部操作栏 -->
    <div class="action-bar">
      <el-button size="large" class="action-btn favorite-btn" @click="handleFavorite">
        <Star class="btn-icon" />
        加入自选
      </el-button>
      <el-button size="large" type="primary" class="action-btn buy-btn" @click="handleBuy">
        模拟买入
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ArrowLeft, Star } from 'lucide-vue-next';
import { ElMessage } from 'element-plus';
import * as echarts from 'echarts';
import { useMarketStore } from '@/store/modules/market';
import type { Security } from '@/api/market';

const route = useRoute();
const router = useRouter();
const marketStore = useMarketStore();
const chartRef = ref<HTMLElement>();
let chartInstance: echarts.ECharts | null = null;
type KLineType = 'day' | 'week' | 'month';
const kLineType = ref<KLineType>('day');

const security = ref<Security | undefined>();

// 格式化价格
const formatPrice = (price: number) => {
  if (price >= 1000) {
    return price.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  }
  return price.toFixed(price >= 100 ? 2 : 4);
};

// 格式化涨跌幅
const formatChange = (change: number) => {
  const prefix = change > 0 ? '+' : '';
  return `${prefix}${change.toFixed(2)}%`;
};

// 获取涨跌样式类
const getChangeClass = (change: number) => {
  if (change > 0) return 'text-up';
  if (change < 0) return 'text-down';
  return '';
};

// 获取风险等级颜色
const getRiskTagType = (level?: string): '' | 'success' | 'warning' | 'danger' | 'info' => {
  if (!level) return 'info';
  const types: Record<string, '' | 'success' | 'warning' | 'danger' | 'info'> = {
    'R1': 'success',
    'R2': 'success',
    'R3': 'warning',
    'R4': 'danger',
    'R5': 'danger'
  };
  return types[level] || 'info';
};

// 获取证券类型名称
const getTypeName = (type?: string) => {
  const names: Record<string, string> = {
    'stock': '股票',
    'fund': '基金',
    'bond': '债券'
  };
  return type ? names[type] || '未知' : '未知';
};

// 生成模拟 K 线数据
const generateMockData = (basePrice: number, type: KLineType) => {
  const dates: string[] = [];
  const prices: number[] = [];
  const now = new Date();
  
  // 算法参数调整
  let count = 30;
  let volatility = 0.05; // 波动率
  let trend = 0; // 趋势倾向
  
  if (type === 'week') {
    count = 12;
    volatility = 0.12;
  } else if (type === 'month') {
    count = 12;
    volatility = 0.25;
    trend = 0.02; // 重趋势
  }
  
  let price = basePrice * (1 - (Math.random() - 0.5) * volatility); // 随机起始点
  
  for (let i = count - 1; i >= 0; i--) {
    const date = new Date(now);
    if (type === 'day') date.setDate(date.getDate() - i);
    if (type === 'week') date.setDate(date.getDate() - i * 7);
    if (type === 'month') date.setMonth(date.getMonth() - i);
    
    // 格式化日期
    const dateStr = type === 'month' 
      ? `${date.getFullYear()}-${date.getMonth() + 1}`
      : `${date.getMonth() + 1}/${date.getDate()}`;
    dates.push(dateStr);
    
    // 随机波动 + 趋势
    const change = (Math.random() - 0.5) * volatility + trend;
    price = price * (1 + change);
    prices.push(Number(price.toFixed(2)));
  }
  
  // 强行修正最后一点为当前价，平滑过度
  prices[count - 1] = basePrice;
  
  return { dates, prices };
};

// 初始化/更新图表
const updateChart = () => {
  if (!chartRef.value || !security.value) return;
  
  if (!chartInstance) {
    chartInstance = echarts.init(chartRef.value);
  }
  
  const { dates, prices } = generateMockData(security.value.currentPrice, kLineType.value);
  const isUp = prices[prices.length - 1] >= prices[0];
  const color = isUp ? '#F56C6C' : '#67C23A';
  
  const option: echarts.EChartsOption = {
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      top: '8%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: dates,
      boundaryGap: false,
      axisLine: { lineStyle: { color: 'rgba(255,255,255,0.1)' } },
      axisLabel: { color: '#64748b', fontSize: 11 },
      axisTick: { show: false }
    },
    yAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: 'rgba(255,255,255,0.05)' } },
      axisLabel: { color: '#64748b', fontSize: 11 },
      min: (value) => Math.floor(value.min * 0.95),
      max: (value) => Math.ceil(value.max * 1.05)
    },
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(15, 23, 42, 0.9)',
      borderColor: 'rgba(255,255,255,0.1)',
      textStyle: { color: '#fff' },
      formatter: (params: any) => {
        const data = params[0];
        return `${data.axisValue}<br/>价格: <strong style="color:${color}">${data.value}</strong>`;
      }
    },
    series: [{
      type: 'line',
      data: prices,
      smooth: true,
      symbol: 'none',
      lineStyle: { color, width: 2 },
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: isUp ? 'rgba(245, 108, 108, 0.4)' : 'rgba(103, 194, 58, 0.4)' },
          { offset: 1, color: 'rgba(0, 0, 0, 0)' }
        ])
      },
      animationDuration: 500
    }]
  };
  
  chartInstance.setOption(option);
};

// 加入自选
const handleFavorite = () => {
  ElMessage.success('已加入自选');
};

// 模拟买入
const handleBuy = () => {
  router.push('/plan');
  ElMessage.info('请在投资计划中创建买入计划');
};

// 窗口大小变化时重绘图表
const handleResize = () => {
  chartInstance?.resize();
};

onMounted(() => {
  const id = Number(route.params.id);
  security.value = marketStore.getSecurityById(id);
  
  if (!security.value) {
    // 如果 store 中没有数据，先加载
    marketStore.fetchSecurities().then(() => {
      security.value = marketStore.getSecurityById(id);
      updateChart();
    });
  } else {
    updateChart();
  }
  
  window.addEventListener('resize', handleResize);
});

onUnmounted(() => {
  chartInstance?.dispose();
  window.removeEventListener('resize', handleResize);
});

watch(() => security.value, () => {
  if (security.value) {
    updateChart();
  }
});

watch(kLineType, () => {
  updateChart();
});
</script>

<style lang="scss" scoped>
.security-detail {
  min-height: calc(100vh - 112px);
  padding: 24px;
  padding-bottom: 100px;
  background: var(--fc-bg-gradient);
}

.back-bar {
  margin-bottom: 16px;
  
  .el-button {
    color: var(--fc-text-muted);
    
    &:hover {
      color: #06b6d4;
    }
  }
}

/* 头部信息 */
.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
  padding: 24px;
  background: var(--fc-panel-hover);
  backdrop-filter: blur(10px);
  border: 1px solid var(--fc-border);
  border-radius: 16px;
}

.security-main {
  display: flex;
  align-items: center;
  gap: 12px;
}

.security-name {
  font-size: 28px;
  font-weight: 700;
  color: white;
  margin: 0;
}

.security-code {
  font-size: 14px;
  color: var(--fc-text-disabled);
}

.risk-tag {
  margin-left: 8px;
}

.price-info {
  text-align: right;
}

.current-price {
  display: block;
  font-size: 36px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}

.change-value {
  font-size: 18px;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}

/* 图表区 */
.chart-section {
  margin-bottom: 24px;
  padding: 24px;
  background: var(--fc-panel-hover);
  backdrop-filter: blur(10px);
  border: 1px solid var(--fc-border);
  border-radius: 16px;
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.section-title {
  font-size: 18px;
  font-weight: 600;
  color: white;
  margin: 0;
}

.chart-tabs {
  display: flex;
  gap: 8px;
}

.chart-tab {
  padding: 6px 12px;
  font-size: 13px;
  color: var(--fc-text-disabled);
  background: var(--fc-panel-hover);
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s ease;
  
  &:hover {
    color: var(--fc-text-muted);
  }
  
  &.active {
    color: white;
    background: rgba(6, 182, 212, 0.2);
  }
}

.chart-container {
  height: 300px;
}

/* 信息区 */
.info-section {
  margin-bottom: 24px;
  padding: 24px;
  background: var(--fc-panel-hover);
  backdrop-filter: blur(10px);
  border: 1px solid var(--fc-border);
  border-radius: 16px;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 24px;
  margin-top: 16px;
  margin-bottom: 24px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.info-label {
  font-size: 13px;
  color: var(--fc-text-disabled);
}

.info-value {
  font-size: 16px;
  font-weight: 600;
  color: white;
}

.description {
  padding-top: 20px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
}

.desc-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--fc-text-muted);
  margin: 0 0 8px 0;
}

.desc-text {
  font-size: 14px;
  color: var(--fc-text-muted);
  line-height: 1.6;
  margin: 0;
}

/* 底部操作栏 */
.action-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  gap: 16px;
  padding: 16px 24px;
  background: rgba(15, 23, 42, 0.95);
  backdrop-filter: blur(12px);
  border-top: 1px solid rgba(255, 255, 255, 0.1);
}

.action-btn {
  flex: 1;
  height: 48px;
  font-size: 16px;
  font-weight: 600;
  border-radius: 12px;
}

.favorite-btn {
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.2);
  color: white;
  
  &:hover {
    background: rgba(255, 255, 255, 0.15);
  }
  
  .btn-icon {
    width: 18px;
    height: 18px;
    margin-right: 8px;
  }
}

.buy-btn {
  background: linear-gradient(135deg, #06b6d4, var(--fc-primary));
  border: none;
  
  &:hover {
    transform: scale(1.02);
    box-shadow: 0 0 20px rgba(79, 70, 229, 0.4);
  }
}

/* 金融色 */
.text-up {
  color: #F56C6C;
}

.text-down {
  color: #67C23A;
}
</style>
