<template>
  <el-dialog
    v-model="visible"
    title="生成投资计划"
    width="680px"
    class="glass-dialog"
    destroy-on-close
    :close-on-click-modal="false"
  >
    <div class="dialog-content">
      <!-- 步骤 1: 场景输入 -->
      <div v-if="step === 1" class="step-input">
        <h3 class="step-title">您想如何优化资产？</h3>
        
        <div class="mode-selector">
          <div 
            class="mode-card" 
            :class="{ active: form.planType === 'CONTRIBUTION' }"
            @click="form.planType = 'CONTRIBUTION'"
          >
            <div class="icon">💰</div>
            <div class="info">
              <h4>我有闲钱 (增量)</h4>
              <p>投入新资金，根据缺口自动补仓</p>
            </div>
          </div>
          
          <div 
            class="mode-card" 
            :class="{ active: form.planType === 'REBALANCE' }"
            @click="form.planType = 'REBALANCE'"
          >
            <div class="icon">⚖️</div>
            <div class="info">
              <h4>仅调整持仓 (存量)</h4>
              <p>卖出超配资产，买入低估资产</p>
            </div>
          </div>
        </div>

        <div v-if="form.planType === 'CONTRIBUTION'" class="amount-input">
          <label>投入金额 (CNY)</label>
          <div class="input-chart-group">
              <el-input-number 
                v-model="form.investMoney" 
                :min="1000" 
                :step="1000" 
                size="large"
                style="width: 100%"
              />
              <!-- 饼图容器 -->
              <div v-if="form.planType === 'CONTRIBUTION'" ref="chartRef" class="chart-container"></div>
          </div>
        </div>

        <div v-if="form.planType === 'CONTRIBUTION' && totalCash > 0" class="liquidity-tip">
           <el-alert :closable="false" show-icon type="warning">
             <template #title>
               系统检测到您的流动现金为 ¥{{ totalCash.toLocaleString() }}，建议保留 ¥{{ emergencyFund.toLocaleString() }} 应急金。
             </template>
           </el-alert>
        </div>

        <div class="tips">
          <el-alert
            title="AI 将根据您的风险等级自动匹配最优资产"
            type="info"
            show-icon
            :closable="false"
          />
        </div>
      </div>

      <!-- 步骤 2: 方案预览 -->
      <div v-else class="step-preview">
        <div class="preview-header">
          <h3 class="step-title">AI 建议如下 (基于您的{{ draft?.riskLabel }}偏好)</h3>
          <el-tag type="warning" effect="dark">预览模式</el-tag>
        </div>
        
        <!-- 风险降级提示 -->
        <el-alert
          v-if="draft && userRiskLevel && draft.riskLevel !== userRiskLevel"
          title="风险降级提示"
          type="warning"
          show-icon
          description="检测到您的流动资金不足 3 万元安全红线，系统已优先为您规划稳健型资产以构建安全垫。"
          class="mb-4"
        />

        <div class="plan-items-container">
          <el-table :data="draft?.items || []" style="width: 100%" height="300">
            <el-table-column prop="action" label="操作" width="80">
              <template #default="{ row }">
                <el-tag :type="row.action === 'BUY' ? 'success' : 'danger'" effect="dark">
                  {{ row.action === 'BUY' ? '买入' : '卖出' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="资产名称" min-width="120">
              <template #default="{ row }">
                <div class="asset-info">
                  <span class="name">{{ row.subType || row.categoryName }}</span>
                  <el-tag v-if="isRiskAsset(row)" type="danger" size="small" effect="plain" class="risk-tag">
                    R{{ getRiskLevel(row) }}
                  </el-tag>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="金额" width="120" align="right">
              <template #default="{ row }">
                <span class="amount">¥{{ row.amount.toLocaleString() }}</span>
              </template>
            </el-table-column>
            <el-table-column label="推荐理由" min-width="200">
              <template #default="{ row }">
                <div class="reason-text">{{ row.reason }}</div>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <div class="plan-meta">
          <label>为方案起个名字</label>
          <el-input v-model="planName" placeholder="例如：2026春季加仓计划" />
        </div>
      </div>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button v-if="step === 2" @click="step = 1">上一步</el-button>
        <el-button @click="handleClose">取消</el-button>
        
        <el-button 
          v-if="step === 1" 
          type="primary" 
          :loading="generating" 
          @click="handleGenerate"
        >
          🤖 生成 AI 建议
        </el-button>
        
        <el-button 
          v-else 
          type="success" 
          :loading="saving" 
          @click="handleSave"
        >
          💾 保存方案
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, nextTick } from 'vue';
import { useInvestmentPlanStore } from '@/store/modules/investmentPlan';
import { useRiskAssessmentStore } from '@/store/modules/riskAssessment';
import * as echarts from 'echarts';
import dayjs from 'dayjs';

const props = defineProps<{
  modelValue: boolean
}>();

const emit = defineEmits(['update:modelValue', 'success']);

const store = useInvestmentPlanStore();
const riskStore = useRiskAssessmentStore();

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
});

// ... (省略中间代码)

const fetchUserRisk = async () => {
    try {
        // 强制从后端拉取最新画像 (Phase 7.7: Sync)
        const data = await riskStore.fetchLatestResult();
        if (data) {
            userRiskLevel.value = data.riskLevel;
        }
    } catch (e) {
        console.error('获取用户风险等级失败', e);
    }
};

