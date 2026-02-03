<template>
  <div class="dashboard">
    <h1 style="color: white; padding: 40px;">Dashboard Placeholder</h1>
    <p style="color: #94a3b8; padding: 0 40px;">您已成功登录。</p>
    <el-button @click="handleLogout" style="margin-left: 40px;">退出登录</el-button>
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
