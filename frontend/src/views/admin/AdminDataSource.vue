<template>
  <div class="admin-page">
    <div class="page-header">
      <div>
        <h2>数据源与抓取</h2>
        <p class="sub">切换演示数据或实时抓取，并查看任务状态</p>
      </div>
      <el-button type="primary" @click="loadStatus" :loading="loading">
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

    <el-card class="summary-card" shadow="never" v-loading="loading">
      <template #header>当前模式</template>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="Mode">
          <el-tag :type="modeTag">{{ mode }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="说明">
          {{ modeDescription }}
        </el-descriptions-item>
      </el-descriptions>
      <div class="action-row">
        <el-button
          type="primary"
          :loading="actionLoading === 'demo'"
          :disabled="mode === 'DEMO_DB'"
          @click="switchMode('DEMO_DB')"
        >
          切换到 DEMO_DB
        </el-button>
        <el-button
          type="warning"
          :loading="actionLoading === 'realtime'"
          :disabled="mode === 'REALTIME'"
          @click="switchMode('REALTIME')"
        >
          切换到 REALTIME
        </el-button>
        <el-button
          type="success"
          plain
          :loading="actionLoading === 'import'"
          @click="importDemo"
        >
          导入演示数据
        </el-button>
      </div>
    </el-card>

    <el-card class="summary-card" shadow="never" v-loading="loading">
      <template #header>抓取任务状态</template>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="Job">{{ job.jobName || 'PY_MARKET_CRAWLER' }}</el-descriptions-item>
        <el-descriptions-item label="Status">
          <el-tag :type="jobStatusTag">{{ job.status || 'STOPPED' }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="LastStartAt">{{ job.lastStartAt || '-' }}</el-descriptions-item>
        <el-descriptions-item label="LastHeartbeatAt">{{ job.lastHeartbeatAt || '-' }}</el-descriptions-item>
        <el-descriptions-item label="LastEndAt">{{ job.lastEndAt || '-' }}</el-descriptions-item>
        <el-descriptions-item label="UpdatedAt">{{ job.updatedAt || '-' }}</el-descriptions-item>
      </el-descriptions>

      <div class="action-row">
        <el-button
          type="primary"
          :loading="actionLoading === 'start'"
          :disabled="job.status === 'RUNNING'"
          @click="startJob"
        >
          启动抓取
        </el-button>
        <el-button
          type="danger"
          :loading="actionLoading === 'stop'"
          :disabled="job.status !== 'RUNNING'"
          @click="stopJob"
        >
          停止抓取
        </el-button>
      </div>

      <el-alert
        v-if="job.lastError"
        :title="`最近错误: ${job.lastError}`"
        type="warning"
        show-icon
        class="error-banner"
      />
    </el-card>

    <el-card class="summary-card" shadow="never">
      <template #header>使用提示</template>
      <ul class="tips">
        <li>DEMO_DB：使用内置演示快照数据，适合演示相关性矩阵与报告模块。</li>
        <li>REALTIME：切换后需启动抓取任务，当前为安全占位模式。</li>
        <li>导入演示数据可重复执行，已存在快照会自动跳过。</li>
      </ul>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import {
  fetchDataSourceStatus,
  switchDataSourceMode,
  importDemoData,
  startRealtimeJob,
  stopRealtimeJob,
} from '@/api/adminDataSource';

const status = ref<any>(null);
const loading = ref(false);
const error = ref('');
const actionLoading = ref('');

const loadStatus = async () => {
  loading.value = true;
  error.value = '';
  try {
    const res: any = await fetchDataSourceStatus();
    status.value = res?.data || null;
  } catch (e: any) {
    error.value = e?.message || '加载状态失败';
  } finally {
    loading.value = false;
  }
};

const runAction = async (key: string, action: () => Promise<any>) => {
  actionLoading.value = key;
  error.value = '';
  try {
    const res: any = await action();
    if (key === 'import') {
      const inserted = res?.data?.insertedSnapshots ?? 0;
      const skipped = res?.data?.skippedSnapshots ?? 0;
      ElMessage.success(`演示数据导入完成，新增 ${inserted} 条，跳过 ${skipped} 条`);
    } else {
      ElMessage.success('操作已提交');
    }
    await loadStatus();
  } catch (e: any) {
    error.value = e?.message || '操作失败';
  } finally {
    actionLoading.value = '';
  }
};

const switchMode = async (mode: string) => {
  await runAction(mode === 'DEMO_DB' ? 'demo' : 'realtime', () => switchDataSourceMode(mode));
};

const importDemo = async () => {
  await runAction('import', () => importDemoData());
};

const startJob = async () => {
  await runAction('start', () => startRealtimeJob());
};

const stopJob = async () => {
  await runAction('stop', () => stopRealtimeJob());
};

const mode = computed(() => (status.value?.mode || 'DEMO_DB').toUpperCase());
const job = computed(() => status.value?.job || {});

const modeDescription = computed(() => {
  if (mode.value === 'REALTIME') {
    return '实时抓取模式（需要启动抓取任务）';
  }
  return '演示数据库模式（使用演示快照）';
});

const modeTag = computed(() => (mode.value === 'REALTIME' ? 'warning' : 'success'));
const jobStatusTag = computed(() => {
  const state = (job.value?.status || 'STOPPED').toUpperCase();
  if (state === 'RUNNING') return 'success';
  if (state === 'FAILED') return 'danger';
  return 'info';
});

onMounted(() => {
  loadStatus();
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

.error-banner {
  margin-bottom: 16px;
}

.summary-card {
  margin-bottom: 16px;
  background: rgba(30, 41, 59, 0.6);
  border: 1px solid rgba(255, 255, 255, 0.08);
}

.action-row {
  margin-top: 16px;
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.tips {
  margin: 0;
  padding-left: 16px;
  color: #cbd5f5;
  font-size: 13px;
  line-height: 1.6;
}
</style>
