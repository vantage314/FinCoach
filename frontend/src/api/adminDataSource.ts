import request from './request';

export const fetchDataSourceStatus = () => {
  return request.get('/admin/api/data-source/status', { baseURL: '' });
};

export const switchDataSourceMode = (mode: string) => {
  return request.post('/admin/api/data-source/mode', { mode }, { baseURL: '' });
};

export const importDemoData = () => {
  return request.post('/admin/api/data-source/demo/import', {}, { baseURL: '' });
};

export const startRealtimeJob = () => {
  return request.post('/admin/api/data-source/realtime/start', {}, { baseURL: '' });
};

export const stopRealtimeJob = () => {
  return request.post('/admin/api/data-source/realtime/stop', {}, { baseURL: '' });
};
