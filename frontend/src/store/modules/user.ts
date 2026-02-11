import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import request from '@/api/request';

interface LoginForm {
    username: string;
    password: string;
}

interface UserInfo {
    id?: number;
    username: string;
    email?: string;
}

export const useUserStore = defineStore('user', () => {
    // 状态
    const token = ref<string | null>(localStorage.getItem('token'));
    const username = ref<string>(localStorage.getItem('username') || '');
    const userInfo = ref<UserInfo | null>(null);

    // 计算属性
    const isLoggedIn = computed(() => !!token.value);

    /**
     * 登录
     * @param form 登录表单
     * @returns Promise<boolean> 是否登录成功
     */
    const login = async (form: LoginForm): Promise<boolean> => {
        try {
            const res: any = await request.post('/auth/login', form);
            if (res.code === 200 && res.data) {
                token.value = res.data;
                username.value = form.username;
                userInfo.value = { username: form.username };

                // 持久化
                localStorage.setItem('token', res.data);
                localStorage.setItem('username', form.username);

                console.log('[UserStore] 登录成功，Token 已保存');
                return true;
            }
            return false;
        } catch (error) {
            console.error('[UserStore] 登录失败:', error);
            throw error;
        }
    };

    /**
     * 设置用户信息并持久化 (兼容旧代码)
     */
    const setUser = (newToken: string, newUsername: string) => {
        token.value = newToken;
        username.value = newUsername;
        userInfo.value = { username: newUsername };
        localStorage.setItem('token', newToken);
        localStorage.setItem('username', newUsername);
    };

    /**
     * 退出登录
     * 清空状态和 localStorage
     */
    const logout = (): Promise<void> => {
        return new Promise((resolve) => {
            token.value = null;
            username.value = '';
            userInfo.value = null;
            localStorage.removeItem('token');
            localStorage.removeItem('username');
            console.log('[UserStore] 用户已退出登录，LocalStorage 已清空');
            resolve();
        });
    };

    /**
     * 初始化时从 localStorage 恢复用户名（如果有）
     */
    const initFromStorage = () => {
        const storedToken = localStorage.getItem('token');
        const storedUsername = localStorage.getItem('username');
        if (storedToken) {
            token.value = storedToken;
        }
        if (storedUsername) {
            username.value = storedUsername;
            userInfo.value = { username: storedUsername };
        }
    };

    return {
        token,
        username,
        userInfo,
        isLoggedIn,
        login,
        setUser,
        logout,
        initFromStorage
    };
});

