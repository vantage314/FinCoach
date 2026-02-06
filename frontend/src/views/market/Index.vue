<template>
  <div class="market-dashboard">
    <div class="header-indices">
      <div class="page-title">
        <h2>市场行情</h2>
        <span class="refresh-time">更新于 {{ currentTime }}</span>
      </div>
      
      <div class="indices-grid">
        <div class="index-card up">
          <div class="card-header"><span class="icon">📈</span><span class="name">上证指数</span></div>
          <div class="card-value">3,089.26</div>
          <div class="card-change">+0.85%</div>
        </div>
        <div class="index-card up">
          <div class="card-header"><span class="icon">🇺🇸</span><span class="name">US 纳斯达克</span></div>
          <div class="card-value">16,892.35</div>
          <div class="card-change">+1.23%</div>
        </div>
        <div class="index-card down">
          <div class="card-header"><span class="icon">🥇</span><span class="name">黄金现货</span></div>
          <div class="card-value">2,035.80</div>
          <div class="card-change">-0.32%</div>
        </div>
        <div class="index-card down">
          <div class="card-header"><span class="icon">🇭🇰</span><span class="name">HK 恒生指数</span></div>
          <div class="card-value">16,589.45</div>
          <div class="card-change">-0.58%</div>
        </div>
      </div>
    </div>

    <div class="news-ticker">
      <div class="ticker-wrapper">
        <span class="volume-icon">📢</span>
        <div class="news-list" :style="{ transform: `translateY(-${currentNewsIndex * 30}px)` }">
          <div 
            v-for="(news, index) in newsList" 
            :key="index" 
            class="news-item"
            @click="viewNews(news)"
          >
            <span class="news-time">{{ news.time }}</span>
            <span class="news-title">{{ news.title }}</span>
          </div>
        </div>
      </div>
      <a class="source-link">实时快讯</a>
    </div>

    <div class="market-table-section">
      <div class="filter-bar">
        <div class="tabs">
          <span v-for="tab in tabs" :key="tab.key" 
            :class="['tab-item', { active: activeTab === tab.key }]" 
            @click="handleTabChange(tab.key)">
            {{ tab.name }}
          </span>
        </div>
        <div class="search-box">
          <el-input v-model="searchKeyword" placeholder="输入代码 / 名称" :prefix-icon="Search" @keyup.enter="handleSearch" class="dark-input"/>
        </div>
      </div>

      <el-table v-loading="marketStore.loading" :data="marketStore.securities" style="width: 100%" class="dark-table" 
        :header-cell-style="{ background: '#1d212b', color: '#909399', borderBottom: '1px solid #363636' }"
        :row-style="{ background: 'transparent', color: '#fff' }">
        
        <el-table-column label="名称/代码" min-width="180">
          <template #default="{ row }">
            <div class="name-cell" @click="goToDetail(row)" style="cursor: pointer">
              <span class="stock-name hover-link">{{ row.name }}</span>
              <span class="stock-code">{{ row.code }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="currentPrice" label="最新价" align="right">
          <template #default="{ row }"><span class="price">¥{{ row.currentPrice }}</span></template>
        </el-table-column>

        <el-table-column prop="changePercent" label="涨跌幅" align="right">
          <template #default="{ row }">
             <span :class="getChangeClass(row.changePercent)">
               {{ row.changePercent > 0 ? '+' : '' }}{{ row.changePercent }}%
             </span>
          </template>
        </el-table-column>

        <el-table-column label="风险等级" align="center">
          <template #default="{ row }">
            <el-tooltip :content="getRiskDesc(row.riskLevel)" placement="top" effect="light">
              <el-tag size="small" :type="getRiskTagType(row.riskLevel)" effect="dark" style="cursor: help">
                {{ row.riskLevel || 'R3' }}
              </el-tag>
            </el-tooltip>
          </template>
        </el-table-column>

        <el-table-column prop="sector" label="板块" align="center">
           <template #default="{ row }"><span class="sector-tag">{{ row.sector || '综合' }}</span></template>
        </el-table-column>

        <el-table-column label="操作" width="100" align="center">
           <template #default="{ row }">
             <el-button 
                link 
                size="small" 
                :type="watchlist.has(row.code) ? 'info' : 'primary'"
                @click.stop="toggleWatchlist(row)"
             >
               {{ watchlist.has(row.code) ? '已添加' : '加自选' }}
             </el-button>
           </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination v-model:current-page="currentPage" v-model:page-size="pageSize" :total="marketStore.total" layout="prev, pager, next" class="dark-pagination" @current-change="loadData"/>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onActivated, onUnmounted } from 'vue';
