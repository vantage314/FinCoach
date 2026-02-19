import request from './request';

export interface AdminUserItem {
  id?: number;
  username?: string;
  email?: string;
  roles?: string[];
  enabled?: boolean;
  createdAt?: string;
  lastLoginAt?: string;
}

export interface AdminUserList {
  items?: AdminUserItem[];
  total?: number;
  page?: number;
  size?: number;
}

export interface AdminUserListQuery {
  page?: number;
  size?: number;
  q?: string;
}

export const fetchAdminUsers = (params: AdminUserListQuery) => {
  return request.get<AdminUserList>('/admin/api/users/list', { params, baseURL: '' });
};

export const toggleAdminUser = (payload: { userId: number; enabled: boolean }) => {
  return request.post('/admin/api/users/toggle', payload, { baseURL: '' });
};
