<template>
  <div class="detail-container">
    <div class="header-bar">
      <div class="left-info">
        <el-button link icon="ArrowLeft" @click="router.back()" class="back-btn">返回</el-button>
        <h2 class="stock-title">
          {{ stockName }} <span class="code">({{ stockCode }})</span>
        </h2>
        <el-tag type="danger" effect="dark" class="status-tag">交易中</el-tag>
      </div>
      <div class="right-actions">
        <el-button type="primary" size="small" @click="handleBuy">买入</el-button>
        <el-button type="warning" size="small" @click="handleSell">卖出</el-button>
        <el-button type="info" size="small" plain @click="toggleWatch">
           {{ isWatched ? '已自选' : '+ 加自选' }}
        </el-button>
      </div>
    </div>

    <div class="data-grid">
      <div class="big-price up">
        180.50 <span class="percent">+3.20%</span>
      </div>
      <div class="detail-items">
        <div class="item"><span>今开</span> <span class="val">178.00</span></div>
        <div class="item"><span>最高</span> <span class="val up">182.30</span></div>
        <div class="item"><span>最低</span> <span class="val down">176.50</span></div>
        <div class="item"><span>成交量</span> <span class="val">32.5万手</span></div>
        <div class="item"><span>换手率</span> <span class="val">1.25%</span></div>
        <div class="item"><span>市盈率</span> <span class="val">22.4</span></div>
      </div>
    </div>

    <div class="chart-section">
      <div class="chart-wrapper" ref="klineChartRef"></div>
      
      <div class="handicap-panel">
        <div class="handicap-title">五档盘口</div>
        <div class="order-book">
          <div class="order-row sell"><span class="label">卖5</span><span class="price">180.55</span><span class="vol">12</span></div>
          <div class="order-row sell"><span class="label">卖4</span><span class="price">180.54</span><span class="vol">45</span></div>
          <div class="order-row sell"><span class="label">卖3</span><span class="price">180.53</span><span class="vol">8</span></div>
          <div class="order-row sell"><span class="label">卖2</span><span class="price">180.52</span><span class="vol">112</span></div>
          <div class="order-row sell"><span class="label">卖1</span><span class="price">180.51</span><span class="vol">33</span></div>
          <div class="divider"></div>
          <div class="order-row buy"><span class="label">买1</span><span class="price">180.50</span><span class="vol">560</span></div>
          <div class="order-row buy"><span class="label">买2</span><span class="price">180.49</span><span class="vol">23</span></div>
          <div class="order-row buy"><span class="label">买3</span><span class="price">180.48</span><span class="vol">14</span></div>
          <div class="order-row buy"><span class="label">买4</span><span class="price">180.47</span><span class="vol">88</span></div>
          <div class="order-row buy"><span class="label">买5</span><span class="price">180.46</span><span class="vol">20</span></div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import * as echarts from 'echarts';
import { ElMessage } from 'element-plus';

const route = useRoute();
const router = useRouter();
const stockCode = route.params.code as string;
const stockName = (route.query.name as string) || '未知证券';
const klineChartRef = ref<HTMLElement | null>(null);
let myChart: echarts.ECharts | null = null;
const isWatched = ref(false);

const handleBuy = () => ElMessage.success('跳转至交易页面 (模拟)');
const handleSell = () => ElMessage.warning('跳转至卖出页面 (模拟)');
const toggleWatch = () => { isWatched.value = !isWatched.value; ElMessage.success(isWatched.value ? '已加入自选' : '已取消自选'); };

// 模拟 K 线数据
const generateData = () => {
  let basePrice = 180;
  const dates = [];
  const data = [];
  for (let i = 0; i < 50; i++) {
    const date = new Date();
    date.setDate(date.getDate() - (50 - i));
    dates.push([date.getFullYear(), date.getMonth() + 1, date.getDate()].join('/'));
    
    const open = basePrice + (Math.random() - 0.5) * 5;
    const close = open + (Math.random() - 0.5) * 3;
    const low = Math.min(open, close) - Math.random();
    const high = Math.max(open, close) + Math.random();
    data.push([open.toFixed(2), close.toFixed(2), low.toFixed(2), high.toFixed(2)]);
    basePrice = close;
  }
  return { dates, data };
};

const initChart = () => {
  if (!klineChartRef.value) return;
  myChart = echarts.init(klineChartRef.value);
  const { dates, data } = generateData();

  const option = {
    backgroundColor: '#1d212b',
    tooltip: { trigger: 'axis', axisPointer: { type: 'cross' } },
    grid: { left: '10%', right: '10%', bottom: '15%' },
    xAxis: { data: dates, axisLine: { lineStyle: { color: '#8392A5' } } },
    yAxis: { scale: true, axisLine: { lineStyle: { color: '#8392A5' } }, splitLine: { show: false } },
    series: [
      {
        type: 'candlestick',
        data: data,
        itemStyle: {
          color: '#FD1050',
          color0: '#0CF49B',
          borderColor: '#FD1050',
          borderColor0: '#0CF49B'
        }
      }
    ]
  };
  myChart.setOption(option);
};

onMounted(() => {
  initChart();
  window.addEventListener('resize', () => myChart?.resize());
});
onUnmounted(() => {
  window.removeEventListener('resize', () => myChart?.resize());
});
</script>

<style scoped>
.detail-container { padding: 20px; background: #14161a; min-height: 100vh; color: #fff; }
.header-bar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; border-bottom: 1px solid #2c3038; padding-bottom: 15px; }
.left-info { display: flex; align-items: center; gap: 15px; }
.back-btn { color: #909399; font-size: 16px; }
.stock-title { margin: 0; font-size: 24px; }
.stock-title .code { font-size: 16px; color: #909399; font-weight: normal; }
.data-grid { display: flex; align-items: center; gap: 40px; margin-bottom: 30px; padding: 20px; background: #1d212b; border-radius: 8px; }
.big-price { font-size: 36px; font-weight: bold; }
.big-price.up { color: #f56c6c; }
.big-price .percent { font-size: 18px; margin-left: 10px; }
.detail-items { display: grid; grid-template-columns: repeat(6, 1fr); gap: 20px; flex: 1; }
.item { display: flex; flex-direction: column; gap: 5px; }
.item span:first-child { color: #909399; font-size: 12px; }
.item .val { font-weight: bold; font-size: 16px; }
.val.up { color: #f56c6c; } .val.down { color: #67c23a; }

.chart-section { display: flex; gap: 20px; height: 500px; }
.chart-wrapper { flex: 1; background: #1d212b; border-radius: 8px; padding: 10px; }
.handicap-panel { width: 240px; background: #1d212b; border-radius: 8px; padding: 15px; }
.handicap-title { font-size: 14px; font-weight: bold; margin-bottom: 10px; border-bottom: 1px solid #363636; padding-bottom: 5px; }
.order-row { display: flex; justify-content: space-between; font-size: 12px; margin-bottom: 6px; }
.order-row .label { color: #909399; }
.order-row.sell .price { color: #67c23a; }
.order-row.buy .price { color: #f56c6c; }
.order-row .vol { color: #fff; }
.divider { height: 1px; background: #363636; margin: 8px 0; }
</style>
