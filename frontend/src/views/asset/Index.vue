<template>
  <div class="asset-manage-container">
    <div class="toolbar">
      <h2>我的资产明细</h2>
      <el-button type="primary" @click="handleAdd">
        <el-icon><Plus /></el-icon> 记一笔
      </el-button>
    </div>

    <el-table :data="assetList" style="width: 100%" v-loading="loading" class="dark-table"
      :header-cell-style="{ background: '#1d212b', color: '#909399', borderBottom: '1px solid #363636' }"
      :row-style="{ background: 'transparent', color: '#fff' }">
      
      <el-table-column prop="assetName" label="资产名称" />
      
      <el-table-column prop="subType" label="类型" width="100">
        <template #default="{ row }">
          <el-tag :type="getTypeTag(row.subType)">{{ formatType(row.subType) }}</el-tag>
        </template>
      </el-table-column>
      
      <el-table-column prop="stockCode" label="关联代码" width="100">
        <template #default="{ row }">
          <span v-if="row.stockCode" style="font-family: monospace; color: #e6a23c">{{ row.stockCode }}</span>
          <span v-else>--</span>
        </template>
      </el-table-column>
      
      <el-table-column prop="quantity" label="持仓数量" align="right">
        <template #default="{ row }">
          {{ row.quantity ? Number(row.quantity) : '--' }}
        </template>
      </el-table-column>
      
      <el-table-column prop="costPrice" label="成本价" align="right">
        <template #default="{ row }">
          <span v-if="row.costPrice">¥{{ formatMoney(row.costPrice) }}</span>
          <span v-else>--</span>
        </template>
      </el-table-column>
      
      <el-table-column label="当前估值" align="right">
        <template #default="{ row }">
          <span style="font-weight: bold">¥{{ formatMoney(row.currentValue) }}</span>
        </template>
      </el-table-column>

      <el-table-column label="操作" width="150" align="center">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
          <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑资产' : '新增资产'" width="500px" class="dark-dialog">
      <el-form :model="form" label-width="100px">
        <el-form-item label="资产类型">
          <el-select v-model="form.subType" placeholder="请选择" @change="handleTypeChange" style="width: 100%">
            <el-option label="股票" value="STOCK" />
            <el-option label="基金" value="FUND" />
            <el-option label="债券" value="BOND" />
            <el-option label="现金/存款" value="CASH" />
          </el-select>
        </el-form-item>

        <el-form-item label="关联证券" v-if="isSecurity">
          <el-select
            v-model="form.stockCode"
            filterable
            remote
            placeholder="输入代码或名称搜索"
             :remote-method="searchStock"
            :loading="searchLoading"
            @change="handleStockSelect"
            style="width: 100%"
          >
            <el-option
              v-for="item in stockOptions"
              :key="item.code"
              :label="item.name + ' (' + item.code + ')'"
              :value="item.code"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="资产名称">
          <el-input v-model="form.assetName" placeholder="如: 招商银行持仓" />
        </el-form-item>

        <el-form-item label="持仓数量" v-if="isSecurity">
          <el-input-number v-model="form.quantity" :min="0" style="width: 100%" />
        </el-form-item>
        
        <el-form-item label="成本单价" v-if="isSecurity">
          <el-input-number v-model="form.costPrice" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>

        <el-form-item label="总金额" v-if="!isSecurity">
          <el-input-number v-model="form.currentValue" :min="0" :precision="2" style="width: 100%" />
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
import { ref, computed, onMounted } from 'vue';
import { Plus } from '@element-plus/icons-vue';
import request from '@/api/request';
import { ElMessage, ElMessageBox } from 'element-plus';

const assetList = ref([]);
const loading = ref(false);
const dialogVisible = ref(false);
const searchLoading = ref(false);
const stockOptions = ref<any[]>([]);

const form = ref<any>({
  id: null,
  assetName: '',
  subType: 'STOCK',
  stockCode: '',
  quantity: 0,
  costPrice: 0,
  currentValue: 0
});

const isSecurity = computed(() => ['STOCK', 'FUND', 'BOND'].includes(form.value.subType));

