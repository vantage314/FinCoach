import { defineStore } from 'pinia';
import { ref, computed } from 'vue';

export const useUserStore = defineStore('user', () => {
    // 状态
    const token = ref<string | null>(localStorage.getItem('token'));
    const username = ref<string>(localStorage.getItem('username') || '');

    // 计算属性
    const isLoggedIn = computed(() => !!token.value);

    /**
     * 设置用户信息并持久化
     */
    const setUser = (newToken: string, newUsername: string) => {
        token.value = newToken;
        username.value = newUsername;
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
        }
    };

    return {
        token,
        username,
        isLoggedIn,
        setUser,
        logout,
        initFromStorage
    };
});
