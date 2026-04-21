<template>
  <div class="profile-container">
    <div class="profile-layout">
      <div class="user-card glass-panel">
        <div class="avatar-section">
          <div class="avatar">{{ userStore.username.charAt(0).toUpperCase() }}</div>
          <h2 class="username">{{ userStore.username }}</h2>
          <p class="role">FinCoach 用户</p>
        </div>
        <div class="info-list">
          <div class="info-item">
            <span class="label">风险偏好</span>
            <el-tag effect="dark" round>{{ riskLabel || '未知' }}</el-tag>
          </div>
          <div class="info-item">
            <span class="label">注册时间</span>
            <span class="value">2026-02-06</span>
          </div>
        </div>
      </div>

      <div class="settings-panel glass-panel">
        <el-tabs v-model="activeTab">
          <el-tab-pane label="资料设置" name="profile">
            <el-form :model="profileForm" label-position="top" class="setting-form">
              <el-form-item label="昵称">
                <el-input v-model="profileForm.nickname" placeholder="请输入昵称" />
              </el-form-item>
              <el-form-item label="邮箱地址">
                <el-input v-model="profileForm.email" placeholder="example@fincoach.com" />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" :loading="loading" @click="handleUpdateProfile">
                  保存修改
                </el-button>
              </el-form-item>
            </el-form>
          </el-tab-pane>
          
          <el-tab-pane label="安全中心" name="security">
            <el-form :model="pwdForm" label-position="top" class="setting-form">
              <el-form-item label="当前密码">
                <el-input v-model="pwdForm.oldPassword" type="password" show-password placeholder="请输入当前密码" />
              </el-form-item>
              <el-form-item label="新密码">
                <el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="至少 6 位字符" />
              </el-form-item>
              <el-form-item label="确认新密码">
                <el-input v-model="pwdForm.confirmPassword" type="password" show-password placeholder="再次输入新密码" />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" :loading="loading" @click="handleChangePassword">
                  修改密码
                </el-button>
              </el-form-item>
            </el-form>
          </el-tab-pane>

          <el-tab-pane label="数据管理 (Demo)" name="data">
            <div class="danger-zone">
              <div class="zone-header">
                <h3>⚠️ 危险区域</h3>
                <p>以下操作不可逆，请谨慎操作。</p>
              </div>
              
              <div class="danger-item">
                <div class="item-info">
                  <h4>重置演示数据</h4>
                  <p>清空所有资产、计划和测评记录，立即恢复到新用户状态。</p>
                </div>
                <el-button type="danger" @click="handleReset">
                  💥 重置所有数据
                </el-button>
              </div>
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { useUserStore } from '@/store/modules/user';
import { useHealthStore } from '@/store/modules/health';
import { resetUserData, updateUserProfile, changePassword, getUserProfile } from '@/api/user';
import { ElMessageBox, ElMessage } from 'element-plus';

const userStore = useUserStore();
const healthStore = useHealthStore();
const activeTab = ref('profile'); // Default to profile for visibility
const loading = ref(false);
const riskLabel = ref('加载中...');

// 表单数据
const profileForm = reactive({
  nickname: '',
  email: ''
});

const pwdForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
});

onMounted(async () => {
  await healthStore.fetchHealthReport();
  if(healthStore.report) {
       riskLabel.value = healthStore.report.userType === 'NOVICE' ? '保守型' : '进取型';
  } else {
       riskLabel.value = '未测评';
  }
  
  try {
    const res: any = await getUserProfile();
    if (res.code === 200 && res.data) {
      profileForm.nickname = res.data.nickname || res.data.username || userStore.username;
      profileForm.email = res.data.email || '';
      userStore.username = profileForm.nickname;
      userStore.email = profileForm.email;
    } else {
      profileForm.nickname = userStore.username;
      profileForm.email = userStore.email || '';
    }
  } catch (error) {
    profileForm.nickname = userStore.username;
    profileForm.email = userStore.email || '';
  }
});

