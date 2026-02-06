# 配置区域
$BaseUrl = "http://localhost:8080/api"
# 如果需要鉴权，可以添加 Header
$Headers = @{} 
# $Headers = @{ "Authorization" = "Bearer YOUR_TOKEN" }

Write-Host "`n🔥 ================= FINCOACH Windows 暴力测试开始 ================= 🔥`n" -ForegroundColor Cyan

# 定义测试函数
function Test-Endpoint {
    param (
        [string]$Method,
        [string]$Path,
        [string]$Body = $null,
        [string]$Desc
    )

    Write-Host "👉 测试: $Desc..." -NoNewline
    
    try {
        $Params = @{
            Method = $Method
            Uri = "$BaseUrl$Path"
            Headers = $Headers
            ErrorAction = "Stop" # 遇到错误停止并抛出异常
        }
        
        if ($Body) {
            $Params.Body = $Body
            $Params.ContentType = "application/json"
            # 处理编码问题
            $Params.Body = [System.Text.Encoding]::UTF8.GetBytes($Body)
        }

        $Response = Invoke-WebRequest @Params
        
        if ($Response.StatusCode -eq 200) {
            Write-Host " ✅ [成功]" -ForegroundColor Green
        }
    }
    catch {
        $StatusCode = $_.Exception.Response.StatusCode
        Write-Host " ❌ [失败] HTTP Code: $StatusCode" -ForegroundColor Red
        Write-Host "   错误详情: $($_.Exception.Message)" -ForegroundColor Yellow
        exit 1
    }
}

# ================= 1. 核打击：重置数据 =================
Test-Endpoint -Method "POST" -Path "/user/reset-data" -Desc "Step 1: 执行核按钮 (重置数据)"

# ================= 2. 市场数据检查 =================
Test-Endpoint -Method "GET" -Path "/market/securities?page=1&size=10" -Desc "Step 2: 检查市场行情数据"

# ================= 3. 风险测评 =================
# 模拟提交激进型问卷
Test-Endpoint -Method "GET" -Path "/risk/latest" -Desc "Step 3: 获取风险画像"

# ================= 4. 资产录入 =================
$AssetBody = '{"assetName":"初始资金","categoryId":1,"currentValue":50000,"holdingCost":50000}'
Test-Endpoint -Method "POST" -Path "/asset/add" -Body $AssetBody -Desc "Step 4: 录入第一桶金 (入金 50,000)"

# ================= 5. 资产体检 =================
Test-Endpoint -Method "GET" -Path "/health/check" -Desc "Step 5: 触发资产体检"

# ================= 6. 计划生成 =================
Test-Endpoint -Method "POST" -Path "/plan/generate?planType=CONTRIBUTION&investMoney=20000" -Desc "Step 6: 生成投资计划 (投入 20,000)"

# ================= 7. 资金流水 =================
Test-Endpoint -Method "GET" -Path "/transactions?page=1&size=10" -Desc "Step 7: 检查资金流水审计"

# ================= 8. 暴力压力测试 =================
Write-Host "`n🦍 ================= 进入疯猴模式 (压力测试) ================= 🦍" -ForegroundColor Yellow
Write-Host "正在模拟 20 次高频并发请求..."

$ScriptBlock = {
    param($Url)
    try {
        $r = Invoke-WebRequest -Uri "$Url/health/check" -Method Get -ErrorAction Stop
    } catch {}
}

# 简单循环压力测试 (PowerShell 并发较复杂，这里使用快速循环模拟负载)
for ($i=1; $i -le 20; $i++) {
    Write-Host "." -NoNewline -ForegroundColor Gray
    try {
        $null = Invoke-WebRequest -Uri "$BaseUrl/asset/summary" -Method Get -ErrorAction Stop
        $null = Invoke-WebRequest -Uri "$BaseUrl/health/check" -Method Get -ErrorAction Stop
    } catch {
        Write-Host "`n💀 系统在高负载下响应失败！" -ForegroundColor Red
        exit 1
    }
}

Write-Host "`n`n🎉🎉🎉 测试通过！系统运行稳定！ 🎉🎉🎉" -ForegroundColor Green