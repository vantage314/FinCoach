<template>
  <div class="plan-dashboard">
    <!-- 头部：投资罗盘 -->
    <header class="dashboard-header">
      <div class="header-left">
        <h1 class="title">我的投资计划</h1>
        <p class="subtitle">Investment Plan & Recommendations</p>
      </div>
      <div class="header-right">
        <el-button type="primary" size="large" :icon="Plus" @click="showCreateDialog = true">
          生成新计划
        </el-button>
      </div>
    </header>

    <!-- 主体：计划时间轴与列表 -->
    <div class="content-body">
      <div class="timeline-container" v-loading="loading">
        <el-empty v-if="!loading && plans.length === 0" description="暂无历史计划，点击右上角生成" />
        
        <el-timeline v-else>
          <el-timeline-item
            v-for="plan in plans"
            :key="plan.id"
            :timestamp="formatTime(plan.createTime)"
            placement="top"
            :type="plan.status === 'executed' ? 'success' : 'primary'"
            :hollow="plan.status !== 'executed'"
          >
            <el-card class="plan-card" shadow="hover" @click="viewPlan(plan)">
              <div class="card-header">
                <div class="plan-info">
                  <span class="plan-name">{{ plan.planName }}</span>
                  <el-tag :type="getRiskTag(plan.riskLevel)" size="small" effect="plain" class="risk-tag">
                    {{ plan.riskLabel }}
                  </el-tag>
                </div>
                <el-tag :type="getStatusType(plan.status)" effect="dark">
                  {{ getStatusLabel(plan.status) }}
                </el-tag>
              </div>
              
              <div class="card-content">
                <div class="stat-item">
                  <label>计划资金</label>
                  <span>¥{{ (plan.investMoney || 0).toLocaleString() }}</span>
                </div>
                <div class="stat-item">
                  <label>总资产概览</label>
                  <span>¥{{ (plan.totalAmount || 0).toLocaleString() }}</span>
                </div>
                <!-- 简要预览前2条建议 -->
                <div class="recommendation-preview">
                  <div v-for="item in plan.items.slice(0, 2)" :key="item.id" class="preview-item">
                    <span :class="['action', item.action.toLowerCase()]">{{ item.action === 'BUY' ? '买入' : '卖出' }}</span>
                    <span class="asset-name">{{ item.subType || item.categoryName }}</span>
                    <span class="amount">¥{{ item.amount }}</span>
                  </div>
                  <div v-if="plan.items.length > 2" class="more-hint">
                    ... 等 {{ plan.items.length }} 条建议
                  </div>
                </div>
              </div>
            </el-card>
          </el-timeline-item>
        </el-timeline>
      </div>
    </div>

    <!-- 创建计划弹窗 -->
    <CreatePlanDialog v-model="showCreateDialog" @success="handlePlanCreated" />
    
    <!-- 详情弹窗 -->
    <PlanDetailDialog v-model="showDetailDialog" :plan="currentPlan" @success="handlePlanExecuted" />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onActivated } from 'vue';
import { getPlanList, executePlan } from '@/api/plan';
import { ElMessage, ElMessageBox } from 'element-plus';
import dayjs from 'dayjs';
import CreatePlanDialog from './components/CreatePlanDialog.vue';
import PlanDetailDialog from './components/PlanDetailDialog.vue';

const showCreateDialog = ref(false);
const showDetailDialog = ref(false);
const currentPlan = ref<any>(null);
const plans = ref<any[]>([]);
const loading = ref(false);

const loadData = () => {
  loading.value = true;
  getPlanList().then((res: any) => {
    if (res.code === 200) {
      // 兼容分页结构或数组
      const raw = res.data;
      plans.value = Array.isArray(raw) ? raw : (raw?.records || []);
    }
  }).finally(() => loading.value = false);
};

// 初始加载 & 切换回页面刷新
onMounted(loadData);
onActivated(loadData);

// 处理一键执行
const handleExecute = async (planId: number) => {
  try {
    await ElMessageBox.confirm('确定要执行该计划吗？资金将自动扣除并买入对应资产。', '执行确认');
    
    const res: any = await executePlan(planId); // 调用执行接口
    if (res.code === 200) {
      ElMessage.success('计划执行成功！请前往资产页面查看。');
      loadData(); // 刷新列表状态
    }
  } catch (error) {
    if (error !== 'cancel') ElMessage.error('执行失败');
  }
};

const handlePlanCreated = () => {
  loadData();
};

const handlePlanExecuted = () => {
  loadData();
};

const viewPlan = (plan: any) => {
  currentPlan.value = plan;
  showDetailDialog.value = true;
};

// 辅助函数
const formatTime = (time: string) => dayjs(time).format('YYYY-MM-DD HH:mm');

const getRiskTag = (level: string) => {
  const map: Record<string, string> = {
    conservative: 'info',
    steady: 'success',
    balanced: 'warning',
    growth: 'danger',
    aggressive: 'danger'
  };
  return map[level] || 'info'; 
};

const getStatusType = (status: string) => {
  return status === 'executed' || status === 'COMPLETED' ? 'success' : 'info';
};

const getStatusLabel = (status: string) => {
  return status === 'executed' || status === 'COMPLETED' ? '已执行' : '待执行';
};
</script>

<style lang="scss" scoped>
@use "@/theme/variables.scss" as *;

.plan-dashboard {
  padding: 24px;
  max-width: 1200px;
  margin: 0 auto;
}

.dashboard-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 32px;

  .title {
    font-size: 28px;
    font-weight: 600;
    color: $text-primary;
    margin: 0 0 8px 0;
  }

  .subtitle {
    color: $text-secondary;
    margin: 0;
  }
}

.timeline-container {
  padding: 0 16px;
}

.plan-card {
  cursor: pointer;
  border-radius: 12px;
  transition: all 0.3s;
  background: rgba(30, 41, 59, 0.6); // 深色半透明
  border: 1px solid rgba(255, 255, 255, 0.05);
  backdrop-filter: blur(10px);

  &:hover {
    transform: translateY(-2px);
    background: rgba(30, 41, 59, 0.8);
    border-color: $primary;
  }
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;

  .plan-info {
    display: flex;
    align-items: center;
    gap: 12px;
  }

  .plan-name {
    font-size: 16px;
    font-weight: 500;
    color: $text-primary;
  }
}

.card-content {
  display: flex;
  gap: 32px;
  align-items: flex-start;

  .stat-item {
    display: flex;
    flex-direction: column;
    gap: 4px;

    label {
      font-size: 12px;
      color: $text-dim;
    }

    span {
      font-size: 18px;
      font-weight: 600;
      color: $text-primary;
      font-family: 'Roboto Mono', monospace;
    }
  }

  .recommendation-preview {
    flex: 1;
    display: flex;
    flex-direction: column;
    gap: 8px;
    padding-left: 24px;
    border-left: 1px solid rgba(255, 255, 255, 0.1);

    .preview-item {
      display: flex;
      align-items: center;
      gap: 12px;
      font-size: 13px;

      .action {
        padding: 2px 6px;
        border-radius: 4px;
        font-size: 12px;
        font-weight: 600;
        
        &.buy { color: $accent-green; background: rgba($accent-green, 0.1); }
        &.sell { color: $accent-red; background: rgba($accent-red, 0.1); }
      }

      .asset-name {
        color: $text-secondary;
        flex: 1;
      }

      .amount {
        color: $text-primary;
        font-family: 'Roboto Mono', monospace;
      }
    }

    .more-hint {
      font-size: 12px;
      color: $text-dim;
    }
  }
}
</style>
