<template>
  <div class="diagnosis-page">
    <header class="page-header">
      <div class="header-content">
        <h1 class="title">资产体检中心</h1>
        <p class="subtitle">AI 智能诊断您的财富健康状况</p>
      </div>
      <div class="user-badge" v-if="report">
        <el-tag 
          :type="report.userType === 'INVESTOR' ? 'success' : 'info'" 
          effect="dark" 
          size="large"
          class="persona-tag"
        >
          <span class="icon">{{ report.userType === 'INVESTOR' ? '📈' : '🔰' }}</span>
          {{ report.userType === 'INVESTOR' ? '进阶投资者' : '储蓄型用户' }}
        </el-tag>
      </div>
    </header>

    <div v-loading="loading" class="diagnosis-content">
      <!-- 错误/空状态 -->
      <div v-if="error || (!loading && !report)" class="empty-state">
         <el-empty :description="error || '暂无体检报告，请先录入资产'">
           <el-button type="primary" @click="retryFetch">刷新重试</el-button>
         </el-empty>
      </div>

      <div v-else-if="report" class="report-container">
        <!-- 核心评分卡片 -->
        <div class="score-section">
          <div class="total-score-card" :class="getScoreClass(report.score)">
            <div class="score-circle">
              <span class="score-value">{{ report.score }}</span>
              <span class="score-label">健康分</span>
            </div>
            <div class="score-level">{{ report.level }}</div>
            <p class="score-desc" v-if="report.userType === 'NOVICE'">
              您的资产结构较为单一，抗通胀能力较弱。
            </p>
            <p class="score-desc" v-else>
              您的资产配置超越了 {{ Math.min(99, report.score + 10) }}% 的用户。
            </p>
          </div>

          <!-- 维度评分 (仅进阶用户显示雷达/维度，新手用户显示通胀对抗图) -->
          <div class="dimensions-card">
            <template v-if="report.userType === 'INVESTOR'">
              <h3 class="card-title">四维健康模型</h3>
              <div class="dimension-grid">
                <div class="dim-item">
                  <span class="label">流动性</span>
                  <el-progress :percentage="report.liquidityScore / 20 * 100" :color="getColor(report.liquidityScore, 20)" />
                  <span class="val">{{ report.liquidityScore }}/20</span>
                </div>
                <div class="dim-item">
                  <span class="label">风险匹配</span>
                  <el-progress :percentage="report.riskMatchScore / 40 * 100" :color="getColor(report.riskMatchScore, 40)" />
                  <span class="val">{{ report.riskMatchScore }}/40</span>
                </div>
                <div class="dim-item">
                  <span class="label">保障力</span>
                  <el-progress :percentage="report.protectionScore / 20 * 100" :color="getColor(report.protectionScore, 20)" />
                  <span class="val">{{ report.protectionScore }}/20</span>
                </div>
                <div class="dim-item">
                  <span class="label">分散度</span>
                  <el-progress :percentage="report.diversityScore / 20 * 100" :color="getColor(report.diversityScore, 20)" />
                  <span class="val">{{ report.diversityScore }}/20</span>
                </div>
              </div>
            </template>
            <template v-else>
              <h3 class="card-title">闲钱激活潜力</h3>
              <div class="novice-insight">
                <div class="inflation-chart">
                  <div class="bar cash" style="height: 60%">
                    <span>现金收益 (~2%)</span>
                  </div>
                  <div class="bar cpi" style="height: 80%">
                    <span>通胀率 (~3%)</span>
                  </div>
                  <div class="bar target" style="height: 100%">
                    <span>理财目标 (>4%)</span>
                  </div>
                </div>
                <p class="insight-text">
                  ⚠️ 警告：长期持有大量现金可能导致购买力缩水。建议您尝试低风险理财产品。
                </p>
              </div>
            </template>
          </div>
        </div>

        <!-- 诊断建议列表 -->
        <h3 class="section-title">AI 优化建议</h3>
        <div class="suggestions-list">
          <div 
            v-for="(suggestion, index) in report.suggestions" 
            :key="index"
            class="suggestion-item"
            :class="suggestion.type"
          >
            <div class="icon-box">
              <span v-if="suggestion.type === 'success'">✅</span>
              <span v-else-if="suggestion.type === 'warning'">⚠️</span>
              <span v-else>💡</span>
            </div>
            <div class="content">{{ suggestion.message }}</div>
          </div>
        </div>
        
        <div class="action-area">
          <el-button type="primary" size="large" @click="$router.push('/plan')">
            前往生成优化方案
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, computed } from 'vue';
import { useHealthStore } from '@/store/modules/health';
import { storeToRefs } from 'pinia';

const healthStore = useHealthStore();
const { report, loading, error } = storeToRefs(healthStore);

onMounted(() => {
  healthStore.fetchHealthReport();
});

const retryFetch = () => {
  healthStore.fetchHealthReport();
};

