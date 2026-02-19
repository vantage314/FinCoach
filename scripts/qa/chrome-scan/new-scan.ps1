param(
  [string]$Timestamp = (Get-Date -Format "yyyyMMdd_HHmm")
)

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..\..\..")
$targetRoot = Join-Path $repoRoot "doc\qa\chrome-scans"
$target = Join-Path $targetRoot $Timestamp
$shots = Join-Path $target "screenshots"
$notes = Join-Path $target "notes.md"

New-Item -ItemType Directory -Force $target | Out-Null
New-Item -ItemType Directory -Force $shots | Out-Null

if (-not (Test-Path $notes)) {
@"
# Chrome DevTools 扫描记录

- 扫描时间：
- 扫描人：
- 目标环境：
- 相关分支/版本：

## 路由访问清单
- [ ] 

## Console errors
- 数量：
- 关键错误：

## Network 4xx/5xx
- 4xx 数量：
- 5xx 数量：

## Largest resources
- 
"@ | Set-Content -NoNewline $notes
}

Write-Host "Scan folder prepared:" $target
Write-Host "Add console.log / network.har / screenshots into the folder."