<template>
  <div class="detail-container">
    <div class="header-bar">
      <div class="left-info">
        <el-button link icon="ArrowLeft" @click="router.back()" class="back-btn">返回</el-button>
        <h2 class="stock-title">
          {{ stockName }} <span class="code">({{ stockCode }})</span>
        </h2>
        <el-tag :type="marketStatus.type" effect="dark" :class="['status-tag', marketStatus.class]">{{ marketStatus.text }}</el-tag>
        <span class="update-time">{{ currentTime }} (北京时间)</span>
      </div>
      <div class="right-actions">
        <el-button type="primary" size="small" @click="handleBuy">买入</el-button>
        <el-button type="warning" size="small" @click="handleSell">卖出</el-button>
        <el-button 
          :type="isWatched ? 'info' : 'primary'" 
          plain 
          size="small" 
          @click="handleToggleWatch"
        >
           {{ isWatched ? '已自选' : '+ 加自选' }}
        </el-button>
      </div>
    </div>

    <div class="price-section">
      <div :class="['main-price', stockData.direction]">
        {{ stockData.price }} <span class="arrow">{{ stockData.arrow }}</span> <span class="percent">{{ stockData.changeText }}</span>
      </div>
      <div class="detail-metrics">
        <div class="metric-item"><span>今开</span> <span class="val">{{ stockData.open }}</span></div>
        <div class="metric-item"><span>最高</span> <span :class="['val', stockData.direction]">{{ stockData.high }}</span></div>
        <div class="metric-item"><span>最低</span> <span class="val down">{{ stockData.low }}</span></div>
        <div class="metric-item"><span>成交量</span> <span class="val">{{ stockData.volume }}</span></div>
        <div class="metric-item"><span>换手率</span> <span class="val">{{ stockData.turnover }}</span></div>
        <div class="metric-item"><span>市盈率(TTM)</span> <span class="val">{{ stockData.pe }}</span></div>
      </div>
    </div>

    <div class="chart-container">
      <div class="kline-wrapper">
        <div class="chart-toolbar">
          <span 
            v-for="p in periods" 
            :key="p" 
            :class="['period-btn', { active: currentPeriod === p }]"
            @click="switchPeriod(p)"
          >
            {{ p }}
          </span>
        </div>
        <div class="echart-box" ref="klineChartRef"></div>
      </div>

      <div class="handicap-panel">
        <div class="panel-title">五档盘口</div>
        <div class="order-book">
          <div v-for="(item, i) in sellOrders" :key="'s'+i" class="order-row sell">
            <span class="label">卖{{ 5-i }}</span>
            <span class="price down">{{ item.price }}</span>
            <span class="vol">{{ item.vol }}</span>
            <div class="bar" :style="{ width: item.percent + '%', backgroundColor: 'rgba(103, 194, 58, 0.2)' }"></div>
          </div>
          <div class="divider"></div>
          <div v-for="(item, i) in buyOrders" :key="'b'+i" class="order-row buy">
            <span class="label">买{{ i+1 }}</span>
            <span class="price up">{{ item.price }}</span>
            <span class="vol">{{ item.vol }}</span>
            <div class="bar" :style="{ width: item.percent + '%', backgroundColor: 'rgba(245, 108, 108, 0.2)' }"></div>
          </div>
        </div>
      </div>
    </div>

    <div class="f10-section">
      <el-tabs v-model="activeTab" class="f10-tabs">
        <el-tab-pane label="F10 公司简况" name="profile">
          <div class="profile-content" v-loading="loadingProfile">
            <div v-if="profile" class="info-grid">
              <div class="info-item full">
                <span class="label">公司全称：</span>
                <span class="value">{{ profile.companyName }}</span>
              </div>
              <div class="info-item">
                <span class="label">董事长：</span>
                <span class="value">{{ profile.chairman }}</span>
              </div>
              <div class="info-item">
                <span class="label">成立日期：</span>
                <span class="value">{{ profile.establishmentDate }}</span>
              </div>
              <div class="info-item">
                <span class="label">上市日期：</span>
                <span class="value">{{ profile.listingDate }}</span>
              </div>
              <div class="info-item">
                <span class="label">员工人数：</span>
                <span class="value">{{ profile.employees || '--' }} 人</span>
              </div>
              <div class="info-item full">
                <span class="label">主营业务：</span>
                <p class="value text-desc">{{ profile.businessScope }}</p>
              </div>
              <div class="info-item full">
                <span class="label">公司官网：</span>
                <a :href="profile.website" target="_blank" class="link">{{ profile.website }}</a>
              </div>
            </div>
            <el-empty v-else description="暂无F10资料，请确保数据库已注入数据" />
          </div>
        </el-tab-pane>

        <el-tab-pane label="财务摘要" name="finance">
          <div class="finance-content" v-loading="loadingFinance">
            <div v-if="financialData" class="finance-grid">
              <div class="item"><span>营业收入</span> <span class="val">{{ financialData.revenue }}</span></div>
              <div class="item"><span>净利润</span> <span class="val">{{ financialData.profit }}</span></div>
              <div class="item"><span>同比增幅</span> <span class="red">{{ financialData.revenue_growth }}</span></div>
              <div class="item"><span>每股收益</span> <span class="val">{{ financialData.eps }}</span></div>
            </div>
            <el-empty v-else description="暂无财务摘要数据" :image-size="80" />
          </div>
        </el-tab-pane>

        <el-tab-pane label="公司公告" name="notice">
          <div class="notice-content" v-loading="loadingNotice">
            <ul v-if="noticeList.length > 0" class="notice-list">
              <li v-for="item in noticeList" :key="item.title">
                <span class="date">{{ item.date }}</span>
                <span class="title">{{ item.title }}</span>
              </li>
            </ul>
            <el-empty v-else description="暂无公司公告" :image-size="80" />
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch, computed, nextTick } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import * as echarts from 'echarts';
import { ElMessage } from 'element-plus';
import dayjs from 'dayjs';
import { getCompanyProfile, toggleWatchlist, getWatchlist } from '@/api/invest';
import { getSecurityDetail, getKLineData, getMarketFinance, getMarketNotices } from '@/api/market';

