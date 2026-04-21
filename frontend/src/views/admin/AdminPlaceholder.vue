<template>
  <div class="admin-page">
    <div class="page-header">
      <div>
        <h2>{{ title }}</h2>
        <p class="sub">Wave3 功能占位，后续补齐详细能力</p>
      </div>
      <el-button type="primary" plain @click="router.push('/admin/data-source')">
        前往数据源与抓取
      </el-button>
    </div>

    <el-card class="summary-card" shadow="never">
      <el-empty description="功能建设中" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';

const route = useRoute();
const router = useRouter();

const title = computed(() => {
  const raw = (route.meta.title as string) || '后台功能';
  return raw.replace(' - FinCoach', '');
});
</script>

<style scoped>
.admin-page {
  min-height: 100vh;
  padding: 24px;
  background: var(--fc-bg-gradient);
  color: var(--fc-text);
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
  color: var(--fc-text-muted);
  font-size: 13px;
}

.summary-card {
  background: var(--fc-panel);
  border: 1px solid var(--fc-border);
}
</style>
