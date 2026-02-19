# Chrome DevTools 扫描归档

本目录用于存放 Chrome DevTools 扫描产物，按时间戳分目录：

- 命名：`YYYYMMDD_HHMM/`
- 每次扫描建议包含：
  - `console.log` 导出文件
  - `network.har`
  - `screenshots/`（关键页面截图）
  - `notes.md`（本次扫描说明/结论）

示例：

```
doc/qa/chrome-scans/20260219_2130/
  console.log
  network.har
  screenshots/
  notes.md
```

注意：仅提交目录结构与说明文件，避免提交大体积二进制文件。

## MCP 扫描流程
1. 运行脚本创建归档目录：
   - `scripts/qa/chrome-scan/new-scan.ps1`
2. 在 Chrome DevTools 中执行 MCP 扫描流程。
3. 导出产物并放入对应时间戳目录。

## 导出内容
- `console.log`（Console 导出）
- `network.har`（Network HAR）
- `screenshots/`（关键页面截图）
- `notes.md`（扫描记录与结论）

## 命名约定
- 目录：`doc/qa/chrome-scans/YYYYMMDD_HHMM/`
- 文件命名尽量与路由或页面对应，便于追踪。
