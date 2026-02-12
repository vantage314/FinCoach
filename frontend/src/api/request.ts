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

    if (typeof res === 'string') {
      return res;
    }

    if (res && typeof res.code !== 'undefined') {
      if (Number(res.code) !== 200) {
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
        const errorMessage = error?.message || 'Request failed';
        const mergedMessage = `${method} ${url} -> ${status || 'NO_STATUS'}: ${backendMessage || errorMessage}`;
        const dataPreview = previewData(response?.data);

        console.error(`[api] Request error: ${mergedMessage}`, {
          message: errorMessage,
          code: error?.code,
          method,
          url,
          baseURL: config?.baseURL,
          status,
          backendCode,
          dataPreview,
        });

        ElMessage.error(mergedMessage);
        return Promise.reject(new Error(mergedMessage));
      }

      const clientMessage = error?.message || 'Network Error';
      const mergedMessage = `${method} ${url} -> NO_STATUS: ${clientMessage}`;
      console.error(`[api] Request error: ${mergedMessage}`, {
        message: clientMessage,
        code: error?.code,
        method,
        url,
        baseURL: config?.baseURL,
      });
      ElMessage.error(mergedMessage);
      return Promise.reject(new Error(mergedMessage));
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
    return appendParams(url, config?.params);
  }
  const normalizedBase = baseURL.replace(/\/$/, '');
  const normalizedUrl = url.replace(/^\//, '');
  const fullUrl = `${normalizedBase}/${normalizedUrl}`;
  return appendParams(fullUrl, config?.params);
};

const appendParams = (url: string, params: any) => {
  if (!params || typeof params !== 'object') {
    return url;
  }
  const searchParams = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (typeof value === 'undefined' || value === null) return;
    searchParams.append(key, String(value));
  });
  const query = searchParams.toString();
  if (!query) return url;
  return url.includes('?') ? `${url}&${query}` : `${url}?${query}`;
};

const previewData = (data: any) => {
  if (typeof data === 'string') {
    return data.slice(0, 1024);
  }
  try {
    return JSON.stringify(data).slice(0, 1024);
  } catch {
    return String(data).slice(0, 1024);
  }
};
