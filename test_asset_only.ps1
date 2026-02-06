$BaseUrl = "http://localhost:8080/api"
$Headers = @{ "Content-Type" = "application/json" }

Write-Host "`n💰 === 资产管理模块专项测试 === 💰`n" -ForegroundColor Cyan

# 🔐 Step 0: 获取访问令牌 (模拟登录)
Write-Host "👉 正在注册/登录以获取 Token..." -NoNewline
$Timestamp = Get-Date -Format "MMddHHmmss"
$UserBody = '{"username":"asset_test_' + $Timestamp + '","password":"Password123!"}'
try {
    # 尝试注册 (忽略已存在错误)
    Invoke-WebRequest -Uri "$BaseUrl/auth/register" -Method Post -Body $UserBody -Headers $Headers -ErrorAction SilentlyContinue | Out-Null
    # 登录
    $LoginResponse = Invoke-WebRequest -Uri "$BaseUrl/auth/login" -Method Post -Body $UserBody -Headers $Headers -ErrorAction Stop
    $Json = $LoginResponse.Content | ConvertFrom-Json
    if ($Json.code -eq 200) {
        $Token = $Json.data
        $Headers["Authorization"] = "Bearer $Token"
        Write-Host " ✅ [Token OK]" -ForegroundColor Green
    } else {
        Write-Host " ❌ [Login FAIL]" -ForegroundColor Red; exit 1
    }
} catch {
    Write-Host " ❌ [Auth ERROR]: $($_.Exception.Message)" -ForegroundColor Red; exit 1
}

# 1. 获取列表 (验证数据解析)
Write-Host "👉 测试: 获取资产列表..." -NoNewline
try {
    $R = Invoke-WebRequest -Uri "$BaseUrl/asset/list" -Method Get -Headers $Headers -ErrorAction Stop
    $Json = $R.Content | ConvertFrom-Json
    
    if ($Json.code -eq 200) {
        $Count = 0
        # 兼容性检查
        if ($Json.data.records) { $Count = $Json.data.records.Count }
        elseif ($Json.data.Count) { $Count = $Json.data.Count }
        elseif ($Json.data -is [array]) { $Count = $Json.data.Count }
        
        Write-Host " ✅ [OK] 当前资产数: $Count" -ForegroundColor Green
    } else {
        Write-Host " ❌ 业务错误: $($Json.message)" -ForegroundColor Red
    }
} catch {
    Write-Host " ❌ 请求失败: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# 2. 获取统计 (验证统计接口)
Write-Host "👉 测试: 获取资产概览(Summary)..." -NoNewline
try {
    $R = Invoke-WebRequest -Uri "$BaseUrl/asset/summary" -Method Get -Headers $Headers -ErrorAction Stop
    Write-Host " ✅ [OK]" -ForegroundColor Green
} catch {
    Write-Host " ❌ [FAIL]" -ForegroundColor Red
}

# 3. 核心测试：新增资产 (验证 Phase 10 的流水 Hook 是否导致崩溃)
Write-Host "👉 测试: 录入一笔测试资金 (¥100)..." -NoNewline
$AssetBody = '{"assetName":"自动化测试资金","categoryId":1,"currentValue":100,"holdingCost":100,"formType":"CASH"}'
try {
    $R = Invoke-WebRequest -Uri "$BaseUrl/asset/add" -Method Post -Body $AssetBody -Headers $Headers -ErrorAction Stop
    $Json = $R.Content | ConvertFrom-Json
    if ($Json.code -eq 200) {
        Write-Host " ✅ [OK] 录入成功" -ForegroundColor Green
    } else {
        Write-Host " ❌ 录入失败: $($Json.message)" -ForegroundColor Red
    }
} catch {
    Write-Host " ❌ [FATAL] 录入接口崩溃 (可能是 TransactionService 注入失败)" -ForegroundColor Red
    Write-Host "    详情: $($_.Exception.Message)" -ForegroundColor Yellow
}

Write-Host "`n请回到浏览器：`n1. 刷新页面，查看是否有'自动化测试资金'。`n2. 点击'资金流水'，确认是否有一笔 +100 的记录。" -ForegroundColor Yellow
