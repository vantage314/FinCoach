<template>
  <div class="invest-dashboard">
    <div class="sidebar-watchlist">
      <div class="panel-header">
        <h3>🎯 我的自选</h3>
        <span class="count">{{ watchlistData.length }} 支标的</span>
      </div>
      
      <div v-if="watchlistData.length === 0" class="empty-state">
        <el-empty description="暂无自选，去市场看看" :image-size="80" />
      </div>

      <div class="watchlist-content">
        <div 
          v-for="item in watchlistData" 
          :key="item.code" 
          class="stock-item"
          @click="goToDetail(item)"
        >
          <div class="stock-info">
            <span class="name">{{ item.name }}</span>
            <span class="code">{{ item.code }}</span>
          </div>
          <div class="stock-price">
            <span class="price">{{ item.currentPrice }}</span>
            <span :class="getChangeClass(item.changePercent)">
              {{ item.changePercent }}%
            </span>
          </div>
          <div class="actions">
             <el-button link type="danger" size="small" @click.stop="handleRemove(item)">
               <el-icon><Delete /></el-icon>
             </el-button>
          </div>
        </div>
      </div>
    </div>

    <div class="main-opportunity">
      <div class="market-radar">
        <div class="section-title">🔥 市场风向标</div>
        <div class="radar-grid">
          <div class="sector-card up">
            <span class="sec-name">新能源</span>
            <span class="sec-val">+3.2%</span>
            <span class="desc">宁德时代领涨</span>
          </div>
          <div class="sector-card up">
            <span class="sec-name">半导体</span>
            <span class="sec-val">+2.1%</span>
            <span class="desc">国产替代加速</span>
          </div>
          <div class="sector-card down">
            <span class="sec-name">房地产</span>
            <span class="sec-val">-1.5%</span>
            <span class="desc">销售数据疲软</span>
          </div>
          <div class="sector-card up">
            <span class="sec-name">人工智能</span>
            <span class="sec-val">+4.5%</span>
            <span class="desc">Sora 模型爆发</span>
          </div>
        </div>
      </div>

      <!-- 新闻快讯区 -->
      <div class="news-section">
        <div class="section-title">📰 财经快讯</div>
        <div class="news-list-wrapper">
          <div 
            v-for="news in newsList" 
            :key="news.id" 
            class="news-row"
            @click="viewNews(news)"
          >
            <span class="news-time">{{ news.publishTime }}</span>
            <span class="news-title">{{ news.title }}</span>
          </div>
          <el-empty v-if="newsList.length === 0" description="暂无快讯" :image-size="60" />
        </div>
      </div>

      <div class="smart-pool">
        <div class="section-title">🚀 今日潜力机会 (系统推荐)</div>
        <el-table 
          :data="opportunityList" 
          style="width: 100%" 
          class="dark-table"
          :header-cell-style="{ background: '#1d212b', color: '#909399', borderBottom: '1px solid #363636' }"
          :row-style="{ background: 'transparent', color: '#fff' }"
        >
          <el-table-column prop="name" label="名称">
            <template #default="{ row }">
              <span class="recommend-name" @click="goToDetail(row)">{{ row.name }}</span>
              <el-tag size="small" effect="dark" type="warning" style="margin-left:5px">热</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="currentPrice" label="现价" />
          <el-table-column prop="changePercent" label="涨幅">
            <template #default="{ row }">
              <span class="text-red">+{{ row.changePercent }}%</span>
            </template>
          </el-table-column>
          <el-table-column prop="peRatio" label="市盈率" />
          <el-table-column label="操作" align="center">
            <template #default="{ row }">
               <el-button 
                 v-if="!isWatched(row.code)"
                 type="primary" 
                 size="small" 
                 plain 
                 @click="handleAdd(row)"
               >
                 + 关注
               </el-button>
               <span v-else class="text-gray">已关注</span>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>

    <!-- 新闻详情抽屉 -->
    <el-drawer
      v-model="showNewsDrawer"
      title="财经快讯"
      direction="rtl"
      size="40%"
      :before-close="handleCloseNews"
    >
      <div class="news-detail-content" v-if="currentNews">
        <h2 class="detail-title">{{ currentNews.title }}</h2>
        <div class="detail-meta">
          <span class="source">{{ currentNews.source || 'FinCoach' }}</span>
          <span class="time">{{ currentNews.publishTime }}</span>
        </div>
        <div class="detail-body">
          <p>{{ currentNews.content }}</p>
        </div>
        
        <div class="related-stock" v-if="currentNews.relatedCode">
          <div class="stock-card" @click="goToDetail({ code: currentNews.relatedCode, name: '关联股票' })">
            <span class="label">相关标的：</span>
            <span class="code">{{ currentNews.relatedCode }}</span>
            <el-icon><ArrowRight /></el-icon>
          </div>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue';
