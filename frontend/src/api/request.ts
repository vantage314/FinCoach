import axios from 'axios';
import { ElMessage } from 'element-plus';

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 10000,
});

// 请求拦截器
request.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// 响应拦截器
request.interceptors.response.use(
  (response) => {
    const res = response.data;
    const config = response.config || {};
    const method = (config.method || 'GET').toUpperCase();
    const url = buildUrl(config);

    if (res && typeof res.code !== 'undefined') {
      if (res.code !== 200) {
        const errorMessage = res.message || `Request failed: ${method} ${url}`;
        console.error('[api] Request failed', {
          method,
          url,
          status: response.status,
          code: res.code,
          message: res.message,
          data: res.data,
        });
        ElMessage.error(errorMessage);
        return Promise.reject(new Error(errorMessage));
      }
      return res;
    }

    return res;
  },
  (error) => {
    const response = error?.response;
    const config = error?.config || {};
    const method = (config.method || 'GET').toUpperCase();
    const url = buildUrl(config);

    if (response && response.status === 401) {
      localStorage.removeItem('token');
      window.location.href = '/login';
    } else {
      if (response) {
        const backendMessage = response?.data?.message;
        const backendCode = response?.data?.code;
        const status = response.status;
        console.error('[api] Request error', {
          method,
          url,
          status,
          code: backendCode,
          message: backendMessage,
          data: response?.data,
        });

        const parts: string[] = [];
        if (backendMessage) parts.push(backendMessage);
        parts.push(`${method} ${url} -> ${status}`);
        if (typeof backendCode !== 'undefined') parts.push(`code=${backendCode}`);
        const mergedMessage = parts.join(' | ');
        ElMessage.error(mergedMessage);
        return Promise.reject(new Error(mergedMessage));
      }

      const clientMessage = error?.message || 'Network Error';
      console.error('[api] Request error', { method, url, message: clientMessage });
      ElMessage.error(clientMessage);
      return Promise.reject(new Error(clientMessage));
    }
    return Promise.reject(error);
  }
);

export default request;

const buildUrl = (config: any) => {
  const baseURL = config?.baseURL || '';
  const url = config?.url || '';
  if (url.startsWith('http://') || url.startsWith('https://')) {
    return url;
  }
  if (!baseURL) {
    return url;
  }
  const normalizedBase = baseURL.replace(/\/$/, '');
  const normalizedUrl = url.replace(/^\//, '');
  return `${normalizedBase}/${normalizedUrl}`;
};
