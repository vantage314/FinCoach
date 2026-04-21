<template>
  <div class="assessment-result">
    <div class="result-container" v-if="result">
      <!-- 左侧：风险温度计 -->
      <div class="thermometer-section">
        <div class="thermometer-wrapper">
          <div class="thermometer">
            <div class="thermometer-body">
              <div 
                class="mercury" 
                :style="{ height: mercuryHeight + '%', background: mercuryGradient }"
              ></div>
              <!-- 刻度线 -->
              <div class="scale-marks">
                <span v-for="n in 5" :key="n"></span>
              </div>
            </div>
            <!-- 底座 -->
            <div class="thermometer-bulb" :style="{ background: mercuryGradient }">
              <span class="bulb-score">{{ result.totalScore }}</span>
            </div>
          </div>
          <div class="thermometer-labels">
            <span>激进</span>
            <span>进取</span>
            <span>平衡</span>
            <span>稳健</span>
            <span>保守</span>
          </div>
        </div>
      </div>

      <!-- 右侧：画像卡片 -->
      <div class="profile-section">
        <div class="profile-card glass-card">
          <div class="profile-header">
            <div class="profile-icon" :style="{ background: mercuryGradient }">
              {{ getIcon(result.riskLevel) }}
            </div>
            <div class="profile-title">
              <h1 class="profile-label">{{ result.label }}</h1>
              <!-- 标签云 -->
              <div class="tag-cloud">
                <el-tag 
                  v-for="tag in sceneTags" 
                  :key="tag" 
                  effect="dark" 
                  round 
                  size="small"
                >
                  {{ tag }}
                </el-tag>
              </div>
            </div>
          </div>
          <p class="profile-desc">{{ result.description }}</p>
          
          <!-- 双轨对比条 -->
          <div class="comparison-box" v-if="result.actualRatio !== undefined">
            <div class="comparison-item">
              <div class="label-row">
                <span class="comp-label">您的现状</span>
                <span class="comp-value gray">{{ actualPercent }}%</span>
              </div>
              <el-progress 
                :percentage="actualPercent" 
                :show-text="false"
                :stroke-width="10"
                color="#64748b"
              />
            </div>
            <div class="comparison-item">
              <div class="label-row">
                <span class="comp-label">理想目标</span>
                <span class="comp-value highlight" :style="{ color: equityColor }">{{ idealPercent }}%</span>
              </div>
              <el-progress 
                :percentage="idealPercent" 
                :show-text="false"
                :stroke-width="10"
                :color="equityColor"
              />
            </div>
          </div>

          <!-- 标准配置饼图 -->
          <div class="allocation-section">
            <h3>科学配置建议</h3>
            <div ref="pieChartRef" class="pie-chart"></div>
          </div>

          <!-- AI 诊断卡片 -->
          <div class="ai-diagnosis-card" v-if="result.diagnosis">
            <span class="ai-icon">🤖</span>
            <p class="ai-text">{{ result.diagnosis }}</p>
          </div>

          <div class="flex gap-4 mt-6 btn-group">
            <el-button type="primary" class="cta-btn flex-1" @click="goToDashboard">
              去调整我的持仓
            </el-button>
            <el-button class="retake-btn flex-1" @click="handleRetake">
              重新测评
            </el-button>
          </div>
        </div>
      </div>
    </div>

    <div class="loading-state" v-else>
      <div class="spinner"></div>
      <p>正在加载测评结果...</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, nextTick } from 'vue';
import { useRouter } from 'vue-router';
import * as echarts from 'echarts';
import { getLatestResult, type RiskAssessmentResult } from '@/api/risk';
import { useRiskAssessmentStore } from '@/store/modules/riskAssessment';

const router = useRouter();
const riskStore = useRiskAssessmentStore();

const result = ref<RiskAssessmentResult | null>(null);
const mercuryHeight = ref(0);
const pieChartRef = ref<HTMLElement | null>(null);
let pieChart: echarts.ECharts | null = null;

// 根据等级获取渐变色
const mercuryGradient = computed(() => {
  if (!result.value) return 'linear-gradient(to top, #06b6d4, #3b82f6)';
  const level = result.value.riskLevel;
  const gradients: Record<string, string> = {
    conservative: 'linear-gradient(to top, #10b981, #34d399)',
    steady: 'linear-gradient(to top, #06b6d4, #22d3ee)',
    balanced: 'linear-gradient(to top, #3b82f6, #60a5fa)',
    growth: 'linear-gradient(to top, #f59e0b, #fbbf24)',
    aggressive: 'linear-gradient(to top, #ef4444, #f87171)'
  };
  return gradients[level] || gradients.balanced;
});

