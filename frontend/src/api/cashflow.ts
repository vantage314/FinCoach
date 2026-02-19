import request from '@/api/request';

export interface CashflowMonth {
  id?: number;
  month: string;
  income: number;
  expense: number;
  net?: number;
}

export const listCashflowMonths = (params?: { from?: string; to?: string }) => {
  return request.get('/app/cashflow/months', { params });
};

export const upsertCashflowMonth = (payload: CashflowMonth) => {
  return request.post('/app/cashflow/upsertMonth', payload);
};

export const importCashflowCsv = (file: File) => {
  const formData = new FormData();
  formData.append('file', file);
  return request.post('/app/cashflow/importCsv', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  });
};
