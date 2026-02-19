<template>
  <div class="admin-page">
    <div class="page-header">
      <div>
        <h2>用户管理</h2>
        <p class="sub">查看用户列表与状态</p>
      </div>
      <el-button type="primary" :loading="loading" @click="reload">
        刷新
      </el-button>
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
          v-model="query"
          class="filter-item"
          placeholder="用户名/邮箱"
          clearable
          @keyup.enter="reload"
        />
        <el-button type="primary" @click="reload">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </div>
    </el-card>

    <el-card class="table-card" shadow="never" v-loading="loading">
      <el-table :data="items" style="width: 100%" empty-text="暂无用户">
        <el-table-column prop="username" label="用户名" min-width="160" />
        <el-table-column prop="email" label="邮箱" min-width="200" />
        <el-table-column label="角色" min-width="180">
          <template #default="{ row }">
            {{ formatRoles(row.roles) }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            {{ formatEnabledText(row.enabled) }}
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="180">
          <template #default="{ row }">
            {{ formatEmptyText(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="最近登录" width="180">
          <template #default="{ row }">
            {{ formatEmptyText(row.lastLoginAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button
              size="small"
              type="primary"
              plain
              :loading="togglingId === row.id"
              :disabled="row.id == null || row.enabled === null || row.enabled === undefined"
              @click="handleToggle(row)"
            >
              {{ row.enabled ? '停用' : '启用' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="table-footer">
        <el-pagination
          background
          layout="total, sizes, prev, pager, next"
          :total="total"
          :page-size="size"
          :current-page="page"
          :page-sizes="[10, 20, 50, 100]"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { fetchAdminUsers, toggleAdminUser, type AdminUserItem } from '@/api/adminUsers';
import { formatEmptyText, formatEnabledText } from '@/utils/labelMap';

const loading = ref(false);
const error = ref('');
const items = ref<AdminUserItem[]>([]);
const query = ref('');
const page = ref(1);
const size = ref(20);
const total = ref(0);
const togglingId = ref<number | null>(null);

const loadUsers = async () => {
  loading.value = true;
  error.value = '';
  try {
    const res: any = await fetchAdminUsers({
      page: page.value,
      size: size.value,
      q: query.value || undefined,
    });
    items.value = res?.data?.items || [];
    total.value = res?.data?.total ?? 0;
  } catch (e: any) {
    error.value = e?.message || '加载用户失败';
  } finally {
    loading.value = false;
  }
};

const reload = () => {
  page.value = 1;
  loadUsers();
};

const reset = () => {
  query.value = '';
  page.value = 1;
  loadUsers();
};

const handlePageChange = (nextPage: number) => {
  page.value = nextPage;
  loadUsers();
};

const handleSizeChange = (nextSize: number) => {
  size.value = nextSize;
  page.value = 1;
  loadUsers();
};

const formatRoles = (roles?: string[]) => {
  if (!roles || roles.length === 0) {
    return formatEmptyText(null);
  }
  return roles.join(', ');
};

const handleToggle = async (row: AdminUserItem) => {
  if (!row.id || row.enabled === null || row.enabled === undefined) return;
  const nextEnabled = !row.enabled;
  try {
    await ElMessageBox.confirm(
      `确认${nextEnabled ? '启用' : '停用'}用户 ${row.username || row.email || row.id}？`,
      '提示',
      { type: 'warning', confirmButtonText: '确定', cancelButtonText: '取消' }
    );
    togglingId.value = row.id;
    await toggleAdminUser({ userId: row.id, enabled: nextEnabled });
    ElMessage.success(`已${nextEnabled ? '启用' : '停用'}`);
    loadUsers();
  } catch (e: any) {
    if (e !== 'cancel' && e !== 'close') {
      error.value = e?.message || '操作失败';
    }
  } finally {
    togglingId.value = null;
  }
};

onMounted(() => {
  loadUsers();
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
  min-width: 220px;
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

.error-banner {
  margin-bottom: 16px;
}
</style>
