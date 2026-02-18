<template>
  <div class="admin-page">
    <div class="page-header">
      <div>
        <h2>数据质量</h2>
        <p class="sub">查看映射缺失、覆盖不足与异常波动</p>
      </div>
      <el-button type="primary" @click="loadQuality" :loading="loading">
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

    <el-card class="table-card" shadow="never">
      <template #header>缺失映射</template>
      <el-table
        :data="quality.missingMappings"
        style="width: 100%"
        v-loading="loading"
        empty-text="暂无缺失映射"
      >
        <el-table-column prop="assetKey" label="AssetKey" min-width="180" />
        <el-table-column prop="reason" label="原因" min-width="220" />
      </el-table>
      <div class="card-tip">
        建议下一步：{{ missingSuggestion }}
      </div>
    </el-card>

    <el-card class="summary-card" shadow="never">
      <template #header>覆盖不足 / 滞后</template>
      <el-descriptions :column="4" border>
        <el-descriptions-item label="资产数">
          {{ quality.snapshotCoverage?.assetsCount ?? 0 }}
        </el-descriptions-item>
        <el-descriptions-item label="覆盖天数">
          {{ quality.snapshotCoverage?.daysCovered ?? 0 }}
        </el-descriptions-item>
        <el-descriptions-item label="最新日期">
          {{ quality.snapshotCoverage?.latestDate || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="滞后天数">
          {{ quality.snapshotCoverage?.lagDays ?? 0 }}
        </el-descriptions-item>
      </el-descriptions>
      <div class="issues">
        <div class="issues-title">Issues</div>
        <ul>
          <li v-for="item in quality.snapshotCoverage?.issues || []" :key="item">
            {{ item }}
          </li>
        </ul>
      </div>
      <div class="card-tip">
        建议下一步：{{ coverageSuggestion }}
      </div>
    </el-card>

    <el-card class="table-card" shadow="never">
      <template #header>异常波动</template>
      <el-table
        :data="quality.anomalies"
        style="width: 100%"
        v-loading="loading"
        empty-text="暂无异常波动"
      >
        <el-table-column prop="assetKey" label="AssetKey" min-width="160" />
        <el-table-column prop="date" label="日期" width="140" />
        <el-table-column label="Change%" width="140">
          <template #default="{ row }">
            {{ formatPct(row.changePct) }}
          </template>
        </el-table-column>
        <el-table-column prop="reason" label="原因" min-width="220" />
      </el-table>
      <div class="card-tip">
        建议下一步：{{ anomalySuggestion }}
      </div>
    </el-card>

    <el-card class="summary-card" shadow="never">
      <template #header>总体建议</template>
      <ul class="tips">
        <li v-for="rec in quality.recommendations || []" :key="rec">
          {{ rec }}
        </li>
      </ul>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { fetchSecuritiesQuality } from '@/api/adminSecurities';

const loading = ref(false);
const error = ref('');
const quality = ref<any>({
  missingMappings: [],
  snapshotCoverage: { assetsCount: 0, daysCovered: 0, latestDate: '', lagDays: 0, issues: [] },
  anomalies: [],
  recommendations: [],
});

const loadQuality = async () => {
  loading.value = true;
  error.value = '';
  try {
    const res: any = await fetchSecuritiesQuality();
    quality.value = res?.data || quality.value;
  } catch (e: any) {
    error.value = e?.message || '加载质量数据失败';
  } finally {
    loading.value = false;
  }
};

const missingSuggestion = computed(() => {
  if (!quality.value?.missingMappings?.length) return '暂无缺失，可继续观察';
  return '前往 证券映射 页面补齐缺失映射';
});

const coverageSuggestion = computed(() => {
  const issues: string[] = quality.value?.snapshotCoverage?.issues || [];
  if (!issues.length) return '覆盖正常，可继续观察';
  if (issues.some((i) => i.includes('没有快照数据'))) {
    return '前往 行情快照 页面导入演示数据';
  }
  if (issues.some((i) => i.includes('覆盖天数不足'))) {
    return '补充快照样本或启动抓取任务';
  }
  if (issues.some((i) => i.includes('滞后'))) {
    return '检查数据源刷新与任务状态';
  }
  return '检查快照数据来源';
});

const anomalySuggestion = computed(() => {
  if (!quality.value?.anomalies?.length) return '暂无异常波动';
  return '复核异常资产的行情来源与输入';
});

const formatPct = (value: number) => {
  if (typeof value !== 'number') return '-';
  return `${(value * 100).toFixed(2)}%`;
};

onMounted(() => {
  loadQuality();
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

.summary-card,
.table-card {
  margin-bottom: 16px;
  background: rgba(30, 41, 59, 0.6);
  border: 1px solid rgba(255, 255, 255, 0.08);
}

.issues {
  margin-top: 16px;
  color: #e2e8f0;
  font-size: 13px;
}

.issues-title {
  font-weight: 600;
  margin-bottom: 6px;
}

.issues ul {
  margin: 0;
  padding-left: 16px;
}

.card-tip {
  margin-top: 12px;
  font-size: 13px;
  color: #cbd5f5;
}

.tips {
  margin: 0;
  padding-left: 16px;
  color: #cbd5f5;
  font-size: 13px;
  line-height: 1.6;
}
</style>