import { useRouter } from 'vue-router';
import { Delete, ArrowRight } from '@element-plus/icons-vue';
import { getWatchlist, toggleWatchlist, getNewsList } from '@/api/invest';
import { getMarketSecurities } from '@/api/market';
import { ElMessage } from 'element-plus';

const router = useRouter();
const watchlistCodes = ref<string[]>([]);
const allSecurities = ref<any[]>([]);

// 新闻相关状态
const newsList = ref<any[]>([]);
const showNewsDrawer = ref(false);
const currentNews = ref<any>(null);

// 计算属性：匹配自选股的详细行情
const watchlistData = computed(() => {
  return allSecurities.value.filter(s => watchlistCodes.value.includes(s.code));
});

// 计算属性：推荐池 (涨幅 > 0 且未关注的)
const opportunityList = computed(() => {
  return allSecurities.value
    .filter(s => s.changePercent > 0)
    .sort((a, b) => b.changePercent - a.changePercent)
    .slice(0, 5);
});

const loadData = async () => {
  try {
    const [watchRes, marketRes] = await Promise.all([
      getWatchlist(),
      getMarketSecurities({ size: 100 })
    ]);

    if (watchRes.code === 200) {
      watchlistCodes.value = watchRes.data || [];
    }
    
    const raw = marketRes.data;
    if (raw?.records) {
      allSecurities.value = raw.records;
    } else if (Array.isArray(raw)) {
      allSecurities.value = raw;
    }
  } catch (error) {
    console.error('加载投资数据失败', error);
  }
};

// 获取新闻列表
const fetchNews = async () => {
  try {
    const res: any = await getNewsList({ limit: 10 });
    if (res.code === 200) {
      newsList.value = res.data || [];
    }
  } catch (error) {
    console.error('获取新闻失败', error);
  }
};

// 打开新闻详情
const viewNews = (news: any) => {
  currentNews.value = news;
  showNewsDrawer.value = true;
};

const handleCloseNews = () => {
  showNewsDrawer.value = false;
};

const goToDetail = (row: any) => {
  router.push(`/market/detail/${row.code}`);
};

const handleRemove = async (row: any) => {
  await toggleWatchlist(row.code);
  watchlistCodes.value = watchlistCodes.value.filter(c => c !== row.code);
  ElMessage.success('已移出');
};

const handleAdd = async (row: any) => {
  await toggleWatchlist(row.code);
  watchlistCodes.value.push(row.code);
  ElMessage.success('已关注');
};

const isWatched = (code: string) => watchlistCodes.value.includes(code);
const getChangeClass = (val: number) => val > 0 ? 'text-red' : (val < 0 ? 'text-green' : 'text-gray');

onMounted(() => {
  loadData();
  fetchNews();
});
</script>

<style scoped>
.invest-dashboard {
  display: flex;
  height: calc(100vh - 80px);
  background: #14161a;
  color: #fff;
  gap: 20px;
  padding: 20px;
}

