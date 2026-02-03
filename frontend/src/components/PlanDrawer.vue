<template>
  <el-drawer
    v-model="visible"
    title="智能调仓与资产优化"
    direction="rtl"
    size="520px"
    :before-close="handleClose"
    class="plan-drawer"
  >
    <!-- 场景选择器 (未生成方案或重新生成时显示) -->
    <div v-if="!plan && !loading" class="scenario-selector">
      <h3>请选择您的场景</h3>
      <div class="scenarios">
        <div 
          class="scenario-card" 
          :class="{ active: planType === 'CONTRIBUTION' }"
          @click="planType = 'CONTRIBUTION'"
        >
          <div class="icon">💰</div>
          <div class="label">我有闲钱/发工资了</div>
          <div class="desc">增量再平衡：将新资金优先分配给缺口最大的资产，尽量不卖出。</div>
        </div>
        
        <div 
          class="scenario-card" 
          :class="{ active: planType === 'REBALANCE' }"
          @click="planType = 'REBALANCE'"
        >
          <div class="icon">⚖️</div>
          <div class="label">仅调整现有持仓结构</div>
          <div class="desc">存量再平衡：传统的“卖高买低”，通过内部调整使比例回归目标。</div>
        </div>
      </div>

      <!-- 金额输入 (增量模式) -->
      <div v-if="planType === 'CONTRIBUTION'" class="amount-input-area">
        <label>投入金额 (元)</label>
        <el-input-number 
          v-model="investMoney" 
          :min="1000" 
          :step="1000" 
          placeholder="请输入金额"
          class="full-width"
        />
        <p class="hint">建议金额不低于 1,000 元，以获得更有效的配置建议。</p>
      </div>

      <div class="action-bar">
        <el-button 
          type="primary" 
          class="generate-btn" 
          @click="fetchPlan"
        >
          🤖 智能生成优化方案
        </el-button>
      </div>
    </div>

    <!-- Loading 状态 -->
    <div v-if="loading" class="loading-state">
      <div class="ai-thinking">
        <div class="thinking-icon">🤖</div>
        <div class="thinking-dots">
          <span></span>
          <span></span>
          <span></span>
        </div>
        <p>AI 正在根据 {{ planType === 'CONTRIBUTION' ? '您的资金' : '当前市场与风险偏好' }} 分析方案...</p>
      </div>
    </div>

    <!-- 计划内容 -->
    <div v-else-if="plan" class="plan-content">
      <!-- 头部信息 -->
      <div class="plan-header">
        <div class="header-top">
          <div class="plan-title-area">
            <h3>智能优化建议</h3>
            <div class="current-base">
              当前基准：
              <el-tag :type="getRiskTagType(plan.riskLevel)" size="small" effect="dark">
                {{ getRiskEmoji(plan.riskLevel) }} {{ plan.riskLabel }}
              </el-tag>
            </div>
          </div>
          <el-button link type="primary" @click="resetScenario">重新选择</el-button>
        </div>
        <div class="stats-row">
          <div class="stat-item">
            <span class="label">当前总资产</span>
            <span class="value">¥ {{ formatNumber(plan.totalAmount) }}</span>
          </div>
          <div v-if="plan.investMoney" class="stat-item">
            <span class="label">计划新增</span>
            <span class="value text-green">+¥ {{ formatNumber(plan.investMoney) }}</span>
          </div>
        </div>
      </div>

      <!-- 建议列表 -->
      <div class="plan-items">
        <div 
          v-for="(item, index) in plan.items" 
          :key="index"
          class="plan-item"
          :class="item.action.toLowerCase()"
        >
          <div class="item-header">
            <span class="action-badge" :class="item.action.toLowerCase()">
              {{ getActionLabel(item.action) }}
            </span>
            <span class="category-name">{{ item.categoryName }}</span>
            <span v-if="item.subType" class="sub-type">{{ item.subType }}</span>
          </div>
          
          <div v-if="item.amount" class="item-amount">
            <span :class="{ 'text-green': item.action === 'BUY', 'text-red': item.action === 'SELL' }">
              {{ item.action === 'BUY' ? '+' : '-' }}¥ {{ formatNumber(item.amount) }}
            </span>
          </div>
          
          <div v-if="item.currentRatio !== undefined" class="ratio-info">
            <span>当前 {{ (item.currentRatio * 100).toFixed(0) }}%</span>
            <span class="arrow">→</span>
            <span class="target">目标 {{ (item.targetRatio * 100).toFixed(0) }}%</span>
          </div>
          
          <p class="item-reason">{{ item.reason }}</p>
        </div>
      </div>

      <!-- 底部按钮 -->
      <div class="plan-footer">
        <el-button 
          v-if="plan.status !== 'executed'"
          type="primary" 
          class="btn-execute"
          @click="handleExecute"
          :loading="executing"
        >
          🚀 一键自动执行方案
        </el-button>
        <el-button 
          v-if="plan.status === 'executed'"
          disabled
          class="btn-executed"
        >
          ✅ 方案已执行完成
        </el-button>
        <div class="footer-secondary" v-if="plan.status !== 'executed'">
          <el-button 
            @click="handleSave"
            :loading="saving"
            plain
          >
            仅保存到仓库
          </el-button>
        </div>
      </div>
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { generatePlan, savePlan, executePlan, type InvestmentPlanVO } from '@/api/plan';
import { useHealthStore } from '@/store/modules/health';

