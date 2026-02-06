<template>
  <div class="market-page">
    <!-- 核心指数区 -->
    <section class="indices-section">
      <div class="section-header">
        <h2 class="section-title">市场行情</h2>
        <span class="update-time">更新于 {{ currentTime }}</span>
      </div>
      <div class="indices-grid">
        <div 
          v-for="index in marketStore.indices" 
          :key="index.code"
          class="index-card"
          :class="{ 'is-up': index.changePercent > 0, 'is-down': index.changePercent < 0 }"
        >
          <div class="index-header">
            <span class="index-icon">{{ index.icon }}</span>
            <span class="index-name">{{ index.name }}</span>
          </div>
          <div class="index-value">{{ formatPrice(index.value) }}</div>
          <div class="index-change" :class="getChangeClass(index.changePercent)">
            {{ formatChange(index.changePercent) }}
          </div>
        </div>
      </div>
    </section>

    <!-- 资讯滚动条 -->
    <section class="news-section">
      <div class="news-ticker">
        <span class="news-icon">📢</span>
        <el-carousel 
          direction="vertical" 
          :autoplay="true" 
          :interval="4000"
          indicator-position="none"
          height="32px"
          class="news-carousel"
        >
          <el-carousel-item v-for="item in marketStore.news" :key="item.id">
            <div class="news-item" @click="openNews(item)">
              <span class="news-time">{{ item.publishTime }}</span>
              <span class="news-title">{{ item.title }}</span>
              <span class="news-source">{{ item.source }}</span>
            </div>
          </el-carousel-item>
        </el-carousel>
      </div>
    </section>

    <!-- 行情列表 -->
    <section class="quotation-section">
      <div class="quotation-header">
        <el-tabs v-model="activeTab" class="market-tabs" @tab-change="handleTabChange">
          <el-tab-pane label="全部" name="all" />
          <el-tab-pane label="股票" name="stock" />
          <el-tab-pane label="基金" name="fund" />
          <el-tab-pane label="债券" name="bond" />
        </el-tabs>

        <div class="search-bar">
          <el-input 
            v-model="searchKeyword" 
            placeholder="输入代码 / 名称" 
            clearable 
            class="market-search"
            @clear="handleSearch" 
            @keyup.enter="handleSearch"
          >
            <template #append>
              <el-button :icon="Search" @click="handleSearch" />
            </template>
          </el-input>
        </div>
      </div>

      <div class="table-wrapper">
        <el-table 
          :data="marketStore.securities" 
          v-loading="marketStore.loading"
          class="market-table"
          :header-cell-style="{ background: 'rgba(0,0,0,0.3)', color: '#94a3b8' }"
          :row-style="{ background: 'transparent' }"
        >
          <el-table-column label="名称/代码" min-width="180">
            <template #default="{ row }">
              <div class="security-info cursor-pointer" @click="goToDetail(row.id)">
                <span class="security-name link-text">{{ row.name }}</span>
                <span class="security-code">{{ row.code }}</span>
              </div>
            </template>
          </el-table-column>
          
          <el-table-column label="最新价" width="140" align="right">
            <template #default="{ row }">
              <span class="price" :class="getChangeClass(row.changePercent)">
                {{ formatPrice(row.currentPrice) }}
              </span>
            </template>
          </el-table-column>
          
          <el-table-column label="涨跌幅" width="120" align="right">
            <template #default="{ row }">
              <span class="change-badge" :class="getChangeClass(row.changePercent)">
                {{ formatChange(row.changePercent) }}
              </span>
            </template>
          </el-table-column>
          
          <el-table-column label="风险等级" width="100" align="center">
            <template #default="{ row }">
              <el-tooltip placement="top" :content="getRiskDesc(row.riskLevel)">
                <el-tag :type="getRiskTagType(row.riskLevel)" size="small" style="cursor: help">
                  {{ row.riskLevel }}
                </el-tag>
              </el-tooltip>
            </template>
          </el-table-column>
          
          <el-table-column label="板块" width="120">
            <template #default="{ row }">
              <span class="sector-tag">{{ row.sector }}</span>
            </template>
          </el-table-column>
          
          <el-table-column label="操作" width="100" align="center">
            <template #default="{ row }">
              <el-button 
                type="primary" 
                link 
                @click="goToDetail(row.id)"
              >
                详情
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 分页器 -->
      <div class="pagination-wrapper">
        <el-pagination
          background
          layout="prev, pager, next"
          :total="marketStore.total"
          :page-size="marketStore.pageSize"
          :current-page="marketStore.currentPage"
          @current-change="handlePageChange"
        />
      </div>
    </section>

    <!-- 新闻详情弹窗 -->
    <el-dialog
      v-model="newsDialogVisible"
      :title="currentNews.title"
      width="600px"
      append-to-body
      class="news-dialog"
    >
      <div class="news-content">
        {{ currentNews.content || currentNews.title }}
      </div>
      <template #footer>
        <div class="news-footer">
          <span class="news-source">来源：{{ currentNews.source }}</span>
          <span class="news-time">发布时间：{{ currentNews.publishTime }}</span>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue';
