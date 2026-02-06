<template>
  <div class="safety-panel glass-panel">
    <div class="panel-content">
      <div class="goal-info">
        <h3>安全垫构建目标</h3>
        <p class="tip">建议您优先存够 3-6 个月生活费</p>
      </div>
      
      <div class="progress-area">
        <el-progress 
            type="dashboard" 
            :percentage="percentage" 
            :color="colors"
            :width="150"
        >
            <template #default="{ percentage }">
                <div class="progress-label">
                    <span class="percentage">{{ percentage }}%</span>
                    <span class="label">达成率</span>
                </div>
            </template>
        </el-progress>
      </div>
      
      <div class="action-area">
        <p class="amount">
            <span class="current">¥{{ currentCash.toLocaleString() }}</span>
            <span class="separator">/</span>
            <span class="target">¥30,000</span>
        </p>
        <div class="gap-info" v-if="gap > 0">
            还差 <span class="gap">¥{{ gap.toLocaleString() }}</span> 加油！
        </div>
        <div class="gap-info success" v-else>
            🎉 目标已达成！
        </div>
        <el-button type="primary" round class="deposit-btn" @click="$emit('add-cash')">
            存一笔
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';

const props = defineProps<{
  currentCash: number
}>();

defineEmits(['add-cash']);

const TARGET = 30000;

const percentage = computed(() => {
  return Math.min(100, Math.floor((props.currentCash / TARGET) * 100));
});

const gap = computed(() => Math.max(0, TARGET - props.currentCash));

const colors = [
  { color: '#f56c6c', percentage: 20 },
  { color: '#e6a23c', percentage: 40 },
  { color: '#5cb87a', percentage: 60 },
  { color: '#1989fa', percentage: 80 },
  { color: '#6f7ad3', percentage: 100 },
];
</script>

<style lang="scss" scoped>
@use "@/theme/variables.scss" as *;

.safety-panel {
  background: rgba(30, 41, 59, 0.6);
  backdrop-filter: blur(12px);
  border-radius: 16px;
  padding: 24px;
  border: 1px solid rgba(255, 255, 255, 0.05);
  margin-bottom: 24px;
}

.panel-content {
  display: flex;
  align-items: center;
  justify-content: space-around;
  
  @media (max-width: 768px) {
    flex-direction: column;
    gap: 24px;
    text-align: center;
  }
}

.goal-info {
  h3 {
    margin: 0 0 8px 0;
    font-size: 20px;
    color: $text-primary;
  }
  .tip {
    margin: 0;
    color: $text-secondary;
    font-size: 13px;
  }
}

.progress-label {
  display: flex;
  flex-direction: column;
  align-items: center;
  
  .percentage {
    font-size: 28px;
    font-weight: 700;
    color: $text-primary;
  }
  
  .label {
    font-size: 12px;
    color: $text-secondary;
  }
}

.action-area {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;

  .amount {
    font-family: 'Roboto Mono', monospace;
    font-size: 18px;
    color: $text-primary;
    margin: 0;
    
    .current { color: $primary; font-size: 24px; font-weight: 600; }
    .separator { margin: 0 8px; color: $text-dim; }
    .target { color: $text-secondary; }
  }
  
  .gap-info {
    font-size: 13px;
    color: $text-secondary;
    
    .gap { color: $warning; font-weight: 600; }
    
    &.success { color: $success; }
  }
  
  .deposit-btn {
    margin-top: 8px;
    padding: 10px 32px;
    font-weight: 600;
  }
}
</style>
