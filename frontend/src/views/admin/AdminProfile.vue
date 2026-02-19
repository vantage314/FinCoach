<template>
  <div class="admin-page">
    <div class="page-header">
      <div>
        <h2>个人中心</h2>
        <p class="sub">管理员信息与会话管理</p>
      </div>
      <el-button type="primary" @click="handleLogout">退出登录</el-button>
    </div>

    <el-card class="profile-card" shadow="never">
      <div class="profile-header">
        <el-avatar :size="54" class="profile-avatar">{{ displayInitial }}</el-avatar>
        <div>
          <div class="profile-name">{{ displayName }}</div>
          <div class="profile-roles">角色：{{ roleText }}</div>
        </div>
      </div>
      <el-divider />
      <el-descriptions :column="2" border>
        <el-descriptions-item label="用户名">
          {{ displayName }}
        </el-descriptions-item>
        <el-descriptions-item label="角色">
          {{ roleText }}
        </el-descriptions-item>
        <el-descriptions-item label="登录状态">
          {{ userStore.isLoggedIn ? '已登录' : '未登录' }}
        </el-descriptions-item>
        <el-descriptions-item label="会话">
          {{ tokenText }}
        </el-descriptions-item>
      </el-descriptions>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { useUserStore } from '@/store/modules/user';

const router = useRouter();
const userStore = useUserStore();

const displayName = computed(() => userStore.username || '管理员');
const displayInitial = computed(() => {
  const name = userStore.username;
  if (name && name.length > 0) return name.charAt(0).toUpperCase();
  return 'A';
});

const roleText = computed(() => {
  const roles = userStore.roles || [];
  if (roles.length === 0) return 'ADMIN';
  return roles.join(', ');
});

const tokenText = computed(() => (userStore.token ? '已绑定' : '未绑定'));

const handleLogout = async () => {
  try {
    await userStore.logout();
    ElMessage.success('已退出登录');
    router.push('/login');
  } catch (error) {
    console.error('[AdminProfile] 退出登录失败:', error);
    ElMessage.error('退出失败，请重试');
  }
};
</script>

<style scoped>
.admin-page {
  min-height: 100vh;
  padding: 24px;
  background: linear-gradient(135deg, #0f172a, #1e293b);
  color: #fff;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  gap: 16px;
  flex-wrap: wrap;
}

.page-header h2 {
  margin: 0;
  font-size: 22px;
}

.sub {
  margin: 6px 0 0;
  color: #94a3b8;
  font-size: 13px;
}

.profile-card {
  background: rgba(30, 41, 59, 0.6);
  border: 1px solid rgba(255, 255, 255, 0.08);
}

.profile-header {
  display: flex;
  align-items: center;
  gap: 16px;
}

.profile-avatar {
  background: linear-gradient(135deg, #3b82f6, #06b6d4);
  color: #fff;
  font-weight: 700;
}

.profile-name {
  font-size: 18px;
  font-weight: 700;
}

.profile-roles {
  margin-top: 4px;
  color: #cbd5f5;
  font-size: 13px;
}
</style>
