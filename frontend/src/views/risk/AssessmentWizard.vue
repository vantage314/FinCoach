<template>
  <div class="assessment-wizard">
    <!-- 进度条 -->
    <div class="progress-section">
      <el-progress
        :percentage="progressPercent"
        :stroke-width="8"
        :show-text="false"
        color="var(--primary-color)"
      />
      <span class="progress-text">{{ currentIndex + 1 }} / {{ questions.length }}</span>
    </div>

    <!-- 题目卡片 -->
    <div class="question-container">
      <transition name="slide-fade" mode="out-in">
        <div :key="currentQuestion.id" class="question-card glass-card">
          <div class="question-category">
            {{ categoryLabels[currentQuestion.category] }}
            <span v-if="currentQuestion.isKiller" class="killer-badge">关键题</span>
          </div>
          <h2 class="question-title">{{ currentQuestion.title }}</h2>
          
          <div class="options-list">
            <button
              v-for="option in currentQuestion.options"
              :key="option.value"
              class="option-btn"
              :class="{ 
                'selected': selectedOption === option.value,
                'disabled': isAnimating 
              }"
              :disabled="isAnimating"
              @click="selectOption(option)"
            >
              <span class="option-label">{{ option.label }}</span>
              <span class="option-check" v-if="selectedOption === option.value">✓</span>
            </button>
          </div>
        </div>
      </transition>
    </div>

    <!-- 底部导航 -->
    <div class="nav-section">
      <el-button
        v-if="currentIndex > 0"
        type="default"
        class="nav-btn"
        @click="prevQuestion"
        :disabled="isAnimating"
      >
        ← 上一题
      </el-button>
    </div>

    <!-- 提交 Loading -->
    <div class="submit-overlay" v-if="isSubmitting">
      <div class="submit-content">
        <div class="spinner"></div>
        <p>正在分析您的风险偏好...</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { questions, categoryLabels, type Question, type Option } from './questions';
import { submitAssessment } from '@/api/risk';
import { useHealthStore } from '@/store/modules/health';

const router = useRouter();
const healthStore = useHealthStore();

const currentIndex = ref(0);
const answers = ref<Record<string, number>>({});
const selectedOption = ref<string | null>(null);
const isAnimating = ref(false);
const isSubmitting = ref(false);

const currentQuestion = computed<Question>(() => questions[currentIndex.value]);
const progressPercent = computed(() => ((currentIndex.value + 1) / questions.length) * 100);

const selectOption = async (option: Option) => {
  if (isAnimating.value) return;
  
  // 选中动画
  selectedOption.value = option.value;
  
  // 记录答案
  const key = `q${currentQuestion.value.id}`;
  answers.value[key] = option.score;
  
  // 防误触锁定
  isAnimating.value = true;
  
  setTimeout(async () => {
    if (currentIndex.value < questions.length - 1) {
      // 下一题
      currentIndex.value++;
      selectedOption.value = null;
    } else {
      // 提交测评
      await submitResult();
    }
    isAnimating.value = false;
  }, 400);
};

const prevQuestion = () => {
  if (currentIndex.value > 0 && !isAnimating.value) {
    currentIndex.value--;
    // 恢复之前的选择
    const key = `q${questions[currentIndex.value].id}`;
    const prevScore = answers.value[key];
    if (prevScore !== undefined) {
      const prevOption = questions[currentIndex.value].options.find(o => o.score === prevScore);
      selectedOption.value = prevOption?.value || null;
    } else {
      selectedOption.value = null;
    }
  }
};

const submitResult = async () => {
  isSubmitting.value = true;
  try {
    const res: any = await submitAssessment(answers.value);
    if (res.code === 200) {
      ElMessage.success('测评完成！');
      await healthStore.fetchHealthReport();
      router.push({ path: '/diagnosis' });
    } else {
      ElMessage.error(res.message || '提交失败');
      isSubmitting.value = false;
    }
  } catch (error) {
    console.error('提交测评失败:', error);
    ElMessage.error('网络请求失败');
    isSubmitting.value = false;
  }
};
</script>

