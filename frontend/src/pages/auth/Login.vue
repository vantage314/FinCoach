<template>
  <div class="login-page">
    <h2 class="title">欢迎回来</h2>
    <p class="subtitle">白领人士个人资产诊断与投资建议系统</p>
    
    <el-form
      ref="loginFormRef"
      :model="loginForm"
      :rules="rules"
      label-position="top"
      class="custom-form"
    >
      <el-form-item label="用户名" prop="username">
        <el-input 
          v-model="loginForm.username" 
          placeholder="请输入用户名"
          :prefix-icon="UserIcon"
        />
      </el-form-item>
      
      <el-form-item label="密码" prop="password">
        <el-input 
          v-model="loginForm.password" 
          type="password" 
          placeholder="请输入密码"
          show-password
          :prefix-icon="LockIcon"
        />
      </el-form-item>

      <div class="form-actions">
        <el-button 
          type="primary" 
          class="submit-btn" 
          :loading="loading"
          @click="handleLogin"
        >
          立即登录
        </el-button>
      </div>

      <div class="footer-links">
        <span>还没有账号?</span>
        <router-link to="/register" class="link">立即注册</router-link>
      </div>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { User as UserIcon, Lock as LockIcon } from 'lucide-vue-next';
import { useUserStore } from '@/store/modules/user';

const router = useRouter();
const userStore = useUserStore();
const loginFormRef = ref();
const loading = ref(false);

const loginForm = reactive({
  username: '',
  password: '',
});

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, message: '用户名不少于 3 位', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码不少于 6 位', trigger: 'blur' },
  ],
};

const handleLogin = async () => {
  if (!loginFormRef.value) return;
  
  await loginFormRef.value.validate(async (valid: boolean) => {
    if (valid) {
      loading.value = true;
      try {
        const success = await userStore.login({
          username: loginForm.username,
          password: loginForm.password,
        });
        if (!success) {
          ElMessage.error('登录失败');
          return;
        }
        ElMessage.success('登录成功');
        const target = userStore.isAdmin ? '/admin/alerts' : '/app/diagnosis';
        router.push(target);
      } catch (error: any) {
        console.error('登录异常:', error);
        ElMessage.error(error.response?.data?.message || error.message || '登录请求失败');
      } finally {
        loading.value = false;
      }
    }
  });
};
</script>

<style lang="scss" scoped>
.login-page {
  color: white;
  
  .title {
    font-size: 28px;
    font-weight: 700;
    margin-bottom: 8px;
    text-align: center;
    background: linear-gradient(to right, #fff, #94a3b8);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
  }
  
  .subtitle {
    font-size: 14px;
    color: var(--fc-text-muted);
    text-align: center;
    margin-bottom: 32px;
  }
}

.custom-form {
  :deep(.el-form-item__label) {
    color: var(--fc-text-muted);
    font-size: 13px;
    padding-bottom: 4px;
  }
  
  :deep(.el-input__wrapper) {
    background-color: rgba(255, 255, 255, 0.05);
    border: 1px solid rgba(255, 255, 255, 0.1);
    box-shadow: none !important;
    border-radius: 12px;
    padding: 8px 12px;
    
    &.is-focus {
      border-color: var(--fc-primary);
      background-color: rgba(255, 255, 255, 0.08);
    }
  }
  
  :deep(.el-input__inner) {
    color: white;
    &::placeholder {
      color: rgba(255, 255, 255, 0.3);
    }
  }
}

.submit-btn {
  width: 100%;
  height: 48px;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 600;
  border: none;
  background: linear-gradient(135deg, #06b6d4, var(--fc-primary));
  transition: all 0.3s ease;
  margin-top: 16px;
  
  &:hover {
    transform: scale(1.02);
    box-shadow: 0 0 20px rgba(79, 70, 229, 0.4);
    opacity: 0.9;
  }
}

.footer-links {
  margin-top: 24px;
  text-align: center;
  font-size: 14px;
  color: var(--fc-text-muted);
  
  .link {
    color: var(--fc-primary);
    margin-left: 8px;
    text-decoration: none;
    font-weight: 500;
    
    &:hover {
      text-decoration: underline;
    }
  }
}
</style>