import { useRouter } from 'vue-router';
import { Search } from '@element-plus/icons-vue';
import { useMarketStore } from '@/store/modules/market';
import type { MarketNews } from '@/api/market';

const router = useRouter();
const marketStore = useMarketStore();
const activeTab = ref('all');
const searchKeyword = ref('');
const newsDialogVisible = ref(false);
const currentNews = ref<MarketNews>({} as MarketNews);

// 当前时间
const currentTime = computed(() => {
  const now = new Date();
  return `${now.getHours().toString().padStart(2, '0')}:${now.getMinutes().toString().padStart(2, '0')}`;
});

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

// 获取风险等级描述
const getRiskDesc = (level: string) => {
  const map: Record<string, string> = {
    'R1': 'R1 (保守型) - 本金安全，收益稳定，风险极低',
    'R2': 'R2 (稳健型) - 风险较低，收益相对稳定',
    'R3': 'R3 (平衡型) - 风险适中，追求稳健增值',
    'R4': 'R4 (进取型) - 风险较高，追求高收益',
    'R5': 'R5 (激进型) - 高风险高收益，适合有经验的投资者'
  };
  return map[level] || '未知风险等级';
};

// 获取风险等级颜色
const getRiskTagType = (level: string): '' | 'success' | 'warning' | 'danger' | 'info' => {
  const types: Record<string, '' | 'success' | 'warning' | 'danger' | 'info'> = {
    'R1': 'success',
    'R2': 'success',
    'R3': 'warning',
    'R4': 'danger',
    'R5': 'danger'
  };
  return types[level] || 'info';
};

// 分页处理
const handlePageChange = (page: number) => {
  marketStore.fetchSecurities({
    type: activeTab.value,
    keyword: searchKeyword.value,
    page: page
  });
};

// 切换 Tab
const handleTabChange = () => {
  handleSearch();
};

// 搜索处理
const handleSearch = () => {
  marketStore.fetchSecurities({
    type: activeTab.value,
    keyword: searchKeyword.value
  });
};

// 跳转详情页
const goToDetail = (id: number) => {
  router.push(`/market/security/${id}`);
};

// 打开新闻详情
const openNews = (news: MarketNews) => {
  currentNews.value = news;
  newsDialogVisible.value = true;
};

// 初始化加载
onMounted(async () => {
  await Promise.all([
    marketStore.fetchIndices(),
    marketStore.fetchNews(),
    marketStore.fetchSecurities()
  ]);
});
</script>

<style lang="scss" scoped>
@use "@/theme/variables.scss" as *;

