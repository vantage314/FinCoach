import request from '@/api/request';

export interface NotificationItem {
  id: number;
  title: string;
  content: string;
  isRead: number;
  createdAt?: string;
  payloadJson?: string;
}

export const fetchNotifications = (params?: { isRead?: number; page?: number; size?: number }) => {
  return request.get('/app/notifications', { params });
};

export const markNotificationRead = (id: number) => {
  return request.put(`/app/notifications/${id}/read`);
};

export const markAllNotificationsRead = () => {
  return request.put('/app/notifications/read-all');
};
