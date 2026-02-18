INSERT INTO fc_health_report (
  user_id,
  report_date,
  risk_score,
  health_score,
  behavior_score,
  metrics_json,
  advice_json,
  rule_version,
  create_time,
  update_time
) VALUES (
  1,
  NOW(),
  68,
  72,
  80,
  '{"scores":{"riskScore":{"value":68,"level":"MED","breakdown":[{"code":"RISK_VOL","weight":0.3,"rawValue":0.12,"scoreContribution":20,"detail":"volatility"},{"code":"RISK_DRAWDOWN","weight":0.7,"rawValue":0.25,"scoreContribution":48,"detail":"drawdown"}]},"assetHealthScore":{"value":72,"level":"GOOD","breakdown":[{"code":"ASSET_LIQ","weight":0.5,"rawValue":0.4,"scoreContribution":36,"detail":"liquidity"},{"code":"ASSET_DIVERSIFY","weight":0.5,"rawValue":0.6,"scoreContribution":36,"detail":"diversification"}]},"behaviorScore":{"value":80,"level":"HIGH","breakdown":[{"code":"BEHAVIOR_DISCIPLINE","weight":1.0,"rawValue":0.8,"scoreContribution":80,"detail":"discipline"}]}},"debtCashflowV1":{"dti":0.28,"surplusRate":0.15,"emergencyFundMonths":4.2,"stressLevel":"MED"},"insuranceGapV1":{"premiumRatio":{"value":0.08,"level":"OK","threshold":0.1},"topGaps":[{"type":"LIFE","gap":300000,"priorityRank":1,"reason":"coverage low"},{"type":"CRITICAL_ILLNESS","gap":150000,"priorityRank":2,"reason":"coverage low"}]},"alertsV1":{"openAlerts":[{"code":"ALERT_DTI_HIGH","severity":"WARN","title":"DTI high","status":"OPEN","createdAt":"2026-02-18 16:50:00"}]},"portfolio":{"correlationMatrixSummary":{"assetsCount":3,"sampleSize":120,"warnings":[]},"correlationMatrix":{"assets":["AAPL","VOO","BND"],"matrix":[[1,0.2,0.1],[0.2,1,0.3],[0.1,0.3,1]]},"rebalanceAdviceV1":{"threshold":0.05,"triggered":true,"warnings":[],"actions":[{"asset":"AAPL","action":"SELL","suggestedWeightDelta":-0.03},{"asset":"BND","action":"BUY","suggestedWeightDelta":0.03}]}}}',
  '{"adviceV2":{"debtCashflowAdviceV1":{"list":[{"title":"Reduce debt ratio","detail":"Lower monthly debt payments or increase income","priority":"HIGH"}]},"debtOptimizerV1":{"strategy":"AVALANCHE","budgetForExtraPayment":300,"tradeoffHint":{"recommendation":"Prioritize high-interest debt"},"plan":[{"name":"Credit Card","priorityRank":1,"recommendedExtraPayment":200,"estimatedMonthsToPayoff":12},{"name":"Personal Loan","priorityRank":2,"recommendedExtraPayment":100,"estimatedMonthsToPayoff":24}]},"insuranceAdviceV1":{"priorityList":[{"type":"LIFE","priorityRank":1,"reason":"coverage low"},{"type":"CRITICAL_ILLNESS","priorityRank":2,"reason":"coverage low"}],"list":[{"title":"Increase life coverage","detail":"Top up to recommended level","priority":"HIGH"}]}}}',
  'M2',
  NOW(),
  NOW()
);
