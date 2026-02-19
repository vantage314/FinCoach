<template>
  <el-container class="top-layout">
    <el-header class="fixed-header">
      <div class="logo-area" @click="router.push('/app/diagnosis')">
        <el-icon :size="26" class="logo-icon"><Monitor /></el-icon>
        <span class="brand-text">FinCoach</span>
      </div>

      <el-menu
        mode="horizontal"
        router
        :default-active="activeMenu"
        class="glass-menu"
        :ellipsis="false"
      >
        <el-menu-item index="/app/diagnosis">
          <el-icon><FirstAidKit /></el-icon>
          <span>体检/报告</span>
        </el-menu-item>
        <el-menu-item index="/app/market">
          <el-icon><TrendCharts /></el-icon>
          <span>市场中心</span>
        </el-menu-item>
        <el-sub-menu index="assets">
          <template #title>
            <el-icon><Wallet /></el-icon>
            <span>我的资产</span>
          </template>
          <el-menu-item index="/app/asset/analysis">
            <el-icon><DataLine /></el-icon>
            <span>资产全景 & 体检</span>
          </el-menu-item>
          <el-menu-item index="/app/asset/manage">
            <el-icon><EditPen /></el-icon>
            <span>账本管理</span>
          </el-menu-item>
          <el-menu-item index="/app/debt">
            <el-icon><Wallet /></el-icon>
            <span>债务管理</span>
          </el-menu-item>
          <el-menu-item index="/app/cashflow">
            <el-icon><DataLine /></el-icon>
            <span>现金流管理</span>
          </el-menu-item>
          <el-menu-item index="/app/dashboard">
            <el-icon><PieChart /></el-icon>
            <span>资产总览</span>
          </el-menu-item>
        </el-sub-menu>
        <el-menu-item index="/app/investment">
          <el-icon><TrendCharts /></el-icon>
          <span>智能投资</span>
        </el-menu-item>
        <el-menu-item index="/app/plan">
          <el-icon><EditPen /></el-icon>
          <span>投资计划</span>
        </el-menu-item>
        <el-menu-item index="/app/chat">
          <el-icon><ChatDotRound /></el-icon>
          <span>AI 咨询</span>
        </el-menu-item>
        <el-menu-item index="/app/user/profile">
          <el-icon><User /></el-icon>
          <span>个人中心</span>
        </el-menu-item>
      </el-menu>

      <div class="user-area">
        <el-button
          v-if="isAdmin"
          type="primary"
          plain
          size="small"
          class="admin-entry"
          @click="router.push('/admin/alerts')"
        >
          进入后台
        </el-button>
        <el-dropdown trigger="click">
          <span class="user-dropdown-link">
            <el-avatar :size="30" class="user-avatar">{{ displayInitial }}</el-avatar>
            <span class="user-name">{{ displayName }}</span>
            <el-icon class="arrow-icon"><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="router.push('/risk/result')">
                我的风险画像
              </el-dropdown-item>
              <el-dropdown-item @click="router.push('/app/user/profile')">
                个人中心
              </el-dropdown-item>
              <el-dropdown-item divided @click="handleLogout">
                退出登录
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </el-header>

    <el-main class="main-content">
      <router-view v-slot="{ Component }">
        <transition name="fade-transform" mode="out-in">
          <component :is="Component" :key="$route.fullPath" />
        </transition>
      </router-view>
    </el-main>
  </el-container>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import {
  ArrowDown,
  ChatDotRound,
  DataLine,
  EditPen,
  FirstAidKit,
  Monitor,
  PieChart,
  TrendCharts,
  User,
  Wallet,
} from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import { useUserStore } from '@/store/modules/user';

const route = useRoute();
const router = useRouter();
const userStore = useUserStore();

const isAdmin = computed(() => userStore.isAdmin);
const activeMenu = computed(() => route.path);

const displayName = computed(() => userStore.username || '用户');
const displayInitial = computed(() => {
  const name = userStore.username;
  if (name && name.length > 0) return name.charAt(0).toUpperCase();
  return 'U';
});

const handleLogout = async () => {
  try {
    await userStore.logout();
    ElMessage.success('已退出登录');
    router.push('/login');
  } catch (error) {
    console.error('[UserTopLayout] 退出登录失败:', error);
    ElMessage.error('退出失败，请重试');
  }
};
</script>

<style lang="scss" scoped>
@use "@/theme/variables.scss" as *;

.top-layout {
  min-height: 100vh;
  background-color: $background-dark;
}

.fixed-header {
  position: sticky;
  top: 0;
  z-index: 1000;
  height: 64px;
  padding: 0 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: rgba(15, 23, 42, 0.6) !important;
  backdrop-filter: blur(14px) !important;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.logo-area {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
  cursor: pointer;
}

.logo-icon {
  color: $primary-color;
}

.brand-text {
  font-size: 18px;
  font-weight: 700;
  color: $text-light;
}

.glass-menu {
  flex: 1;
  display: flex;
  justify-content: center;
  background: transparent !important;
  border-bottom: none !important;
  height: 64px;

  :deep(.el-menu-item),
  :deep(.el-sub-menu__title) {
    height: 64px;
    line-height: 64px;
    color: $text-dim;
    border-bottom: 2px solid transparent;
    padding: 0 16px;
    background: transparent !important;
  }

  :deep(.el-menu-item.is-active) {
    color: $text-light;
    border-bottom-color: $primary-color;
  }
}

.user-area {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}

.admin-entry {
  margin-right: 4px;
}

.user-dropdown-link {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  color: $text-light;
}

.user-avatar {
  background: linear-gradient(135deg, $primary-color, #06b6d4);
  color: white;
  font-weight: 600;
}

.user-name {
  font-size: 13px;
  max-width: 100px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.arrow-icon {
  font-size: 12px;
  color: $text-dim;
}

.main-content {
  padding: 20px 24px;
  background-color: $background-dark;
  min-height: calc(100vh - 64px);
}

.fade-transform-enter-active,
.fade-transform-leave-active {
  transition: opacity 0.3s ease, transform 0.3s ease;
}

.fade-transform-enter-from {
  opacity: 0;
  transform: translateY(8px);
}

.fade-transform-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}
</style>