const route = useRoute();
const router = useRouter();
const stockCode = route.params.code as string;
const stockName = (route.query.name as string) || '证券详情';

// 状态
const currentTime = ref(dayjs().format('HH:mm:ss'));
let timer: any = null;
const marketStatusTick = ref(Date.now());
const isWatched = ref(false);
const activeTab = ref('profile');
const currentPeriod = ref('日K');
const periods = ['分时', '日K', '周K', '月K'];

const marketStatus = computed(() => {
  // 1. 获取当前 UTC 时间，并强制转换为北京时间 (UTC+8)
  const d = new Date(marketStatusTick.value);
  const utc = d.getTime() + (d.getTimezoneOffset() * 60000);
  const bjTime = new Date(utc + (3600000 * 8)); // 北京时间

  const day = bjTime.getDay(); // 0是周日, 6是周六
  const hour = bjTime.getHours();
  const minute = bjTime.getMinutes();
  const time = hour * 60 + minute; // 转换为分钟数

  // 2. 判断周末
  if (day === 0 || day === 6) {
    return { text: '已休市', class: 'closed', type: 'info' };
  }

  // 3. 判断交易时段 (A股: 9:30-11:30, 13:00-15:00)
  const isTrading = (time >= 570 && time <= 690) || (time >= 780 && time <= 900);

  // 4. 判断午休时段 (11:30 - 13:00)
  const isBreak = (time > 690 && time < 780);

  if (isTrading) {
    return { text: '交易中', class: 'trading', type: 'danger' }; // 红色
  } else if (isBreak) {
    return { text: '午休中', class: 'closed', type: 'warning' }; // 橙色/灰色
  } else {
    return { text: '已休市', class: 'closed', type: 'info' }; // 灰色
  }
});

// 数据
const profile = ref<any>(null);
const loadingProfile = ref(false);
const financialData = ref<any | null>(null);
const loadingFinance = ref(false);
const noticeList = ref<Array<{ date: string; title: string }>>([]);
const loadingNotice = ref(false);

