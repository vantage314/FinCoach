import request from './request';

export interface SecuritiesMappingQuery {
  keyword?: string;
  enabled?: number;
  page?: number;
  size?: number;
}

export interface SecuritiesSnapshotQuery {
  assetKey?: string;
  startDate?: string;
  endDate?: string;
  page?: number;
  size?: number;
}

export const fetchSecuritiesMappings = (params: SecuritiesMappingQuery) => {
  return request.get('/admin/api/securities/mapping', { params, baseURL: '' });
};

export const createSecuritiesMapping = (payload: any) => {
  return request.post('/admin/api/securities/mapping', payload, { baseURL: '' });
};

export const updateSecuritiesMapping = (id: number, payload: any) => {
  return request.put(`/admin/api/securities/mapping/${id}`, payload, { baseURL: '' });
};

export const toggleSecuritiesMapping = (id: number, enabled: boolean) => {
  return request.post(
    `/admin/api/securities/mapping/${id}/toggle`,
    {},
    { params: { enabled }, baseURL: '' }
  );
};

export const deleteSecuritiesMapping = (id: number) => {
  return request.delete(`/admin/api/securities/mapping/${id}`, { baseURL: '' });
};

export const fetchSecuritiesSnapshots = (params: SecuritiesSnapshotQuery) => {
  return request.get('/admin/api/securities/snapshots', { params, baseURL: '' });
};

export const fetchSecuritiesQuality = () => {
  return request.get('/admin/api/securities/quality', { baseURL: '' });
};
