<template>
  <div class="dashboard">
    <header class="dashboard-header">
      <div class="header-content">
        <h1 class="title">资产总览</h1>
        <p class="subtitle">管理您的投资组合，智能分析资产配置</p>
      </div>
    </header>

    <!-- 统计卡片 -->
    <div class="stats-grid">
      <div class="stat-card">
        <div class="stat-label">总资产</div>
        <div class="stat-value">¥ {{ formatNumber(summary.totalAmount) }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">资产数量</div>
        <div class="stat-value">{{ totalCount }} 笔</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">单笔投资红线</div>
        <div class="stat-value warning">¥ {{ formatNumber(summary.investmentLimit) }}</div>
      </div>
      <!-- 风险测评入口卡片 -->
      <div class="stat-card risk-card" @click="goToRiskAssessment">
        <div class="risk-card-content">
          <span class="risk-icon">{{ riskResult ? getIcon(riskResult.riskLevel) : '📋' }}</span>
          <div class="risk-info">
            <div class="risk-label">{{ riskResult ? '我的风险画像' : '风险测评' }}</div>
            <div class="risk-value">{{ riskResult ? riskResult.label : '立即测评 →' }}</div>
          </div>
        </div>
      </div>
    </div>

    <!-- Phase 9: 智能看板 -->
    <div class="adaptive-panel-row">
      <transition name="el-zoom-in-top" mode="out-in">
        <SafetyGoalPanel 
            v-if="healthStore.report?.userType === 'NOVICE'" 
            :current-cash="summary.categoryDistribution['1'] || 0"
            @add-cash="openAddCashDialog"
        />
        <InvestorOverview 
            v-else 
            :total-assets="summary.totalAmount"
        />
      </transition>
    </div>

    <!-- 资产健康度卡片 -->
    <div class="health-row">
      <HealthScoreCard @generate-plan="showPlanDrawer = true" />
    </div>

    <!-- 搜索栏 -->
    <div class="search-bar">
      <div class="search-left">
        <el-select
          v-model="selectedCategory"
          placeholder="全部分类"
          clearable
          class="category-select"
          @change="handleSearch"
        >
          <el-option
            v-for="cat in categories"
            :key="cat.id"
            :label="cat.name"
            :value="cat.id"
          />
        </el-select>
        <el-input
          v-model="searchKeyword"
          placeholder="搜索资产名称..."
          class="search-input"
          clearable
          @keyup.enter="handleSearch"
        >
          <template #prefix>
            <Search class="search-icon" />
          </template>
        </el-input>
        <el-button type="primary" class="search-btn" @click="handleSearch">
          搜索
        </el-button>
      </div>
      <el-button plain class="trans-btn" @click="showTransactionDrawer = true">
          <FileText class="icon" />
          资金流水
      </el-button>
      <el-button type="primary" class="add-btn" @click="showDialog = true">
        <Plus class="icon" />
        新增资产
      </el-button>
      <el-button 
        type="danger" 
        class="delete-btn" 
        :disabled="selectedIds.length === 0"
        @click="handleBatchDelete"
      >
        <Trash2 class="icon" />
        批量删除 ({{ selectedIds.length }})
      </el-button>
    </div>

    <!-- 资产列表 -->
    <div class="asset-table-wrapper">
      <el-table 
        :data="assets" 
        style="width: 100%" 
        class="asset-table"
        :header-cell-style="{ background: 'rgba(0,0,0,0.3)', color: '#94a3b8' }"
        :row-style="{ background: 'transparent' }"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column prop="assetName" label="资产名称" min-width="150">
          <template #default="{ row }">
            <span class="asset-name">{{ row.assetName }}</span>
            <span v-if="row.assetCode" class="asset-code">{{ row.assetCode }}</span>
          </template>
        </el-table-column>
        
        <el-table-column prop="categoryId" label="分类" width="140">
          <template #default="{ row }">
            <el-tag :type="getCategoryTagType(row.categoryId)" size="small">
              {{ getCategoryName(row.categoryId) }}
            </el-tag>
            <span v-if="row.subType" class="sub-type-tag">{{ row.subType }}</span>
          </template>
        </el-table-column>
        
        <el-table-column prop="currentValue" label="当前价值" width="140" align="right">
          <template #default="{ row }">
            <span class="money">¥ {{ formatNumber(row.currentValue) }}</span>
          </template>
        </el-table-column>
        
        <el-table-column prop="holdingCost" label="持有成本" width="140" align="right">
          <template #default="{ row }">
            <span v-if="row.holdingCost" class="money dim">
              ¥ {{ formatNumber(row.holdingCost) }}
            </span>
            <span v-else class="money dim">--</span>
          </template>
        </el-table-column>
        
        <el-table-column label="盈亏" width="120" align="right">
          <template #default="{ row }">
            <span :class="getProfitClass(row)">
              {{ getProfitText(row) }}
            </span>
          </template>
        </el-table-column>
        
        <el-table-column label="操作" width="80" align="center">
          <template #default="{ row }">
            <el-button 
              type="danger" 
              :icon="Trash2" 
              circle 
              size="small"
              @click="handleDeleteRow(row)"
            />
          </template>
        </el-table-column>
      </el-table>
      
      <el-empty v-if="assets.length === 0 && !loading" description="暂无资产，点击右上角添加" />

      <!-- 分页 -->
      <div class="pagination-wrapper" v-if="totalCount > 0">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="totalCount"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          background
          @size-change="handlePageChange"
          @current-change="handlePageChange"
        />
      </div>
    </div>

    <!-- 资产录入弹窗 -->
    <AssetInputDialog v-model="showDialog" @success="refreshData" />
    
    <!-- 调仓计划抽屉 -->
    <PlanDrawer v-model="showPlanDrawer" @success="refreshData" />

    <!-- 资金流水抽屉 -->
    <TransactionDrawer v-model="showTransactionDrawer" />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onActivated } from 'vue';
