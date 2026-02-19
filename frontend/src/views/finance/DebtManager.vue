<template>
  <div class="finance-page">
    <el-card class="glass-card">
      <template #header>
        <div class="card-header">
          <span>债务管理</span>
        </div>
      </template>
      <el-empty description="页面建设中" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
</script>

<style scoped>
.finance-page {
  padding: 16px;
}
.glass-card {
  background: #1d212b;
  border: 1px solid rgba(255, 255, 255, 0.08);
}
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: #fff;
  font-weight: 600;
}
</style>
