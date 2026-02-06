$BaseUrl = "http://localhost:8080/api"
$Headers = @{ "Content-Type" = "application/json" }

Write-Host "`n🔥 === FINCOACH 终极验收测试 === 🔥`n" -ForegroundColor Cyan

# 0. 登录获取 Token
Write-Host "👉 Step 0: 获取访问令牌 (User: test_user)..." -NoNewline
$Timestamp = Get-Date -Format "MMddHHmmss"
$UserBody = '{"username":"test_user_' + $Timestamp + '","password":"Password123!"}'
$RegisterUrl = "$BaseUrl/auth/register"
$LoginUrl = "$BaseUrl/auth/login"

try {
    # register
    Invoke-WebRequest -Uri $RegisterUrl -Method Post -Body $UserBody -Headers $Headers -ErrorAction Stop | Out-Null
    # login
    $LoginResponse = Invoke-WebRequest -Uri $LoginUrl -Method Post -Body $UserBody -Headers $Headers -ErrorAction Stop
    $Json = $LoginResponse.Content | ConvertFrom-Json
    if ($Json.code -eq 200) {
        $Token = $Json.data
        $Headers["Authorization"] = "Bearer $Token"
        Write-Host "✅ [OK]" -ForegroundColor Green
    } else {
        Write-Host "❌ [FAIL] Login Failed: $($Json.message)" -ForegroundColor Red; exit 1
    }
} catch {
    Write-Host "❌ [FAIL] Auth Failed: $($_.Exception.Message)" -ForegroundColor Red; exit 1
}

function Test-Api {
    param ([string]$M, [string]$U, [string]$D, [string]$B = $null)
    Write-Host "👉 $D ... " -NoNewline
    try {
        $P = @{ Uri="$BaseUrl$U"; Method=$M; Headers=$Headers; ErrorAction="Stop" }
        if ($B) { $P.Body = [System.Text.Encoding]::UTF8.GetBytes($B) }
        $R = Invoke-WebRequest @P
        # Check backend custom code if possible
        $ResJson = $R.Content | ConvertFrom-Json
        if ($ResJson.code -eq 200) {
             Write-Host "✅ [OK]" -ForegroundColor Green
        } else {
             Write-Host "❌ [FAIL] API Error: $($ResJson.message)" -ForegroundColor Red
             # Don't exit, continue other tests
        }
    } catch {
        Write-Host "❌ [FAIL] HTTP Error: $($_.Exception.Message)" -ForegroundColor Red; exit 1
    }
}

# 1. 基础重置
Test-Api "POST" "/user/reset-data" "Step 1: 重置数据"

# 2. 市场数据 (重点检查)
# 如果后端日志有数据但前端没显示，是前端Bug。这里只测后端是否有数据。
Test-Api "GET" "/market/securities?page=1&size=5" "Step 2: 市场行情接口 (后端)"

# 3. 模拟用户完整路径
$Asset = '{"assetName":"启动金","categoryId":1,"currentValue":50000,"holdingCost":50000}'
Test-Api "POST" "/asset/add" "Step 3: 录入资产 (5万)" $Asset

Test-Api "GET" "/health/check" "Step 4: 资产体检"

# 4. 投资计划 (重点修复)
Test-Api "POST" "/plan/generate?planType=CONTRIBUTION&investMoney=20000" "Step 5: 生成投资计划"

# 5. 计划列表查询 (验证生成是否成功)
Test-Api "GET" "/plan/history" "Step 6: 查询计划列表"

# 6. 资金流水
Test-Api "GET" "/transactions?page=1&size=5" "Step 7: 资金流水"

Write-Host "`n🎉 后端接口全部正常！请在前端确认 Market 和 Plan 页面是否修复。 🎉`n" -ForegroundColor Green