const loadList = async () => {
  loading.value = true;
  try {
    const res: any = await request.get('/asset/list');
    // 🔥 核心修复：确保赋值给 assetList 的永远是数组
    if (Array.isArray(res)) {
      assetList.value = res;
    } else if (res && res.code === 200 && Array.isArray(res.data)) {
      assetList.value = res.data;
    } else {
      assetList.value = [];
    }
  } catch (e) {
    assetList.value = [];
    console.error('加载资产列表失败', e);
  } finally {
    loading.value = false;
  }
};

const handleAdd = () => {
  form.value = { id: null, subType: 'STOCK', quantity: 0, costPrice: 0, currentValue: 0, assetName: '' };
  dialogVisible.value = true;
};

const handleEdit = (row: any) => {
  form.value = { ...row };
  dialogVisible.value = true;
  if (row.stockCode) {
    stockOptions.value = [{ code: row.stockCode, name: row.assetName.replace('持仓', '') }];
  }
};

const searchStock = async (query: string) => {
  if (query && query.length >= 2) {
    searchLoading.value = true;
    try {
      const res = await request.get<any>('/market/securities', { params: { keyword: query, size: 20 } });
      stockOptions.value = res.records || res || [];
    } catch (e) {
      stockOptions.value = [];
    }
    searchLoading.value = false;
  } else {
    stockOptions.value = [];
  }
};

const handleStockSelect = (code: string) => {
  const item = stockOptions.value.find(op => op.code === code);
  if (item) {
    form.value.assetName = item.name + '持仓';
  }
};

const submitForm = async () => {
  if (isSecurity.value && form.value.quantity > 0 && form.value.currentValue === 0) {
    form.value.currentValue = form.value.quantity * form.value.costPrice;
  }
  
  try {
    await request.post('/asset/add', form.value);
    ElMessage.success('保存成功');
    dialogVisible.value = false;
    loadList();
  } catch (e) {
    ElMessage.error('保存失败');
  }
};

const handleDelete = (row: any) => {
  ElMessageBox.confirm('确定删除该资产记录吗?', '提示', { type: 'warning' }).then(async () => {
    try {
      await request.delete('/asset', { data: [row.id] });
      ElMessage.success('删除成功');
      loadList();
    } catch (e) {
      ElMessage.error('删除失败');
    }
  }).catch(() => {});
};

const handleTypeChange = () => {
  if (!isSecurity.value) {
    form.value.stockCode = '';
    form.value.quantity = 0;
  }
};

const formatType = (type: string) => ({ STOCK: '股票', FUND: '基金', BOND: '债券', CASH: '现金' }[type] || type || '--');
const getTypeTag = (type: string) => ({ STOCK: '', FUND: 'success', BOND: 'warning', CASH: 'info' }[type] || '');
const formatMoney = (val: number) => val != null ? Number(val).toLocaleString('en-US', { minimumFractionDigits: 2 }) : '0.00';

onMounted(() => loadList());
</script>

<style scoped>
/* 深色主题适配 */
.asset-manage-container { padding: 20px; background: #14161a; min-height: 100vh; color: #fff; }
.toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
h2 { margin: 0; font-size: 20px; }

/* 表格样式覆盖 */
.dark-table { --el-table-border-color: #363636; --el-table-bg-color: #1d212b; --el-table-tr-bg-color: #1d212b; --el-table-header-bg-color: #1d212b; }
:deep(.el-table__inner-wrapper::before) { display: none; }
:deep(.el-table td.el-table__cell), :deep(.el-table th.el-table__cell.is-leaf) { border-bottom: 1px solid #363636; }
:deep(.el-table--enable-row-hover .el-table__body tr:hover > td.el-table__cell) { background-color: #2b303c !important; }

/* 弹窗适配 */
:deep(.el-dialog) { background: #1d212b; }
:deep(.el-dialog__title) { color: #fff; }
:deep(.el-form-item__label) { color: #dcdfe6; }
:deep(.el-input__wrapper), :deep(.el-select__wrapper) { background-color: #2b303c; box-shadow: 0 0 0 1px #4c4d4f inset; }
:deep(.el-input__inner) { color: #fff; }
</style>
