<template>
  <div class="health-card" :class="levelClass" @click="handleClick">
    <div class="score-section">
      <div class="score-ring" :style="ringStyle">
        <span class="score-value">{{ animatedScore }}</span>
      </div>
      <span class="score-label">{{ report?.level || '待检测' }}</span>
    </div>
    <div class="suggestions-section">
      <div 
        v-for="(item, index) in topSuggestions" 
        :key="index"
        class="suggestion-item"
        :class="item.type"
      >
        {{ item.message }}
      </div>
      <div v-if="!report" class="empty-hint">
        点击进行资产体检
      </div>
    </div>
    <div class="action-section" v-if="report">
      <el-button 
        type="primary" 
        size="small" 
        @click.stop="handleGeneratePlan"
      >
        生成优化方案
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch, onMounted } from 'vue';
import { useHealthStore } from '@/store/modules/health';

const healthStore = useHealthStore();

const report = computed(() => healthStore.report);
const animatedScore = ref(0);

// 分数动画
watch(() => report.value?.score, (newScore) => {
  if (newScore !== undefined) {
    animateScore(newScore);
  }
}, { immediate: true });

const animateScore = (target: number) => {
  const duration = 1000;
  const start = animatedScore.value;
  const startTime = Date.now();
  
  const animate = () => {
    const elapsed = Date.now() - startTime;
    const progress = Math.min(elapsed / duration, 1);
    // easeOutCubic
    const eased = 1 - Math.pow(1 - progress, 3);
    animatedScore.value = Math.round(start + (target - start) * eased);
    
    if (progress < 1) {
      requestAnimationFrame(animate);
    }
  };
  
  requestAnimationFrame(animate);
};

const levelClass = computed(() => {
  if (!report.value) return 'empty';
  const score = report.value.score;
  if (score >= 80) return 'excellent';
  if (score >= 60) return 'good';
  if (score >= 40) return 'fair';
  return 'poor';
});

const ringStyle = computed(() => {
  const score = report.value?.score || 0;
  const percentage = score;
  const color = getScoreColor(score);
  return {
    background: `conic-gradient(${color} ${percentage}%, rgba(255,255,255,0.1) ${percentage}%)`
  };
});

const getScoreColor = (score: number) => {
  if (score >= 80) return '#10b981';
  if (score >= 60) return '#f59e0b';
  return '#ef4444';
};

const topSuggestions = computed(() => {
  return (report.value?.suggestions || []).slice(0, 2);
});

const emit = defineEmits<{
  (e: 'generate-plan'): void;
}>();

const handleClick = () => {
  healthStore.fetchHealthReport();
};

const handleGeneratePlan = () => {
  emit('generate-plan');
};

onMounted(() => {
  if (!report.value) {
    healthStore.fetchHealthReport();
  }
});
</script>

<style lang="scss" scoped>
.health-card {
  background: rgba(255, 255, 255, 0.05);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 16px;
  padding: 20px;
  display: flex;
  gap: 20px;
  cursor: pointer;
  transition: all 0.3s ease;
  
  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.2);
  }
  
  &.excellent {
    border-color: rgba(16, 185, 129, 0.3);
  }
  
  &.good {
    border-color: rgba(245, 158, 11, 0.3);
  }
  
  &.fair, &.poor {
    border-color: rgba(239, 68, 68, 0.3);
  }
}

.score-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.score-ring {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  
  &::before {
    content: '';
    position: absolute;
    inset: 6px;
    background: #1e293b;
    border-radius: 50%;
  }
}

.score-value {
  position: relative;
  z-index: 1;
  font-size: 24px;
  font-weight: 700;
  color: white;
  font-variant-numeric: tabular-nums;
}

.score-label {
  font-size: 12px;
  color: #94a3b8;
}

.suggestions-section {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 8px;
}

.suggestion-item {
  font-size: 13px;
  line-height: 1.5;
  padding: 6px 10px;
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.04);
  
  &.success {
    color: #34d399;
  }
  
  &.warning {
    color: #fbbf24;
  }
  
  &.info {
    color: #60a5fa;
  }
}

.empty-hint {
  color: #64748b;
  font-size: 13px;
}

.action-section {
  display: flex;
  align-items: center;
  
  .el-button {
    background: linear-gradient(135deg, #06b6d4, #3b82f6);
    border: none;
    
    &:hover {
      opacity: 0.9;
    }
  }
}
</style>