import { useMarketStore } from '@/store/modules/market';
import { Search } from '@element-plus/icons-vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import dayjs from 'dayjs';

const router = useRouter();
const marketStore = useMarketStore();
const searchKeyword = ref('');
const activeTab = ref('all');
const currentPage = ref(1);
const pageSize = ref(10);
const currentTime = ref(dayjs().format('HH:mm'));

// --- 新闻相关 ---
const currentNewsIndex = ref(0);
const newsList = [
  { time: '08:00', title: '美联储会议纪要释放鸽派信号，美股期指全线上涨', content: '美联储最新会议纪要显示，多数官员认为通胀已得到控制，降息预期升温。' },
  { time: '09:30', title: '宁德时代发布新一代神行电池，充电10分钟续航400公里', content: '宁德时代今日发布神行超充电池，开启新能源车超充时代。' },
  { time: '10:15', title: '北向资金净流入超50亿元，贵州茅台获大幅加仓', content: '今日早盘北向资金持续流入，白酒、新能源板块领涨。' },
  { time: '11:00', title: '腾讯控股回购金额创历史新高，彰显管理层信心', content: '腾讯控股连续20日进行股票回购，累计金额已超百亿港元。' },
  { time: '13:45', title: '国家统计局：前三季度GDP同比增长5.2%，经济持续回升向好', content: '统计数据显示，工业生产平稳增长，消费市场加快恢复。' }
];
// 新闻滚动定时器
let newsTimer: any = null;
const startNewsTicker = () => {
  newsTimer = setInterval(() => {
    currentNewsIndex.value = (currentNewsIndex.value + 1) % newsList.length;
  }, 3000);
};
const viewNews = (news: any) => {
  ElMessageBox.alert(news.content, news.title, { confirmButtonText: '关闭' });
};

// --- 风险提示 ---
const getRiskDesc = (level: string) => {
  const map: any = {
    'R1': 'R1 (谨慎型): 极低风险，适合保守投资者，如国债、货币基金。',
    'R2': 'R2 (稳健型): 较低风险，收益波动小，如债券基金。',
    'R3': 'R3 (平衡型): 中等风险，收益与风险并存，如混合基金。',
    'R4': 'R4 (进取型): 较高风险，追求高收益，如股票、股票基金。',
    'R5': 'R5 (激进型): 极高风险，本金亏损概率大，如期货、期权。'
  };
  return map[level] || '风险等级';
};

// --- 自选逻辑 (模拟) ---
const watchlist = reactive(new Set<string>());
const toggleWatchlist = (row: any) => {
  if (watchlist.has(row.code)) {
    watchlist.delete(row.code);
    ElMessage.info(`已将 ${row.name} 移出自选`);
  } else {
    watchlist.add(row.code);
    ElMessage.success({
      message: `成功将 ${row.name} 加入自选监控`,
      type: 'success',
      duration: 2000
    });
  }
};

// --- 路由跳转 ---
const goToDetail = (row: any) => {
  router.push(`/market/detail/${row.code}?name=${encodeURIComponent(row.name)}`);
};

// --- 基础逻辑 ---
const tabs = [
  { name: '全部', key: 'all' },
  { name: '股票', key: 'STOCK' },
  { name: '基金', key: 'FUND' },
  { name: '债券', key: 'BOND' }
];

const loadData = () => {
  marketStore.fetchSecurities({
    page: currentPage.value, size: pageSize.value, type: activeTab.value === 'all' ? undefined : activeTab.value, keyword: searchKeyword.value
  });
};
const handleTabChange = (key: string) => { activeTab.value = key; currentPage.value = 1; loadData(); };
const handleSearch = () => { currentPage.value = 1; loadData(); };
const getChangeClass = (val: number) => val > 0 ? 'text-red' : (val < 0 ? 'text-green' : 'text-gray');
const getRiskTagType = (level: string) => (level === 'R5' || level === 'R4') ? 'danger' : (level === 'R3' ? 'warning' : 'success');