// 真实行情数据
const stockData = ref({
  price: '--',
  changeText: '--',
  arrow: '',
  direction: '',
  open: '--',
  high: '--',
  low: '--',
  volume: '--',
  turnover: '--',
  pe: '--',
  statusText: '加载中',
  statusType: 'info' as any,
});
// 模拟盘口数据（后续可从新浪接口获取真实五档）
// 真实五档盘口数据
const sellOrders = ref<any[]>([]);
const buyOrders = ref<any[]>([]);

// 🔥 辅助：计算盘口百分比条长度（相对于当前最大挂单量）
const updateOrderBook = (data: any) => {
  // 卖盘: ask5 -> ask1 (界面上从上到下: 卖5...卖1)
  const asks = [
    { p: data.ask5Price, v: data.ask5Vol },
    { p: data.ask4Price, v: data.ask4Vol },
    { p: data.ask3Price, v: data.ask3Vol },
    { p: data.ask2Price, v: data.ask2Vol },
    { p: data.ask1Price, v: data.ask1Vol },
  ];
  // 买盘: bid1 -> bid5 (界面上从上到下: 买1...买5)
  const bids = [
    { p: data.bid1Price, v: data.bid1Vol },
    { p: data.bid2Price, v: data.bid2Vol },
    { p: data.bid3Price, v: data.bid3Vol },
    { p: data.bid4Price, v: data.bid4Vol },
    { p: data.bid5Price, v: data.bid5Vol },
  ];

  // 找最大挂单量做分母
  const maxVol = Math.max(
    ...asks.map(i => i.v || 0),
    ...bids.map(i => i.v || 0),
    1 // 避免除以0
  );

  sellOrders.value = asks.map(item => ({
    price: item.p ? Number(item.p).toFixed(2) : '--',
    vol: item.v ? (item.v / 100).toFixed(0) : '--', // 股转手
    percent: item.v ? (item.v / maxVol) * 100 : 0
  }));

  buyOrders.value = bids.map(item => ({
    price: item.p ? Number(item.p).toFixed(2) : '--',
    vol: item.v ? (item.v / 100).toFixed(0) : '--',
    percent: item.v ? (item.v / maxVol) * 100 : 0
  }));
};

// 图表相关
const klineChartRef = ref<HTMLElement | null>(null);
let myChart: echarts.ECharts | null = null;
let priceTimer: any = null; // 价格轮询定时器
let resizeHandler: (() => void) | null = null;

// 🔥 从后端获取实时价格和详细行情
const fetchRealPrice = async () => {
  try {
    const res: any = await getSecurityDetail(stockCode);
    if (res.code === 200 && res.data) {
      const d = res.data;
      const price = Number(d.currentPrice);
      const change = Number(d.changePercent);
      
      if (price > 0) {
        stockData.value.price = price.toFixed(2);
        stockData.value.direction = change > 0 ? 'up' : (change < 0 ? 'down' : '');
        stockData.value.arrow = change > 0 ? '↑' : (change < 0 ? '↓' : '—');
        stockData.value.changeText = (change > 0 ? '+' : '') + change.toFixed(2) + '%';
        stockData.value.statusText = '交易中';
        stockData.value.statusType = 'danger';
        
        // 🔥 读取真实详细行情字段
        stockData.value.open = d.openPrice ? Number(d.openPrice).toFixed(2) : '--';
        stockData.value.high = d.highPrice ? Number(d.highPrice).toFixed(2) : '--';
        stockData.value.low = d.lowPrice ? Number(d.lowPrice).toFixed(2) : '--';
        stockData.value.volume = d.volume ? formatVolume(d.volume) : '--';
        stockData.value.turnover = d.turnover ? formatTurnover(d.turnover) : '--';
        stockData.value.pe = d.peTtm ? Number(d.peTtm).toFixed(1) : '--';

        // 🔥 更新五档盘口
        updateOrderBook(d);
      } else {
        stockData.value.price = '停牌';
        stockData.value.statusText = '停牌';
        stockData.value.statusType = 'info';
      }
    }
  } catch (e) {
    console.warn('获取实时价格失败');
  }
};

