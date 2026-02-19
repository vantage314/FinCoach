<template>
  <div class="admin-page">
    <div class="page-header">
      <div>
        <h2>预警管理</h2>
        <p class="sub">查看并确认当前预警记录</p>
      </div>
      <div class="filters">
        <el-select v-model="status" class="filter-item" placeholder="状态">
          <el-option label="OPEN" value="OPEN" />
          <el-option label="ACKED" value="ACKED" />
          <el-option label="RESOLVED" value="RESOLVED" />
          <el-option label="ALL" value="ALL" />
        </el-select>
        <el-input-number
          v-model="limit"
          class="filter-item"
          :min="1"
          :max="1000"
          :step="50"
          controls-position="right"
        />
        <el-button type="primary" @click="loadAlerts">查询</el-button>
      </div>
    </div>

    <el-alert
      v-if="error"
      :title="error"
      type="error"
      show-icon
      class="error-banner"
    />

    <el-card class="table-card" shadow="never">
      <el-table
        :data="items"
        style="width: 100%"
        v-loading="loading"
        empty-text="暂无预警"
      >
        <el-table-column prop="alertId" label="ID" width="90" />
        <el-table-column prop="code" label="Code" min-width="160" />
        <el-table-column label="Severity" width="120">
          <template #default="{ row }">
            <el-tag :type="severityTag(row.severity)" size="small">
              {{ row.severity || 'UNKNOWN' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="Title" min-width="240" />
        <el-table-column label="Status" width="120">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)" size="small">
              {{ row.status || 'OPEN' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="CreatedAt" min-width="180" />
        <el-table-column prop="reportId" label="ReportId" width="120" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button
              size="small"
              type="success"
              :disabled="row.status === 'ACKED' || ackingId === row.alertId"
              @click="ackAlert(row)"
            >
              ACK
            </el-button>
            <el-button
              size="small"
              type="danger"
              plain
              :disabled="row.status === 'RESOLVED' || resolvingId === row.alertId"
              @click="resolveAlert(row)"
            >
              RESOLVE
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="table-footer">
        <span>总数：{{ total }}</span>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import { ackAdminAlerts, fetchAdminAlerts, resolveAdminAlerts, type AdminAlertItem } from '@/api/adminAlerts';

const status = ref('OPEN');
const limit = ref(200);
const items = ref<AdminAlertItem[]>([]);
const total = ref(0);
const loading = ref(false);
const error = ref('');
const ackingId = ref<number | null>(null);
const resolvingId = ref<number | null>(null);

const loadAlerts = async () => {
  loading.value = true;
  error.value = '';
  try {
    const res: any = await fetchAdminAlerts({
      status: status.value,
      limit: limit.value,
    });
    items.value = res?.data?.items || [];
    total.value = res?.data?.total ?? items.value.length;
  } catch (e: any) {
    error.value = e?.message || '拉取预警失败';
  } finally {
    loading.value = false;
  }
};

const ackAlert = async (row: AdminAlertItem) => {
  if (!row?.alertId) {
    ElMessage.warning('无效的 alertId');
    return;
  }
  ackingId.value = row.alertId;
  try {
    await ackAdminAlerts([{ alertId: row.alertId }]);
    ElMessage.success('ACK 成功');
    await loadAlerts();
  } catch (e: any) {
    error.value = e?.message || 'ACK 失败';
  } finally {
    ackingId.value = null;
  }
};

const resolveAlert = async (row: AdminAlertItem) => {
  if (!row?.alertId) {
    ElMessage.warning('无效的 alertId');
    return;
  }
  resolvingId.value = row.alertId;
  try {
    await resolveAdminAlerts([{ alertId: row.alertId }]);
    ElMessage.success('RESOLVE 成功');
    await loadAlerts();
  } catch (e: any) {
    error.value = e?.message || 'RESOLVE 失败';
  } finally {
    resolvingId.value = null;
  }
};

const statusTag = (value?: string) => {
  const normalized = (value || 'OPEN').toUpperCase();
  return normalized === 'ACKED' ? 'success' : 'warning';
};

const severityTag = (value?: string) => {
  const normalized = (value || '').toUpperCase();
  if (normalized === 'DANGER') return 'danger';
  if (normalized === 'CRITICAL') return 'danger';
  if (normalized === 'WARN') return 'warning';
  if (normalized === 'INFO') return 'info';
  return 'info';
};

onMounted(() => {
  loadAlerts();
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

.filters {
  display: flex;
  align-items: center;
  gap: 12px;
}

.filter-item {
  min-width: 160px;
}

.error-banner {
  margin-bottom: 16px;
}

.table-card {
  background: rgba(30, 41, 59, 0.6);
  border: 1px solid rgba(255, 255, 255, 0.08);
}

.table-footer {
  display: flex;
  justify-content: flex-end;
  padding-top: 12px;
  color: #94a3b8;
  font-size: 12px;
}
</style>