const props = defineProps<{
  modelValue: boolean;
}>();

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void;
  (e: 'success'): void;
}>();

const router = useRouter();
const healthStore = useHealthStore();
const visible = ref(false);
const loading = ref(false);
const saving = ref(false);
const executing = ref(false);
const plan = ref<InvestmentPlanVO | null>(null);

// 场景选择
const planType = ref<'CONTRIBUTION' | 'REBALANCE'>('CONTRIBUTION');
const investMoney = ref<number>(10000);

watch(() => props.modelValue, (newVal) => {
  visible.value = newVal;
  if (newVal) {
    plan.value = null; // 每次打开重新选择
  }
});

watch(visible, (newVal) => {
  emit('update:modelValue', newVal);
});

const resetScenario = () => {
  plan.value = null;
};

const fetchPlan = async () => {
  loading.value = true;
  plan.value = null;
  
  try {
    // 模拟 AI 思考延迟提高仪式感
    await new Promise(resolve => setTimeout(resolve, 800));
    
    const res: any = await generatePlan(planType.value, planType.value === 'CONTRIBUTION' ? investMoney.value : undefined);
    if (res.code === 200) {
      plan.value = res.data;
    } else {
      ElMessage.error(res.message || '生成失败');
    }
  } catch (error) {
    ElMessage.error('网络错误');
  } finally {
    loading.value = false;
  }
};

const handleSave = async () => {
  if (!plan.value) return;
  
  try {
    const { value: planName } = await ElMessageBox.prompt('请输入方案名称', '保存方案', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputPlaceholder: '如：2月定投方案 / 结构调整计划',
      inputValidator: (val) => val ? true : '方案名称不能为空'
    });

    saving.value = true;
    plan.value.planName = planName;
    const res: any = await savePlan(plan.value);
    if (res.code === 200) {
      ElMessage.success('方案已保存至计划仓库');
      visible.value = false;
    } else {
      ElMessage.error(res.message || '保存失败');
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('网络错误');
    }
  } finally {
    saving.value = false;
  }
};

