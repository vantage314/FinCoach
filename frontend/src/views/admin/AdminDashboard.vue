<template>
  <div class="admin-page">
    <div class="page-header">
      <div>
        <h2>仪表盘</h2>
        <p class="sub">后台概览与关键指标</p>
      </div>
      <el-button type="primary" :loading="loading" @click="reload">
        刷新
      </el-button>
    </div>

    <el-row :gutter="16" class="card-row">
      <el-col :xs="24" :md="6" v-for="card in cards" :key="card.key">
        <el-card class="metric-card" shadow="never" v-loading="loading">
          <div class="metric-label">{{ card.label }}</div>
          <div class="metric-value">{{ card.value }}</div>
          <div class="metric-sub">{{ card.sub }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="table-card" shadow="never" v-loading="loading">
      <template #header>最新预警</template>
      <el-table :data="alerts" style="width: 100%" empty-text="暂无数据">
        <el-table-column prop="title" label="预警" min-width="200" />
        <el-table-column prop="level" label="等级" width="120" />
        <el-table-column prop="createdAt" label="时间" width="180" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';

const loading = ref(false);
const alerts = ref<any[]>([]);

const cards = ref([
  { key: 'users', label: '用户数', value: '--', sub: '全部用户' },
  { key: 'alerts', label: '未处理预警', value: '--', sub: '待处理' },
  { key: 'crawler', label: '爬虫心跳', value: '--', sub: '最近一次' },
  { key: 'reports', label: '最近体检', value: '--', sub: '最新报告' },
]);

const reload = () => {
  // Placeholder for future API wiring
};
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

.card-row {
  margin-bottom: 16px;
}

.metric-card {
  background: rgba(30, 41, 59, 0.6);
  border: 1px solid rgba(255, 255, 255, 0.08);
}

.metric-label {
  color: #94a3b8;
  font-size: 13px;
}

.metric-value {
  font-size: 26px;
  font-weight: 700;
  margin: 10px 0 4px;
}

.metric-sub {
  color: #64748b;
  font-size: 12px;
}

.table-card {
  background: rgba(30, 41, 59, 0.6);
  border: 1px solid rgba(255, 255, 255, 0.08);
}
</style>
