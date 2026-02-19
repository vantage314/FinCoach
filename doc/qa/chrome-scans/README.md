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
