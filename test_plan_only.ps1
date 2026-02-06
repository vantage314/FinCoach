$BaseUrl = "http://localhost:8080/api"
$Headers = @{ "Content-Type" = "application/json" }

Write-Host "`n🤖 === 投资计划模块专项测试 === 🤖`n" -ForegroundColor Cyan

# 🔐 Step 0: 获取访问令牌 (模拟登录)
Write-Host "👉 正在注册/登录以获取 Token..." -NoNewline
$Timestamp = Get-Date -Format "MMddHHmmss"
$UserBody = '{"username":"plan_test_' + $Timestamp + '","password":"Password123!"}'
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

# 1. 准备资金 (为了能执行计划，先发 5万块钱)
Write-Host "👉 准备: 确保账户有钱 (存入 ¥50,000)..." -NoNewline
$AssetBody = '{"assetName":"计划测试资金","categoryId":1,"currentValue":50000,"holdingCost":50000,"formType":"CASH"}'
try {
    $R = Invoke-WebRequest -Uri "$BaseUrl/asset/add" -Method Post -Body $AssetBody -Headers $Headers -ErrorAction Stop
    Write-Host " ✅ [OK]" -ForegroundColor Green
} catch { Write-Host " (异常: $($_.Exception.Message))" -ForegroundColor Gray }

# 2. 生成计划
Write-Host "`n👉 测试: 生成定投计划 (投入 ¥10,000)..." -NoNewline
try {
    $R = Invoke-WebRequest -Uri "$BaseUrl/plan/generate?planType=CONTRIBUTION&investMoney=10000" -Method Post -Headers $Headers -ErrorAction Stop
    $Json = $R.Content | ConvertFrom-Json
    if ($Json.code -eq 200) {
        Write-Host " ✅ [OK] 生成成功" -ForegroundColor Green
    } else {
        Write-Host " ❌ 失败: $($Json.message)" -ForegroundColor Red; exit 1
    }
} catch {
    Write-Host " ❌ 接口崩溃: $($_.Exception.Message)" -ForegroundColor Red; exit 1
}

# 3. 查询计划 (获取 PlanID)
Write-Host "👉 测试: 查询最新计划..." -NoNewline
$PlanId = 0
try {
    $R = Invoke-WebRequest -Uri "$BaseUrl/plan/list" -Method Get -Headers $Headers -ErrorAction Stop
    $Raw = $R.Content
    $Json = $Raw | ConvertFrom-Json
    
    # 支持多种结构
    $List = $null
    if ($Json.data.records) { 
        $List = $Json.data.records 
    } elseif ($Json.data -is [array]) {
        $List = $Json.data
    } else {
        $List = @()
    }
    
    if ($List.Count -gt 0) {
        $PlanId = $List[0].id
        Write-Host " ✅ [OK] 找到基础 ID: $PlanId (状态: $($List[0].status))" -ForegroundColor Green
    } else {
        Write-Host " ❌ 列表仍为空。Raw Response: $Raw" -ForegroundColor Red; exit 1
    }
} catch { Write-Host " ❌ 查询失败: $($_.Exception.Message)" -ForegroundColor Red; exit 1 }

# 4. 执行计划 (最容易挂的一步)
if ($PlanId -gt 0) {
    Write-Host "👉 测试: 执行计划 (ID: $PlanId)..." -NoNewline
    try {
        $R = Invoke-WebRequest -Uri "$BaseUrl/plan/execute?planId=$PlanId" -Method Post -Headers $Headers -ErrorAction Stop
        $Json = $R.Content | ConvertFrom-Json
        if ($Json.code -eq 200) {
            Write-Host " ✅ [OK] 执行成功！资金已划转。" -ForegroundColor Green
        } else {
            Write-Host " ❌ 执行被拒绝: $($Json.message)" -ForegroundColor Red
        }
    } catch {
        Write-Host " ❌ [FATAL] 执行接口崩溃 (TransactionService 钩子可能出错)" -ForegroundColor Red
        Write-Host "    详情: $($_.Exception.Message)" -ForegroundColor Yellow
    }
}

Write-Host "`n请回到浏览器验证：`n1. 投资计划页状态是否变更为 'COMPLETED'。`n2. 资金流水页是否有 'BUY' 记录。" -ForegroundColor Yellow
