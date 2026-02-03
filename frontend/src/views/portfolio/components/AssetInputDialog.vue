<template>
  <el-dialog
    v-model="visible"
    title="录入新资产"
    width="500px"
    center
    append-to-body
    class="asset-dialog"
    :before-close="handleClose"
  >
    <el-form 
      ref="formRef" 
      :model="form" 
      :rules="rules" 
      label-position="top"
      class="asset-form"
    >
      <!-- 主分类选择 -->
      <el-form-item label="资产类型" prop="categoryId">
        <el-radio-group v-model="form.categoryId" class="category-group">
          <el-radio-button 
            v-for="cat in categories" 
            :key="cat.id" 
            :value="cat.id"
          >
            {{ cat.name }}
          </el-radio-button>
        </el-radio-group>
      </el-form-item>

      <!-- 子类型选择 (仅金融投资显示) -->
      <el-form-item v-if="form.categoryId === 2" label="投资品种" prop="subType">
        <el-radio-group v-model="form.subType" size="small" class="subtype-group">
          <el-radio-button value="股票">股票</el-radio-button>
          <el-radio-button value="基金">基金</el-radio-button>
          <el-radio-button value="债券">债券</el-radio-button>
        </el-radio-group>
      </el-form-item>

      <!-- 资产名称 -->
      <el-form-item label="资产名称" prop="assetName">
        <el-input 
          v-model="form.assetName" 
          :placeholder="getAssetNamePlaceholder()"
          maxlength="50"
          show-word-limit
        />
      </el-form-item>

      <!-- 金额 (突出显示) -->
      <el-form-item :label="getAmountLabel()" prop="currentValue">
        <el-input
          v-model.number="form.currentValue"
          type="number"
          placeholder="请输入金额"
          class="amount-input"
        >
          <template #prefix>
            <span class="currency-prefix">¥</span>
          </template>
          <template #suffix>
            <span class="currency-suffix">元</span>
          </template>
        </el-input>
      </el-form-item>

      <!-- 持有成本 (金融投资/固定资产显示) -->
      <el-form-item v-if="showHoldingCost" label="持有成本 (选填)">
        <el-input
          v-model.number="form.holdingCost"
          type="number"
          placeholder="可选"
          class="full-width"
        >
          <template #prefix>
            <span class="currency-prefix">¥</span>
          </template>
        </el-input>
      </el-form-item>

      <!-- 资产代码 (仅金融投资显示) -->
      <el-form-item v-if="form.categoryId === 2" label="资产代码 (选填)">
        <el-input 
          v-model="form.assetCode" 
          placeholder="如股票代码：600519"
          maxlength="20"
        />
      </el-form-item>

      <el-form-item>
        <el-button 
          type="primary" 
          class="submit-btn"
          :loading="loading"
          :disabled="!canSubmit"
          @click="handleSubmit"
        >
          确认录入
        </el-button>
      </el-form-item>
    </el-form>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, watch, computed, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import { getCategories, addAsset, type AssetCategory, type AssetItemDTO } from '@/api/asset';

const props = defineProps<{
  modelValue: boolean;
}>();

const emit = defineEmits(['update:modelValue', 'success']);

const visible = ref(props.modelValue);
const formRef = ref();
const loading = ref(false);
const categories = ref<AssetCategory[]>([]);

const form = reactive<AssetItemDTO>({
  categoryId: 1,
  subType: '',
  assetName: '',
  currentValue: 0,
  holdingCost: undefined,
  assetCode: ''
});

const rules = {
  categoryId: [{ required: true, message: '请选择资产类型', trigger: 'change' }],
  assetName: [
    { required: true, message: '请输入资产名称', trigger: 'blur' },
    { min: 2, max: 50, message: '长度在 2 到 50 个字符', trigger: 'blur' }
  ],
  currentValue: [
    { required: true, message: '请输入金额', trigger: 'blur' }
  ],
  subType: [
    { 
      validator: (_rule: any, value: string, callback: Function) => {
        if (form.categoryId === 2 && !value) {
          callback(new Error('请选择投资品种'));
        } else {
          callback();
        }
      }, 
      trigger: 'change' 
    }
  ]
};

// 动态显示逻辑
const showHoldingCost = computed(() => {
  return form.categoryId === 2 || form.categoryId === 3;
});

// 提交按钮是否可用
const canSubmit = computed(() => {
  if (!form.categoryId || !form.assetName || !form.currentValue) return false;
  if (form.categoryId === 2 && !form.subType) return false;
  return true;
});

// 动态标签和占位符
const getAssetNamePlaceholder = () => {
  const placeholders: Record<number, string> = {
    1: '如：招商银行储蓄卡、微信零钱',
    2: '如：贵州茅台、易方达蓝筹精选',
    3: '如：北京朝阳区住宅、特斯拉Model Y'
  };
  return placeholders[form.categoryId] || '请输入资产名称';
};

const getAmountLabel = () => {
  const labels: Record<number, string> = {
    1: '当前余额',
    2: '当前市值',
    3: '当前估值'
  };
  return labels[form.categoryId] || '总金额';
};

