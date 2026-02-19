import request from '@/api/request';

export interface DebtItem {
  id?: number;
  debtType: string;
  principal?: number;
  apr?: number;
  termMonths?: number | null;
  monthlyPayment?: number | null;
  remainingBalance: number;
  startDate?: string | null;
  endDate?: string | null;
}

export const listDebts = () => {
  return request.get('/app/debt/list');
};

export const upsertDebt = (payload: DebtItem) => {
  return request.post('/app/debt/upsert', payload);
};

export const deleteDebt = (id: number) => {
  return request.post('/app/debt/delete', { id });
};
