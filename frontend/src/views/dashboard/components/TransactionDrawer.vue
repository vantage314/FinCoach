<template>
  <el-drawer
    v-model="visible"
    title="📜 资金流水与交易明细"
    direction="rtl"
    size="40%"
    :before-close="handleClose"
  >
    <div class="transaction-container" v-loading="loading">
      <el-timeline v-if="transactions.length > 0">
        <el-timeline-item
          v-for="item in transactions"
          :key="item.id"
          :timestamp="formatTime(item.createTime)"
          placement="top"
          :color="getColor(item.amount)"
        >
          <el-card class="transaction-card" :body-style="{ padding: '10px' }">
            <div class="card-header">
              <el-tag :type="getTypeTag(item.transType)" effect="dark" size="small">{{ item.transType }}</el-tag>
              <span class="amount" :class="item.amount >= 0 ? 'text-success' : 'text-danger'">
                {{ item.amount >= 0 ? '+' : '' }}{{ formatAmount(item.amount) }}
              </span>
            </div>
            <div class="card-body">
              <span class="asset-name">{{ item.assetName || '未知资产' }}</span>
              <span class="remark" v-if="item.remark">({{ item.remark }})</span>
            </div>
          </el-card>
        </el-timeline-item>
      </el-timeline>

      <el-empty v-else description="暂无交易记录" />

      <div class="pagination-container" v-if="total > 0">
        <el-pagination
          layout="prev, pager, next"
          :total="total"
          :page-size="pageSize"
          v-model:current-page="currentPage"
          @current-change="fetchData"
        />
      </div>
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue';
import { getTransactions, type TransactionRecord } from '@/api/transaction';
import dayjs from 'dayjs';

const props = defineProps<{
  modelValue: boolean;
}>();

const emit = defineEmits(['update:modelValue']);

const visible = ref(false);
const loading = ref(false);
const transactions = ref<TransactionRecord[]>([]);
const currentPage = ref(1);
const pageSize = ref(20);
const total = ref(0);

watch(() => props.modelValue, (val) => {
  visible.value = val;
  if (val) {
    fetchData();
  }
});

watch(visible, (val) => {
  emit('update:modelValue', val);
});

const fetchData = async () => {
  loading.value = true;
  try {
    const res = await getTransactions({ page: currentPage.value, size: pageSize.value });
    if (res.data.code === 200) {
      const pageData = res.data.data;
      transactions.value = pageData.records;
      total.value = pageData.total;
    }
  } catch (error) {
    console.error('Failed to fetch transactions', error);
  } finally {
    loading.value = false;
  }
};

const handleClose = (done: () => void) => {
  done();
};

const formatTime = (time: string) => {
  return dayjs(time).format('YYYY-MM-DD HH:mm:ss');
};

const formatAmount = (amount: number) => {
  return amount.toLocaleString('zh-CN', { style: 'currency', currency: 'CNY' });
};

const getColor = (amount: number) => {
  return amount >= 0 ? '#67C23A' : '#F56C6C';
};

const getTypeTag = (type: string) => {
  switch (type) {
    case 'DEPOSIT': return 'success';
    case 'WITHDRAW': return 'warning';
    case 'BUY': return 'danger';
    case 'SELL': return 'primary';
    case 'HOLD': return 'info';
    case 'DELETE': return 'info';
    default: return 'info';
  }
};
</script>

<style scoped lang="scss">
.transaction-container {
  padding: 20px;
}

.transaction-card {
  border-radius: 8px;
  
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 8px;

    .amount {
      font-weight: bold;
      font-size: 16px;
      font-family: 'Consolas', monospace;

      &.text-success {
        color: #67C23A;
      }
      &.text-danger {
        color: #F56C6C;
      }
    }
  }

  .card-body {
    display: flex;
    flex-direction: column;
    font-size: 13px;
    color: #606266;

    .asset-name {
      font-weight: 500;
      color: #303133;
    }
    
    .remark {
      color: #909399;
      margin-top: 4px;
    }
  }
}

.pagination-container {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}
</style>