const equityColor = computed(() => {
  if (!result.value) return '#3b82f6';
  const level = result.value.riskLevel;
  const colors: Record<string, string> = {
    conservative: '#10b981',
    steady: '#06b6d4',
    balanced: '#3b82f6',
    growth: '#f59e0b',
    aggressive: '#ef4444'
  };
  return colors[level] || colors.balanced;
});

// 场景标签
const sceneTags = computed(() => {
  if (!result.value) return [];
  const tagMap: Record<string, string[]> = {
    conservative: ['稳健存款', '安全第一', '零风险'],
    steady: ['理财优选', '跑赢通胀', '固收为主'],
    balanced: ['股债均衡', '攻守兼备', '中长期'],
    growth: ['权益优先', '长线布局', '波动容忍'],
    aggressive: ['高风险高收益', 'All in股市', '激进配置']
  };
  return tagMap[result.value.riskLevel] || [];
});

const actualPercent = computed(() => {
  if (!result.value || result.value.actualRatio === undefined) return 0;
  return Math.round(result.value.actualRatio * 100);
});

const idealPercent = computed(() => {
  if (!result.value || result.value.idealRatio === undefined) return 40;
  return Math.round(result.value.idealRatio * 100);
});

// 配置比例（根据风险等级预设）
const allocationData = computed(() => {
  if (!result.value) return { equity: 40, bond: 40, cash: 20 };
  const presets: Record<string, { equity: number; bond: number; cash: number }> = {
    conservative: { equity: 10, bond: 60, cash: 30 },
    steady: { equity: 25, bond: 55, cash: 20 },
    balanced: { equity: 40, bond: 45, cash: 15 },
    growth: { equity: 60, bond: 30, cash: 10 },
    aggressive: { equity: 80, bond: 15, cash: 5 }
  };
  return presets[result.value.riskLevel] || presets.balanced;
});

const getIcon = (level: string) => {
  const icons: Record<string, string> = {
    conservative: '🛡️',
    steady: '📈',
    balanced: '⚖️',
    growth: '🚀',
    aggressive: '🔥'
  };
  return icons[level] || '⚖️';
};

const goToDashboard = () => {
  router.push('/dashboard');
};

const handleRetake = () => {
  riskStore.resetAssessment();
  router.push('/risk/assessment');
};

const initPieChart = () => {
  if (!pieChartRef.value) return;
  
  pieChart = echarts.init(pieChartRef.value);
  const data = allocationData.value;
  
  pieChart.setOption({
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c}%'
    },
    legend: {
      orient: 'horizontal',
      bottom: 0,
      textStyle: { color: '#94a3b8' },
      itemGap: 20
    },
    series: [{
      type: 'pie',
      radius: ['45%', '70%'],
      center: ['50%', '45%'],
      avoidLabelOverlap: false,
      itemStyle: {
        borderRadius: 6,
        borderColor: '#1e293b',
        borderWidth: 2
      },
      label: {
        show: false
      },
      emphasis: {
        label: {
          show: true,
          fontSize: 14,
          fontWeight: 'bold',
          color: '#ffffff'
        }
      },
      data: [
        { 
          value: data.equity, 
          name: '权益类',
          itemStyle: { color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
            { offset: 0, color: '#ef4444' },
            { offset: 1, color: '#f59e0b' }
          ])}
        },
        { 
          value: data.bond, 
          name: '固收类',
          itemStyle: { color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
            { offset: 0, color: '#3b82f6' },
            { offset: 1, color: '#06b6d4' }
          ])}
        },
        { 
          value: data.cash, 
          name: '现金类',
          itemStyle: { color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
            { offset: 0, color: '#10b981' },
            { offset: 1, color: '#34d399' }
          ])}
        }
      ]
    }]
  });
};

const fetchResult = async () => {
  try {
    const res: any = await getLatestResult();
    if (res.code === 200) {
      result.value = res.data;
      // 触发温度计动画
      setTimeout(() => {
        mercuryHeight.value = Math.min(100, result.value!.totalScore);
      }, 200);
      // 初始化饼图
      await nextTick();
      setTimeout(initPieChart, 300);
    }
  } catch (error) {
    console.error('获取结果失败:', error);
  }
};

onMounted(() => {
  fetchResult();
});
</script>

<style lang="scss" scoped>
.assessment-result {
  min-height: 100vh;
  background: var(--fc-bg-gradient);
  padding: 40px 20px;
  display: flex;
  justify-content: center;
  align-items: center;
}

.result-container {
  display: flex;
  gap: 60px;
  max-width: 1000px;
  width: 100%;
  
  @media (max-width: 768px) {
    flex-direction: column;
    align-items: center;
  }
}

