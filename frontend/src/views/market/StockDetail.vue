<template>
  <div class="detail-container">
    <div class="header-bar">
      <div class="left-info">
        <el-button link icon="ArrowLeft" @click="router.back()" class="back-btn">返回</el-button>
        <h2 class="stock-title">
          {{ stockName }} <span class="code">({{ stockCode }})</span>
        </h2>
        <el-tag type="danger" effect="dark" class="status-tag">交易中</el-tag>
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
      <div class="main-price up">
        180.50 <span class="arrow">↑</span> <span class="percent">+3.20%</span>
      </div>
      <div class="detail-metrics">
        <div class="metric-item"><span>今开</span> <span class="val">178.00</span></div>
        <div class="metric-item"><span>最高</span> <span class="val up">182.30</span></div>
        <div class="metric-item"><span>最低</span> <span class="val down">176.50</span></div>
        <div class="metric-item"><span>成交量</span> <span class="val">32.5万手</span></div>
        <div class="metric-item"><span>换手率</span> <span class="val">1.25%</span></div>
        <div class="metric-item"><span>市盈率(TTM)</span> <span class="val">22.4</span></div>
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
            <span class="price">{{ item.price }}</span>
            <span class="vol">{{ item.vol }}</span>
            <div class="bar" :style="{ width: item.percent + '%' }"></div>
          </div>
          <div class="divider"></div>
          <div v-for="(item, i) in buyOrders" :key="'b'+i" class="order-row buy">
            <span class="label">买{{ i+1 }}</span>
            <span class="price">{{ item.price }}</span>
            <span class="vol">{{ item.vol }}</span>
            <div class="bar" :style="{ width: item.percent + '%' }"></div>
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
            <el-table v-if="reportList.length > 0" :data="reportList" class="dark-table" style="width: 100%"
              :header-cell-style="{ background: '#1d212b', color: '#909399' }">
              <el-table-column prop="reportName" label="报告期" width="120" />
              <el-table-column prop="revenue" label="营收" />
              <el-table-column prop="revenueGrowth" label="营收同比" />
              <el-table-column prop="netProfit" label="净利润" />
              <el-table-column prop="profitGrowth" label="利润同比" />
              <el-table-column prop="eps" label="每股收益" />
              <el-table-column prop="roe" label="ROE" />
            </el-table>
            <el-empty v-else description="暂无财务报表数据" :image-size="80" />
          </div>
        </el-tab-pane>

        <el-tab-pane label="公司公告" name="notice">
          <div class="notice-content" v-loading="loadingNotice">
            <div v-if="noticeList.length > 0" class="notice-list">
              <div 
                class="notice-item" 
                v-for="item in noticeList" 
                :key="item.id"
                @click="handleNoticeClick(item)"
              >
                <span class="date">{{ item.publishDate }}</span>
                <span class="title">{{ item.title }}</span>
                <el-tag size="small" effect="plain">{{ item.type || 'PDF' }}</el-tag>
              </div>
            </div>
            <el-empty v-else description="暂无公司公告" :image-size="80" />
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import * as echarts from 'echarts';
import { ElMessage } from 'element-plus';
import dayjs from 'dayjs';
import { getCompanyProfile, toggleWatchlist, getWatchlist, getFinancialReports, getCompanyNotices } from '@/api/invest';

const route = useRoute();
const router = useRouter();
const stockCode = route.params.code as string;
const stockName = (route.query.name as string) || '证券详情';

// 状态
const currentTime = ref(dayjs().format('HH:mm:ss'));
let timer: any = null;
const isWatched = ref(false);
const activeTab = ref('profile');
const currentPeriod = ref('日K');
const periods = ['分时', '日K', '周K', '月K'];

// 数据
const profile = ref<any>(null);
const loadingProfile = ref(false);
const reportList = ref<any[]>([]);
const loadingFinance = ref(false);
const noticeList = ref<any[]>([]);
const loadingNotice = ref(false);

