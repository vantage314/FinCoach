INSERT INTO fc_alert_record (user_id, rule_key, severity, payload_json, message, status, created_at)
VALUES (1, 'ALERT_DTI_HIGH', 'WARN', '{"reportId":101}', 'DTI high', 'OPEN', NOW());

INSERT INTO fc_alert_record (user_id, rule_key, severity, payload_json, message, status, created_at)
VALUES (1, 'ALERT_RISK_HIGH', 'CRITICAL', '{"reportId":102}', 'Risk high', 'OPEN', NOW());
