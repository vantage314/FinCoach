$ErrorActionPreference = 'Stop'

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot '..\\..')
Set-Location $repoRoot

New-Item -ItemType Directory -Force -Path doc\qa | Out-Null
New-Item -ItemType Directory -Force -Path doc\qa\screenshots\20260212 | Out-Null

Write-Host "Installing QA dependencies..."
Push-Location scripts\qa
& npm install

Write-Host "Installing Playwright (Chromium)..."
& npx playwright install chromium

Write-Host "Running Playwright audit..."
& npx playwright test flow_audit.spec.ts --reporter=line
Pop-Location