// 模拟盘口数据
const sellOrders = [
  { price: '180.55', vol: 12, percent: 10 },
  { price: '180.54', vol: 45, percent: 30 },
  { price: '180.53', vol: 8, percent: 5 },
  { price: '180.52', vol: 112, percent: 80 },
  { price: '180.51', vol: 33, percent: 20 },
];
const buyOrders = [
  { price: '180.50', vol: 560, percent: 90 },
  { price: '180.49', vol: 23, percent: 15 },
  { price: '180.48', vol: 14, percent: 10 },
  { price: '180.47', vol: 88, percent: 60 },
  { price: '180.46', vol: 20, percent: 12 },
];

// 图表相关
const klineChartRef = ref<HTMLElement | null>(null);
let myChart: echarts.ECharts | null = null;

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

const loadFinance = async () => {
  loadingFinance.value = true;
  try {
    const res: any = await getFinancialReports(stockCode);
    if (res.code === 200) {
      reportList.value = res.data || [];
    }
  } finally {
    loadingFinance.value = false;
  }
};

const loadNotices = async () => {
  loadingNotice.value = true;
  try {
    const res: any = await getCompanyNotices(stockCode);
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

const handleNoticeClick = (item: any) => {
  ElMessage.success(`正在打开公告: ${item.title}`);
  // 模拟打开 PDF
  if (item.link) {
    window.open(item.link, '_blank');
  } else {
    window.open('about:blank', '_blank');
  }
};

// 监听 Tab 切换，懒加载数据
watch(activeTab, (val) => {
  if (val === 'finance' && reportList.value.length === 0) {
    loadFinance();
  } else if (val === 'notice' && noticeList.value.length === 0) {
    loadNotices();
  }
});

// K线模拟数据生成器
const generateKData = (period: string) => {
  const count = period === '日K' ? 60 : (period === '周K' ? 30 : 20);
  let basePrice = 180;
  const dates = [];
  const data = [];
  for (let i = 0; i < count; i++) {
    const date = dayjs().subtract(count - i, period === '日K' ? 'day' : (period === '周K' ? 'week' : 'month')).format('YYYY-MM-DD');
    dates.push(date);
    
    const volatility = period === '日K' ? 5 : 15;
    const open = basePrice + (Math.random() - 0.5) * volatility;
    const close = open + (Math.random() - 0.5) * volatility;
    const low = Math.min(open, close) - Math.random() * 2;
    const high = Math.max(open, close) + Math.random() * 2;
    data.push([open.toFixed(2), close.toFixed(2), low.toFixed(2), high.toFixed(2)]);
    basePrice = close;
  }
  return { dates, data };
};

const initChart = (period = '日K') => {
  if (!klineChartRef.value) return;
  if (!myChart) myChart = echarts.init(klineChartRef.value);
  
  const { dates, data } = generateKData(period);
  
  const option = {
    backgroundColor: '#1d212b',
    tooltip: { trigger: 'axis', axisPointer: { type: 'cross' } },
    grid: { left: '5%', right: '5%', bottom: '10%', top: '10%' },
    xAxis: { data: dates, axisLine: { lineStyle: { color: '#606266' } } },
    yAxis: { scale: true, splitLine: { lineStyle: { color: '#2c3038' } }, axisLine: { show: false } },
    series: [{
      type: 'candlestick',
      data: data,
      itemStyle: { color: '#f56c6c', color0: '#67c23a', borderColor: '#f56c6c', borderColor0: '#67c23a' }
    }]
  };
  myChart.setOption(option);
};

const switchPeriod = (p: string) => {
  currentPeriod.value = p;
  initChart(p);
};

onMounted(() => {
  loadProfile();
  checkWatchStatus();
  initChart();
  window.addEventListener('resize', () => myChart?.resize());
  timer = setInterval(() => { currentTime.value = dayjs().format('HH:mm:ss'); }, 1000);
});

onUnmounted(() => {
  window.removeEventListener('resize', () => myChart?.resize());
  if (timer) clearInterval(timer);
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

/* 公告列表 */
.notice-list { padding: 5px 0; }
.notice-item { display: flex; justify-content: space-between; align-items: center; padding: 12px 0; border-bottom: 1px solid #2c3038; cursor: pointer; }
.notice-item:hover { color: #409eff; }
.notice-item .date { color: #909399; margin-right: 15px; font-family: monospace; width: 100px; }
.notice-item .title { flex: 1; }
</style>