import { useRouter } from 'vue-router';
import { Plus, Search, Trash2, FileText } from 'lucide-vue-next';
import { ElMessage, ElMessageBox } from 'element-plus';
import { getAssetList, getAssetSummary, getCategories, deleteAssets, type AssetItem, type PortfolioSummary, type AssetCategory } from '@/api/asset';
import { getLatestResult, type RiskAssessmentResult } from '@/api/risk';
import { useHealthStore } from '@/store/modules/health';
import { useAssetStore } from '@/store/modules/asset';
import AssetInputDialog from '../portfolio/components/AssetInputDialog.vue';
import HealthScoreCard from '@/components/HealthScoreCard.vue';
import PlanDrawer from '@/components/PlanDrawer.vue';
import SafetyGoalPanel from './components/SafetyGoalPanel.vue';
import InvestorOverview from './components/InvestorOverview.vue';
import TransactionDrawer from './components/TransactionDrawer.vue';
import { storeToRefs } from 'pinia';

const router = useRouter();
const healthStore = useHealthStore();
const assetStore = useAssetStore();
const { assets, summary, loading, total: totalCount } = storeToRefs(assetStore);

const showDialog = ref(false);
const showPlanDrawer = ref(false);
const showTransactionDrawer = ref(false);
// const assets = ref<AssetItem[]>([]); // Removed
const categories = ref<AssetCategory[]>([]);
const searchKeyword = ref('');
const riskResult = ref<RiskAssessmentResult | null>(null);
const selectedCategory = ref<number | undefined>(undefined);
const selectedIds = ref<number[]>([]);
const currentPage = ref(1);
const pageSize = ref(10);
// const totalCount = ref(0); // Removed