const handleGenerate = async () => {
  generating.value = true;
  try {
    const res = await store.generateDraft(form.value.investMoney, form.value.planType as any);
    if (res) {
      draft.value = res;
      step.value = 2;
      // 默认名称
      const typeStr = form.value.planType === 'CONTRIBUTION' ? '增量计划' : '调仓计划';
      planName.value = `${dayjs().format('MM-DD')} ${typeStr}`;
    }
  } finally {
    generating.value = false;
  }
};

const handleSave = async () => {
  if (!draft.value) return;
  saving.value = true;
  try {
    const planToSave = {
      ...draft.value,
      planName: planName.value || '未命名计划'
    };
    
    const success = await store.saveCurrentPlan(planToSave);
    if (success) {
      emit('success');
      handleClose();
    }
  } finally {
    saving.value = false;
  }
};

// Phase 7.5: 获取资产摘要以显示资金建议
import { getAssetSummary } from '@/api/asset';
const totalCash = ref(0);
const emergencyFund = 30000;

const fetchLiquidityInfo = async () => {
  try {
    const res: any = await getAssetSummary();
    if (res.code === 200 && res.data.categoryDistribution) {
       // 后端返回的是 Map<String, BigDecimal>，key 为中文
       totalCash.value = res.data.categoryDistribution['现金储蓄'] || 0;
    }
  } catch (e) {
    console.error('获取资产摘要失败', e);
  }
};

// 计算属性：资金分布数据 (用于 ECharts)
const distributionData = computed(() => {
    // 逻辑需与后端保持一致：gap = 30000 - currentCash
    const gap = Math.max(0, emergencyFund - totalCash.value);
    
    // safeAmount = min(gap, investMoney)
    const safe = gap > 0 ? Math.min(gap, form.value.investMoney) : 0;
    
    // riskyAmount = investMoney - safeAmount
    const risk = Math.max(0, form.value.investMoney - safe);
    
    return [
      { value: safe, name: '🔒 安全垫补足', itemStyle: { color: '#67C23A' } }, // Green
      { value: risk, name: '🚀 进取投资', itemStyle: { color: '#F56C6C' } }  // Red
    ];
});

// 初始化/更新图表
const initChart = () => {
    if (!chartRef.value) return;
    
    if (!chartInstance) {
        chartInstance = echarts.init(chartRef.value);
    }
    
    const option = {
        tooltip: {
            trigger: 'item',
            formatter: '{b}: {c}元 ({d}%)'
        },
        legend: {
            bottom: '0%',
            left: 'center',
            textStyle: { color: '#ccc' }
        },
        series: [
            {
                name: '资金构成',
                type: 'pie',
                radius: ['40%', '70%'],
                avoidLabelOverlap: false,
                itemStyle: {
                    borderRadius: 10,
                    borderColor: '#1e293b', // Match dialog bg
                    borderWidth: 2
                },
                label: {
                    show: false,
                    position: 'center'
                },
                emphasis: {
                    label: {
                        show: true,
                        fontSize: 14,
                        fontWeight: 'bold',
                        color: '#fff'
                    }
                },
                labelLine: {
                    show: false
                },
                data: distributionData.value
            }
        ]
    };
    
    chartInstance.setOption(option);
};

// 监听金额变化更新图表
watch(() => form.value.investMoney, () => {
    if (visible.value && step.value === 1) {
        initChart();
    }
});

// 监听 totalCash 变化也要更新
watch(totalCash, () => {
     if (visible.value && step.value === 1) {
        initChart();
    }
});

const handleClose = () => {
  visible.value = false;
};

// 辅助方法
const isRiskAsset = (item: any) => {
  return item.action === 'BUY' && item.categoryName === '金融投资';
};

const getRiskLevel = (item: any) => {
    const name = item.subType || '';
    if (name.includes('茅台') || name.includes('腾讯')) return 4;
    // 简单根据名称判断一下，实际应由后端返回
    if (name.includes('国债') || name.includes('货币')) return 1; 
    if (name.includes('ETF')) return 3;
    return 3;
};
</script>

<style lang="scss" scoped>
@use "@/theme/variables.scss" as *;

.step-input {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.step-title {
  font-size: 18px;
  color: $text-primary;
  margin: 0;
}

.mode-selector {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.mode-card {
  border: 2px solid rgba(255, 255, 255, 0.1);
  border-radius: 12px;
  padding: 20px;
  cursor: pointer;
  background: rgba(255, 255, 255, 0.05);
  display: flex;
  gap: 16px;
  align-items: center;
  transition: all 0.3s;

  &:hover {
    background: rgba(255, 255, 255, 0.1);
  }

  &.active {
    border-color: $primary;
    background: rgba($primary, 0.1);

    h4 { color: $primary; }
  }

  .icon {
    font-size: 32px;
  }

  .info {
    h4 {
      margin: 0 0 4px 0;
      color: $text-primary;
    }
    p {
      margin: 0;
      font-size: 12px;
      color: $text-secondary;
    }
  }
}

.amount-input {
  label {
    display: block;
    margin-bottom: 8px;
    color: $text-secondary;
  }
}

.preview-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.asset-info {
  display: flex;
  align-items: center;
  gap: 8px;
  
  .name {
    font-weight: 500;
  }
  
  .risk-tag {
    transform: scale(0.9);
  }
}

.amount {
  font-family: 'Roboto Mono', monospace;
  font-weight: 500;
}

.reason-text {
  font-size: 12px;
  color: $text-secondary;
  line-height: 1.4;
}

.plan-meta {
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  
  label {
    display: block;
    margin-bottom: 8px;
    color: $text-secondary;
  }
}

.chart-container {
    width: 100%;
    height: 160px;
    margin-top: 12px;
}

.mb-4 {
    margin-bottom: 16px;
}
</style>
