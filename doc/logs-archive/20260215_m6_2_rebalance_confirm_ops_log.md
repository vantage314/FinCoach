# M6-2 Rebalance Confirm Integrity Ops Log

> 日期：2026-02-15

## 背景
- 再平衡确认需要绑定“当前建议动作”，防止确认时篡改或过期。
- 引入确定性 JSON + SHA256 hash，实现 actionsHash 校验。

## 变更范围
- 新增工具类：
  - `CanonicalJsonHelper`（确定性 JSON 序列化）
  - `HashHelper.sha256Hex`
- 报告生成时在 `advice.rebalance` 写入：
  - `actionsHash`（`sha256:...`）
  - `confirmable=true`
- `POST /api/app/rebalance/confirm` 增加 `actionsHash` 校验：
  - 不一致返回 `400 REBALANCE_HASH_MISMATCH`
  - 行为事件 `meta_json` 记录 `actionsHash`

## actionsHash 绑定口径
- 绑定字段：
  - `actions`
  - `targetAllocation`
  - `currentAllocation`
  - `thresholds`
  - `strategy`
- `CanonicalJsonHelper` 对 Map key 排序、List 保序、null 保留，输出紧凑 JSON。
- `actionsHash = "sha256:" + sha256Hex(canonicalJson)`

## 安全与约束
- `/api/app/**` 不接收 `userId`，统一从 `UserContext` 获取。
- reportId 归属校验：不存在 `404`、越权 `403`。