const getScoreClass = (score: number) => {
  if (score >= 80) return 'excellent';
  if (score >= 60) return 'good';
  return 'risk';
};

const getColor = (score: number, max: number) => {
  const ratio = score / max;
  if (ratio >= 0.8) return '#67C23A';
  if (ratio >= 0.6) return '#409EFF';
  return '#F56C6C';
};
</script>

<style lang="scss" scoped>
@use "@/theme/variables.scss" as *;

.diagnosis-page {
  min-height: 100vh;
  padding: 32px;
  background: linear-gradient(135deg, #0f172a, #1e293b);
  color: white;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 32px;

  .title {
    font-size: 28px;
    font-weight: 700;
    margin: 0 0 8px 0;
    background: linear-gradient(to right, #fff, #94a3b8);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
  }

  .subtitle {
    color: $text-secondary;
    margin: 0;
  }
}

.persona-tag {
  font-size: 16px;
  padding: 8px 16px;
  height: auto;
  
  .icon {
    margin-right: 8px;
  }
}

.score-section {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
  margin-bottom: 32px;
}

.total-score-card {
  background: rgba(255, 255, 255, 0.05);
  border-radius: 16px;
  padding: 32px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border: 1px solid rgba(255, 255, 255, 0.1);
  position: relative;
  overflow: hidden;

  &.excellent { box-shadow: 0 0 30px rgba(103, 194, 58, 0.15); border-color: rgba(103, 194, 58, 0.3); }
  &.good { box-shadow: 0 0 30px rgba(64, 158, 255, 0.15); border-color: rgba(64, 158, 255, 0.3); }
  &.risk { box-shadow: 0 0 30px rgba(245, 108, 108, 0.15); border-color: rgba(245, 108, 108, 0.3); }

  .score-circle {
    position: relative;
    width: 120px;
    height: 120px;
    border-radius: 50%;
    border: 4px solid rgba(255, 255, 255, 0.1);
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    margin-bottom: 16px;
    
    .score-value {
      font-size: 48px;
      font-weight: 800;
      line-height: 1;
    }
    
    .score-label {
      font-size: 12px;
      color: $text-dim;
      margin-top: 4px;
    }
  }

  .score-level {
    font-size: 24px;
    font-weight: 600;
    margin-bottom: 8px;
  }

  .score-desc {
    color: $text-secondary;
    text-align: center;
    font-size: 14px;
    margin: 0;
  }
}

.dimensions-card {
  background: rgba(255, 255, 255, 0.05);
  border-radius: 16px;
  padding: 24px;
  border: 1px solid rgba(255, 255, 255, 0.1);

  .card-title {
    margin: 0 0 24px 0;
    font-size: 18px;
    color: $text-primary;
  }
}

.dimension-grid {
  display: flex;
  flex-direction: column;
  gap: 20px;

  .dim-item {
    display: grid;
    grid-template-columns: 80px 1fr 60px;
    align-items: center;
    gap: 16px;
    
    .label { color: $text-secondary; font-size: 14px; }
    .val { color: $text-primary; text-align: right; font-family: 'Roboto Mono'; font-size: 14px; }
  }
}

.novice-insight {
  height: 200px;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;

  .inflation-chart {
    flex: 1;
    display: flex;
    align-items: flex-end;
    justify-content: space-around;
    padding-bottom: 16px;
    border-bottom: 1px solid rgba(255, 255, 255, 0.1);

    .bar {
      width: 60px;
      border-radius: 4px 4px 0 0;
      position: relative;
      transition: all 0.3s;
      
      span {
        position: absolute;
        top: -25px;
        left: 50%;
        transform: translateX(-50%);
        font-size: 12px;
        white-space: nowrap;
        color: $text-dim;
      }
      
      &.cash { background: #909399; opacity: 0.7; }
      &.cpi { background: #F56C6C; opacity: 0.9; }
      &.target { background: #67C23A; }
    }
  }

  .insight-text {
    margin-top: 16px;
    font-size: 13px;
    color: #E6A23C;
    line-height: 1.5;
  }
}

.section-title {
  margin: 0 0 20px 0;
  font-size: 20px;
  color: $text-primary;
}

.suggestions-list {
  display: grid;
  gap: 16px;
  margin-bottom: 32px;
}

.suggestion-item {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding: 16px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.05);

  .icon-box {
    font-size: 20px;
  }

  .content {
    font-size: 15px;
    line-height: 1.6;
    color: $text-primary;
  }

  &.success { background: rgba(103, 194, 58, 0.1); border-color: rgba(103, 194, 58, 0.2); }
  &.warning { background: rgba(230, 162, 60, 0.1); border-color: rgba(230, 162, 60, 0.2); }
  &.info { background: rgba(64, 158, 255, 0.1); border-color: rgba(64, 158, 255, 0.2); }
}

.action-area {
  display: flex;
  justify-content: center;
}
</style>
