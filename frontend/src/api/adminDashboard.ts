import request from './request';

export interface AdminDashboardSummary {
  usersTotal?: number;
  alertsOpen?: number;
  latestCrawlerHeartbeatAt?: string;
  latestHealthReportAt?: string;
  notificationsUnreadTotal?: number;
  systemStatus?: string;
  warnings?: string[];
}

export const fetchAdminDashboardSummary = () => {
  return request.get<AdminDashboardSummary>('/admin/api/dashboard/summary', { baseURL: '' });
};
