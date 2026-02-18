<template>
  <div class="admin-page">
    <div class="page-header">
      <div>
        <h2>Debug 快照</h2>
        <p class="sub">市场调试快照与关键摘要</p>
      </div>
      <el-button type="primary" @click="loadSnapshot">刷新</el-button>
    </div>

    <el-alert
      v-if="error"
      :title="error"
      type="error"
      show-icon
      class="error-banner"
    />

    <el-card class="summary-card" shadow="never" v-loading="loading">
      <template #header>核心摘要</template>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="UserId">{{ snapshot?.userId ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="RequestId">{{ snapshot?.requestId ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="Scores">{{ scoreSummaryText }}</el-descriptions-item>
        <el-descriptions-item label="DebtCashflow">{{ debtCashflowText }}</el-descriptions-item>
        <el-descriptions-item label="DebtOptimizer">{{ debtOptimizerText }}</el-descriptions-item>
        <el-descriptions-item label="InsuranceGap">{{ insuranceGapText }}</el-descriptions-item>
        <el-descriptions-item label="Alerts">{{ alertsSummaryText }}</el-descriptions-item>
        <el-descriptions-item label="Correlation">{{ correlationSummaryText }}</el-descriptions-item>
        <el-descriptions-item label="CorrelationWarnings" :span="2">{{ correlationWarnings }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-row :gutter="16" class="card-row">
      <el-col :xs="24" :md="12">
        <el-card class="summary-card" shadow="never">
          <template #header>分数摘要</template>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="Risk">
              {{ snapshot?.scoreSummary?.risk?.value ?? '-' }} / {{ snapshot?.scoreSummary?.risk?.level ?? '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="AssetHealth">
              {{ snapshot?.scoreSummary?.assetHealth?.value ?? '-' }} / {{ snapshot?.scoreSummary?.assetHealth?.level ?? '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="Behavior">
              {{ snapshot?.scoreSummary?.behavior?.value ?? '-' }} / {{ snapshot?.scoreSummary?.behavior?.level ?? '-' }}
            </el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>
      <el-col :xs="24" :md="12">
        <el-card class="summary-card" shadow="never">
          <template #header>预警摘要</template>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="OpenCount">{{ snapshot?.alertsSummary?.openCount ?? '-' }}</el-descriptions-item>
            <el-descriptions-item label="CriticalCount">{{ snapshot?.alertsSummary?.criticalCount ?? '-' }}</el-descriptions-item>
            <el-descriptions-item label="TopCodes">{{ (snapshot?.alertsSummary?.topCodes || []).join(', ') || '-' }}</el-descriptions-item>
            <el-descriptions-item label="LastCreatedAt">{{ snapshot?.alertsSummary?.lastCreatedAt ?? '-' }}</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="summary-card" shadow="never">
      <template #header>Correlation Matrix</template>
      <div v-if="matrixAssets.length && matrixRows.length" class="matrix-table">
        <table>
          <thead>
            <tr>
              <th></th>
              <th v-for="asset in matrixAssets" :key="asset">{{ asset }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(row, rowIndex) in matrixRows" :key="rowIndex">
              <td class="row-label">{{ matrixAssets[rowIndex] || `#${rowIndex + 1}` }}</td>
              <td v-for="(value, colIndex) in row" :key="colIndex">
                {{ formatNum(value) }}
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <el-empty v-else description="未启用/样本不足" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { fetchMarketDebugLatest } from '@/api/adminDebug';

const snapshot = ref<any>(null);
const loading = ref(false);
const error = ref('');

const loadSnapshot = async () => {
  loading.value = true;
  error.value = '';
  try {
    const res: any = await fetchMarketDebugLatest(true);
    snapshot.value = res?.data || null;
  } catch (e: any) {
    error.value = e?.message || '加载快照失败';
  } finally {
    loading.value = false;
  }
};

const formatNum = (value: any) => {
  if (value === null || value === undefined || Number.isNaN(Number(value))) return '-';
  return Number(value).toFixed(2);
};

const matrixAssets = computed(() => snapshot.value?.correlationMatrix?.assets || []);
const matrixRows = computed(() => snapshot.value?.correlationMatrix?.matrix || []);

const scoreSummaryText = computed(() => {
  const risk = snapshot.value?.scoreSummary?.risk?.value;
  const asset = snapshot.value?.scoreSummary?.assetHealth?.value;
  const behavior = snapshot.value?.scoreSummary?.behavior?.value;
  if (risk == null && asset == null && behavior == null) return '-';
  return `R:${risk ?? '-'} A:${asset ?? '-'} B:${behavior ?? '-'}`;
});

const debtCashflowText = computed(() => {
  const dti = snapshot.value?.debtCashflowSummary?.dti;
  const surplus = snapshot.value?.debtCashflowSummary?.surplusRate;
  if (dti == null && surplus == null) return '-';
  return `DTI:${formatNum(dti)} Surplus:${formatNum(surplus)}`;
});

const debtOptimizerText = computed(() => {
  const strategy = snapshot.value?.debtOptimizerSummary?.strategy;
  const budget = snapshot.value?.debtOptimizerSummary?.budgetForExtraPayment;
  if (!strategy && budget == null) return '-';
  return `${strategy || '-'} / ${formatNum(budget)}`;
});

const insuranceGapText = computed(() => {
  const ratio = snapshot.value?.insuranceGapSummary?.premiumRatio;
  const topGap = snapshot.value?.insuranceGapSummary?.topGapType;
  if (ratio == null && !topGap) return '-';
  return `Ratio:${formatNum(ratio)} Top:${topGap || '-'}`;
});

const alertsSummaryText = computed(() => {
  const open = snapshot.value?.alertsSummary?.openCount;
  const critical = snapshot.value?.alertsSummary?.criticalCount;
  if (open == null && critical == null) return '-';
  return `Open:${open ?? '-'} Critical:${critical ?? '-'}`;
});

const correlationSummaryText = computed(() => {
  const assets = snapshot.value?.correlationMatrixSummary?.assetsCount;
  const sample = snapshot.value?.correlationMatrixSummary?.sampleSize;
  if (assets == null && sample == null) return '-';
  return `Assets:${assets ?? '-'} Sample:${sample ?? '-'}`;
});

const correlationWarnings = computed(() => {
  const warnings = snapshot.value?.correlationMatrixSummary?.warnings || [];
  return warnings.length ? warnings.join(', ') : '暂无';
});

onMounted(() => {
  loadSnapshot();
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

.card-row {
  margin-bottom: 8px;
}

.matrix-table {
  overflow-x: auto;
}

.matrix-table table {
  width: 100%;
  border-collapse: collapse;
  font-size: 12px;
}

.matrix-table th,
.matrix-table td {
  border: 1px solid rgba(255, 255, 255, 0.08);
  padding: 6px 8px;
  text-align: center;
}

.matrix-table th {
  color: #94a3b8;
  font-weight: 600;
}

.row-label {
  text-align: left;
  color: #e2e8f0;
  font-weight: 600;
}
</style>
