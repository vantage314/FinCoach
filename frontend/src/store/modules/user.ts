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
    nickname?: string;
    email?: string;
    roles?: string[];
    authorities?: string[];
    role?: string;
}

export const useUserStore = defineStore('user', () => {
    // 状态
    const token = ref<string | null>(localStorage.getItem('token'));
    const username = ref<string>(localStorage.getItem('username') || '');
    const email = ref<string>(localStorage.getItem('email') || '');
    const userInfo = ref<UserInfo | null>(null);
    const roles = ref<string[]>([]);

    // 计算属性
    const isLoggedIn = computed(() => !!token.value);
    const isAdmin = computed(() => {
        const derived = extractRolesFromPayload(parseJwtPayload(token.value || ''));
        return derived.some((role) => isAdminRole(role));
    });

    /**
     * 登录
     * @param form 登录表单
     * @returns Promise<boolean> 是否登录成功
     */
    const login = async (form: LoginForm): Promise<boolean> => {
        try {
            const res: any = await request.post('/auth/login', form);
            if (res.code === 200 && res.data) {
                const { token: loginToken } = normalizeLoginResponse(res.data);
                if (!loginToken) {
                    return false;
                }
                token.value = loginToken;
                username.value = form.username;
                userInfo.value = { username: form.username };
                roles.value = syncRolesFromToken(loginToken);

                // 持久化
                localStorage.setItem('token', loginToken);
                localStorage.setItem('username', form.username);

                console.debug('[UserStore] 登录成功，roles=', roles.value);
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
    const setUser = (newToken: string, newUsername: string, newEmail?: string, newRoles?: string[]) => {
        token.value = newToken;
        username.value = newUsername;
        email.value = newEmail || '';
        userInfo.value = { username: newUsername, email: newEmail, roles: newRoles };
        roles.value = syncRolesFromToken(newToken);
        localStorage.setItem('token', newToken);
        localStorage.setItem('username', newUsername);
        if (newEmail !== undefined) {
            localStorage.setItem('email', newEmail);
        }
    };

    /**
     * 退出登录
     * 清空状态和 localStorage
     */
    const logout = (): Promise<void> => {
        return new Promise((resolve) => {
            token.value = null;
            username.value = '';
            email.value = '';
            userInfo.value = null;
            roles.value = [];
            localStorage.removeItem('token');
            localStorage.removeItem('username');
            localStorage.removeItem('email');
            localStorage.removeItem('roles');
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
        const storedEmail = localStorage.getItem('email');
        if (storedToken) {
            token.value = storedToken;
            roles.value = syncRolesFromToken(storedToken);
        }
        if (storedUsername) {
            username.value = storedUsername;
            userInfo.value = { username: storedUsername, email: storedEmail || undefined };
        }
        if (storedEmail) {
            email.value = storedEmail;
        }
    };

    return {
        token,
        username,
        email,
        userInfo,
        roles,
        isLoggedIn,
        isAdmin,
        login,
        setUser,
        logout,
        initFromStorage
    };
});

const isAdminRole = (role: string) => {
    const normalized = role?.toUpperCase?.() || '';
    return normalized === 'ADMIN' || normalized === 'ROLE_ADMIN';
};

const normalizeRoles = (input: unknown): string[] => {
    if (Array.isArray(input)) {
        return input.map((item) => String(item)).filter((item) => item.length > 0);
    }
    if (typeof input === 'string') {
        return input.split(',').map((item) => item.trim()).filter((item) => item.length > 0);
    }
    return [];
};

const parseJwtPayload = (token: string): Record<string, unknown> | null => {
    if (!token || typeof token !== 'string') return null;
    const parts = token.split('.');
    if (parts.length !== 3) return null;
    try {
        const payload = parts[1].replace(/-/g, '+').replace(/_/g, '/');
        const padded = payload + '==='.slice((payload.length + 3) % 4);
        const json = atob(padded);
        return JSON.parse(json);
    } catch {
        return null;
    }
};

const extractRolesFromPayload = (payload: Record<string, unknown> | null): string[] => {
    if (!payload) return [];
    const rawRoles = (payload as any).roles ?? (payload as any).authorities ?? (payload as any).role;
    return normalizeRoles(rawRoles);
};

const normalizeLoginResponse = (data: any): { token: string } => {
    if (!data) return { token: '' };
    if (typeof data === 'string') {
        return { token: data };
    }
    const token = data.token || data.accessToken || data.jwt || data.data;
    return { token: typeof token === 'string' ? token : '' };
};

const syncRolesFromToken = (tokenValue: string | null): string[] => {
    if (!tokenValue) {
        return [];
    }
    if (tokenValue.split('.').length !== 3) {
        return [];
    }
    const payload = parseJwtPayload(tokenValue);
    const derived = extractRolesFromPayload(payload);
    localStorage.setItem('roles', JSON.stringify(derived));
    console.debug('[UserStore] roles from token', derived);
    return derived;
};

