<template>
  <div class="novice-panel glass-card">
    <div class="panel-header">
      <div class="header-left">
        <h3 class="title">您当前处于：{{ personaTag || '新手起步期' }}</h3>
        <p class="subtitle">打好地基，才能盖高楼！首要目标是构建充足的应急储备金。</p>
      </div>
      <div class="header-right">
        <el-tag type="success" size="large" effect="dark">🛡️ 基础保障优先</el-tag>
      </div>
    </div>

    <div class="goal-container">
      <div class="goal-info">
        <span class="label">安全储备目标</span>
        <span class="value">¥ {{ formatNumber(safetyThreshold) }}</span>
      </div>
      <div class="goal-progress">
        <el-progress 
            :text-inside="true" 
            :stroke-width="24" 
            :percentage="safetyProgress" 
            status="success"
            striped
            striped-flow
        />
      </div>
      <div class="gap-info" v-if="liquidityGap > 0">
        <span class="gap-text">距离达标还需: <span class="highlight">¥ {{ formatNumber(liquidityGap) }}</span></span>
      </div>
      <div class="congrats-info" v-else>
        <span class="success-text">🎉 恭喜！您的安全垫已达标，可以开启进阶投资之旅！</span>
      </div>
    </div>

    <div class="action-area" v-if="liquidityGap > 0">
      <el-button type="primary" size="large" class="deposit-btn" @click="$emit('add-cash')">
        💰 存一笔现金
      </el-button>
      <p class="tip">建议将闲置资金存入货币基金或银行存款</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { defineProps } from 'vue';

const props = defineProps<{
  safetyThreshold: number;
  liquidityGap: number;
  safetyProgress: number;
  personaTag: string;
}>();

defineEmits(['add-cash']);

const formatNumber = (num: number) => {
  return (num || 0).toLocaleString('zh-CN');
};
</script>

<style lang="scss" scoped>
@use "@/theme/variables.scss" as *;

.glass-card {
    background: rgba(255, 255, 255, 0.05);
    backdrop-filter: blur(10px);
    border: 1px solid rgba(255, 255, 255, 0.1);
    border-radius: 16px;
    padding: 24px;
    margin-bottom: 24px;
}

.panel-header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    margin-bottom: 24px;

    .title {
        font-size: 20px;
        font-weight: 700;
        color: white;
        margin-bottom: 8px;
    }

    .subtitle {
        color: #94a3b8;
        font-size: 14px;
    }
}

.goal-container {
    background: rgba(0, 0, 0, 0.2);
    border-radius: 12px;
    padding: 20px;
    margin-bottom: 24px;
}

.goal-info {
    display: flex;
    justify-content: space-between;
    margin-bottom: 12px;
    
    .label {
        color: #cbd5e1;
    }
    
    .value {
        color: white;
        font-weight: 700;
        font-size: 18px;
    }
}

.gap-info {
    margin-top: 12px;
    text-align: right;
    
    .gap-text {
        color: #94a3b8;
        font-size: 14px;
        
        .highlight {
            color: #f59e0b; // Amber
            font-weight: 700;
            font-size: 16px;
        }
    }
}

.congrats-info {
    margin-top: 12px;
    text-align: center;
    .success-text {
         color: #10b981; // Emerald
         font-weight: 700;
    }
}

.action-area {
    display: flex;
    flex-direction: column;
    align-items: center;
    
    .deposit-btn {
        width: 200px;
        font-weight: 700;
        margin-bottom: 12px;
    }
    
    .tip {
        color: #64748b;
        font-size: 12px;
    }
}
</style>
