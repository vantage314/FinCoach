<template>
  <div class="forbidden-page">
    <el-result
      icon="warning"
      title="无权限访问"
      sub-title="当前账号没有访问该页面的权限"
    >
      <template #extra>
        <el-button type="primary" @click="router.push('/dashboard')">返回首页</el-button>
      </template>
    </el-result>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router';

const router = useRouter();
</script>

<style scoped>
.forbidden-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #0f172a, #1e293b);
  color: #fff;
}
</style>