onMounted(() => { loadData(); startNewsTicker(); });
onActivated(() => { loadData(); });
onUnmounted(() => { if(newsTimer) clearInterval(newsTimer); });
</script>

<style scoped>
.market-dashboard { background-color: #14161a; min-height: 100%; padding: 20px; color: #fff; }
.header-indices { margin-bottom: 24px; }
.page-title { display: flex; justify-content: space-between; align-items: flex-end; margin-bottom: 16px; }
.page-title h2 { margin: 0; font-size: 20px; font-weight: 600; }
.refresh-time { font-size: 12px; color: #606266; }
.indices-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; }
.index-card { background: #1d212b; border-radius: 8px; padding: 16px; border: 1px solid #2c3038; }
.index-card .card-header { display: flex; align-items: center; gap: 8px; margin-bottom: 12px; color: #909399; font-size: 13px; }
.index-card .card-value { font-size: 24px; font-weight: bold; margin-bottom: 4px; }
.index-card .card-change { font-size: 14px; font-weight: 500; }
.index-card.up .card-change, .index-card.up .card-value { color: #f56c6c; }
.index-card.down .card-change, .index-card.down .card-value { color: #67c23a; }

/* 新闻跑马灯 */
.news-ticker { background: rgba(43, 48, 60, 0.5); border-radius: 4px; padding: 0 16px; display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; border-left: 4px solid #f56c6c; height: 40px; overflow: hidden; }
.ticker-wrapper { flex: 1; height: 100%; overflow: hidden; position: relative; display: flex; align-items: center; }
.volume-icon { margin-right: 12px; }
.news-list { transition: transform 0.5s ease; position: absolute; top: 5px; left: 30px; width: 100%; }
.news-item { height: 30px; display: flex; align-items: center; cursor: pointer; }
.news-item:hover .news-title { text-decoration: underline; color: #409eff; }
.news-time { color: #909399; margin-right: 12px; font-size: 12px; }
.news-title { color: #dcdfe6; font-size: 13px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; max-width: 80%; }
.source-link { font-size: 12px; color: #909399; cursor: pointer; white-space: nowrap; margin-left: 10px;}

.market-table-section { background: #1d212b; padding: 20px; border-radius: 8px; }
.filter-bar { display: flex; justify-content: space-between; margin-bottom: 20px; border-bottom: 1px solid #363636; padding-bottom: 10px; }
.tabs { display: flex; gap: 30px; }
.tab-item { cursor: pointer; color: #909399; padding-bottom: 10px; transition: color 0.3s; }
.tab-item.active { color: #409eff; font-weight: 600; border-bottom: 2px solid #409eff; }
:deep(.dark-input .el-input__wrapper) { background-color: #2b303c; box-shadow: none; border: 1px solid #4c4d4f; }
:deep(.dark-input .el-input__inner) { color: #fff; }

.dark-table { --el-table-border-color: #363636; --el-table-bg-color: #1d212b; --el-table-tr-bg-color: #1d212b; --el-table-header-bg-color: #1d212b; }
:deep(.el-table__inner-wrapper::before) { display: none; }
:deep(.el-table td.el-table__cell), :deep(.el-table th.el-table__cell.is-leaf) { border-bottom: 1px solid #363636; }
:deep(.el-table--enable-row-hover .el-table__body tr:hover > td.el-table__cell) { background-color: #2b303c !important; }
.name-cell { display: flex; flex-direction: column; }
.stock-name { font-size: 14px; font-weight: bold; color: #fff; }
.stock-name.hover-link:hover { color: #409eff; text-decoration: underline; }
.stock-code { font-size: 12px; color: #909399; }
.text-red { color: #f56c6c; } .text-green { color: #67c23a; } .text-gray { color: #909399; }
.sector-tag { background: #2b303c; padding: 2px 8px; border-radius: 4px; font-size: 12px; color: #b1b3b8; }
.pagination-wrapper { display: flex; justify-content: flex-end; margin-top: 20px; }
:deep(.dark-pagination button), :deep(.dark-pagination .el-pager li) { background: transparent !important; color: #909399; }
:deep(.dark-pagination .el-pager li.is-active) { color: #409eff; font-weight: bold; }
</style>