<style lang="scss" scoped>
.assessment-wizard {
  min-height: 100vh;
  background: var(--fc-bg-gradient);
  padding: 40px 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
  position: relative;
}

.progress-section {
  width: 100%;
  max-width: 600px;
  margin-bottom: 40px;
  
  :deep(.el-progress-bar__outer) {
    background-color: rgba(255, 255, 255, 0.1);
  }
  
  .progress-text {
    display: block;
    text-align: center;
    margin-top: 12px;
    color: var(--fc-text-muted);
    font-size: 14px;
    font-variant-numeric: tabular-nums;
  }
}

.question-container {
  width: 100%;
  max-width: 600px;
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* Glassmorphism 毛玻璃效果 */
.glass-card {
  background: var(--fc-panel-hover);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid var(--fc-border);
  box-shadow: 
    0 8px 32px rgba(0, 0, 0, 0.3),
    inset 0 1px 0 rgba(255, 255, 255, 0.1);
}

.question-card {
  width: 100%;
  border-radius: 24px;
  padding: 40px;
}

.question-category {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #06b6d4;
  font-size: 14px;
  font-weight: 500;
  margin-bottom: 16px;
}

.killer-badge {
  background: linear-gradient(135deg, #f59e0b, #ef4444);
  color: white;
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 10px;
  font-weight: 600;
}

.question-title {
  color: #ffffff;
  font-size: 24px;
  font-weight: 600;
  line-height: 1.4;
  margin-bottom: 32px;
}

.options-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.option-btn {
  width: 100%;
  padding: 18px 24px;
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 16px;
  color: #e2e8f0;
  font-size: 16px;
  text-align: left;
  cursor: pointer;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  display: flex;
  justify-content: space-between;
  align-items: center;
  
  &:hover:not(.disabled) {
    background: rgba(6, 182, 212, 0.15);
    border-color: #06b6d4;
    transform: translateY(-3px);
    box-shadow: 0 8px 20px rgba(6, 182, 212, 0.2);
  }
  
  &.selected {
    background: linear-gradient(135deg, rgba(6, 182, 212, 0.3), rgba(79, 70, 229, 0.3));
    border-color: #06b6d4;
    box-shadow: 0 0 20px rgba(6, 182, 212, 0.3);
  }
  
  &.disabled {
    opacity: 0.6;
    cursor: not-allowed;
    pointer-events: none;
  }
}

.option-check {
  width: 24px;
  height: 24px;
  background: #06b6d4;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 14px;
  font-weight: bold;
}

.nav-section {
  width: 100%;
  max-width: 600px;
  margin-top: 32px;
  display: flex;
  justify-content: flex-start;
}

.nav-btn {
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.2);
  color: #e2e8f0;
  border-radius: 12px;
  
  &:hover {
    background: rgba(255, 255, 255, 0.15);
    border-color: rgba(255, 255, 255, 0.3);
  }
}

/* 提交 Loading 遮罩 */
.submit-overlay {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.95);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100;
}

.submit-content {
  text-align: center;
  color: #ffffff;
  
  p {
    margin-top: 24px;
    font-size: 18px;
    color: var(--fc-text-muted);
  }
}

.spinner {
  width: 60px;
  height: 60px;
  border: 3px solid rgba(255, 255, 255, 0.1);
  border-top-color: #06b6d4;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin: 0 auto;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* 滑动淡入动画 */
.slide-fade-enter-active {
  transition: all 0.35s ease-out;
}

.slide-fade-leave-active {
  transition: all 0.25s ease-in;
}

.slide-fade-enter-from {
  opacity: 0;
  transform: translateX(40px);
}

.slide-fade-leave-to {
  opacity: 0;
  transform: translateX(-40px);
}
</style>
