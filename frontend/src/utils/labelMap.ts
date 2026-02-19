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
  Mode: '模式',
  Description: '说明',
  Job: '任务',
  Status: '状态',
  CrawlerMode: '抓取方式',
  Interval: '间隔',
  LastStartAt: '最近启动',
  LastHeartbeatAt: '最近心跳',
  Stale: '是否过期',
  SecondsSinceHeartbeat: '心跳间隔(秒)',
  LastEndAt: '最近结束',
  Time: '时间',
  Type: '类型',
  Message: '消息',
  ImportDemo: '导入演示数据',
  SwitchTo: '切换到',
  Start: '启动',
  Stop: '停止',
  Recover: '恢复',
  ModeDemo: '演示',
  ModeRealtime: '实时',
  ModeDaemon: '守护',
  ModeOnce: '单次',
  StatusRunning: '运行中',
  StatusStopped: '已停止',
  StatusStopping: '停止中',
  StatusFailed: '失败',
  StatusOk: '正常',
  StatusStale: '过期',
  Summary: '摘要',
  AssetsCount: '资产数',
  SampleSize: '样本数',
  LatestDate: '最新日期',
  Date: '日期',
  PriceClose: '收盘价',
  Currency: '币种',
  Source: '来源',
  MissingMappings: '缺失映射',
  Coverage: '覆盖不足/滞后',
  CoverageDays: '覆盖天数',
  LagDays: '滞后天数',
  Issues: '问题',
  Anomalies: '异常波动',
  Reason: '原因',
  ChangePct: '涨跌幅',
  Recommendations: '总体建议',
  StaleCount: '过期',
  RecoverCount: '恢复',
  RestartCount: '重启',
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

export const formatMarketText = (value: unknown) => {
  if (value === null || value === undefined || value === '') {
    return formatEmptyText(value);
  }
  return formatMarket(value);
};

export const formatEnabledText = (value: unknown) => {
  const formatted = formatEnabled(value);
  return formatted === '-' ? formatEmptyText(value) : formatted;
};

export const formatDataSourceMode = (value: unknown) => {
  if (value === null || value === undefined || value === '') {
    return formatEmptyText(value);
  }
  const normalized = String(value).trim().toUpperCase();
  if (normalized === 'REALTIME') return getAdminLabel('ModeRealtime');
  if (normalized === 'DEMO_DB' || normalized === 'DEMO') return getAdminLabel('ModeDemo');
  return String(value);
};

export const formatJobStatus = (value: unknown) => {
  if (value === null || value === undefined || value === '') {
    return formatEmptyText(value);
  }
  const normalized = String(value).trim().toUpperCase();
  if (normalized === 'RUNNING') return getAdminLabel('StatusRunning');
  if (normalized === 'STOPPING') return getAdminLabel('StatusStopping');
  if (normalized === 'STOPPED') return getAdminLabel('StatusStopped');
  if (normalized === 'FAILED') return getAdminLabel('StatusFailed');
  return String(value);
};

export const formatCrawlerMode = (value: unknown) => {
  if (value === null || value === undefined || value === '') {
    return formatEmptyText(value);
  }
  const normalized = String(value).trim().toUpperCase();
  if (normalized === 'DAEMON') return getAdminLabel('ModeDaemon');
  if (normalized === 'ONCE' || normalized === 'RUN_ONCE') return getAdminLabel('ModeOnce');
  return String(value);
};

export const formatStaleStatus = (value: unknown) => {
  if (value === null || value === undefined || value === '') {
    return formatEmptyText(value);
  }
  if (value === true || value === 'STALE') return getAdminLabel('StatusStale');
  if (value === false || value === 'OK') return getAdminLabel('StatusOk');
  return String(value);
};
