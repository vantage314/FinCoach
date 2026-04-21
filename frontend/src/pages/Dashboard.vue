<template>
  <div class="dashboard" style="padding: 40px;">
    <h1 style="color: var(--fc-text-strong);">Dashboard Placeholder</h1>
    <p style="color: var(--fc-text-muted);">您已成功登录。</p>
    <el-button @click="handleLogout" style="margin-top: 16px;">退出登录</el-button>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';

const router = useRouter();

const handleLogout = () => {
  localStorage.removeItem('token');
  ElMessage.success('已退出登录');
  router.push('/login');
};
</script>
