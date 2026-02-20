WITH rule_set AS (
  SELECT id FROM fc_score_rule_set WHERE code = 'DEFAULT' ORDER BY version DESC LIMIT 1
)
INSERT INTO fc_score_rule_param
  (rule_set_id, param_key, param_value, value_type, min_value, max_value, description, updated_at)
SELECT rule_set.id, 'DEBT_PAYMENT_RATIO_MAX', '0.35', 'DECIMAL', '0', '1', 'Max debt payment ratio', NOW()
FROM rule_set
ON CONFLICT (rule_set_id, param_key) DO NOTHING;

WITH rule_set AS (
  SELECT id FROM fc_score_rule_set WHERE code = 'DEFAULT' ORDER BY version DESC LIMIT 1
)
INSERT INTO fc_score_rule_param
  (rule_set_id, param_key, param_value, value_type, min_value, max_value, description, updated_at)
SELECT rule_set.id, 'SURPLUS_RATE_MIN', '0.10', 'DECIMAL', '0', '1', 'Min surplus rate', NOW()
FROM rule_set
ON CONFLICT (rule_set_id, param_key) DO NOTHING;

WITH rule_set AS (
  SELECT id FROM fc_score_rule_set WHERE code = 'DEFAULT' ORDER BY version DESC LIMIT 1
)
INSERT INTO fc_score_rule_param
  (rule_set_id, param_key, param_value, value_type, min_value, max_value, description, updated_at)
SELECT rule_set.id, 'REB_THRESHOLD', '0.05', 'DECIMAL', '0', '1', 'Rebalance deviation threshold', NOW()
FROM rule_set
ON CONFLICT (rule_set_id, param_key) DO NOTHING;

WITH rule_set AS (
  SELECT id FROM fc_score_rule_set WHERE code = 'DEFAULT' ORDER BY version DESC LIMIT 1
)
UPDATE fc_score_rule_param
SET param_value = '6', updated_at = NOW()
WHERE rule_set_id = (SELECT id FROM rule_set)
  AND param_key = 'EMERGENCY_MONTHS_MIN'
  AND param_value = '3';
