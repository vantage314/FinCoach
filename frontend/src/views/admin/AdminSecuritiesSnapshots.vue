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
          {{ label('ImportDemo') }}
        </el-button>
        <el-button type="primary" @click="loadSnapshots" :loading="loading">
          {{ label('Refresh') }}
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
          :placeholder="assetKeyPlaceholder"
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
        <el-button type="primary" @click="handleSearch">{{ label('Query') }}</el-button>
        <el-button @click="handleReset">{{ label('Reset') }}</el-button>
      </div>
    </el-card>

    <el-card class="summary-card" shadow="never" v-loading="loading">
      <template #header>{{ label('Summary') }}</template>
      <el-descriptions :column="3" border>
        <el-descriptions-item :label="label('AssetsCount')">
          {{ summary.assetsCount ?? 0 }}
        </el-descriptions-item>
        <el-descriptions-item :label="label('SampleSize')">
          {{ summary.sampleSize ?? 0 }}
        </el-descriptions-item>
        <el-descriptions-item :label="label('LatestDate')">
          {{ formatEmptyText(summary.latestDate) }}
        </el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card class="table-card" shadow="never">
      <el-table
        :data="items"
        style="width: 100%"
        v-loading="loading"
        :empty-text="`${label('Empty')}快照`"
      >
        <el-table-column :label="label('Date')" width="140">
          <template #default="{ row }">
            {{ formatEmptyText(row.date) }}
          </template>
        </el-table-column>
        <el-table-column :label="label('AssetKey')" min-width="160">
          <template #default="{ row }">
            {{ formatEmptyText(row.assetKey) }}
          </template>
        </el-table-column>
        <el-table-column :label="label('PriceClose')" min-width="140">
          <template #default="{ row }">
            {{ formatEmptyText(row.price) }}
          </template>
        </el-table-column>
        <el-table-column :label="label('Currency')" width="120">
          <template #default="{ row }">
            {{ formatEmptyText(row.currency) }}
          </template>
        </el-table-column>
        <el-table-column :label="label('Source')" min-width="160">
          <template #default="{ row }">
            {{ formatEmptyText(row.source) }}
          </template>
        </el-table-column>
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
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { fetchSecuritiesSnapshots } from '@/api/adminSecurities';
import { importDemoData } from '@/api/adminDataSource';
import { formatEmptyText, getAdminLabel } from '@/utils/labelMap';

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
const label = (key: string) => getAdminLabel(key);
const assetKeyPlaceholder = computed(() => `${label('AssetKey')} / ${label('Source')}`);

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
  background: var(--fc-bg-gradient);
  color: var(--fc-text);
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
  color: var(--fc-text-muted);
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
  background: var(--fc-panel);
  border: 1px solid var(--fc-border);
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
  background: var(--fc-panel);
  border: 1px solid var(--fc-border);
}

.table-card {
  background: var(--fc-panel);
  border: 1px solid var(--fc-border);
}

.table-footer {
  display: flex;
  justify-content: flex-end;
  padding-top: 12px;
}
</style>
