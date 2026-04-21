<template>
  <div class="register-page">
    <h2 class="title">加入 FinCoach</h2>
    <p class="subtitle">创建您的个人智慧理财中心</p>
    
    <el-form
      ref="registerFormRef"
      :model="registerForm"
      :rules="rules"
      label-position="top"
      class="custom-form"
    >
      <el-form-item label="用户名" prop="username">
        <el-input 
          v-model="registerForm.username" 
          placeholder="设置您的用户名"
          :prefix-icon="UserIcon"
        />
      </el-form-item>
      
      <el-form-item label="密码" prop="password">
        <el-input 
          v-model="registerForm.password" 
          type="password" 
          placeholder="设置登录密码"
          show-password
          :prefix-icon="LockIcon"
        />
      </el-form-item>

      <el-form-item label="确认密码" prop="confirmPassword">
        <el-input 
          v-model="registerForm.confirmPassword" 
          type="password" 
          placeholder="请再次输入密码"
          show-password
          :prefix-icon="CheckCircleIcon"
        />
      </el-form-item>

      <div class="form-actions">
        <el-button 
          type="primary" 
          class="submit-btn" 
          :loading="loading"
          @click="handleRegister"
        >
          注册账户
        </el-button>
      </div>

      <div class="footer-links">
        <span>已有账号?</span>
        <router-link to="/login" class="link">立即登录</router-link>
      </div>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { User as UserIcon, Lock as LockIcon, CheckCircle as CheckCircleIcon } from 'lucide-vue-next';
import request from '@/api/request';

const router = useRouter();
const registerFormRef = ref();
const loading = ref(false);

const registerForm = reactive({
  username: '',
  password: '',
  confirmPassword: '',
});

const validateConfirmPassword = (rule: any, value: string, callback: any) => {
  if (value === '') {
    callback(new Error('请再次输入密码'));
  } else if (value !== registerForm.password) {
    callback(new Error('两次输入密码不一致!'));
  } else {
    callback();
  }
};

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, message: '用户名不少于 3 位', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码不少于 6 位', trigger: 'blur' },
  ],
  confirmPassword: [
    { validator: validateConfirmPassword, trigger: 'blur' }
  ],
};

const handleRegister = async () => {
  if (!registerFormRef.value) return;
  
  await registerFormRef.value.validate(async (valid: boolean) => {
    if (valid) {
      loading.value = true;
      try {
        const res: any = await request.post('/auth/register', {
          username: registerForm.username,
          password: registerForm.password
        });
        if (res.code === 200) {
          ElMessage.success('注册成功，请登录');
          router.push('/login');
        } else {
          ElMessage.error(res.message || '注册失败');
        }
      } catch (error: any) {
        console.error('请求异常:', error);
        ElMessage.error(error.response?.data?.message || error.message || '网络请求失败');
      } finally {
        loading.value = false;
      }
    }
  });
};
</script>

<style lang="scss" scoped>
.register-page {
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
