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
        // 如果有 token，可以在这里注入
        // const token = localStorage.getItem('token');
        // if (token) {
        //   config.headers['Authorization'] = 'Bearer ' + token;
        // }
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
