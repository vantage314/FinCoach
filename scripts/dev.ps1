param(
    [ValidateSet('all','backend','frontend')]
    [string]$Mode = 'all',
    [string]$JavaHome
)

Set-StrictMode -Version Latest

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot '..')
Set-Location $repoRoot

function Write-Info($message) {
    Write-Host "[dev.ps1] $message"
}

function Load-EnvFile($path) {
    if (!(Test-Path $path)) {
        Write-Warning "Missing $path. Copy .env.example to .env before running backend."
        return
    }

    Get-Content $path | ForEach-Object {
        $line = $_.Trim()
        if ($line.Length -eq 0) { return }
        if ($line.StartsWith('#')) { return }

        $idx = $line.IndexOf('=')
        if ($idx -lt 1) { return }

        $name = $line.Substring(0, $idx).Trim()
        $value = $line.Substring($idx + 1).Trim()
        if ($name.Length -gt 0) {
            Set-Item -Path "Env:$name" -Value $value
        }
    }
}

if ($JavaHome) {
    $env:JAVA_HOME = $JavaHome
    $env:Path = "$JavaHome\bin;$env:Path"
    Write-Info "JAVA_HOME set to $JavaHome"
}

$backendProc = $null
$frontendProc = $null

if ($Mode -eq 'all' -or $Mode -eq 'backend') {
    $backendDir = Join-Path $repoRoot 'backend'
    Load-EnvFile (Join-Path $backendDir '.env')

    $port = $env:SERVER_PORT
    if (-not $port) { $port = 8080 }

    Write-Info "Starting backend (Spring Boot) on port $port..."
    $backendProc = Start-Process -FilePath "mvn" -ArgumentList "-q -DskipTests spring-boot:run" -WorkingDirectory $backendDir -PassThru
    Write-Info "Backend PID: $($backendProc.Id)"
    Write-Info "Backend URL: http://localhost:$port"
}

if ($Mode -eq 'all' -or $Mode -eq 'frontend') {
    $frontendDir = Join-Path $repoRoot 'frontend'
    $nodeModules = Join-Path $frontendDir 'node_modules'
    if (!(Test-Path $nodeModules)) {
        Write-Info "Installing frontend dependencies..."
        Push-Location $frontendDir
        & npm install
        Pop-Location
    }

    Write-Info "Starting frontend (Vite)..."
    $frontendProc = Start-Process -FilePath "npm" -ArgumentList "run dev" -WorkingDirectory $frontendDir -PassThru
    Write-Info "Frontend PID: $($frontendProc.Id)"
    Write-Info "Frontend URL: http://localhost:5173"
}

Write-Host ""
Write-Info "Use Ctrl+C in the respective window or stop by PID:"
if ($backendProc) { Write-Host "  Stop backend: Stop-Process -Id $($backendProc.Id)" }
if ($frontendProc) { Write-Host "  Stop frontend: Stop-Process -Id $($frontendProc.Id)" }
