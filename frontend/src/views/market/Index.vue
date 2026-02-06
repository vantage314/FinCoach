<template>
  <div class="market-dashboard">
    <div class="header-indices">
      <div class="page-title">
        <h2>市场行情</h2>
        <span class="refresh-time">更新于 {{ currentTime }}</span>
      </div>
      
      <div class="indices-grid">
        <div class="index-card up">
          <div class="card-header">
            <span class="icon">📈</span>
            <span class="name">上证指数</span>
          </div>
          <div class="card-value">3,089.26</div>
          <div class="card-change">+0.85%</div>
        </div>

        <div class="index-card up">
          <div class="card-header">
            <span class="icon">🇺🇸</span>
            <span class="name">US 纳斯达克</span>
          </div>
          <div class="card-value">16,892.35</div>
          <div class="card-change">+1.23%</div>
        </div>

        <div class="index-card down">
          <div class="card-header">
            <span class="icon">🥇</span>
            <span class="name">黄金现货</span>
          </div>
          <div class="card-value">2,035.80</div>
          <div class="card-change">-0.32%</div>
        </div>

        <div class="index-card down">
          <div class="card-header">
            <span class="icon">🇭🇰</span>
            <span class="name">HK 恒生指数</span>
          </div>
          <div class="card-value">16,589.45</div>
          <div class="card-change">-0.58%</div>
        </div>
      </div>
    </div>

    <div class="news-ticker">
      <div class="ticker-content">
        <span class="volume-icon">📢</span>
        <span class="ticker-text">08:00 美联储会议纪要释放鸽派信号，美股期指上涨；宁德时代发布新一代神行电池...</span>
      </div>
      <a class="source-link">华尔街见闻</a>
    </div>

    <div class="market-table-section">
      <div class="filter-bar">
        <div class="tabs">
          <span 
            v-for="tab in tabs" 
            :key="tab.key"
            :class="['tab-item', { active: activeTab === tab.key }]"
            @click="handleTabChange(tab.key)"
          >
            {{ tab.name }}
          </span>
          <div class="active-line" :style="activeLineStyle"></div>
        </div>
        
        <div class="search-box">
          <el-input
            v-model="searchKeyword"
            placeholder="输入代码 / 名称"
            :prefix-icon="Search"
            @keyup.enter="handleSearch"
            class="dark-input"
          />
        </div>
      </div>

      <el-table 
        v-loading="marketStore.loading"
        :data="marketStore.securities" 
        style="width: 100%"
        class="dark-table"
        :header-cell-style="{ background: '#1d212b', color: '#909399', borderBottom: '1px solid #363636' }"
        :row-style="{ background: 'transparent', color: '#fff' }"
      >
        <el-table-column label="名称/代码" min-width="180">
          <template #default="{ row }">
            <div class="name-cell">
              <span class="stock-name">{{ row.name }}</span>
              <span class="stock-code">{{ row.code }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="currentPrice" label="最新价" align="right">
          <template #default="{ row }">
            <span class="price">¥{{ row.currentPrice }}</span>
          </template>
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
            <el-tag size="small" :type="getRiskTagType(row.riskLevel)" effect="dark">
              {{ row.riskLevel || 'R3' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="sector" label="板块" align="center">
           <template #default="{ row }">
             <span class="sector-tag">{{ row.sector || '综合' }}</span>
           </template>
        </el-table-column>

        <el-table-column label="操作" width="100" align="center">
           <template #default>
             <el-button link type="primary" class="op-btn">加自选</el-button>
           </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="marketStore.total"
          layout="prev, pager, next"
          class="dark-pagination"
          @current-change="loadData"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onActivated, computed } from 'vue';
import { useMarketStore } from '@/store/modules/market';
import { Search } from '@element-plus/icons-vue';
import dayjs from 'dayjs';

const marketStore = useMarketStore();
const searchKeyword = ref('');
const activeTab = ref('all');
const currentPage = ref(1);
const pageSize = ref(10);
const currentTime = ref(dayjs().format('HH:mm'));

const tabs = [
  { name: '全部', key: 'all' },
  { name: '股票', key: 'STOCK' },
  { name: '基金', key: 'FUND' },
  { name: '债券', key: 'BOND' }
];

// 计算 Tabs 下划线位置 (简化版)
const activeLineStyle = computed(() => {
  const index = tabs.findIndex(t => t.key === activeTab.value);
  return { left: `${index * 60}px` }; // 假设每个 tab 宽 60px
});

const loadData = () => {
  marketStore.fetchSecurities({
    page: currentPage.value,
    size: pageSize.value,
    type: activeTab.value === 'all' ? undefined : activeTab.value,
    keyword: searchKeyword.value
  });
};

const handleTabChange = (key: string) => {
  activeTab.value = key;
  currentPage.value = 1;
  loadData();
};

const handleSearch = () => {
  currentPage.value = 1;
  loadData();
};

const getChangeClass = (val: number) => {
  if (val > 0) return 'text-red';
  if (val < 0) return 'text-green';
  return 'text-gray';
};

const getRiskTagType = (level: string) => {
  if (level === 'R5' || level === 'R4') return 'danger';
  if (level === 'R3') return 'warning';
  return 'success';
};

onMounted(() => loadData());
onActivated(() => loadData());
</script>

<style scoped>
/* 全局暗色背景适配 */
.market-dashboard {
  background-color: #14161a; /* 深色背景 */
  min-height: 100%;
  padding: 20px;
  color: #fff;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
}

/* 1. 顶部指数卡片 */
.header-indices { margin-bottom: 24px; }
.page-title { display: flex; justify-content: space-between; align-items: flex-end; margin-bottom: 16px; }
.page-title h2 { margin: 0; font-size: 20px; font-weight: 600; }
.refresh-time { font-size: 12px; color: #606266; }

.indices-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; }
.index-card {
  background: #1d212b;
  border-radius: 8px;
  padding: 16px;
  border: 1px solid #2c3038;
}
.index-card .card-header { display: flex; align-items: center; gap: 8px; margin-bottom: 12px; color: #909399; font-size: 13px; }
.index-card .card-value { font-size: 24px; font-weight: bold; margin-bottom: 4px; }
.index-card .card-change { font-size: 14px; font-weight: 500; }
.index-card.up .card-change, .index-card.up .card-value { color: #f56c6c; }
.index-card.down .card-change, .index-card.down .card-value { color: #67c23a; }

/* 2. 新闻跑马灯 */
.news-ticker {
  background: rgba(43, 48, 60, 0.5);
  border-radius: 4px;
  padding: 10px 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  border-left: 4px solid #f56c6c;
}
.ticker-content { display: flex; align-items: center; gap: 8px; font-size: 13px; color: #dcdfe6; }
.source-link { font-size: 12px; color: #909399; cursor: pointer; }

/* 3. 过滤器与表格 */
.market-table-section { background: #1d212b; padding: 20px; border-radius: 8px; }
.filter-bar { display: flex; justify-content: space-between; margin-bottom: 20px; border-bottom: 1px solid #363636; padding-bottom: 10px; }
.tabs { display: flex; gap: 30px; position: relative; }
.tab-item { cursor: pointer; color: #909399; padding-bottom: 10px; transition: color 0.3s; }
.tab-item.active { color: #409eff; font-weight: 600; }
.active-line { position: absolute; bottom: -11px; height: 2px; width: 30px; background: #409eff; transition: left 0.3s; }

/* 暗黑输入框 */
:deep(.dark-input .el-input__wrapper) { background-color: #2b303c; box-shadow: none; border: 1px solid #4c4d4f; }
:deep(.dark-input .el-input__inner) { color: #fff; }

/* 暗黑表格强制覆盖 */
.dark-table { --el-table-border-color: #363636; --el-table-bg-color: #1d212b; --el-table-tr-bg-color: #1d212b; --el-table-header-bg-color: #1d212b; }
:deep(.el-table__inner-wrapper::before) { display: none; } /* 去掉底部白线 */
:deep(.el-table td.el-table__cell), :deep(.el-table th.el-table__cell.is-leaf) { border-bottom: 1px solid #363636; }
:deep(.el-table--enable-row-hover .el-table__body tr:hover > td.el-table__cell) { background-color: #2b303c !important; }

.name-cell { display: flex; flex-direction: column; }
.stock-name { font-size: 14px; font-weight: bold; color: #fff; }
.stock-code { font-size: 12px; color: #909399; }
.text-red { color: #f56c6c; }
.text-green { color: #67c23a; }
.sector-tag { background: #2b303c; padding: 2px 8px; border-radius: 4px; font-size: 12px; color: #b1b3b8; }
.op-btn { color: #409eff; }

/* 暗黑分页 */
.pagination-wrapper { display: flex; justify-content: flex-end; margin-top: 20px; }
:deep(.dark-pagination button) { background: transparent !important; color: #fff; }
:deep(.dark-pagination .el-pager li) { background: transparent !important; color: #909399; }
:deep(.dark-pagination .el-pager li.is-active) { color: #409eff; font-weight: bold; }
</style>
