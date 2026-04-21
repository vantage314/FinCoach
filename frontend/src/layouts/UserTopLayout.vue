<template>
  <el-container class="top-layout">
    <el-header class="fixed-header">
      <div class="logo-area" @click="router.push('/app/diagnosis')">
        <el-icon :size="26" class="logo-icon"><Monitor /></el-icon>
        <span class="brand-text">白领人士个人资产诊断与投资建议系统</span>
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
        <el-dropdown trigger="click" @visible-change="handleNotifyVisible">
          <span class="notify-trigger">
            <el-badge :value="unreadCount" :max="99" class="notify-badge">
              <el-icon class="notify-icon"><Bell /></el-icon>
            </el-badge>
          </span>
          <template #dropdown>
            <div class="notify-dropdown">
              <div class="notify-header">
                <span>通知中心</span>
                <el-button text size="small" @click.stop="markAllRead">全部已读</el-button>
              </div>
              <div v-if="notifyLoading" class="notify-empty">加载中...</div>
              <div v-else>
                <div v-if="notifications.length === 0" class="notify-empty">暂无通知</div>
                <div v-else class="notify-list">
                  <div
                    v-for="item in notifications"
                    :key="item.id"
                    class="notify-item"
                    @click="openNotification(item)"
                  >
                    <div class="notify-title">
                      <span :class="item.isRead === 1 ? 'read' : 'unread'">{{ item.title }}</span>
                      <span class="notify-time">{{ formatTime(item.createdAt) }}</span>
                    </div>
                    <div class="notify-content">{{ item.content }}</div>
                  </div>
                </div>
              </div>
              <div class="notify-footer">
                <el-button text size="small" @click="goNotifications">查看全部</el-button>
              </div>
            </div>
          </template>
        </el-dropdown>
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
        <Suspense>
          <template #default>
            <transition name="fade-transform" mode="out-in">
              <component :is="Component" :key="$route.fullPath" />
            </transition>
          </template>
          <template #fallback>
            <div class="route-loading">页面加载中...</div>
          </template>
        </Suspense>
      </router-view>
    </el-main>
  </el-container>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import {
  ArrowDown,
  Bell,
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
import { fetchNotifications, markAllNotificationsRead, markNotificationRead } from '@/api/notification';

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

const notifications = ref<any[]>([]);
const unreadCount = ref(0);
const notifyLoading = ref(false);

const loadNotifications = async () => {
  notifyLoading.value = true;
  try {
    const res: any = await fetchNotifications({ page: 1, size: 10 });
    const data = res?.data ?? res;
    notifications.value = Array.isArray(data?.list) ? data.list : [];
    unreadCount.value = Number(data?.unreadCount ?? 0);
  } catch (error) {
    notifications.value = [];
    unreadCount.value = 0;
    console.error('[UserTopLayout] load notifications failed', error);
  } finally {
    notifyLoading.value = false;
  }
};

const handleNotifyVisible = (visible: boolean) => {
  if (visible) {
    loadNotifications();
  }
};

const markAllRead = async () => {
  try {
    await markAllNotificationsRead();
    ElMessage.success('已全部标记已读');
    loadNotifications();
  } catch (error) {
    ElMessage.error('操作失败');
  }
};

const resolveRoute = (payloadJson?: string) => {
  if (!payloadJson) return null;
  try {
    const payload = JSON.parse(payloadJson);
    if (payload && typeof payload.route === 'string') {
      return payload.route;
    }
  } catch (error) {
    return null;
  }
  return null;
};

const openNotification = async (item: any) => {
  try {
    if (item.isRead !== 1) {
      await markNotificationRead(item.id);
    }
  } catch (error) {
    console.error('[UserTopLayout] mark read failed', error);
  }
  const routePath = resolveRoute(item.payloadJson);
  if (routePath) {
    router.push(routePath);
  } else {
    router.push('/app/notifications');
  }
  loadNotifications();
};

const goNotifications = () => {
  router.push('/app/notifications');
};

const formatTime = (value: any) => {
  if (!value) return '';
  return String(value).replace('T', ' ').slice(0, 16);
};

onMounted(() => loadNotifications());
</script>

<style lang="scss" scoped>
.top-layout {
  min-height: 100vh;
  background-color: var(--fc-bg);
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
  backdrop-filter: blur(var(--fc-blur)) !important;
  border-bottom: 1px solid var(--fc-border);
}

.logo-area {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
  cursor: pointer;
}

.logo-icon {
  color: var(--fc-primary);
}

.brand-text {
  font-size: 18px;
  font-weight: 700;
  color: var(--fc-text);
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
    color: var(--fc-text-muted);
    border-bottom: 2px solid transparent;
    padding: 0 16px;
    background: transparent !important;
  }

  :deep(.el-menu-item.is-active) {
    color: var(--fc-text);
    border-bottom-color: var(--fc-primary);
  }
}

.user-area {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}

.notify-trigger {
  display: flex;
  align-items: center;
  cursor: pointer;
}

.notify-icon {
  color: var(--fc-text);
  font-size: 18px;
}

.notify-dropdown {
  width: 320px;
  padding: 12px;
  background: var(--fc-panel-strong);
  border: 1px solid var(--fc-border);
  border-radius: 10px;
  color: var(--fc-text);
}

.notify-header,
.notify-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.notify-footer {
  margin-top: 8px;
}

.notify-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: 260px;
  overflow: auto;
}

.notify-item {
  padding: 8px;
  border-radius: 6px;
  background: var(--fc-panel-hover);
  cursor: pointer;
}

.notify-item:hover {
  background: rgba(255, 255, 255, 0.08);
}

.notify-title {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  margin-bottom: 4px;
}

.notify-title .unread {
  font-weight: 600;
  color: #93c5fd;
}

.notify-title .read {
  color: var(--fc-text-muted);
}

.notify-time {
  font-size: 11px;
  color: var(--fc-text-muted);
}

.notify-content {
  font-size: 12px;
  color: var(--fc-text);
}

.notify-empty {
  padding: 16px 0;
  text-align: center;
  color: var(--fc-text-muted);
  font-size: 12px;
}


.user-dropdown-link {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  color: var(--fc-text);
}

.user-avatar {
  background: linear-gradient(135deg, var(--fc-primary), #06b6d4);
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
  color: var(--fc-text-muted);
}

.main-content {
  padding: 20px 24px;
  background-color: var(--fc-bg);
  min-height: calc(100vh - 64px);
}

.route-loading {
  color: var(--fc-text-muted);
  padding: 32px 0;
  text-align: center;
  font-size: 14px;
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
