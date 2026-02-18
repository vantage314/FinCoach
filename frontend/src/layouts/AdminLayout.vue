<template>
  <el-container class="split-layout">
    <el-aside width="220px" class="sidebar">
      <div class="brand" @click="router.push('/admin/alerts')">
        <el-icon class="brand-icon"><Setting /></el-icon>
        <span class="brand-text">后台管理</span>
      </div>

      <el-menu
        router
        :default-active="activeMenu"
        class="side-menu"
        :collapse="false"
      >
        <el-menu-item index="/admin/alerts">
          <el-icon><Bell /></el-icon>
          <span>预警管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/debug">
          <el-icon><Monitor /></el-icon>
          <span>Debug 快照</span>
        </el-menu-item>
        <el-sub-menu index="data-securities">
          <template #title>
            <el-icon><Setting /></el-icon>
            <span>数据与证券</span>
          </template>
          <el-menu-item index="/admin/securities/mapping">
            <el-icon><Setting /></el-icon>
            <span>证券映射</span>
          </el-menu-item>
          <el-menu-item index="/admin/securities/snapshots">
            <el-icon><Setting /></el-icon>
            <span>行情快照</span>
          </el-menu-item>
          <el-menu-item index="/admin/securities/quality">
            <el-icon><Setting /></el-icon>
            <span>数据质量</span>
          </el-menu-item>
          <el-menu-item index="/admin/data-source">
            <el-icon><Setting /></el-icon>
            <span>数据源与抓取</span>
          </el-menu-item>
        </el-sub-menu>
      </el-menu>

      <div class="sidebar-footer">
        <el-button type="primary" plain size="small" @click="router.push('/app/diagnosis')">
          返回用户端
        </el-button>
      </div>
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
import { ArrowDown, Bell, Monitor, Setting } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import { useUserStore } from '@/store/modules/user';

const route = useRoute();
const router = useRouter();
const userStore = useUserStore();

const activeMenu = computed(() => route.path);
const displayName = computed(() => userStore.username || '管理员');
const displayInitial = computed(() => {
  const name = userStore.username;
  if (name && name.length > 0) return name.charAt(0).toUpperCase();
  return 'A';
});

const pageTitle = computed(() => {
  const raw = (route.meta.title as string) || '后台管理';
  return raw.replace(' - FinCoach', '');
});

const handleLogout = async () => {
  try {
    await userStore.logout();
    ElMessage.success('已退出登录');
    router.push('/login');
  } catch (error) {
    console.error('[AdminLayout] 退出登录失败:', error);
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
  background: rgba(15, 23, 42, 0.9);
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
  font-size: 17px;
  font-weight: 700;
}

.side-menu {
  flex: 1;
  border-right: none !important;
  background: transparent !important;

  :deep(.el-menu-item) {
    color: $text-dim;
    background: transparent !important;
  }

  :deep(.el-menu-item.is-active) {
    color: $text-light;
    background: rgba(59, 130, 246, 0.12) !important;
    border-left: 3px solid $primary-color;
  }
}

.sidebar-footer {
  padding: 16px 20px;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
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
