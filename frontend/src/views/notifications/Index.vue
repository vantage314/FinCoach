<template>
  <div class="notify-page">
    <div class="toolbar">
      <div>
        <h2>通知中心</h2>
        <div class="subtitle">查看系统提醒与更新记录</div>
      </div>
      <div class="actions">
        <el-button type="primary" plain @click="markAllRead">全部已读</el-button>
      </div>
    </div>

    <el-tabs v-model="activeTab" @tab-click="loadList">
      <el-tab-pane label="全部" name="all" />
      <el-tab-pane label="未读" name="unread" />
    </el-tabs>

    <el-table
      :data="notifications"
      style="width: 100%"
      v-loading="loading"
      class="dark-table"
      :header-cell-style="{ background: '#1d212b', color: '#909399', borderBottom: '1px solid #363636' }"
      :row-style="{ background: 'transparent', color: '#fff' }"
    >
      <el-table-column prop="title" label="标题" width="220" />
      <el-table-column prop="content" label="内容" />
      <el-table-column prop="createdAt" label="时间" width="180">
        <template #default="{ row }">
          {{ formatTime(row.createdAt) }}
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="row.isRead === 1 ? 'info' : 'warning'">
            {{ row.isRead === 1 ? '已读' : '未读' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120" align="center">
        <template #default="{ row }">
          <el-button
            v-if="row.isRead !== 1"
            link
            type="primary"
            size="small"
            @click="markRead(row)"
          >
            标记已读
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pager">
      <el-pagination
        background
        layout="prev, pager, next"
        :total="total"
        :page-size="pageSize"
        v-model:current-page="page"
        @current-change="loadList"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { fetchNotifications, markAllNotificationsRead, markNotificationRead } from '@/api/notification';

const activeTab = ref<'all' | 'unread'>('all');
const notifications = ref<any[]>([]);
const loading = ref(false);
const page = ref(1);
const pageSize = ref(20);
const total = ref(0);

const loadList = async () => {
  loading.value = true;
  try {
    const isRead = activeTab.value === 'unread' ? 0 : undefined;
    const res: any = await fetchNotifications({ isRead, page: page.value, size: pageSize.value });
    const data = res?.data ?? res;
    notifications.value = Array.isArray(data?.list) ? data.list : [];
    total.value = Number(data?.total ?? 0);
  } catch (error) {
    notifications.value = [];
    total.value = 0;
    console.error('[Notifications] load failed', error);
  } finally {
    loading.value = false;
  }
};

const markRead = async (row: any) => {
  try {
    await markNotificationRead(row.id);
    ElMessage.success('已标记已读');
    loadList();
  } catch (error) {
    ElMessage.error('操作失败');
  }
};

const markAllRead = async () => {
  try {
    await markAllNotificationsRead();
    ElMessage.success('已全部标记已读');
    loadList();
  } catch (error) {
    ElMessage.error('操作失败');
  }
};

const formatTime = (value: any) => {
  if (!value) return '--';
  return String(value).replace('T', ' ').slice(0, 16);
};

onMounted(() => loadList());
</script>

<style scoped>
.notify-page {
  padding: 20px;
  background: #14161a;
  min-height: 100vh;
  color: #fff;
}
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  gap: 16px;
  flex-wrap: wrap;
}
h2 {
  margin: 0;
  font-size: 20px;
}
.subtitle {
  font-size: 12px;
  color: #a0a3af;
  margin-top: 4px;
}
.dark-table {
  --el-table-border-color: #363636;
  --el-table-bg-color: #1d212b;
  --el-table-tr-bg-color: #1d212b;
  --el-table-header-bg-color: #1d212b;
}
:deep(.el-table__inner-wrapper::before) {
  display: none;
}
:deep(.el-table td.el-table__cell),
:deep(.el-table th.el-table__cell.is-leaf) {
  border-bottom: 1px solid #363636;
}
:deep(.el-table--enable-row-hover .el-table__body tr:hover > td.el-table__cell) {
  background-color: #2b303c !important;
}
.pager {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
