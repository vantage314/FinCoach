$BaseUrl = "http://localhost:8080/api"
$Headers = @{ "Content-Type" = "application/json" }

Write-Host "`n📊 === 市场中心专项测试 === 📊`n" -ForegroundColor Cyan

# 🔐 Step 0: 获取访问令牌 (模拟登录)
Write-Host "👉 正在注册/登录以获取 Token..." -NoNewline
$Timestamp = Get-Date -Format "MMddHHmmss"
$UserBody = '{"username":"market_test_' + $Timestamp + '","password":"Password123!"}'
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

# 1. 测试分页数据
Write-Host "👉 测试: 获取第 1 页数据..." -NoNewline
try {
    $R = Invoke-WebRequest -Uri "$BaseUrl/market/securities?page=1&size=5" -Method Get -Headers $Headers -ErrorAction Stop
    $Json = $R.Content | ConvertFrom-Json
    
    if ($Json.code -eq 200) {
        $Count = 0
        if ($Json.data.records) { $Count = $Json.data.records.Count }
        elseif ($Json.data.Count) { $Count = $Json.data.Count }
        
        Write-Host " ✅ [OK] 获取到 $Count 条数据" -ForegroundColor Green
        # 打印第一条数据名称，确保不是空的
        if ($Count -gt 0) {
            $Name = if($Json.data.records) { $Json.data.records[0].name } else { $Json.data[0].name }
            Write-Host "   📝 第一条数据: $Name" -ForegroundColor Gray
        }
    } else {
        Write-Host " ❌ 业务状态码错误: $($Json.code)" -ForegroundColor Red
    }
} catch {
    Write-Host " ❌ 请求失败: $($_.Exception.Message)" -ForegroundColor Red
}

# 2. 测试筛选 (比如只查股票)
Write-Host "`n👉 测试: 筛选类型=STOCK..." -NoNewline
try {
    $R = Invoke-WebRequest -Uri "$BaseUrl/market/securities?page=1&size=5&type=STOCK" -Method Get -Headers $Headers -ErrorAction Stop
    Write-Host " ✅ [OK]" -ForegroundColor Green
} catch {
    Write-Host " ❌ [FAIL]" -ForegroundColor Red
}

Write-Host "`n请回到浏览器刷新市场中心，确认列表是否已显示。" -ForegroundColor Yellow
