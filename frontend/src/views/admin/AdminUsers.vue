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
        <el-table-column prop="roles" label="角色" min-width="180" />
        <el-table-column prop="enabled" label="状态" width="120" />
        <el-table-column prop="createdAt" label="创建时间" width="180" />
        <el-table-column prop="lastLoginAt" label="最近登录" width="180" />
        <el-table-column label="操作" width="140" fixed="right">
          <template #default>
            <el-button size="small" type="primary" plain disabled>启用/停用</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';

const loading = ref(false);
const items = ref<any[]>([]);
const query = ref('');

const reload = () => {
  // Placeholder for future API wiring
};

const reset = () => {
  query.value = '';
  reload();
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
</style>
