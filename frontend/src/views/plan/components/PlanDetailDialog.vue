<template>
  <el-dialog
    v-model="visible"
    :title="plan?.planName || '计划详情'"
    width="800px"
    class="glass-dialog"
    destroy-on-close
  >
    <div class="dialog-content" v-if="plan">
      <!-- 头部状态栏 -->
      <div class="status-bar">
        <div class="info-group">
          <label>计划类型</label>
          <span>{{ plan.planType === 'CONTRIBUTION' ? '增量投资' : '存量再平衡' }}</span>
        </div>
        <div class="info-group">
          <label>资金规模</label>
          <span class="amount">¥{{ (plan.investMoney || plan.totalAmount).toLocaleString() }}</span>
        </div>
        <div class="status-badge">
           <el-tag :type="plan.status === 'executed' ? 'success' : 'primary'" effect="dark" size="large">
             {{ plan.status === 'executed' ? '已执行' : '待执行' }}
           </el-tag>
        </div>
      </div>

      <!-- 明细列表 -->
      <el-table :data="plan.items" style="width: 100%" class="detail-table">
        <el-table-column prop="action" label="操作" width="80">
          <template #default="{ row }">
            <el-tag v-if="row.action === 'WARNING'" type="warning" effect="dark">
               警告
            </el-tag>
            <el-tag v-else :type="row.action === 'BUY' ? 'success' : 'danger'" effect="light">
              {{ row.action === 'BUY' ? '买入' : '卖出' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="资产 / 代码" min-width="160">
          <template #default="{ row }">
            <div class="asset-col">
              <span class="main-name">{{ row.subType || row.categoryName }}</span>
              <span class="sub-name">{{ row.categoryName }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="金额" width="140" align="right">
          <template #default="{ row }">
            <span class="amount-cell">¥{{ row.amount.toLocaleString() }}</span>
          </template>
        </el-table-column>
        <el-table-column label="AI 推荐理由" min-width="240">
           <template #default="{ row }">
             <span class="reason">{{ row.reason }}</span>
           </template>
        </el-table-column>
      </el-table>

      <!-- 执行结果反馈 -->
      <div v-if="plan.status === 'executed'" class="executed-feedback">
         <el-icon color="#67C23A"><CircleCheckFilled /></el-icon>
         <span>该计划已于 {{ formatTime(plan.updateTime || Date.now()) }} 执行完毕，资产已更新。</span>
      </div>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="visible = false">关闭</el-button>
        
        <el-button 
          v-if="plan?.status !== 'executed'"
          type="primary" 
          size="large"
          class="execute-btn"
          :loading="executing" 
          @click="handleExecute"
        >
          🚀 一键执行计划
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import { useInvestmentPlanStore } from '@/store/modules/investmentPlan';
import { CircleCheckFilled } from '@element-plus/icons-vue';
import { ElMessageBox } from 'element-plus';
import dayjs from 'dayjs';

const props = defineProps<{
  modelValue: boolean,
  plan: any
}>();

const emit = defineEmits(['update:modelValue', 'success']);

const store = useInvestmentPlanStore();
const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
});

const executing = ref(false);

const handleExecute = () => {
  ElMessageBox.confirm(
    '确认执行该计划吗？系统将自动创建对应的资产记录并更新余额。',
    '执行确认',
    {
      confirmButtonText: '立即执行',
      cancelButtonText: '再想想',
      type: 'warning',
    }
  ).then(async () => {
    executing.value = true;
    try {
      const success = await store.executePlanById(props.plan.id);
      if (success) {
        emit('success');
        visible.value = false;
      }
    } finally {
      executing.value = false;
    }
  });
};

const formatTime = (time: string | number) => dayjs(time).format('YYYY-MM-DD HH:mm');
</script>

<style lang="scss" scoped>
.status-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: var(--fc-panel-hover);
  padding: 16px 24px;
  border-radius: 8px;
  margin-bottom: 24px;

  .info-group {
    display: flex;
    flex-direction: column;
    gap: 4px;
    
    label {
      font-size: 12px;
      color: var(--fc-text-muted);
    }
    
    span {
      color: var(--fc-text);
      font-weight: 500;
      
      &.amount {
        font-family: 'Roboto Mono', monospace;
        font-size: 18px;
      }
    }
  }
}

.asset-col {
  display: flex;
  flex-direction: column;
  
  .main-name {
    font-weight: 600;
    color: var(--fc-text);
  }
  
  .sub-name {
    font-size: 12px;
    color: var(--fc-text-muted);
  }
}

.amount-cell {
  font-family: 'Roboto Mono', monospace;
  font-weight: 600;
}

.reason {
  font-size: 13px;
  color: var(--fc-text-muted);
  line-height: 1.5;
}

.executed-feedback {
  margin-top: 24px;
  padding: 12px;
  background: rgba(16, 185, 129, 0.1);
  border: 1px solid rgba(16, 185, 129, 0.2);
  border-radius: 8px;
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--fc-success);
  font-size: 14px;
}

.execute-btn {
  padding: 0 32px;
  font-weight: 600;
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.4);
}
</style>
