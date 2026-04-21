<template>
  <div class="finance-page">
    <div class="toolbar">
      <div>
        <h2>现金流管理</h2>
        <div class="subtitle">按月记录收入与支出，自动计算净结余</div>
      </div>
      <div class="actions">
        <el-date-picker
          v-model="filters.from"
          type="month"
          value-format="YYYY-MM"
          placeholder="开始月份"
          class="filter-item"
        />
        <el-date-picker
          v-model="filters.to"
          type="month"
          value-format="YYYY-MM"
          placeholder="结束月份"
          class="filter-item"
        />
        <el-button type="primary" @click="loadList">查询</el-button>
        <el-upload
          :show-file-list="false"
          :before-upload="handleImport"
          accept=".csv"
        >
          <el-button type="primary" plain :loading="importing">导入 CSV</el-button>
        </el-upload>
        <el-button plain @click="downloadTemplate">下载模板</el-button>
        <el-button type="primary" plain @click="handleAdd">
          <el-icon><Plus /></el-icon>
          记录月份
        </el-button>
      </div>
    </div>

    <div class="charts-row">
      <el-card class="chart-card" shadow="never">
        <template #header>
          <span>净结余趋势</span>
        </template>
        <v-chart class="chart-instance" :option="cashflowTrendOption" autoresize />
      </el-card>
    </div>

    <el-table
      :data="cashflowList"
      style="width: 100%"
      v-loading="loading"
      class="dark-table"
      :header-cell-style="{ background: '#1d212b', color: '#909399', borderBottom: '1px solid #363636' }"
      :row-style="{ background: 'transparent', color: '#fff' }"
    >
      <el-table-column prop="month" label="月份" width="120" />
      <el-table-column prop="income" label="收入" align="right">
        <template #default="{ row }">¥{{ formatMoney(row.income) }}</template>
      </el-table-column>
      <el-table-column prop="expense" label="支出" align="right">
        <template #default="{ row }">¥{{ formatMoney(row.expense) }}</template>
      </el-table-column>
      <el-table-column prop="net" label="净结余" align="right">
        <template #default="{ row }">
          <span :class="Number(row.net) >= 0 ? 'positive' : 'negative'">
            ¥{{ formatMoney(row.net) }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120" align="center">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑现金流' : '新增现金流'" width="480px" class="dark-dialog">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="月份" prop="month">
          <el-date-picker v-model="form.month" type="month" value-format="YYYY-MM" style="width: 100%" />
        </el-form-item>
        <el-form-item label="收入" prop="income">
          <el-input-number v-model="form.income" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="支出" prop="expense">
          <el-input-number v-model="form.expense" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="净结余">
          <el-input :model-value="netPreview" disabled />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitForm">确定</el-button>
        </span>
      </template>
    </el-dialog>

    <el-dialog v-model="importDialogVisible" title="导入结果" width="720px" class="dark-dialog">
      <div class="import-summary">
        <div>总行数：{{ importResult.totalRows }}</div>
        <div>成功：{{ importResult.successRows }}</div>
        <div>失败：{{ importResult.errorRows }}</div>
      </div>
      <el-table
        v-if="importErrors.length > 0"
        :data="importErrors"
        style="width: 100%"
        class="dark-table"
        :header-cell-style="{ background: '#1d212b', color: '#909399', borderBottom: '1px solid #363636' }"
        :row-style="{ background: 'transparent', color: '#fff' }"
      >
        <el-table-column prop="row" label="行号" width="80" />
        <el-table-column prop="field" label="字段" width="140" />
        <el-table-column label="错误信息">
          <template #default="{ row }">
            {{ row.message || row.reason || row.error || '--' }}
          </template>
        </el-table-column>
        <el-table-column prop="raw" label="原始行" />
      </el-table>
      <template #footer>
        <span class="dialog-footer">
          <el-button type="primary" @click="importDialogVisible = false">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import { Plus } from '@element-plus/icons-vue';
import type { FormInstance, FormRules } from 'element-plus';
import { listCashflowMonths, upsertCashflowMonth, importCashflowCsv } from '@/api/cashflow';
import { use } from 'echarts/core';
import { CanvasRenderer } from 'echarts/renderers';
import { LineChart } from 'echarts/charts';
import { GridComponent, TooltipComponent } from 'echarts/components';
import VChart from 'vue-echarts';

use([CanvasRenderer, LineChart, GridComponent, TooltipComponent]);

interface CashflowForm {
  id?: number | null;
  month: string | null;
  income: number | null;
  expense: number | null;
}

const loading = ref(false);
const cashflowList = ref<any[]>([]);
const dialogVisible = ref(false);
const importing = ref(false);
const importDialogVisible = ref(false);
const importResult = ref({ totalRows: 0, successRows: 0, errorRows: 0 });
const importErrors = ref<any[]>([]);
const formRef = ref<FormInstance>();
const filters = ref<{ from: string | null; to: string | null }>({ from: null, to: null });

const form = ref<CashflowForm>({
  id: null,
  month: null,
  income: 0,
  expense: 0,
});

const rules: FormRules = {
  month: [{ required: true, message: '请选择月份', trigger: 'change' }],
  income: [{ required: true, message: '请输入收入', trigger: 'blur' }],
  expense: [{ required: true, message: '请输入支出', trigger: 'blur' }],
};

const netPreview = computed(() => {
  const income = Number(form.value.income ?? 0);
  const expense = Number(form.value.expense ?? 0);
  const net = income - expense;
  return `¥${formatMoney(net)}`;
});

const cashflowTrendOption = computed(() => {
  const sorted = [...cashflowList.value].sort((a, b) => String(a.month).localeCompare(String(b.month)));
  const months = sorted.map((item) => item.month);
  const netValues = sorted.map((item) => Number(item.net ?? Number(item.income || 0) - Number(item.expense || 0)));
  return {
    grid: { left: '8%', right: '6%', top: '12%', bottom: '12%' },
    tooltip: { trigger: 'axis' },
    xAxis: {
      type: 'category',
      data: months,
      axisLabel: { color: '#cbd5f5' },
    },
    yAxis: {
      type: 'value',
      axisLabel: { color: '#cbd5f5' },
    },
    series: [
      {
        type: 'line',
        data: netValues,
        smooth: true,
        areaStyle: { opacity: 0.2 },
        lineStyle: { width: 2 },
      },
    ],
  };
});

const loadList = async () => {
  loading.value = true;
  try {
    const res: any = await listCashflowMonths({
      from: filters.value.from || undefined,
      to: filters.value.to || undefined,
    });
    const data = res?.data ?? res;
    cashflowList.value = Array.isArray(data) ? data : [];
  } catch (error) {
    cashflowList.value = [];
    console.error('[CashflowManager] load list failed', error);
  } finally {
    loading.value = false;
  }
};

const handleAdd = () => {
  form.value = {
    id: null,
    month: null,
    income: 0,
    expense: 0,
  };
  dialogVisible.value = true;
};

const handleImport = async (file: File) => {
  importing.value = true;
  try {
    const res: any = await importCashflowCsv(file);
    const data = res?.data ?? res;
    const successRows = Number(data?.successRows ?? data?.successCount ?? 0);
    const errorRows = Number(data?.errorRows ?? data?.failCount ?? 0);
    const totalRows = Number(data?.totalRows ?? successRows + errorRows);
    const errors = data?.errors ?? data?.failures ?? [];
    importResult.value = { totalRows, successRows, errorRows };
    importErrors.value = Array.isArray(errors) ? errors : [];
    importDialogVisible.value = true;
    ElMessage.success('导入完成');
    loadList();
  } catch (error) {
    ElMessage.error('导入失败');
  } finally {
    importing.value = false;
  }
  return false;
};

const downloadTemplate = () => {
  const header = 'month,income,expense';
  const sample = '2026-01,12000,8000';
  const csv = `${header}\n${sample}\n`;
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = 'cashflow_import_template.csv';
  link.click();
  URL.revokeObjectURL(url);
};

const handleEdit = (row: any) => {
  form.value = {
    id: row.id ?? null,
    month: row.month ?? null,
    income: toNumber(row.income),
    expense: toNumber(row.expense),
  };
  dialogVisible.value = true;
};

const submitForm = async () => {
  if (!formRef.value) return;
  await formRef.value.validate(async (valid) => {
    if (!valid) return;
    try {
      const payload = {
        month: form.value.month,
        income: Number(form.value.income ?? 0),
        expense: Number(form.value.expense ?? 0),
      };
      await upsertCashflowMonth(payload);
      ElMessage.success('保存成功');
      dialogVisible.value = false;
      loadList();
    } catch (error) {
      ElMessage.error('保存失败');
    }
  });
};

const formatMoney = (val: any) =>
  val != null ? Number(val).toLocaleString('en-US', { minimumFractionDigits: 2 }) : '0.00';

const toNumber = (val: any) => {
  if (val == null) return null;
  const num = Number(val);
  return Number.isNaN(num) ? null : num;
};

onMounted(() => loadList());
</script>

<style scoped>
.finance-page {
  padding: 20px;
  background: #14161a;
  min-height: 100vh;
  color: var(--fc-text);
}
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  gap: 16px;
  flex-wrap: wrap;
}
.actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
h2 {
  margin: 0;
  font-size: 20px;
}
.subtitle {
  font-size: 12px;
  color: #a0a3af;
  margin-top: 4px;
}
.filter-item {
  width: 140px;
}
.dark-table {
  --el-table-border-color: #363636;
  --el-table-bg-color: #1d212b;
  --el-table-tr-bg-color: #1d212b;
  --el-table-header-bg-color: #1d212b;
}
:deep(.chart-card) {
  background: #1d212b;
  border: 1px solid var(--fc-border);
  margin-bottom: 20px;
}
.charts-row {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 16px;
  margin-bottom: 20px;
}
.chart-instance {
  width: 100%;
  height: 260px;
}
:deep(.el-table__inner-wrapper::before) {
  display: none;
}
:deep(.el-table td.el-table__cell),
:deep(.el-table th.el-table__cell.is-leaf) {
  border-bottom: 1px solid #363636;
}
:deep(.el-table--enable-row-hover .el-table__body tr:hover > td.el-table__cell) {
  background-color: #2b303c !important;
}
:deep(.el-dialog) {
  background: #1d212b;
}
:deep(.el-dialog__title) {
  color: var(--fc-text);
}
:deep(.el-form-item__label) {
  color: #dcdfe6;
}
:deep(.el-input__wrapper),
:deep(.el-input-number__decrease),
:deep(.el-input-number__increase) {
  background-color: #2b303c;
  box-shadow: 0 0 0 1px #4c4d4f inset;
}
:deep(.el-input__inner) {
  color: var(--fc-text);
}
.positive {
  color: #67c23a;
}
.negative {
  color: #f56c6c;
}
.import-summary {
  display: flex;
  gap: 20px;
  margin-bottom: 16px;
  color: #cbd5f5;
}
</style>