// 🔥 成交量格式化: 1234567 -> 123.46万手
const formatVolume = (vol: number): string => {
  const hands = vol / 100; // 股 -> 手
  if (hands >= 10000) return (hands / 10000).toFixed(2) + '万手';
  return hands.toFixed(0) + '手';
};

// 🔥 成交额格式化: 1234567 -> 123.46万 或 1.23亿
const formatTurnover = (amount: number): string => {
  if (amount >= 100000000) return (amount / 100000000).toFixed(2) + '亿';
  if (amount >= 10000) return (amount / 10000).toFixed(2) + '万';
  return amount.toFixed(2);
};

// API 调用
const loadProfile = async () => {
  loadingProfile.value = true;
  try {
    const res: any = await getCompanyProfile(stockCode);
    if (res.code === 200 && res.data) {
      profile.value = res.data;
    }
  } finally {
    loadingProfile.value = false;
  }
};

const loadFinanceSummary = async () => {
  loadingFinance.value = true;
  try {
    const res: any = await getMarketFinance(stockCode);
    if (res.code === 200 && res.data && Object.keys(res.data).length > 0) {
      financialData.value = res.data;
    } else {
      financialData.value = null;
    }
  } finally {
    loadingFinance.value = false;
  }
};

const loadNotices = async () => {
  loadingNotice.value = true;
  try {
    const res: any = await getMarketNotices(stockCode);
    if (res.code === 200) {
      noticeList.value = res.data || [];
    }
  } finally {
    loadingNotice.value = false;
  }
};

const checkWatchStatus = async () => {
  const res: any = await getWatchlist();
  if (res.code === 200 && res.data) {
    isWatched.value = res.data.includes(stockCode);
  }
};

const handleToggleWatch = async () => {
  await toggleWatchlist(stockCode);
  isWatched.value = !isWatched.value;
  ElMessage.success(isWatched.value ? '已加入自选' : '已移出自选');
};

const handleBuy = () => ElMessage.success('跳转至交易页面 (模拟)');
const handleSell = () => ElMessage.warning('跳转至卖出页面 (模拟)');

// 监听 Tab 切换，懒加载数据
watch(activeTab, (val) => {
  if (val === 'finance' && !financialData.value) {
    loadFinanceSummary();
  } else if (val === 'notice' && noticeList.value.length === 0) {
    loadNotices();
  }
});

// 🔥 K线周期到新浪 scale 映射
const periodToType: Record<string, string> = {
  '日K': 'day',
  '周K': 'week',
  '月K': 'month',
  '分时': '60min',
};

