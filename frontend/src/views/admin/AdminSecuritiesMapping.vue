<template>
  <div class="admin-page">
    <div class="page-header">
      <div>
        <h2>证券映射</h2>
        <p class="sub">管理资产关键字与交易所代码映射关系</p>
      </div>
      <div class="header-actions">
        <el-button type="primary" @click="openCreate">{{ label('Add') }}映射</el-button>
        <el-button @click="loadMappings" :loading="loading">{{ label('Refresh') }}</el-button>
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
          v-model="query.keyword"
          class="filter-item"
          :placeholder="assetKeyPlaceholder"
          clearable
          @keyup.enter="handleSearch"
        />
        <el-select v-model="enabledFilter" class="filter-item" :placeholder="label('Enabled')">
          <el-option label="全部" value="ALL" />
          <el-option :label="label('EnabledOn')" value="ENABLED" />
          <el-option :label="label('Disabled')" value="DISABLED" />
        </el-select>
        <el-button type="primary" @click="handleSearch">{{ label('Query') }}</el-button>
        <el-button @click="handleReset">{{ label('Reset') }}</el-button>
      </div>
    </el-card>

    <el-card class="table-card" shadow="never">
      <el-table
        :data="items"
        style="width: 100%"
        v-loading="loading"
        empty-text="暂无映射"
      >
        <el-table-column prop="keyword" :label="label('AssetKey')" min-width="180" />
        <el-table-column prop="ticker" :label="label('Ticker')" min-width="160" />
        <el-table-column :label="label('Market')" min-width="120">
          <template #default="{ row }">
            {{ formatMarketText(row.market) }}
          </template>
        </el-table-column>
        <el-table-column :label="label('Enabled')" min-width="160">
          <template #default="{ row }">
            <el-space size="8" alignment="center">
              <el-switch
                v-model="row.enabled"
                :active-value="1"
                :inactive-value="0"
                @change="(val: number) => handleToggle(row, val)"
              />
              <span class="status-text">{{ formatEnabledText(row.enabled) }}</span>
            </el-space>
          </template>
        </el-table-column>
        <el-table-column :label="label('UpdatedAt')" min-width="180">
          <template #default="{ row }">
            {{ formatEmptyText(row.updatedAt) }}
          </template>
        </el-table-column>
        <el-table-column :label="label('Operation')" width="140" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" plain @click="openEdit(row)">
              {{ label('Edit') }}
            </el-button>
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

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="520px">
      <el-form :model="form" label-width="110px">
        <el-form-item :label="label('AssetKey')" required>
          <el-input v-model="form.keyword" placeholder="例如 STOCK" />
        </el-form-item>
        <el-form-item :label="label('Ticker')" required>
          <el-input v-model="form.ticker" placeholder="例如 SPY.US" />
        </el-form-item>
        <el-form-item :label="label('Market')">
          <el-input v-model="form.market" placeholder="US / HK / CN" />
        </el-form-item>
        <el-form-item :label="label('Priority')">
          <el-input-number v-model="form.priority" :min="0" :max="999" />
        </el-form-item>
        <el-form-item :label="label('Enabled')">
          <el-switch v-model="form.enabled" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">
          保存
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted, computed } from 'vue';
import { ElMessage } from 'element-plus';
import {
  fetchSecuritiesMappings,
  createSecuritiesMapping,
  updateSecuritiesMapping,
  toggleSecuritiesMapping,
} from '@/api/adminSecurities';
import {
  formatEmptyText,
  formatEnabledText,
  formatMarketText,
  getAdminLabel
} from '@/utils/labelMap';

const items = ref<any[]>([]);
const total = ref(0);
const loading = ref(false);
const error = ref('');
const saving = ref(false);

const query = reactive({
  keyword: '',
  enabled: undefined as number | undefined,
  page: 1,
  size: 20,
});

const enabledFilter = ref('ALL');

const dialogVisible = ref(false);
const dialogMode = ref<'create' | 'edit'>('create');
const form = reactive({
  id: null as number | null,
  keyword: '',
  ticker: '',
  market: '',
  priority: 0,
  enabled: 1,
});

const label = (key: string) => getAdminLabel(key);
const dialogTitle = computed(() =>
  dialogMode.value === 'create' ? `${label('Add')}映射` : `${label('Edit')}映射`
);
const assetKeyPlaceholder = computed(() => `${label('AssetKey')} / 关键词`);

const loadMappings = async () => {
  loading.value = true;
  error.value = '';
  try {
    const res: any = await fetchSecuritiesMappings({
      keyword: query.keyword || undefined,
      enabled: query.enabled,
      page: query.page,
      size: query.size,
    });
    items.value = res?.data?.items || [];
    total.value = res?.data?.total ?? 0;
  } catch (e: any) {
    error.value = e?.message || '加载映射失败';
  } finally {
    loading.value = false;
  }
};

const handleSearch = () => {
  query.page = 1;
  if (enabledFilter.value === 'ENABLED') query.enabled = 1;
  else if (enabledFilter.value === 'DISABLED') query.enabled = 0;
  else query.enabled = undefined;
  loadMappings();
};

const handleReset = () => {
  query.keyword = '';
  enabledFilter.value = 'ALL';
  query.enabled = undefined;
  query.page = 1;
  loadMappings();
};

const handlePageChange = (page: number) => {
  query.page = page;
  loadMappings();
};

const handleSizeChange = (size: number) => {
  query.size = size;
  query.page = 1;
  loadMappings();
};

const openCreate = () => {
  dialogMode.value = 'create';
  form.id = null;
  form.keyword = '';
  form.ticker = '';
  form.market = '';
  form.priority = 0;
  form.enabled = 1;
  dialogVisible.value = true;
};

const openEdit = (row: any) => {
  dialogMode.value = 'edit';
  form.id = row.id;
  form.keyword = row.keyword || '';
  form.ticker = row.ticker || '';
  form.market = row.market || '';
  form.priority = row.priority ?? 0;
  form.enabled = row.enabled ?? 1;
  dialogVisible.value = true;
};

const handleSave = async () => {
  if (!form.keyword || !form.ticker) {
    ElMessage.warning(`${label('AssetKey')} 与 ${label('Ticker')} 必填`);
    return;
  }
  saving.value = true;
  try {
    const payload = {
      keyword: form.keyword,
      ticker: form.ticker,
      market: form.market || null,
      priority: form.priority ?? 0,
      enabled: form.enabled ?? 1,
    };
    if (dialogMode.value === 'create') {
      await createSecuritiesMapping(payload);
      ElMessage.success('新增成功');
    } else if (form.id) {
      await updateSecuritiesMapping(form.id, payload);
      ElMessage.success('更新成功');
    }
    dialogVisible.value = false;
    await loadMappings();
  } catch (e: any) {
    error.value = e?.message || '保存失败';
  } finally {
    saving.value = false;
  }
};

const handleToggle = async (row: any, value: number) => {
  const previous = value === 1 ? 0 : 1;
  try {
    await toggleSecuritiesMapping(row.id, value === 1);
    ElMessage.success(value === 1 ? `已${label('EnabledOn')}` : `已${label('Disabled')}`);
  } catch (e: any) {
    row.enabled = previous;
    error.value = e?.message || '切换失败';
  }
};

onMounted(() => {
  loadMappings();
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

.table-card {
  background: var(--fc-panel);
  border: 1px solid var(--fc-border);
}

.table-footer {
  display: flex;
  justify-content: flex-end;
  padding-top: 12px;
}

.status-text {
  font-size: 12px;
  color: #cbd5f5;
}
</style>
