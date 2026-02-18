import request from './request';

export interface AdminAlertItem {
    alertId?: number;
    code?: string;
    severity?: string;
    title?: string;
    status?: string;
    createdAt?: string;
    reportId?: number;
}

export interface AdminAlertList {
    total?: number;
    items?: AdminAlertItem[];
}

export interface AdminAlertQuery {
    limit?: number;
    status?: string;
}

export const fetchAdminAlerts = (params: AdminAlertQuery) => {
    return request.get<AdminAlertList>('/admin/api/alerts', {
        params,
        baseURL: '',
    });
};

export const ackAdminAlerts = (alerts: Array<{ alertId: number }>) => {
    return request.post('/admin/api/alerts/ack', { alerts }, { baseURL: '' });
};