const initChart = async (period = '日K') => {
  if (!klineChartRef.value) return;
  if (!myChart) myChart = echarts.init(klineChartRef.value);

  // 显示加载状态
  myChart.showLoading({ text: '加载 K 线数据...' });

  try {
    const typeParam = periodToType[period] || 'day';
    const res: any = await getKLineData(stockCode, typeParam);
    
    // 新浪返回 JSON 数组: [{day, open, high, low, close, volume}, ...]
    let kdata: any[] = [];
    if (Array.isArray(res)) {
      kdata = res;
    } else if (res.data && Array.isArray(res.data)) {
      kdata = res.data;
    }

    if (kdata.length === 0) {
      myChart.hideLoading();
      myChart.setOption({ title: { text: '暂无K线数据', left: 'center', top: 'center', textStyle: { color: '#909399' } } });
      return;
    }

    const dates = kdata.map((item: any) => item.day);
    const values = kdata.map((item: any) => [
      parseFloat(item.open),
      parseFloat(item.close),
      parseFloat(item.low),
      parseFloat(item.high)
    ]);
    const volumes = kdata.map((item: any) => parseInt(item.volume));

    myChart.hideLoading();

    const option = {
      backgroundColor: '#1d212b',
      tooltip: {
        trigger: 'axis',
        axisPointer: { type: 'cross' },
        formatter: (params: any) => {
          const p = params[0];
          if (!p || !p.data) return '';
          const [open, close, low, high] = p.data;
          return `${p.axisValue}<br/>开: ${open}<br/>收: ${close}<br/>低: ${low}<br/>高: ${high}`;
        }
      },
      grid: [
        { left: '8%', right: '5%', bottom: '30%', top: '8%' },
        { left: '8%', right: '5%', bottom: '8%', top: '75%' }
      ],
      xAxis: [
        {
          data: dates,
          axisLine: { lineStyle: { color: '#606266' } },
          axisLabel: { show: false },
          gridIndex: 0
        },
        {
          data: dates,
          axisLine: { lineStyle: { color: '#606266' } },
          axisLabel: { fontSize: 10, color: '#909399' },
          gridIndex: 1
        }
      ],
      yAxis: [
        { scale: true, splitLine: { lineStyle: { color: '#2c3038' } }, axisLine: { show: false }, gridIndex: 0 },
        { 
          scale: true, 
          splitLine: { show: false }, 
          axisLine: { show: false }, 
          gridIndex: 1,
          axisLabel: {
            formatter: (val: number) => {
              return (val / 10000).toFixed(0) + '万';
            }
          }
        }
      ],
      series: [
        {
          type: 'candlestick',
          data: values,
          xAxisIndex: 0,
          yAxisIndex: 0,
          itemStyle: {
            color: '#f56c6c',      // 涨: 红色
            color0: '#67c23a',     // 跌: 绿色
            borderColor: '#f56c6c',
            borderColor0: '#67c23a'
          }
        },
        {
          type: 'bar',
          data: volumes,
          xAxisIndex: 1,
          yAxisIndex: 1,
          itemStyle: {
            color: (params: any) => {
              const idx = params.dataIndex;
              const [open, close] = values[idx];
              return close >= open ? '#f56c6c' : '#67c23a';
            }
          }
        }
      ]
    };
    myChart.setOption(option, true);
    requestAnimationFrame(() => myChart?.resize());

  } catch (e) {
    myChart.hideLoading();
    console.warn('K线数据加载失败', e);
    myChart.setOption({ title: { text: 'K线数据加载失败', left: 'center', top: 'center', textStyle: { color: '#f56c6c' } } });
  }
};

const switchPeriod = (p: string) => {
  currentPeriod.value = p;
  initChart(p);
};

onMounted(() => {
  loadProfile();
  checkWatchStatus();
  loadFinanceSummary();
  loadNotices();
  fetchRealPrice();  // 🔥 立即获取真实价格
  initChart();
  nextTick(() => {
    resizeHandler = () => {
      if (!myChart) return;
      requestAnimationFrame(() => myChart?.resize());
    };
    window.addEventListener('resize', resizeHandler);
  });
  timer = setInterval(() => { 
    currentTime.value = dayjs().format('HH:mm:ss'); 
    marketStatusTick.value = Date.now();
  }, 1000);
  // 🔥 每 3 秒刷新价格
  priceTimer = setInterval(() => { fetchRealPrice(); }, 3000);
});

onUnmounted(() => {
  if (resizeHandler) {
    window.removeEventListener('resize', resizeHandler);
    resizeHandler = null;
  }
  if (timer) clearInterval(timer);
  if (priceTimer) clearInterval(priceTimer);
});
</script>

