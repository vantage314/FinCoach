import axios from 'axios';
import { ElMessage } from 'element-plus';

// 创建 axios 实例
const service = axios.create({
    baseURL: '/api', // 配合 vite.config.ts 的代理或 Nginx 转发
    timeout: 10000,  // 请求超时时间
});

// request 拦截器
service.interceptors.request.use(
    (config) => {
        // 🔥 修复点：从 localStorage 获取 Token 并添加到请求头
        // 注意：这里假设你登录时保存的 key 是 'token'。
        // 如果你用的是 Pinia UserStore，也可以从 store 获取，但 localStorage 最稳妥。
        const token = localStorage.getItem('token');

        if (token) {
            // 标准 JWT 格式通常是 'Bearer ' + token，或者是直接 token
            // 根据你的后端 SecurityFilter 配置，这里先尝试直接放 token 或加前缀
            // 假设后端是标准的 Authorization: Bearer xxx
            config.headers['Authorization'] = token.startsWith('Bearer ') ? token : `Bearer ${token}`;

            // 如果你的后端用的是自定义 header (如 X-User-Token)，请改为:
            // config.headers['X-User-Token'] = token;
        }
        return config;
    },
    (error) => {
        return Promise.reject(error);
    }
);

// response 拦截器
service.interceptors.response.use(
    (response) => {
        const res = response.data;
        // 这里的逻辑根据你的后端约定调整
        // 假设后端返回 { code: 200, data: ..., message: ... }
        if (res.code !== 200) {
            // 如果是业务错误，可以在这里统一弹出
            // ElMessage.error(res.message || 'Error');

            // 但为了灵活性，有时候我们也直接返回 res 让调用方处理
            return res;
        } else {
            return res;
        }
    },
    (error) => {
        console.error('err' + error);
        ElMessage.error(error.message || '请求失败');
        return Promise.reject(error);
    }
);

export default service;