const formatNumber = (num: number) => {
  return (num || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
};

const getCategoryName = (id: number) => {
  const cat = categories.value.find(c => c.id === id);
  return cat?.name || '未知';
};

const getCategoryTagType = (id: number): '' | 'success' | 'warning' | 'info' | 'danger' => {
  const types: Record<number, '' | 'success' | 'warning' | 'info' | 'danger'> = {
    1: 'success',   // 现金-绿色
    2: '',          // 金融投资-蓝色
    3: 'warning'    // 固定资产-橙色
  };
  return types[id] || 'info';
};

const getProfitClass = (row: AssetItem) => {
  // 仅投资理财(2)显示盈亏
  if (row.categoryId !== 2 || !row.holdingCost) return 'profit';
  const profit = (row.currentValue || 0) - (row.holdingCost || 0);
  if (profit > 0) return 'profit positive';
  if (profit < 0) return 'profit negative';
  return 'profit';
};

const getProfitText = (row: AssetItem) => {
  // 仅投资理财(2)计算盈亏
  if (row.categoryId !== 2) return '--';
  const cost = row.holdingCost || 0;
  if (cost === 0) return '--';
  const profit = row.currentValue - cost;
  const prefix = profit > 0 ? '+' : '';
  return prefix + formatNumber(profit);
};

const fetchAssets = async () => {
    // 使用后端筛选
    const params: { categoryId?: number; assetName?: string; page?: number; size?: number } = {};
    if (selectedCategory.value) {
      params.categoryId = selectedCategory.value;
    }
    if (searchKeyword.value) {
      params.assetName = searchKeyword.value;
    }
    
    // 如果后端支持分页，直接传参
    // params.page = currentPage.value;
    // params.size = pageSize.value;
    
    await assetStore.getList(params);
};

const fetchSummary = async () => {
  await assetStore.getSummary();
};

const fetchCategories = async () => {
  try {
    const res: any = await getCategories();
    if (res.code === 200) {
      categories.value = res.data;
    }
  } catch (error) {
    console.error('获取分类失败:', error);
  }
};

const handleSearch = () => {
  currentPage.value = 1;
  fetchAssets();
};

const handlePageChange = () => {
  fetchAssets();
};

const refreshData = async () => {
  console.log('🔄 刷新资产数据...');
  // 并行请求优化性能
  await Promise.all([
    fetchAssets(),
    fetchSummary(),
    healthStore.fetchHealthReport() // 联动刷新健康度
  ]);
};

const handleSelectionChange = (selection: AssetItem[]) => {
  selectedIds.value = selection.map(item => item.id);
};

const handleBatchDelete = async () => {
  if (selectedIds.value.length === 0) return;
  
  try {
    await ElMessageBox.confirm(
      `确定要删除选中的 ${selectedIds.value.length} 笔资产吗？此操作不可撤销。`,
      '批量删除确认',
      { type: 'warning', confirmButtonText: '确认删除', cancelButtonText: '取消' }
    );
    
    const res: any = await deleteAssets(selectedIds.value);
    if (res.code === 200) {
      ElMessage.success(res.data || '删除成功');
      selectedIds.value = [];
      refreshData();
    } else {
      ElMessage.error(res.message || '删除失败');
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('删除失败:', error);
      ElMessage.error('删除失败');
    }
  }
};

const handleDeleteRow = async (row: AssetItem) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除「${row.assetName}」吗？`,
      '删除确认',
      { type: 'warning', confirmButtonText: '确认', cancelButtonText: '取消' }
    );
    
    const res: any = await deleteAssets([row.id]);
    if (res.code === 200) {
      ElMessage.success('删除成功');
      refreshData();
    } else {
      ElMessage.error(res.message || '删除失败');
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('删除失败:', error);
      ElMessage.error('删除失败');
    }
  }
};

// 风险测评相关
const getIcon = (level: string) => {
  const icons: Record<string, string> = {
    conservative: '🛡️',
    steady: '📈',
    balanced: '⚖️',
    growth: '🚀',
    aggressive: '🔥'
  };
  return icons[level] || '⚖️';
};

const goToRiskAssessment = () => {
  if (riskResult.value) {
    router.push('/risk/result');
  } else {
    router.push('/risk/assessment');
  }
};

const fetchRiskResult = async () => {
  try {
    const res: any = await getLatestResult();
    if (res.code === 200) {
      riskResult.value = res.data;
    }
  } catch (error) {
    // 未测评过，忽略错误
  }
};

// 封装数据加载逻辑
const loadData = () => {
  console.log('[Dashboard] 加载数据...');
  fetchCategories();
  refreshData();
  fetchRiskResult();
};

// 组件挂载时加载数据
onMounted(() => {
  loadData();
});

// 组件激活，重新加载数据
onActivated(() => {
  console.log('[Dashboard] 组件激活，重新加载数据');
  loadData();
});

const openAddCashDialog = () => {
    showDialog.value = true;
};
</script>

<script lang="ts">
export default {
    components: {
        SafetyGoalPanel,
        InvestorOverview
    }
}
</script>

<style lang="scss" scoped>
@use "@/theme/variables.scss" as *;

.dashboard {
  min-height: 100vh;
  background: linear-gradient(135deg, #0f172a, #1e293b);
  padding: 32px;
  color: white;
}

.dashboard-header {
  margin-bottom: 32px;
  
  .title {
    font-size: 28px;
    font-weight: 700;
    background: linear-gradient(to right, #fff, #94a3b8);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    margin-bottom: 4px;
  }
  
  .subtitle {
    color: #64748b;
    font-size: 14px;
  }
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 24px;
}

.stat-card {
  background: rgba(255, 255, 255, 0.05);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 16px;
  padding: 24px;
  
  .stat-label {
    color: #64748b;
    font-size: 14px;
    margin-bottom: 8px;
  }
  
  .stat-value {
    font-size: 24px;
    font-weight: 700;
    font-variant-numeric: tabular-nums;
    
    &.warning {
      color: #f59e0b;
    }
  }
}

/* 风险测评入口卡片 */
.risk-card {
  cursor: pointer;
  background: linear-gradient(135deg, rgba(6, 182, 212, 0.15), rgba(79, 70, 229, 0.15)) !important;
  border-color: rgba(6, 182, 212, 0.3) !important;
  transition: all 0.3s ease;
  
  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 8px 24px rgba(6, 182, 212, 0.3);
    border-color: #06b6d4 !important;
  }
}

.risk-card-content {
  display: flex;
  align-items: center;
  gap: 16px;
}

.risk-icon {
  font-size: 36px;
}

.risk-info {
  .risk-label {
    color: #94a3b8;
    font-size: 13px;
    margin-bottom: 4px;
  }
  
  .risk-value {
    font-size: 18px;
    font-weight: 600;
    color: #06b6d4;
  }
}

.health-row {
  margin-bottom: 24px;
}

.adaptive-panel-row {
    margin-bottom: 24px;
    animation: fadeIn 0.5s ease-out;
}

@keyframes fadeIn {
    from { opacity: 0; transform: translateY(10px); }
    to { opacity: 1; transform: translateY(0); }
}

.search-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  
  .search-left {
    display: flex;
    gap: 12px;
  }
  
  .search-input {
    width: 280px;
    
    :deep(.el-input__wrapper) {
      background-color: rgba(255, 255, 255, 0.05) !important;
      box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.1) inset !important;
      border-radius: 12px;
    }
    
    :deep(.el-input__inner) {
      color: #ffffff !important;
      
      &::placeholder {
        color: rgba(255, 255, 255, 0.3) !important;
      }
    }
  }
  
  .search-icon {
    width: 16px;
    height: 16px;
    color: #64748b;
  }
  
  .search-btn {
    border-radius: 12px;
  }
  
  .category-select {
    width: 140px;
    
    :deep(.el-select__wrapper) {
      background-color: rgba(255, 255, 255, 0.05) !important;
      box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.1) inset !important;
      border-radius: 12px;
    }
    
    :deep(.el-select__placeholder) {
      color: rgba(255, 255, 255, 0.5);
    }
    
    :deep(.el-select__selected-item) {
      color: #ffffff;
    }
  }
}

.add-btn {
  height: 44px;
  padding: 0 24px;
  border-radius: 12px;
  font-weight: 600;
  background: linear-gradient(135deg, #06b6d4, $primary-color);
  border: none;
  
  .icon {
    width: 18px;
    height: 18px;
    margin-right: 8px;
  }
  
  &:hover {
    transform: scale(1.02);
    box-shadow: 0 0 20px rgba(79, 70, 229, 0.4);
  }
}

.delete-btn {
  height: 44px;
  padding: 0 20px;
  border-radius: 12px;
  font-weight: 600;
  margin-left: 12px;
  
  .icon {
    width: 16px;
    height: 16px;
    margin-right: 6px;
  }
}

.trans-btn {
  height: 44px;
  padding: 0 20px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  color: #94a3b8;
  margin-right: 12px;
  font-weight: 600;

  .icon {
    width: 16px;
    height: 16px;
    margin-right: 6px;
  }

  &:hover {
    background: rgba(255, 255, 255, 0.1);
    color: white;
    border-color: rgba(255, 255, 255, 0.2);
  }
}

.asset-table-wrapper {
  background: rgba(255, 255, 255, 0.03);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 16px;
  padding: 20px;
}

.asset-table {
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

.asset-name {
  font-weight: 500;
}

.asset-code {
  margin-left: 8px;
  color: #64748b;
  font-size: 12px;
}

.sub-type-tag {
  margin-left: 6px;
  color: #06b6d4;
  font-size: 12px;
  font-weight: 500;
}

.money {
  font-variant-numeric: tabular-nums;
  font-weight: 500;
  
  &.dim {
    color: #64748b;
  }
}

.profit {
  font-variant-numeric: tabular-nums;
  font-weight: 500;
  
  &.positive {
    color: #ef4444; // 红涨
  }
  
  &.negative {
    color: #22c55e; // 绿跌
  }
}

.pagination-wrapper {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
  
  :deep(.el-pagination) {
    --el-pagination-bg-color: rgba(255, 255, 255, 0.05);
    --el-pagination-text-color: #94a3b8;
    --el-pagination-button-color: #94a3b8;
    --el-pagination-hover-color: #06b6d4;
  }
  
  :deep(.el-pagination.is-background .el-pager li) {
    background-color: rgba(255, 255, 255, 0.05);
    color: #94a3b8;
    
    &.is-active {
      background-color: $primary-color;
      color: white;
    }
  }
}
</style>
