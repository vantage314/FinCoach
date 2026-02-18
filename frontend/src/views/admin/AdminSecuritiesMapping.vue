<template>
  <div class="admin-page">
    <div class="page-header">
      <div>
        <h2>证券映射</h2>
        <p class="sub">管理资产关键字与交易所代码映射关系</p>
      </div>
      <div class="header-actions">
        <el-button type="primary" @click="openCreate">新增映射</el-button>
        <el-button @click="loadMappings" :loading="loading">刷新</el-button>
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
          placeholder="AssetKey / 关键字"
          clearable
          @keyup.enter="handleSearch"
        />
        <el-select v-model="enabledFilter" class="filter-item" placeholder="启用状态">
          <el-option label="全部" value="ALL" />
          <el-option label="启用" value="ENABLED" />
          <el-option label="禁用" value="DISABLED" />
        </el-select>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>
    </el-card>

    <el-card class="table-card" shadow="never">
      <el-table
        :data="items"
        style="width: 100%"
        v-loading="loading"
        empty-text="暂无映射"
      >
        <el-table-column prop="keyword" label="AssetKey" min-width="180" />
        <el-table-column prop="ticker" label="Ticker" min-width="160" />
        <el-table-column prop="market" label="Market" min-width="120" />
        <el-table-column label="Enabled" width="120">
          <template #default="{ row }">
            <el-switch
              v-model="row.enabled"
              :active-value="1"
              :inactive-value="0"
              @change="(val: number) => handleToggle(row, val)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="updatedAt" label="UpdatedAt" min-width="180" />
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" plain @click="openEdit(row)">
              编辑
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
        <el-form-item label="AssetKey" required>
          <el-input v-model="form.keyword" placeholder="例如 STOCK" />
        </el-form-item>
        <el-form-item label="Ticker" required>
          <el-input v-model="form.ticker" placeholder="例如 SPY.US" />
        </el-form-item>
        <el-form-item label="Market">
          <el-input v-model="form.market" placeholder="US / HK / CN" />
        </el-form-item>
        <el-form-item label="Priority">
          <el-input-number v-model="form.priority" :min="0" :max="999" />
        </el-form-item>
        <el-form-item label="Enabled">
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

const dialogTitle = computed(() =>
  dialogMode.value === 'create' ? '新增映射' : '编辑映射'
);

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
    ElMessage.warning('AssetKey 与 Ticker 必填');
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
    ElMessage.success(value === 1 ? '已启用' : '已禁用');
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
