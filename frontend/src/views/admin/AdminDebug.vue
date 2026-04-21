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
        <el-descriptions-item :label="label('UserId')">{{ formatEmpty(snapshot?.userId) }}</el-descriptions-item>
        <el-descriptions-item :label="label('RequestId')">{{ formatEmpty(snapshot?.requestId) }}</el-descriptions-item>
        <el-descriptions-item :label="label('Scores')">{{ scoreSummaryText }}</el-descriptions-item>
        <el-descriptions-item :label="label('DebtCashflow')">{{ debtCashflowText }}</el-descriptions-item>
        <el-descriptions-item :label="label('DebtOptimizer')">{{ debtOptimizerText }}</el-descriptions-item>
        <el-descriptions-item :label="label('InsuranceGap')">{{ insuranceGapText }}</el-descriptions-item>
        <el-descriptions-item :label="label('Alerts')">{{ alertsSummaryText }}</el-descriptions-item>
        <el-descriptions-item :label="label('Correlation')">{{ correlationSummaryText }}</el-descriptions-item>
        <el-descriptions-item :label="label('CorrelationWarnings')" :span="2">{{ correlationWarnings }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-row :gutter="16" class="card-row">
      <el-col :xs="24" :md="12">
        <el-card class="summary-card" shadow="never">
          <template #header>分数摘要</template>
          <el-descriptions :column="1" border>
            <el-descriptions-item :label="label('Risk')">
              {{ snapshot?.scoreSummary?.risk?.value ?? '-' }} / {{ snapshot?.scoreSummary?.risk?.level ?? '-' }}
            </el-descriptions-item>
            <el-descriptions-item :label="label('AssetHealth')">
              {{ snapshot?.scoreSummary?.assetHealth?.value ?? '-' }} / {{ snapshot?.scoreSummary?.assetHealth?.level ?? '-' }}
            </el-descriptions-item>
            <el-descriptions-item :label="label('Behavior')">
              {{ snapshot?.scoreSummary?.behavior?.value ?? '-' }} / {{ snapshot?.scoreSummary?.behavior?.level ?? '-' }}
            </el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>
      <el-col :xs="24" :md="12">
        <el-card class="summary-card" shadow="never">
          <template #header>预警摘要</template>
          <el-descriptions :column="1" border>
            <el-descriptions-item :label="label('OpenCount')">{{ formatEmpty(snapshot?.alertsSummary?.openCount) }}</el-descriptions-item>
            <el-descriptions-item :label="label('CriticalCount')">{{ formatEmpty(snapshot?.alertsSummary?.criticalCount) }}</el-descriptions-item>
            <el-descriptions-item :label="label('TopCodes')">{{ formatEmpty((snapshot?.alertsSummary?.topCodes || []).join(', ')) }}</el-descriptions-item>
            <el-descriptions-item :label="label('LastCreatedAt')">{{ formatEmpty(snapshot?.alertsSummary?.lastCreatedAt) }}</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="summary-card" shadow="never">
      <template #header>Correlation Matrix</template>
      <el-collapse v-if="matrixAssets.length && matrixRows.length" v-model="matrixOpen">
        <el-collapse-item name="matrix" :title="`Matrix (${matrixAssets.length}x${matrixAssets.length})`">
          <div class="matrix-table">
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
        </el-collapse-item>
      </el-collapse>
      <el-empty v-else description="未启用/样本不足" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { fetchMarketDebugLatest } from '@/api/adminDebug';
import { formatEmpty, formatEmptyText, getAdminLabel } from '@/utils/labelMap';

const snapshot = ref<any>(null);
const loading = ref(false);
const error = ref('');
const matrixOpen = ref<string[]>([]);
const label = (key: string) => getAdminLabel(key);

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
  return warnings.length ? warnings.join(', ') : formatEmptyText('');
});

onMounted(() => {
  loadSnapshot();
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

.error-banner {
  margin-bottom: 16px;
}

.summary-card {
  margin-bottom: 16px;
  background: var(--fc-panel);
  border: 1px solid var(--fc-border);
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
  border: 1px solid var(--fc-border);
  padding: 6px 8px;
  text-align: center;
}

.matrix-table th {
  color: var(--fc-text-muted);
  font-weight: 600;
}

.row-label {
  text-align: left;
  color: #e2e8f0;
  font-weight: 600;
}
</style>
