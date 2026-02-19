import request from './request';

export const fetchAdviceRuleSets = () => {
  return request.get('/admin/api/advice/rulesets', { baseURL: '' });
};

export const createAdviceRuleSet = (payload: any) => {
  return request.post('/admin/api/advice/rulesets', payload, { baseURL: '' });
};

export const updateAdviceRuleSet = (id: number, payload: any) => {
  return request.put(/admin/api/advice/rulesets/, payload, { baseURL: '' });
};

export const enableAdviceRuleSet = (id: number) => {
  return request.put(/admin/api/advice/rulesets//enable, {}, { baseURL: '' });
};

export const fetchAdviceRuleParams = (code: string) => {
  return request.get(/admin/api/advice/rulesets//params, { baseURL: '' });
};

export const updateAdviceRuleParams = (code: string, payload: any[]) => {
  return request.put(/admin/api/advice/rulesets//params, payload, { baseURL: '' });
};