// 处理资料更新
const handleUpdateProfile = async () => {
  if (!profileForm.nickname) return ElMessage.warning('昵称不能为空');
  loading.value = true;
  try {
    await updateUserProfile({ nickname: profileForm.nickname, email: profileForm.email });
    ElMessage.success('资料已更新');
    userStore.username = profileForm.nickname; // 更新本地状态
    userStore.email = profileForm.email;
  } catch (error: any) {
    console.error(error);
    ElMessage.error(error.message || '更新失败');
  } finally {
    loading.value = false;
  }
};

// 处理密码修改
const handleChangePassword = async () => {
  if (!pwdForm.oldPassword || !pwdForm.newPassword) return ElMessage.warning('请填写完整');
  if (pwdForm.newPassword !== pwdForm.confirmPassword) return ElMessage.error('两次输入的密码不一致');
  
  loading.value = true;
  try {
    await changePassword({ 
      oldPassword: pwdForm.oldPassword, 
      newPassword: pwdForm.newPassword 
    });
    ElMessage.success('密码修改成功，请重新登录');
    userStore.logout();
    window.location.reload();
  } catch (error: any) {
    console.error(error);
    ElMessage.error(error.message || '修改失败');
  } finally {
    loading.value = false;
  }
};

const handleReset = () => {
  ElMessageBox.confirm(
    '此操作将永久删除您账号下的所有资产、计划和测评记录。是否继续？',
    '核按钮警告',
    {
      confirmButtonText: '确定重置',
      cancelButtonText: '取消',
      type: 'error',
      icon: 'WarnTriangleFilled'
    }
  ).then(async () => {
    try {
      await resetUserData();
      ElMessage.success('重置成功，正在退出...');
      // 强制登出并刷新
      userStore.logout();
      window.location.reload();
    } catch (error) {
      console.error(error);
      ElMessage.error('重置失败');
    }
  });
};
</script>

<style lang="scss" scoped>
.profile-container {
  padding: 32px;
  min-height: 100vh;
  background: var(--fc-bg-gradient);
  color: white;
}

.profile-layout {
  display: grid;
  grid-template-columns: 300px 1fr;
  gap: 24px;
  max-width: 1200px;
  margin: 0 auto;
}

.glass-panel {
  background: var(--fc-panel-hover);
  backdrop-filter: blur(10px);
  border: 1px solid var(--fc-border);
  border-radius: 16px;
  padding: 24px;
}

/* 左侧样式 */
.avatar-section {
  text-align: center;
  margin-bottom: 32px;
  
  .avatar {
    width: 80px;
    height: 80px;
    background: linear-gradient(135deg, var(--fc-primary), #06b6d4);
    border-radius: 50%;
    margin: 0 auto 16px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 32px;
    font-weight: bold;
    color: white;
  }
  
  .username { font-size: 20px; font-weight: 600; margin-bottom: 4px; }
  .role { color: var(--fc-text-muted); font-size: 14px; }
}

.info-list {
  .info-item {
    display: flex;
    justify-content: space-between;
    padding: 12px 0;
    border-bottom: 1px solid rgba(255, 255, 255, 0.05);
    font-size: 14px;
    
    .label { color: var(--fc-text-muted); }
  }
}

/* 右侧危险区域 */
.danger-zone {
  border: 1px solid #ef4444;
  border-radius: 8px;
  background: rgba(239, 68, 68, 0.05);
  overflow: hidden;
  
  .zone-header {
    background: rgba(239, 68, 68, 0.1);
    padding: 16px;
    
    h3 { color: #ef4444; margin: 0 0 4px 0; font-size: 16px; }
    p { color: #fca5a5; margin: 0; font-size: 13px; }
  }
  
  .danger-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 24px;
    
    .item-info {
      h4 { margin: 0 0 4px 0; font-weight: 600; }
      p { color: var(--fc-text-muted); margin: 0; font-size: 13px; }
    }
  }
}

:deep(.el-tabs__item) { color: var(--fc-text-muted); }
:deep(.el-tabs__item.is-active) { color: var(--fc-primary); }
:deep(.el-tabs__nav-wrap::after) { background-color: rgba(255,255,255,0.1); }
</style>