/* 左侧边栏 */
.sidebar-watchlist {
  width: 300px;
  background: #1d212b;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  border: 1px solid #2c3038;
}
.panel-header {
  padding: 16px;
  border-bottom: 1px solid #363636;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.panel-header h3 { margin: 0; font-size: 16px; }
.count { font-size: 12px; color: #909399; }
.watchlist-content { flex: 1; overflow-y: auto; }
.stock-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #2c3038;
  cursor: pointer;
  transition: background 0.2s;
}
.stock-item:hover { background: #2b303c; }
.stock-info { display: flex; flex-direction: column; }
.stock-info .name { font-size: 14px; font-weight: bold; }
.stock-info .code { font-size: 12px; color: #909399; }
.stock-price { display: flex; flex-direction: column; align-items: flex-end; }
.stock-price .price { font-weight: bold; font-family: monospace; }
.text-red { color: #f56c6c; } .text-green { color: #67c23a; } .text-gray { color: #909399; }

/* 右侧主区域 */
.main-opportunity { flex: 1; display: flex; flex-direction: column; gap: 20px; }
.market-radar { background: #1d212b; padding: 20px; border-radius: 8px; }
.section-title { font-size: 16px; font-weight: bold; margin-bottom: 16px; border-left: 4px solid #409eff; padding-left: 10px; }
.radar-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 15px; }
.sector-card { background: #2b303c; padding: 15px; border-radius: 6px; display: flex; flex-direction: column; gap: 5px; }
.sector-card .sec-name { font-size: 13px; color: #b1b3b8; }
.sector-card .sec-val { font-size: 20px; font-weight: bold; }
.sector-card.up .sec-val { color: #f56c6c; }
.sector-card.down .sec-val { color: #67c23a; }
.sector-card .desc { font-size: 12px; color: #606266; }

/* 新闻区 */
.news-section { background: #1d212b; padding: 20px; border-radius: 8px; }
.news-list-wrapper { max-height: 180px; overflow-y: auto; }
.news-row { 
  display: flex; 
  align-items: center; 
  padding: 10px 0; 
  border-bottom: 1px solid #2c3038; 
  cursor: pointer;
  transition: background 0.2s;
}
.news-row:hover { background: #2b303c; }
.news-time { color: #909399; font-size: 12px; margin-right: 15px; font-family: monospace; }
.news-title { font-size: 14px; color: #dcdfe6; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.news-row:hover .news-title { color: #409eff; }

.smart-pool { background: #1d212b; padding: 20px; border-radius: 8px; flex: 1; }
.recommend-name { cursor: pointer; font-weight: bold; }
.recommend-name:hover { color: #409eff; text-decoration: underline; }

/* 表格适配 */
.dark-table { --el-table-border-color: #363636; --el-table-bg-color: #1d212b; --el-table-tr-bg-color: #1d212b; --el-table-header-bg-color: #1d212b; }
:deep(.el-table td.el-table__cell), :deep(.el-table th.el-table__cell.is-leaf) { border-bottom: 1px solid #363636; }
:deep(.el-table--enable-row-hover .el-table__body tr:hover > td.el-table__cell) { background-color: #2b303c !important; }

/* 新闻详情样式 */
.news-detail-content { padding: 0 10px; }
.detail-title { font-size: 20px; font-weight: bold; color: #303133; margin-bottom: 15px; line-height: 1.4; }
.detail-meta { display: flex; gap: 15px; color: #909399; font-size: 13px; margin-bottom: 25px; border-bottom: 1px solid #eee; padding-bottom: 15px; }
.detail-body { font-size: 16px; line-height: 1.8; color: #606266; white-space: pre-wrap; }
.related-stock { margin-top: 30px; border-top: 1px dashed #ddd; padding-top: 20px; }
.stock-card { background: #f5f7fa; padding: 15px; border-radius: 8px; display: flex; align-items: center; cursor: pointer; transition: all 0.2s; }
.stock-card:hover { background: #ecf5ff; }
.stock-card .label { font-weight: bold; color: #303133; }
.stock-card .code { color: #409eff; font-family: monospace; margin: 0 10px; font-weight: bold; }
</style>
