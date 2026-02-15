# HealthV2 M5-C Import/Export Ops Change Log

## 背景与目的
- M5-C 目标是把 HealthV2 从“可计算”推进到“可批量录入/可标准导出”，减少手工录入成本并提升报告复用效率。
- 本轮聚焦 CSV 模板下载、CSV 批量导入、报告导出（JSON/Markdown），不引入新的业务域模型。

## 变更范围
- 模板下载：资产/负债/现金流 CSV 模板。
- CSV 导入：assets、liabilities、cashflows 三类导入。
- 报告导出：按 reportId 导出 json/md。

## API 路径清单（7 个）
1. `GET /api/app/import/templates/assets.csv`
2. `GET /api/app/import/templates/liabilities.csv`
3. `GET /api/app/import/templates/cashflows.csv`
4. `POST /api/app/import/assets` (`multipart/form-data`, `file`)
5. `POST /api/app/import/liabilities` (`multipart/form-data`, `file`)
6. `POST /api/app/import/cashflows` (`multipart/form-data`, `file`)
7. `GET /api/app/health-reports/{id}/export?format=json|md`

## 返回结构说明
- CSV 导入统一返回 `CsvImportResultDTO`：
  - `successCount`: 成功行数
  - `failCount`: 失败行数
  - `failures`: 失败明细数组
  - `failures[].row`: 行号（header=1，数据从 2 开始）
  - `failures[].reason`: 失败原因
  - `failures[].raw`: 原始行内容
- 设计原则：即使部分失败也返回 200，由 `failCount/failures` 告知可修复行。

## 校验规则摘要
### assets
- `type` 非空
- `amount > 0`
- `currency` 为空默认 `CNY`
- `riskLevel` 为空默认 `MEDIUM`
- `name` 为空默认 `type`

### liabilities
- `principal > 0`
- `interestRate >= 0`（大于 1 的值由现有负债服务归一化为小数制）
- `remainingMonths > 0`
- `monthlyPayment >= 0`

### cashflows
- `month` 支持 `YYYY-MM` 或 `YYYY-MM-DD`，统一归一到 `YYYY-MM`
- `income >= 0`
- `fixedExpense >= 0`
- `variableExpense >= 0`
- `monthlyDebtPayment >= 0`（空值默认 0）

## 安全与权限
- 用户身份来自 JWT（`UserContext.getCurrentUserId()`），导入不接受外部 `userId` 参数。
- 导出接口按 `reportId` 校验归属：
  - 报告不存在：`404`
  - 报告不属于当前用户：`403`
- 所有导入/导出均在 `/api/app/**` 受鉴权体系保护。

## 已完成 commits
- `bf81de6894ee459ac9d56b8ac36bb09df84c26e1`：模板下载接口落地（含编译基线修复）。
- `fa86f35e6932198150c50ec309404a3db470c22d`：报告导出支持 `json/md`。
- `8ce70add7b04500883ca9fd93968ec351a95eb8d`：assets CSV 导入 + 行级失败返回。
- `2bd974382ac8c156cb42c52680dd579fb5edda68`：liabilities/cashflows 导入 + M5-C 自测文档。

## 常见问题与排障
- 编码问题：建议使用 UTF-8，无 BOM；若出现中文乱码先检查编辑器导出编码。
- 换行问题：Windows/Unix 换行均可，空白行会被跳过。
- 逗号限制：当前按 `split(',')` 解析，字段值不应含逗号。
- 金额格式：使用标准数字格式，禁止千分位与货币符号。
- 月份格式：仅支持 `YYYY-MM` 或 `YYYY-MM-DD`。

## 已知限制
- 当前 CSV 解析不支持复杂引号转义与逗号嵌套场景。
- 大文件流式优化、错误聚合统计维度仍可增强。
- 后续可替换为 OpenCSV（CSV）与 POI（Excel）以提升兼容性。

## 下一步
- 可在后续 M5/M6 增补行为事件写点（导入成功行事件化）。
- 可补齐更细粒度审计字段（文件名、批次号、来源端）。
- 可扩展 Excel 导入与 PDF 导出链路。
