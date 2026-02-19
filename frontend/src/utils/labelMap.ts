export const adminLabelMap: Record<string, string> = {
  UserId: '用户ID',
  RequestId: '请求ID',
  Scores: '评分摘要',
  DebtCashflow: '债务与现金流',
  DebtOptimizer: '债务优化',
  InsuranceGap: '保险缺口',
  Alerts: '预警',
  Correlation: '相关性',
  CorrelationWarnings: '相关性预警',
  Risk: '风险',
  AssetHealth: '资产健康',
  Behavior: '行为',
  OpenCount: '未处理数量',
  CriticalCount: '严重数量',
  TopCodes: '高频规则',
  LastCreatedAt: '最新时间',
  AssetKey: '资产关键字',
  Ticker: '证券代码',
  Market: '市场',
  Enabled: '启用状态',
  EnabledOn: '启用',
  Disabled: '停用',
  UpdatedAt: '更新时间',
  Operation: '操作',
  Query: '查询',
  Reset: '重置',
  Add: '新增',
  Edit: '编辑',
  Refresh: '刷新',
  Priority: '优先级',
  Empty: '暂无',
};

export const getAdminLabel = (key: string, fallback?: string) => {
  return adminLabelMap[key] || fallback || key;
};

export const formatEmpty = (value: unknown, fallback = '-') => {
  if (value === null || value === undefined) return fallback;
  if (typeof value === 'string' && value.trim().length === 0) return fallback;
  if (typeof value === 'number' && Number.isNaN(value)) return fallback;
  return String(value);
};

export const formatMarket = (value: unknown) => {
  if (value === null || value === undefined || value === '') {
    return formatEmpty(value);
  }
  const normalized = String(value).trim().toUpperCase();
  if (normalized === 'US') return '美股';
  if (normalized === 'CN') return 'A股';
  if (normalized === 'CASH') return '现金';
  return String(value);
};

export const formatEnabled = (value: unknown) => {
  if (value === true || value === 1 || value === '1') return getAdminLabel('EnabledOn');
  if (value === false || value === 0 || value === '0') return getAdminLabel('Disabled');
  return formatEmpty(value);
};

export const formatEmptyText = (value: unknown) => {
  return formatEmpty(value, getAdminLabel('Empty'));
};