// 切换分类时重置字段
watch(() => form.categoryId, () => {
  form.subType = '';
  form.assetCode = '';
  if (form.categoryId === 1) {
    form.holdingCost = undefined;
  }
});

watch(() => props.modelValue, (val) => {
  visible.value = val;
});

watch(visible, (val) => {
  emit('update:modelValue', val);
});

const fetchCategories = async () => {
  try {
    const res: any = await getCategories();
    if (res.code === 200) {
      categories.value = res.data;
    }
  } catch (error) {
    console.error('获取分类失败:', error);
  }
};

const handleSubmit = async () => {
  if (!formRef.value) return;
  
  await formRef.value.validate(async (valid: boolean) => {
    if (valid) {
      if (form.currentValue <= 0) {
        ElMessage.error('金额必须大于 0');
        return;
      }
      
      loading.value = true;
      try {
        const submitData = { ...form };
        // 非金融投资类清空 subType
        if (form.categoryId !== 2) {
          submitData.subType = undefined;
        }
        
        const res: any = await addAsset(submitData);
        if (res.code === 200) {
          ElMessage.success('资产录入成功');
          emit('success');
          handleClose();
        } else {
          ElMessage.error(res.message || '录入失败');
        }
      } catch (error: any) {
        console.error('录入异常:', error);
        ElMessage.error(error.response?.data?.message || '网络请求失败');
      } finally {
        loading.value = false;
      }
    }
  });
};

const handleClose = () => {
  visible.value = false;
  formRef.value?.resetFields();
  Object.assign(form, {
    categoryId: 1,
    subType: '',
    assetName: '',
    currentValue: 0,
    holdingCost: undefined,
    assetCode: ''
  });
};

onMounted(() => {
  fetchCategories();
});
</script>

<style lang="scss" scoped>
@import "@/theme/variables.scss";

.asset-dialog {
  :deep(.el-dialog) {
    background: linear-gradient(135deg, #1e293b, #0f172a) !important;
    border: 1px solid rgba(255, 255, 255, 0.1);
    border-radius: 16px;
  }
  
  :deep(.el-dialog__header) {
    border-bottom: 1px solid rgba(255, 255, 255, 0.1);
    padding: 20px;
  }
  
  :deep(.el-dialog__title) {
    color: white !important;
    font-weight: 600;
  }
  
  :deep(.el-dialog__body) {
    padding: 24px;
  }
  
  :deep(.el-dialog__headerbtn .el-dialog__close) {
    color: #94a3b8;
  }
}

.asset-form {
  :deep(.el-form-item__label) {
    color: #94a3b8 !important;
    font-size: 13px;
  }
  
  /* 强制输入框背景为白色，文字为黑色 */
  :deep(.el-input__wrapper),
  :deep(.el-textarea__inner) {
    background-color: #ffffff !important;
    box-shadow: 0 0 0 1px #dcdfe6 inset !important;
    border-radius: 12px;
  }
  
  :deep(.el-input__inner) {
    color: #000000 !important;
    font-weight: 500;
    font-size: 15px;
    
    &::placeholder {
      color: #a8abb2 !important;
    }
  }
  
  /* 聚焦时的边框颜色 */
  :deep(.el-input__wrapper.is-focus) {
    box-shadow: 0 0 0 1px var(--el-color-primary) inset !important;
  }
}

.category-group {
  width: 100%;
  display: flex;
  
  :deep(.el-radio-button) {
    flex: 1;
    
    .el-radio-button__inner {
      width: 100%;
      background-color: rgba(255, 255, 255, 0.05);
      border-color: rgba(255, 255, 255, 0.15);
      color: #94a3b8;
      font-weight: 500;
      
      &:hover {
        color: white;
      }
    }
    
    &.is-active .el-radio-button__inner {
      background: linear-gradient(135deg, #06b6d4, $primary-color);
      border-color: transparent;
      color: white;
    }
  }
}

.subtype-group {
  :deep(.el-radio-button) {
    .el-radio-button__inner {
      background-color: rgba(255, 255, 255, 0.05);
      border-color: rgba(255, 255, 255, 0.15);
      color: #94a3b8;
      
      &:hover {
        color: white;
      }
    }
    
    &.is-active .el-radio-button__inner {
      background-color: #06b6d4;
      border-color: #06b6d4;
      color: white;
    }
  }
}

.amount-input {
  :deep(.el-input__wrapper) {
    background-color: #ffffff !important;
  }
  
  :deep(.el-input__inner) {
    font-size: 20px !important;
    font-weight: 600;
    color: #000000 !important;
  }
}

.currency-prefix {
  color: #606266;
  font-weight: 500;
  font-size: 16px;
}

.currency-suffix {
  color: #64748b;
  font-size: 14px;
}

.full-width {
  width: 100%;
}

.submit-btn {
  width: 100%;
  height: 48px;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 600;
  background: linear-gradient(135deg, #06b6d4, $primary-color);
  border: none;
  margin-top: 16px;
  transition: all 0.3s ease;
  
  &:hover:not(:disabled) {
    transform: scale(1.02);
    box-shadow: 0 0 20px rgba(79, 70, 229, 0.4);
  }
  
  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
}
</style>