<style scoped>
.detail-container { padding: 20px; background: #14161a; min-height: 100vh; color: #fff; }

/* 头部 */
.header-bar { display: flex; justify-content: space-between; align-items: center; padding-bottom: 20px; border-bottom: 1px solid #2c3038; }
.left-info { display: flex; align-items: center; gap: 12px; }
.stock-title { margin: 0; font-size: 24px; }
.stock-title .code { font-size: 16px; color: #909399; font-weight: normal; }
.update-time { font-size: 12px; color: #606266; margin-left: 10px; }
.status-tag.trading { background-color: #f56c6c !important; border-color: #f56c6c !important; color: #fff !important; }
.status-tag.closed { background-color: #606266 !important; border-color: #606266 !important; color: #fff !important; }
.status-tag.closed.el-tag--warning { background-color: #e6a23c !important; border-color: #e6a23c !important; color: #fff !important; }

/* 价格区 */
.price-section { display: flex; align-items: center; gap: 40px; padding: 20px 0; }
.main-price { font-size: 40px; font-weight: bold; font-family: monospace; }
.main-price.up { color: #f56c6c; }
.main-price .percent { font-size: 18px; margin-left: 10px; }
.detail-metrics { display: grid; grid-template-columns: repeat(6, 1fr); gap: 20px; flex: 1; }
.metric-item { display: flex; flex-direction: column; }
.metric-item span:first-child { color: #909399; font-size: 12px; }
.metric-item .val { font-weight: bold; font-size: 16px; margin-top: 4px; }
.val.up { color: #f56c6c; } .val.down { color: #67c23a; }

/* 图表与盘口 */
.chart-container { display: flex; gap: 20px; height: 500px; margin-bottom: 20px; }
.kline-wrapper { flex: 1; background: #1d212b; border-radius: 8px; display: flex; flex-direction: column; border: 1px solid #2c3038; }
.chart-toolbar { padding: 10px; display: flex; gap: 10px; border-bottom: 1px solid #2c3038; }
.period-btn { cursor: pointer; padding: 4px 12px; border-radius: 4px; font-size: 13px; color: #909399; }
.period-btn.active { background: #2b303c; color: #409eff; font-weight: bold; }
.echart-box { flex: 1; }

.handicap-panel { width: 260px; background: #1d212b; border-radius: 8px; border: 1px solid #2c3038; padding: 15px; }
.panel-title { font-weight: bold; margin-bottom: 15px; font-size: 14px; border-left: 3px solid #f56c6c; padding-left: 8px; }
.order-row { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; font-size: 12px; position: relative; }
.order-row .label { color: #909399; width: 30px; z-index: 1; }
.order-row .price { font-weight: bold; width: 60px; text-align: right; z-index: 1; }
.order-row.sell .price { color: #67c23a; } .order-row.buy .price { color: #f56c6c; }
.order-row .vol { color: #fff; z-index: 1; }
.order-row .bar { position: absolute; right: 0; top: 0; bottom: 0; background: rgba(255, 255, 255, 0.05); z-index: 0; }
.divider { height: 1px; background: #2c3038; margin: 10px 0; }

/* F10 资料 */
.f10-section { background: #1d212b; border-radius: 8px; padding: 20px; border: 1px solid #2c3038; }
:deep(.el-tabs__item) { color: #909399; }
:deep(.el-tabs__item.is-active) { color: #409eff; }
:deep(.el-tabs__nav-wrap::after) { background-color: #2c3038; }
.info-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 20px; padding: 10px; }
.info-item { display: flex; gap: 10px; }
.info-item.full { grid-column: span 2; }
.info-item .label { color: #909399; white-space: nowrap; }
.info-item .value { color: #dcdfe6; line-height: 1.5; }
.text-desc { margin: 0; font-size: 13px; color: #b1b3b8; }
.link { color: #409eff; text-decoration: none; }

/* 财务表格 */
.finance-content { padding: 10px 0; }
.dark-table { --el-table-border-color: #363636; --el-table-bg-color: #1d212b; --el-table-tr-bg-color: #1d212b; }
:deep(.el-table td.el-table__cell), :deep(.el-table th.el-table__cell.is-leaf) { border-bottom: 1px solid #363636; }
.finance-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 16px; padding: 8px 0; }
.finance-grid .item { display: flex; justify-content: space-between; align-items: center; padding: 12px; background: #181b22; border: 1px solid #2c3038; border-radius: 6px; }
.finance-grid .val { font-weight: bold; }
.finance-grid .red { color: #f56c6c; font-weight: bold; }

/* 公告列表 */
.notice-list { padding: 5px 0; margin: 0; list-style: none; }
.notice-list li { display: flex; justify-content: space-between; align-items: center; padding: 12px 0; border-bottom: 1px solid #2c3038; }
.notice-list .date { color: #909399; margin-right: 15px; font-family: monospace; width: 100px; }
.notice-list .title { flex: 1; }
</style>
