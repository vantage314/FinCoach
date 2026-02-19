<template>
  <div class="finance-page">
    <div class="toolbar">
      <div>
        <h2>债务管理</h2>
        <div class="subtitle">记录当前债务与月供，支持 APR 小数或百分比输入</div>
      </div>
      <el-button type="primary" @click="handleAdd">
        <el-icon><Plus /></el-icon>
        新增债务
      </el-button>
    </div>

    <el-table
      :data="debtList"
      style="width: 100%"
      v-loading="loading"
      class="dark-table"
      :header-cell-style="{ background: '#1d212b', color: '#909399', borderBottom: '1px solid #363636' }"
      :row-style="{ background: 'transparent', color: '#fff' }"
    >
      <el-table-column prop="debtType" label="类型" width="120">
        <template #default="{ row }">
          <el-tag :type="tagType(row.debtType)">{{ formatType(row.debtType) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="apr" label="APR" width="110" align="right">
        <template #default="{ row }">
          {{ formatApr(row.apr) }}
        </template>
      </el-table-column>
      <el-table-column prop="remainingBalance" label="剩余本金" align="right">
        <template #default="{ row }">
          ¥{{ formatMoney(row.remainingBalance) }}
        </template>
      </el-table-column>
      <el-table-column prop="monthlyPayment" label="月供" align="right">
        <template #default="{ row }">
          {{ row.monthlyPayment != null ? `¥${formatMoney(row.monthlyPayment)}` : '--' }}
        </template>
      </el-table-column>
      <el-table-column prop="isActive" label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="row.isActive === 1 ? 'success' : 'info'">
            {{ row.isActive === 1 ? '有效' : '已停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160" align="center">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
          <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑债务' : '新增债务'" width="520px" class="dark-dialog">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
        <el-form-item label="债务类型" prop="debtType">
          <el-select v-model="form.debtType" placeholder="请选择" style="width: 100%">
            <el-option label="房贷" value="MORTGAGE" />
            <el-option label="车贷" value="CAR" />
            <el-option label="消费贷" value="CONSUMER" />
            <el-option label="信用卡" value="CREDITCARD" />
            <el-option label="其他" value="OTHER" />
          </el-select>
        </el-form-item>
        <el-form-item label="年化利率 APR" prop="apr">
          <el-input-number v-model="form.apr" :min="0" :precision="4" style="width: 100%" />
          <div class="form-tip">支持小数(0.12)或百分比(12)</div>
        </el-form-item>
        <el-form-item label="剩余本金" prop="remainingBalance">
          <el-input-number v-model="form.remainingBalance" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="初始本金" prop="principal">
          <el-input-number v-model="form.principal" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="期限(月)" prop="termMonths">
          <el-input-number v-model="form.termMonths" :min="1" :precision="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="月供(可选)" prop="monthlyPayment">
          <el-input-number v-model="form.monthlyPayment" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="起始日期" prop="startDate">
          <el-date-picker v-model="form.startDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="结束日期" prop="endDate">
          <el-date-picker v-model="form.endDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitForm">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Plus } from '@element-plus/icons-vue';
import type { FormInstance, FormRules } from 'element-plus';
import { deleteDebt, listDebts, upsertDebt } from '@/api/debt';

interface DebtForm {
  id: number | null;
  debtType: string;
  principal: number | null;
  apr: number | null;
  termMonths: number | null;
  monthlyPayment: number | null;
  remainingBalance: number | null;
  startDate?: string | null;
  endDate?: string | null;
}

const loading = ref(false);
const debtList = ref<any[]>([]);
const dialogVisible = ref(false);
const formRef = ref<FormInstance>();
const form = ref<DebtForm>({
  id: null,
  debtType: 'CREDITCARD',
  principal: 0,
  apr: 0.12,
  termMonths: null,
  monthlyPayment: null,
  remainingBalance: 0,
  startDate: null,
  endDate: null,
});

const rules: FormRules = {
  debtType: [{ required: true, message: '请选择债务类型', trigger: 'change' }],
  apr: [{ required: true, message: '请输入 APR', trigger: 'blur' }],
  remainingBalance: [{ required: true, message: '请输入剩余本金', trigger: 'blur' }],
  principal: [{ required: true, message: '请输入初始本金', trigger: 'blur' }],
  termMonths: [{ type: 'number', min: 1, message: '期限需大于 0', trigger: 'blur' }],
};

const loadList = async () => {
  loading.value = true;
  try {
    const res: any = await listDebts();
    const data = res?.data ?? res;
    debtList.value = Array.isArray(data) ? data : [];
  } catch (error) {
    debtList.value = [];
    console.error('[DebtManager] load list failed', error);
  } finally {
    loading.value = false;
  }
};

const handleAdd = () => {
  form.value = {
    id: null,
    debtType: 'CREDITCARD',
    principal: 0,
    apr: 0.12,
    termMonths: null,
    monthlyPayment: null,
    remainingBalance: 0,
    startDate: null,
    endDate: null,
  };
  dialogVisible.value = true;
};

const handleEdit = (row: any) => {
  form.value = {
    id: row.id ?? null,
    debtType: row.debtType || 'CREDITCARD',
    principal: toNumber(row.principal),
    apr: toNumber(row.apr),
    termMonths: row.termMonths ?? null,
    monthlyPayment: toNumber(row.monthlyPayment),
    remainingBalance: toNumber(row.remainingBalance),
    startDate: row.startDate || null,
    endDate: row.endDate || null,
  };
  dialogVisible.value = true;
};

const handleDelete = (row: any) => {
  ElMessageBox.confirm('确定删除该债务记录吗?', '提示', { type: 'warning' })
    .then(async () => {
      await deleteDebt(row.id);
      ElMessage.success('删除成功');
      loadList();
    })
    .catch(() => {});
};

const submitForm = async () => {
  if (!formRef.value) return;
  await formRef.value.validate(async (valid) => {
    if (!valid) return;
    try {
      const payload = { ...form.value };
      await upsertDebt(payload);
      ElMessage.success('保存成功');
      dialogVisible.value = false;
      loadList();
    } catch (error) {
      ElMessage.error('保存失败');
    }
  });
};

const formatType = (type: string) => {
  const mapping: Record<string, string> = {
    MORTGAGE: '房贷',
    CAR: '车贷',
    CONSUMER: '消费贷',
    CREDITCARD: '信用卡',
    OTHER: '其他',
  };
  return mapping[type] || type || '--';
};

const tagType = (type: string) => {
  const mapping: Record<string, string> = {
    MORTGAGE: 'warning',
    CAR: 'info',
    CONSUMER: 'success',
    CREDITCARD: 'danger',
    OTHER: '',
  };
  return mapping[type] || '';
};

const formatApr = (apr: any) => {
  if (apr == null) return '--';
  const value = Number(apr);
  if (Number.isNaN(value)) return '--';
  const pct = value <= 1 ? value * 100 : value;
  return `${pct.toFixed(2)}%`;
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
  color: #fff;
}
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
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
.dark-table {
  --el-table-border-color: #363636;
  --el-table-bg-color: #1d212b;
  --el-table-tr-bg-color: #1d212b;
  --el-table-header-bg-color: #1d212b;
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
  color: #fff;
}
:deep(.el-form-item__label) {
  color: #dcdfe6;
}
:deep(.el-input__wrapper),
:deep(.el-select__wrapper),
:deep(.el-input-number__decrease),
:deep(.el-input-number__increase) {
  background-color: #2b303c;
  box-shadow: 0 0 0 1px #4c4d4f inset;
}
:deep(.el-input__inner) {
  color: #fff;
}
.form-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}
</style>
