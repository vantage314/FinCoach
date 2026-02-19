# Chrome 扫描脚本

本目录包含用于创建 Chrome DevTools 扫描归档目录的辅助脚本。

## 使用方式
```powershell
# 生成新的扫描目录（默认时间戳）
./new-scan.ps1

# 指定时间戳
./new-scan.ps1 -Timestamp "20260219_2210"
```

脚本会创建以下结构：
```
doc/qa/chrome-scans/YYYYMMDD_HHMM/
  screenshots/
  notes.md
```

备注：脚本不会自动运行 MCP 或导出任何 DevTools 产物，仅用于准备归档目录。