.thermometer-section {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.thermometer-wrapper {
  display: flex;
  gap: 16px;
}

.thermometer {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.thermometer-body {
  width: 50px;
  height: 280px;
  background: rgba(255, 255, 255, 0.08);
  border-radius: 25px;
  position: relative;
  overflow: hidden;
  border: 2px solid rgba(255, 255, 255, 0.1);
}

.mercury {
  position: absolute;
  bottom: 0;
  left: 4px;
  right: 4px;
  border-radius: 21px;
  transition: height 1.8s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.scale-marks {
  position: absolute;
  inset: 10px 0;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 0 8px;
  
  span {
    height: 2px;
    background: rgba(255, 255, 255, 0.2);
  }
}

/* 温度计底座 */
.thermometer-bulb {
  width: 70px;
  height: 70px;
  border-radius: 50%;
  margin-top: -20px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 3px solid rgba(255, 255, 255, 0.2);
  box-shadow: 0 0 30px rgba(6, 182, 212, 0.4);
}

.bulb-score {
  color: white;
  font-size: 24px;
  font-weight: 700;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.3);
}

.thermometer-labels {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  height: 280px;
  font-size: 12px;
  color: var(--fc-text-disabled);
  padding: 10px 0;
}

.profile-section {
  flex: 1;
  max-width: 500px;
}

.glass-card {
  background: var(--fc-panel-hover);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid var(--fc-border);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
}

.profile-card {
  border-radius: 24px;
  padding: 32px;
}

.profile-header {
  display: flex;
  gap: 20px;
  margin-bottom: 16px;
}

.profile-icon {
  width: 72px;
  height: 72px;
  border-radius: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 36px;
  flex-shrink: 0;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.3);
}

.profile-title {
  flex: 1;
}

.profile-label {
  color: #ffffff;
  font-size: 28px;
  font-weight: 700;
  margin-bottom: 8px;
}

.tag-cloud {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  
  .el-tag {
    background: rgba(255, 255, 255, 0.1);
    border-color: rgba(255, 255, 255, 0.2);
  }
}

.profile-desc {
  color: var(--fc-text-muted);
  font-size: 15px;
  line-height: 1.6;
  margin-bottom: 24px;
}

/* 双轨对比条 */
.comparison-box {
  background: var(--fc-panel-hover);
  border-radius: 16px;
  padding: 20px;
  margin-bottom: 24px;
}

.comparison-item {
  margin-bottom: 16px;
  
  &:last-child {
    margin-bottom: 0;
  }
}

.label-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.comp-label {
  color: var(--fc-text-muted);
  font-size: 13px;
}

.comp-value {
  font-size: 18px;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
  
  &.gray {
    color: var(--fc-text-disabled);
  }
  
  &.highlight {
    text-shadow: 0 0 10px currentColor;
  }
}

.allocation-section {
  margin-bottom: 24px;
  
  h3 {
    color: #e2e8f0;
    font-size: 15px;
    margin-bottom: 12px;
  }
}

.pie-chart {
  width: 100%;
  height: 180px;
}

/* AI 诊断卡片 */
.ai-diagnosis-card {
  background: linear-gradient(135deg, rgba(6, 182, 212, 0.15), rgba(79, 70, 229, 0.1));
  border: 1px solid rgba(6, 182, 212, 0.3);
  border-radius: 16px;
  padding: 16px 20px;
  margin-bottom: 24px;
  display: flex;
  gap: 14px;
  align-items: flex-start;
}

.ai-icon {
  font-size: 28px;
  flex-shrink: 0;
}

.ai-text {
  color: #e2e8f0;
  font-size: 14px;
  line-height: 1.7;
  margin: 0;
}

.cta-btn {
  width: 100%;
  height: 50px;
  border-radius: 14px;
  font-size: 16px;
  font-weight: 600;
  background: linear-gradient(135deg, #06b6d4, var(--primary-color));
  border: none;
  transition: all 0.3s ease;
  
  &:hover {
    transform: scale(1.02);
    box-shadow: 0 8px 24px rgba(6, 182, 212, 0.4);
  }
}

.loading-state {
  text-align: center;
  color: var(--fc-text-muted);
  
  .spinner {
    width: 50px;
    height: 50px;
    border: 3px solid rgba(255, 255, 255, 0.1);
    border-top-color: #06b6d4;
    border-radius: 50%;
    animation: spin 1s linear infinite;
    margin: 0 auto 20px;
  }
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.btn-group {
    display: flex;
    gap: 16px;
    margin-top: 24px;
}

.flex-1 {
    flex: 1;
}

.retake-btn {
    height: 50px;
    border-radius: 14px;
    font-size: 16px;
    background: rgba(255,255,255,0.05);
    border: 1px solid rgba(255,255,255,0.1);
    color: var(--fc-text-muted);
}

.retake-btn:hover {
    background: rgba(255,255,255,0.1);
    color: white;
    border-color: rgba(255,255,255,0.2);
}
</style>
