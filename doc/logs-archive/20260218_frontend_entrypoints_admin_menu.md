# 2026-02-18 Frontend Admin Entrypoints & Health Report Cards

## 改动范围
- 新增页面：`/admin/alerts`（预警管理）、`/admin/debug`（Debug 快照）、`/403`（无权限页）。
- 健康报告页补齐 7 个模块卡片（评分/相关性矩阵/再平衡/负债现金流/债务优化/保险缺口/预警）。
- 新增 admin API 封装与 Vite 代理 `/admin`。

## 路由与菜单
- 路由：新增 `/admin/alerts`、`/admin/debug`、`/403`。
- 菜单：TopLayout 顶部菜单新增“后台管理”分组，仅 `isAdmin=true` 显示。
- 权限守卫：路由 meta.roles 包含 `ADMIN` 时，非管理员重定向 `/403` 并提示无权限。

## isAdmin 判定
- 解析 JWT payload 的 `roles/authorities/role` 字段（若存在），以 `ADMIN` 或 `ROLE_ADMIN` 判断。
- 无上述字段则默认非管理员（菜单隐藏，路由被 403）。

## Vite Proxy
- 新增 `/admin` 代理到 `http://localhost:8080`，用于 `/admin/api/*` 调用。

## 验证步骤
1. 普通账号登录后进入 `/diagnosis`：应看到新增模块卡片（评分/预警等），无数据时显示“暂无”。
2. 使用含 `ADMIN` 角色的 Token 登录：顶部菜单出现“后台管理”，点击进入 `/admin/alerts` 拉取预警列表并可 ACK。
3. 进入 `/admin/debug`：能展示 summaries 与 correlation matrix（若存在）。

## 截图点位说明
- 报告页：`/diagnosis` 新增“体检扩展模块”卡片区块截图。
- 后台管理：顶部菜单出现“后台管理”分组截图。
- Debug 页：correlation matrix 表格与 summaries 卡片截图。

## 回滚方案
- `git revert a49d7bf8`
- `git revert HEAD` (docs)
