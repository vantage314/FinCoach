<template>
  <el-container class="split-layout">
    <el-aside width="240px" class="sidebar">
      <div class="brand" @click="router.push('/app/diagnosis')">
        <el-icon class="brand-icon"><Monitor /></el-icon>
        <span class="brand-text">FinCoach</span>
      </div>

      <el-menu
        router
        :default-active="activeMenu"
        class="side-menu"
        :collapse="false"
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

    </el-aside>

    <el-container class="content-shell">
      <el-header class="topbar">
        <div class="page-title">{{ pageTitle }}</div>
        <div class="user-area">
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
  Wallet
} from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import { useUserStore } from '@/store/modules/user';

const route = useRoute();
const router = useRouter();
const userStore = useUserStore();

const activeMenu = computed(() => route.path);

const displayName = computed(() => userStore.username || '用户');
const displayInitial = computed(() => {
  const name = userStore.username;
  if (name && name.length > 0) return name.charAt(0).toUpperCase();
  return 'U';
});

const pageTitle = computed(() => {
  const raw = (route.meta.title as string) || 'FinCoach';
  return raw.replace(' - FinCoach', '');
});

const handleLogout = async () => {
  try {
    await userStore.logout();
    ElMessage.success('已退出登录');
    router.push('/login');
  } catch (error) {
    console.error('[UserLayout] 退出登录失败:', error);
    ElMessage.error('退出失败，请重试');
  }
};
</script>

<style lang="scss" scoped>
@use "@/theme/variables.scss" as *;

.split-layout {
  min-height: 100vh;
  background-color: $background-dark;
}

.sidebar {
  background: rgba(15, 23, 42, 0.85);
  border-right: 1px solid rgba(255, 255, 255, 0.08);
  display: flex;
  flex-direction: column;
}

.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 18px 20px;
  color: $text-light;
  cursor: pointer;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
}

.brand-icon {
  color: $primary-color;
}

.brand-text {
  font-size: 18px;
  font-weight: 700;
}

.side-menu {
  flex: 1;
  border-right: none !important;
  background: transparent !important;

  :deep(.el-menu-item),
  :deep(.el-sub-menu__title) {
    color: $text-dim;
    background: transparent !important;
  }

  :deep(.el-menu-item.is-active) {
    color: $text-light;
    background: rgba(59, 130, 246, 0.12) !important;
    border-left: 3px solid $primary-color;
  }
}


.content-shell {
  min-height: 100vh;
}

.topbar {
  height: 56px;
  padding: 0 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: rgba(15, 23, 42, 0.6);
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.page-title {
  color: $text-light;
  font-size: 16px;
  font-weight: 600;
}

.user-area {
  display: flex;
  align-items: center;
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
  min-height: calc(100vh - 56px);
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