.market-page {
  min-height: calc(100vh - 112px);
  padding: 24px;
  background: linear-gradient(135deg, #0f172a, #1e293b);
}

/* 指数区 */
.indices-section {
  margin-bottom: 24px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.section-title {
  font-size: 24px;
  font-weight: 700;
  color: white;
  background: linear-gradient(to right, #fff, #94a3b8);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.update-time {
  font-size: 12px;
  color: #64748b;
}

.indices-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.index-card {
  background: rgba(255, 255, 255, 0.05);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 16px;
  padding: 20px;
  transition: all 0.3s ease;
  
  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.2);
  }
  
  &.is-up {
    border-color: rgba(245, 108, 108, 0.3);
    background: linear-gradient(135deg, rgba(245, 108, 108, 0.08), rgba(255, 255, 255, 0.03));
  }
  
  &.is-down {
    border-color: rgba(103, 194, 58, 0.3);
    background: linear-gradient(135deg, rgba(103, 194, 58, 0.08), rgba(255, 255, 255, 0.03));
  }
}

.index-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.index-icon {
  font-size: 20px;
}

.index-name {
  font-size: 14px;
  color: #94a3b8;
}

.index-value {
  font-size: 28px;
  font-weight: 700;
  color: white;
  font-variant-numeric: tabular-nums;
  margin-bottom: 4px;
}

.index-change {
  font-size: 16px;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}

/* 资讯滚动条 */
.news-section {
  margin-bottom: 24px;
}

.news-ticker {
  display: flex;
  align-items: center;
  gap: 12px;
  background: rgba(6, 182, 212, 0.1);
  border: 1px solid rgba(6, 182, 212, 0.2);
  border-radius: 12px;
  padding: 8px 16px;
}

.news-icon {
  font-size: 20px;
  flex-shrink: 0;
}

.news-carousel {
  flex: 1;
  
  :deep(.el-carousel__container) {
    height: 32px !important;
  }
}

.news-item {
  display: flex;
  align-items: center;
  gap: 12px;
  height: 32px;
  line-height: 32px;
  cursor: pointer;
  transition: opacity 0.2s;
  
  &:hover {
    opacity: 0.8;
  }
}

.news-time {
  color: #06b6d4;
  font-size: 12px;
  font-weight: 600;
}

.news-title {
  color: white;
  font-size: 14px;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.news-source {
  color: #64748b;
  font-size: 12px;
}

/* 行情列表 */
.quotation-section {
  background: rgba(255, 255, 255, 0.03);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 16px;
  padding: 20px;
}

.quotation-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.market-tabs {
  :deep(.el-tabs__header) {
    margin-bottom: 0;
    border-bottom: none;
  }
  
  :deep(.el-tabs__item) {
    color: #64748b;
    font-weight: 500;
    padding-bottom: 15px;
    
    &.is-active {
      color: #06b6d4;
    }
    
    &:hover {
      color: #06b6d4;
    }
  }
  
  :deep(.el-tabs__active-bar) {
    background-color: #06b6d4;
  }
}

.search-bar {
  width: 280px;
  padding-bottom: 10px;
}

.market-search {
  :deep(.el-input__wrapper) {
    background-color: rgba(255, 255, 255, 0.05) !important;
    box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.1) inset !important;
    border-radius: 12px 0 0 12px;
  }
  
  :deep(.el-input__inner) {
    color: #ffffff !important;
  }
  
  :deep(.el-input-group__append) {
    background-color: rgba(255, 255, 255, 0.1);
    box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.1) inset;
    color: #64748b;
    border-radius: 0 12px 12px 0;
  }
}

.table-wrapper {
  border-radius: 12px;
  overflow: hidden;
  margin-bottom: 16px;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  padding: 0 16px;
}

.market-table {
  background: transparent !important;
  
  :deep(.el-table__body-wrapper) {
    background: transparent;
  }
  
  :deep(.el-table__row) {
    background: transparent !important;
    
    &:hover > td {
      background: rgba(255, 255, 255, 0.05) !important;
    }
  }
  
  :deep(td) {
    border-bottom: 1px solid rgba(255, 255, 255, 0.05) !important;
    color: white;
  }
  
  :deep(th) {
    border-bottom: 1px solid rgba(255, 255, 255, 0.1) !important;
  }
}

.security-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  
  &.cursor-pointer {
    cursor: pointer;
    
    &:hover .link-text {
      color: #06b6d4;
      text-decoration: underline;
    }
  }
}

.security-name {
  font-weight: 600;
  color: white;
  transition: color 0.2s ease;
}

.security-code {
  font-size: 12px;
  color: #64748b;
}

.price {
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}

.change-badge {
  display: inline-block;
  padding: 4px 8px;
  border-radius: 6px;
  font-weight: 600;
  font-size: 13px;
  font-variant-numeric: tabular-nums;
  
  &.text-up {
    background: rgba(245, 108, 108, 0.15);
  }
  
  &.text-down {
    background: rgba(103, 194, 58, 0.15);
  }
}

.sector-tag {
  color: #94a3b8;
  font-size: 13px;
}

/* 新闻内容弹窗 */
.news-content {
  line-height: 1.8;
  color: #334155;
  font-size: 15px;
  padding: 10px 0;
}

.news-footer {
  display: flex;
  justify-content: space-between;
  color: #94a3b8;
  font-size: 12px;
  border-top: 1px solid #e2e8f0;
  padding-top: 12px;
}

/* 金融色定义 */
.text-up {
  color: #F56C6C;
}

.text-down {
  color: #67C23A;
}
</style>
