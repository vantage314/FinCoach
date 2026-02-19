<template>
  <div class="admin-page">
    <div class="page-header">
      <div>
        <h2>建议规则集</h2>
        <p class="sub">配置再平衡阈值、风险分档与行为评分参数</p>
      </div>
      <div class="header-actions">
        <el-button @click="loadRuleSets" :loading="loadingRuleSets">刷新</el-button>
      </div>
    </div>

    <el-alert
      v-if="error"
      :title="error"
      type="error"
      show-icon
      class="error-banner"
    />

    <el-card class="table-card" shadow="never">
      <div class="card-title">规则集列表</div>
      <el-table
        :data="ruleSets"
        v-loading="loadingRuleSets"
        highlight-current-row
        @row-click="handleSelect"
        empty-text="暂无规则集"
      >
        <el-table-column prop="code" label="Code" min-width="120" />
        <el-table-column prop="version" label="Version" width="100" />
        <el-table-column prop="description" label="Description" min-width="200" />
        <el-table-column label="Enabled" width="120">
          <template #default="{ row }">
            <el-tag v-if="row.enabled === 1" type="success">启用中</el-tag>
            <el-tag v-else type="info">未启用</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="updatedAt" label="Updated" min-width="180" />
        <el-table-column label="操作" width="140">
          <template #default="{ row }">
            <el-button size="small" type="primary" plain @click.stop="handleEnable(row)">
              启用
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card class="table-card" shadow="never">
      <div class="card-title">
        参数编辑
        <span v-if="selectedRuleSet" class="card-sub">{{ selectedRuleSet.code }} v{{ selectedRuleSet.version }}</span>
      </div>
      <el-table
        :data="params"
        v-loading="loadingParams"
        empty-text="请选择规则集"
      >
        <el-table-column prop="paramKey" label="Key" min-width="180" />
        <el-table-column label="Value" min-width="260">
          <template #default="{ row }">
            <el-input
              v-if="isTextType(row.valueType)"
              v-model="row.paramValue"
              type="textarea"
              :rows="2"
              placeholder="请输入参数值"
            />
            <el-input
              v-else-if="isNumericType(row.valueType)"
              v-model="row.paramValue"
              placeholder="请输入数字"
            />
            <el-switch
              v-else-if="row.valueType === 'BOOL'"
              v-model="row.paramValue"
              :active-value="'true'"
              :inactive-value="'false'"
            />
            <el-input v-else v-model="row.paramValue" placeholder="请输入参数值" />
          </template>
        </el-table-column>
        <el-table-column prop="valueType" label="Type" width="120" />
        <el-table-column prop="updatedAt" label="Updated" min-width="180" />
      </el-table>
      <div class="table-footer">
        <el-button @click="loadParams" :disabled="!selectedRuleSet" :loading="loadingParams">刷新参数</el-button>
        <el-button type="primary" @click="saveParams" :disabled="!selectedRuleSet" :loading="savingParams">
          保存参数
        </el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { ElMessage } from 'element-plus';
import {
  fetchAdviceRuleSets,
  enableAdviceRuleSet,
  fetchAdviceRuleParams,
  updateAdviceRuleParams,
} from '@/api/adminAdvice';

const ruleSets = ref<any[]>([]);
const params = ref<any[]>([]);
const selectedRuleSet = ref<any | null>(null);

const loadingRuleSets = ref(false);
const loadingParams = ref(false);
const savingParams = ref(false);
const error = ref('');

const loadRuleSets = async () => {
  loadingRuleSets.value = true;
  error.value = '';
  try {
    const res: any = await fetchAdviceRuleSets();
    ruleSets.value = res?.data || [];
    if (selectedRuleSet.value) {
      const refreshed = ruleSets.value.find((r) => r.id === selectedRuleSet.value.id);
      if (refreshed) {
        selectedRuleSet.value = refreshed;
      }
    }
  } catch (e: any) {
    error.value = e?.message || '加载规则集失败';
  } finally {
    loadingRuleSets.value = false;
  }
};

const handleSelect = (row: any) => {
  selectedRuleSet.value = row;
  loadParams();
};

const loadParams = async () => {
  if (!selectedRuleSet.value) {
    params.value = [];
    return;
  }
  loadingParams.value = true;
  error.value = '';
  try {
    const res: any = await fetchAdviceRuleParams(selectedRuleSet.value.code);
    params.value = res?.data || [];
  } catch (e: any) {
    error.value = e?.message || '加载参数失败';
  } finally {
    loadingParams.value = false;
  }
};

const handleEnable = async (row: any) => {
  try {
    await enableAdviceRuleSet(row.id);
    ElMessage.success('已启用规则集');
    await loadRuleSets();
  } catch (e: any) {
    error.value = e?.message || '启用失败';
  }
};

const saveParams = async () => {
  if (!selectedRuleSet.value) return;
  const invalid = params.value.find((p) => !isValidParam(p));
  if (invalid) {
    ElMessage.error('参数格式不正确');
    return;
  }
  savingParams.value = true;
  try {
    const payload = params.value.map((p) => ({
      paramKey: p.paramKey,
      paramValue: String(p.paramValue ?? ''),
      valueType: p.valueType,
    }));
    await updateAdviceRuleParams(selectedRuleSet.value.code, payload);
    ElMessage.success('参数已保存');
    await loadParams();
  } catch (e: any) {
    error.value = e?.message || '保存参数失败';
  } finally {
    savingParams.value = false;
  }
};

const isNumericType = (valueType: string) => ['INT', 'DECIMAL'].includes(String(valueType).toUpperCase());
const isTextType = (valueType: string) => ['JSON', 'STRING'].includes(String(valueType).toUpperCase());

const isValidParam = (param: any) => {
  const type = String(param.valueType || '').toUpperCase();
  const raw = param.paramValue;
  if (type === 'INT') {
    const num = Number(raw);
    return Number.isInteger(num);
  }
  if (type === 'DECIMAL') {
    const num = Number(raw);
    return !Number.isNaN(num);
  }
  if (type === 'JSON') {
    try {
      JSON.parse(raw || '{}');
      return true;
    } catch {
      return false;
    }
  }
  return true;
};

onMounted(() => {
  loadRuleSets();
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

.header-actions {
  display: flex;
  gap: 12px;
}

.error-banner {
  margin-bottom: 16px;
}

.table-card {
  margin-bottom: 16px;
  background: rgba(30, 41, 59, 0.6);
  border: 1px solid rgba(255, 255, 255, 0.08);
}

.card-title {
  font-size: 14px;
  color: #e2e8f0;
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.card-sub {
  color: #94a3b8;
  font-size: 12px;
}

.table-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding-top: 12px;
}
</style>
