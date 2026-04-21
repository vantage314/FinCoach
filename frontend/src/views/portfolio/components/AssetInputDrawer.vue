<template>
  <el-drawer
    v-model="visible"
    title="录入新资产"
    direction="rtl"
    size="420px"
    class="asset-drawer"
    :before-close="handleClose"
  >
    <el-form 
      ref="formRef" 
      :model="form" 
      :rules="rules" 
      label-position="top"
      class="asset-form"
    >
      <el-form-item label="资产分类" prop="categoryId">
        <el-select 
          v-model="form.categoryId" 
          placeholder="请选择资产分类"
          class="full-width"
        >
          <el-option
            v-for="cat in categories"
            :key="cat.id"
            :label="cat.name"
            :value="cat.id"
          />
        </el-select>
      </el-form-item>

      <el-form-item label="资产名称" prop="assetName">
        <el-input 
          v-model="form.assetName" 
          placeholder="如：贵州茅台、余额宝"
          maxlength="50"
          show-word-limit
        />
      </el-form-item>

      <el-form-item label="当前市值" prop="currentValue">
        <el-input-number
          v-model="form.currentValue"
          :min="0.01"
          :precision="2"
          :controls="false"
          class="full-width"
          placeholder="请输入金额"
        >
          <template #prefix>
            <span class="currency-prefix">¥</span>
          </template>
        </el-input-number>
      </el-form-item>

      <el-form-item label="持有成本">
        <el-input-number
          v-model="form.holdingCost"
          :min="0"
          :precision="2"
          :controls="false"
          class="full-width"
          placeholder="可选"
        >
          <template #prefix>
            <span class="currency-prefix">¥</span>
          </template>
        </el-input-number>
      </el-form-item>

      <el-form-item label="资产代码">
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
          @click="handleSubmit"
        >
          确认录入
        </el-button>
      </el-form-item>
    </el-form>
  </el-drawer>
</template>

<script setup lang="ts">
import { ref, reactive, watch, onMounted } from 'vue';
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
  categoryId: 0,
  assetName: '',
  currentValue: 0,
  holdingCost: 0,
  assetCode: ''
});

const rules = {
  categoryId: [{ required: true, message: '请选择资产分类', trigger: 'change' }],
  assetName: [
    { required: true, message: '请输入资产名称', trigger: 'blur' },
    { min: 2, max: 50, message: '长度在 2 到 50 个字符', trigger: 'blur' }
  ],
  currentValue: [
    { required: true, message: '请输入当前市值', trigger: 'blur' }
  ]
};

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
        ElMessage.error('当前市值必须大于 0');
        return;
      }
      
      loading.value = true;
      try {
        const res: any = await addAsset(form);
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
    categoryId: 0,
    assetName: '',
    currentValue: 0,
    holdingCost: 0,
    assetCode: ''
  });
};

onMounted(() => {
  fetchCategories();
});
</script>

<style lang="scss" scoped>
.asset-drawer {
  :deep(.el-drawer) {
    background: rgba(15, 23, 42, 0.95);
    backdrop-filter: blur(15px);
  }
  
  :deep(.el-drawer__header) {
    background: transparent;
    border-bottom: 1px solid rgba(255, 255, 255, 0.1);
    color: white;
    margin-bottom: 0;
    padding: 20px;
  }
  
  :deep(.el-drawer__body) {
    background: transparent;
    padding: 24px;
  }
  
  :deep(.el-drawer__title) {
    color: white;
    font-weight: 600;
  }
}

.asset-form {
  :deep(.el-form-item__label) {
    color: var(--fc-text-muted);
    font-size: 13px;
  }
  
  :deep(.el-input__wrapper),
  :deep(.el-select__wrapper),
  :deep(.el-input-number) {
    background-color: rgba(255, 255, 255, 0.05);
    border: 1px solid var(--fc-border);
    border-radius: 12px;
    box-shadow: none !important;
    
    &.is-focus, &:focus-within {
      border-color: #06b6d4;
      box-shadow: 0 0 12px rgba(6, 182, 212, 0.3) !important;
    }
  }
  
  :deep(.el-input__inner) {
    color: white;
    
    &::placeholder {
      color: rgba(255, 255, 255, 0.3);
    }
  }
  
  :deep(.el-select__placeholder) {
    color: rgba(255, 255, 255, 0.3);
  }
  
  :deep(.el-input-number .el-input__wrapper) {
    padding-left: 32px;
  }
}

.currency-prefix {
  color: var(--fc-text-muted);
  font-weight: 500;
  position: absolute;
  left: 12px;
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
  background: linear-gradient(135deg, #06b6d4, var(--fc-primary));
  border: none;
  margin-top: 16px;
  transition: all 0.3s ease;
  
  &:hover {
    transform: scale(1.02);
    box-shadow: 0 0 20px rgba(79, 70, 229, 0.4);
  }
}
</style>