const handleExecute = async () => {
  if (!plan.value) return;

  try {
    await ElMessageBox.confirm(
      '系统将自动为您创建对应的资产记录（名称标记为“新入库”）。确认执行吗？',
      '一键执行确认',
      {
        confirmButtonText: '确定执行',
        cancelButtonText: '取消',
        type: 'warning',
        confirmButtonClass: 'execute-confirm-btn'
      }
    );

    executing.value = true;
    
    // 如果是实时生成的草稿，先保存获取 ID
    let planId = plan.value.id;
    if (!planId) {
      plan.value.planName = `自动执行方案-${new Date().getMonth() + 1}${new Date().getDate()}`;
      const saveRes: any = await savePlan(plan.value);
      planId = saveRes.data;
    }

    const res: any = await executePlan(planId);
    if (res.code === 200) {
      ElMessage.success('资产已更新，正在刷新仪表盘...');
      visible.value = false;
      
      // 核心联动：刷新数据
      emit('success');
      healthStore.fetchHealthReport();
      
      router.push('/dashboard');
    } else {
      ElMessage.error(res.message || '执行失败');
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error(error);
    }
  } finally {
    executing.value = false;
  }
};

const handleClose = () => {
  visible.value = false;
};

const formatNumber = (num: number) => {
  return (num || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
};

const getActionLabel = (action: string) => {
  const labels: Record<string, string> = {
    'BUY': '🟢 买入',
    'SELL': '🔴 卖出',
    'INFO': 'ℹ️ 提示'
  };
  return labels[action] || action;
};

const getRiskTagType = (level: string) => {
  const types: Record<string, string> = {
    'conservative': 'success',
    'steady': 'info',
    'balanced': 'primary',
    'growth': 'warning',
    'aggressive': 'danger'
  };
  return types[level] || 'primary';
};

const getRiskEmoji = (level: string) => {
  const emojis: Record<string, string> = {
    'conservative': '🛡️',
    'steady': '🧱',
    'balanced': '⚖️',
    'growth': '📈',
    'aggressive': '🔥'
  };
  return emojis[level] || '👤';
};
</script>

<style lang="scss" scoped>
.plan-drawer {
  :deep(.el-drawer__header) {
    margin-bottom: 0;
    padding: 20px 24px;
    border-bottom: 1px solid #e5e7eb;
    background: #ffffff;
  }
  
  :deep(.el-drawer__body) {
    padding: 0;
    display: flex;
    flex-direction: column;
    background: #f1f5f9;
  }
}

// 场景选择器样式
.scenario-selector {
  padding: 24px;
  
  h3 {
    margin: 0 0 20px 0;
    font-size: 18px;
    color: #1e293b;
  }

  .scenarios {
    display: flex;
    flex-direction: column;
    gap: 16px;
  }

  .scenario-card {
    background: #ffffff;
    border: 2px solid transparent;
    border-radius: 12px;
    padding: 16px;
    cursor: pointer;
    transition: all 0.2s;
    box-shadow: 0 1px 3px rgba(0,0,0,0.05);

    &:hover {
      border-color: #cbd5e1;
      transform: translateY(-2px);
    }

    &.active {
      border-color: #3b82f6;
      background: #eff6ff;
      
      .label { color: #1e40af; }
    }

    .icon { font-size: 24px; margin-bottom: 8px; }
    .label { font-weight: 600; font-size: 16px; margin-bottom: 4px; }
    .desc { font-size: 13px; color: #64748b; line-height: 1.4; }
  }
}

.amount-input-area {
  margin-top: 24px;
  padding: 16px;
  background: #ffffff;
  border-radius: 12px;
  
  label {
    display: block;
    font-size: 14px;
    font-weight: 500;
    color: #475569;
    margin-bottom: 12px;
  }
  
  .full-width {
    width: 100%;
  }
  
  .hint {
    margin-top: 8px;
    font-size: 12px;
    color: #94a3b8;
  }
}

.action-bar {
  margin-top: 32px;
  
  .generate-btn {
    width: 100%;
    height: 48px;
    font-size: 16px;
    font-weight: 600;
    background: linear-gradient(135deg, #3b82f6, #06b6d4);
    border: none;
    border-radius: 24px;
    box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3);
    
    &:hover {
      opacity: 0.9;
      transform: translateY(-1px);
    }
  }
}

.loading-state {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 400px;
}

.ai-thinking {
  text-align: center;
  
  .thinking-icon {
    font-size: 64px;
    margin-bottom: 24px;
    filter: drop-shadow(0 0 12px rgba(6, 182, 212, 0.3));
    animation: pulse 2s ease-in-out infinite;
  }
  
  .thinking-dots {
    display: flex;
    justify-content: center;
    gap: 8px;
    margin: 16px 0;
    
    span {
      width: 10px;
      height: 10px;
      background: #06b6d4;
      border-radius: 50%;
      animation: dot-bounce 1.4s ease-in-out infinite;
      
      &:nth-child(2) { animation-delay: 0.2s; }
      &:nth-child(3) { animation-delay: 0.4s; }
    }
  }
  
  p {
    color: #475569;
    font-size: 15px;
    font-weight: 500;
  }
}

@keyframes pulse {
  0%, 100% { transform: scale(1); opacity: 0.8; }
  50% { transform: scale(1.1); opacity: 1; }
}

@keyframes dot-bounce {
  0%, 80%, 100% { transform: scale(0.4); opacity: 0.3; }
  40% { transform: scale(1.2); opacity: 1; }
}

.plan-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.plan-header {
  padding: 20px 24px;
  background: #ffffff;
  border-bottom: 1px solid #e2e8f0;
  
  .header-top {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;
  }
  
  .stats-row {
    display: flex;
    gap: 24px;
  }
  
  .stat-item {
    .label {
      display: block;
      font-size: 12px;
      color: #94a3b8;
      margin-bottom: 4px;
    }
    .value {
      font-size: 18px;
      font-weight: 700;
      color: #1e293b;
      
      &.text-green { color: #10b981; }
    }
  }
}

.risk-badge {
  padding: 6px 12px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 600;
  
  &.conservative { background: #d1fae5; color: #059669; }
  &.steady { background: #cffafe; color: #0891b2; }
  &.balanced { background: #dbeafe; color: #2563eb; }
  &.growth { background: #fef3c7; color: #d97706; }
  &.aggressive { background: #fee2e2; color: #dc2626; }
}

.plan-items {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
}

.plan-item {
  background: #ffffff;
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 16px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.02);
  
  &.buy { border-left: 5px solid #10b981; }
  &.sell { border-left: 5px solid #ef4444; }
  &.info { border-left: 5px solid #3b82f6; }
}

.item-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}

.action-badge {
  font-size: 12px;
  padding: 3px 10px;
  border-radius: 6px;
  font-weight: 600;
  
  &.buy { background: #d1fae5; color: #059669; }
  &.sell { background: #fee2e2; color: #dc2626; }
  &.info { background: #dbeafe; color: #2563eb; }
}

.category-name {
  font-weight: 700;
  font-size: 16px;
  color: #1e293b;
}

.sub-type {
  font-size: 12px;
  color: #64748b;
  background: #f1f5f9;
  padding: 2px 8px;
  border-radius: 4px;
}

.item-amount {
  font-size: 24px;
  font-weight: 800;
  margin: 12px 0;
  font-family: 'JetBrains Mono', monospace;
  
  .text-green { color: #059669; }
  .text-red { color: #dc2626; }
}

.ratio-info {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 13px;
  color: #64748b;
  margin-bottom: 12px;
  padding: 8px 12px;
  background: #f8fafc;
  border-radius: 8px;
  width: fit-content;
  
  .arrow { color: #cbd5e1; font-weight: bold; }
  .target { color: #3b82f6; font-weight: 600; }
}

.item-reason {
  font-size: 13px;
  color: #475569;
  line-height: 1.6;
  margin: 0;
  background: #fffbeb;
  padding: 10px;
  border-radius: 8px;
}

.plan-footer {
  padding: 24px;
  background: #ffffff;
  border-top: 1px solid #e2e8f0;
  display: flex;
  flex-direction: column;
  gap: 12px;
  
  .btn-execute {
    height: 48px;
    font-size: 16px;
    font-weight: 700;
    border-radius: 12px;
    background: linear-gradient(135deg, #10b981, #059669);
    border: none;
    box-shadow: 0 4px 12px rgba(16, 185, 129, 0.3);
    
    &:hover { filter: brightness(1.1); }
  }
}
</style>
