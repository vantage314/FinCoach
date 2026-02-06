import request from './request';

export interface TransactionRecord {
    id: number;
    userId: number;
    relatedAssetId?: number;
    assetName?: string;
    transType: string;
    amount: number;
    balanceAfter?: number;
    remark?: string;
    createTime: string;
}

export interface TransactionQuery {
    page?: number;
    size?: number;
}

export function getTransactions(params?: TransactionQuery) {
    return request.get('/transactions', { params });
}
