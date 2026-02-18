<template>
  <div class="admin-page">
    <div class="page-header">
      <div>
        <h2>行情快照</h2>
        <p class="sub">浏览已入库的快照数据与覆盖情况</p>
      </div>
      <div class="header-actions">
        <el-button
          type="success"
          plain
          :loading="importing"
          @click="importDemo"
        >
          导入演示数据
        </el-button>
        <el-button type="primary" @click="loadSnapshots" :loading="loading">
          刷新
        </el-button>
      </div>
    </div>

    <el-alert
      v-if="error"
      :title="error"
      type="error"
      show-icon
      class="error-banner"
    />

    <el-card class="filter-card" shadow="never">
      <div class="filters">
        <el-input
          v-model="query.assetKey"
          class="filter-item"
          placeholder="AssetKey / 来源"
          clearable
          @keyup.enter="handleSearch"
        />
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
        />
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>
    </el-card>

    <el-card class="summary-card" shadow="never" v-loading="loading">
      <template #header>摘要</template>
      <el-descriptions :column="3" border>
        <el-descriptions-item label="资产数">
          {{ summary.assetsCount ?? 0 }}
        </el-descriptions-item>
        <el-descriptions-item label="样本数">
          {{ summary.sampleSize ?? 0 }}
        </el-descriptions-item>
        <el-descriptions-item label="最新日期">
          {{ summary.latestDate || '-' }}
        </el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card class="table-card" shadow="never">
      <el-table
        :data="items"
        style="width: 100%"
        v-loading="loading"
        empty-text="暂无快照"
      >
        <el-table-column prop="date" label="日期" width="140" />
        <el-table-column prop="assetKey" label="AssetKey" min-width="160" />
        <el-table-column prop="price" label="Price/Close" min-width="140" />
        <el-table-column prop="currency" label="Currency" width="120" />
        <el-table-column prop="source" label="Source" min-width="160" />
      </el-table>

      <div class="table-footer">
        <el-pagination
          background
          layout="total, sizes, prev, pager, next"
          :total="total"
          :page-size="query.size"
          :current-page="query.page"
          :page-sizes="[10, 20, 50, 100]"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import { fetchSecuritiesSnapshots } from '@/api/adminSecurities';
import { importDemoData } from '@/api/adminDataSource';

const items = ref<any[]>([]);
const total = ref(0);
const summary = ref<any>({});
const loading = ref(false);
const importing = ref(false);
const error = ref('');

const query = reactive({
  assetKey: '',
  page: 1,
  size: 20,
});

const dateRange = ref<string[] | null>(null);

const loadSnapshots = async () => {
  loading.value = true;
  error.value = '';
  try {
    const params: any = {
      assetKey: query.assetKey || undefined,
      page: query.page,
      size: query.size,
    };
    if (dateRange.value && dateRange.value.length === 2) {
      params.startDate = dateRange.value[0];
      params.endDate = dateRange.value[1];
    }
    const res: any = await fetchSecuritiesSnapshots(params);
    items.value = res?.data?.items || [];
    total.value = res?.data?.total ?? 0;
    summary.value = res?.data?.summary || {};
  } catch (e: any) {
    error.value = e?.message || '加载快照失败';
  } finally {
    loading.value = false;
  }
};

const handleSearch = () => {
  query.page = 1;
  loadSnapshots();
};

const handleReset = () => {
  query.assetKey = '';
  dateRange.value = null;
  query.page = 1;
  loadSnapshots();
};

const handlePageChange = (page: number) => {
  query.page = page;
  loadSnapshots();
};

const handleSizeChange = (size: number) => {
  query.size = size;
  query.page = 1;
  loadSnapshots();
};

const importDemo = async () => {
  importing.value = true;
  try {
    const res: any = await importDemoData();
    const inserted = res?.data?.insertedSnapshots ?? 0;
    const skipped = res?.data?.skippedSnapshots ?? 0;
    ElMessage.success(`演示数据导入完成，新增 ${inserted} 条，跳过 ${skipped} 条`);
    await loadSnapshots();
  } catch (e: any) {
    error.value = e?.message || '导入失败';
  } finally {
    importing.value = false;
  }
};

onMounted(() => {
  loadSnapshots();
});
</script>

<style scoped>
.admin-page {
  min-height: 100vh;
  padding: 24px;
  background: linear-gradient(135deg, #0f172a, #1e293b);
  color: #fff;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  gap: 16px;
  flex-wrap: wrap;
}

.page-header h2 {
  margin: 0;
  font-size: 22px;
}

.sub {
  margin: 6px 0 0;
  color: #94a3b8;
  font-size: 13px;
}

.header-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.error-banner {
  margin-bottom: 16px;
}

.filter-card {
  margin-bottom: 16px;
  background: rgba(30, 41, 59, 0.6);
  border: 1px solid rgba(255, 255, 255, 0.08);
}

.filters {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.filter-item {
  min-width: 180px;
}

.summary-card {
  margin-bottom: 16px;
  background: rgba(30, 41, 59, 0.6);
  border: 1px solid rgba(255, 255, 255, 0.08);
}

.table-card {
  background: rgba(30, 41, 59, 0.6);
  border: 1px solid rgba(255, 255, 255, 0.08);
}

.table-footer {
  display: flex;
  justify-content: flex-end;
  padding-top: 12px;
}
</style>
