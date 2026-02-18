<template>
  <el-container class="top-layout">
    <el-header class="fixed-header">
      <!-- Logo 区域 -->
      <div class="logo-area" @click="router.push('/dashboard')">
        <el-icon :size="28" class="logo-icon"><Monitor /></el-icon>
        <span class="brand-text">白领人士个人资产诊断与投资建议系统</span>
      </div>

      <!-- 导航菜单 -->
      <el-menu
        mode="horizontal"
        router
        :default-active="activeMenu"
        class="glass-menu"
        :ellipsis="false"
      >
        <el-menu-item index="/market">市场中心</el-menu-item>
        
        <!-- 🔥 Phase 14.8: 资产相关功能合并为子菜单 -->
        <el-sub-menu index="assets">
          <template #title>
            <el-icon><Wallet /></el-icon>
            <span>我的资产</span>
          </template>
          <el-menu-item index="/asset/analysis">
            <el-icon><DataLine /></el-icon>
            <span>资产全景 & 体检</span>
          </el-menu-item>
          <el-menu-item index="/asset/manage">
            <el-icon><EditPen /></el-icon>
            <span>账本管理 (记一笔)</span>
          </el-menu-item>
          <el-menu-item index="/dashboard">
            <el-icon><PieChart /></el-icon>
            <span>资产总览</span>
          </el-menu-item>
        </el-sub-menu>
        
        <el-menu-item index="/diagnosis">
           <el-icon><FirstAidKit /></el-icon>
           <span>智能诊断</span>
        </el-menu-item>
        <el-menu-item index="/investment">
          <el-icon><TrendCharts /></el-icon>
          <span>智能投资</span>
        </el-menu-item>
        <el-menu-item index="/plan">投资计划</el-menu-item>
        <el-menu-item index="/chat">AI 咨询</el-menu-item>
        <el-sub-menu v-if="isAdmin" index="admin">
          <template #title>
            <el-icon><Setting /></el-icon>
            <span>后台管理</span>
          </template>
          <el-menu-item index="/admin/alerts">
            <el-icon><Bell /></el-icon>
            <span>预警管理</span>
          </el-menu-item>
          <el-menu-item index="/admin/debug">
            <el-icon><Monitor /></el-icon>
            <span>Debug 快照</span>
          </el-menu-item>
        </el-sub-menu>
      </el-menu>

      <!-- 用户区域 -->
      <div class="user-area">
        <el-dropdown trigger="click">
          <span class="user-dropdown-link">
            <el-avatar :size="32" class="user-avatar">
              {{ displayInitial }}
            </el-avatar>
            <span class="user-name">{{ displayName }}</span>
            <el-icon class="arrow-icon"><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="router.push('/risk/result')">
                 我的风险画像
              </el-dropdown-item>
              <el-dropdown-item @click="router.push('/user/profile')">个人中心</el-dropdown-item>
              <el-dropdown-item divided @click="handleLogout">
                退出登录
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </el-header>

    <!-- 主内容区 -->
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
import { Monitor, ArrowDown, TrendCharts, FirstAidKit, Wallet, DataLine, EditPen, PieChart, Setting, Bell } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import { useUserStore } from '@/store/modules/user';

const route = useRoute();
const router = useRouter();
const userStore = useUserStore();
const isAdmin = computed(() => userStore.isAdmin);

// 当前激活的菜单项
const activeMenu = computed(() => route.path);

// 显示用户名
const displayName = computed(() => userStore.username || '用户');

// 显示头像首字母
const displayInitial = computed(() => {
  const name = userStore.username;
  if (name && name.length > 0) {
    return name.charAt(0).toUpperCase();
  }
  return 'U';
});

// 退出登录
const handleLogout = async () => {
  try {
    await userStore.logout();
    ElMessage.success('已退出登录');
    router.push('/login');
  } catch (error) {
    console.error('[TopLayout] 退出登录失败:', error);
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

/* 固定顶部导航栏 - Glassmorphism 风格 */
.fixed-header {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 1000;
  height: 64px;
  padding: 0 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  
  /* 增强的 Glassmorphism 效果 */
  background: rgba(15, 23, 42, 0.6) !important;
  backdrop-filter: blur(16px) !important;
  -webkit-backdrop-filter: blur(16px) !important;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  box-shadow: 0 4px 30px rgba(0, 0, 0, 0.1);
}

/* Logo 区域 */
.logo-area {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
  cursor: pointer;
  transition: opacity 0.2s ease;
  
  &:hover {
    opacity: 0.85;
  }
}

.logo-icon {
  color: $primary-color;
}

.brand-text {
  font-size: 20px;
  font-weight: 700;
  background: linear-gradient(135deg, #06b6d4, $primary-color);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

/* 导航菜单 - 覆写 Element Plus 默认样式 */
.glass-menu {
  flex: 1;
  display: flex;
  justify-content: center;
  background: transparent !important;
  border-bottom: none !important;
  height: 64px;
  
  :deep(.el-menu-item) {
    height: 64px;
    line-height: 64px;
    color: $text-dim;
    border-bottom: 2px solid transparent;
    padding: 0 20px;
    font-size: 14px;
    transition: all 0.3s ease;
    background: transparent !important;
    
    &:hover {
      color: $text-light;
      background: rgba(255, 255, 255, 0.05) !important;
    }
    
    &.is-active {
      color: $text-light;
      border-bottom-color: $primary-color;
      background: transparent !important;
    }
    
    &.is-disabled {
      opacity: 0.4;
      cursor: not-allowed;
    }
  }
}

/* 用户区域 */
.user-area {
  flex-shrink: 0;
}

.user-dropdown-link {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  color: $text-light;
  padding: 8px 12px;
  border-radius: 8px;
  transition: background-color 0.3s ease;
  
  &:hover {
    background: rgba(255, 255, 255, 0.05);
  }
}

.user-avatar {
  background: linear-gradient(135deg, $primary-color, #06b6d4);
  color: white;
  font-weight: 600;
}

.user-name {
  font-size: 14px;
  max-width: 100px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.arrow-icon {
  font-size: 12px;
  color: $text-dim;
}

/* 主内容区域 */
.main-content {
  margin-top: 64px;
  padding: 24px;
  min-height: calc(100vh - 64px);
  background-color: $background-dark;
}

/* 页面切换动画 */
.fade-transform-enter-active,
.fade-transform-leave-active {
  transition: opacity 0.3s ease, transform 0.3s ease;
}

.fade-transform-enter-from {
  opacity: 0;
  transform: translateY(10px);
}

.fade-transform-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}
</style>
