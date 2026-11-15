# BANTADS — campanha completa de testes (ordem do RELATORIO-TESTES-COMPLETO.md)
#
# Uso:
#   .\run-test-campanha.ps1              # frota já no ar
#   .\run-test-campanha.ps1 -FromZero    # docker compose down -v + up --build
#   .\run-test-campanha.ps1 -FailFast    # para na primeira falha
#
param(
    [switch]$FromZero,
    [switch]$FailFast,
    [switch]$SkipHttpie,
    [switch]$SkipPlaywright,
    [switch]$SkipFrontendUnit
)

$ErrorActionPreference = "Stop"
$Root = $PSScriptRoot
$LogDir = Join-Path $Root "test-logs"
$Stamp = Get-Date -Format "yyyyMMdd-HHmmss"
$Summary = Join-Path $LogDir "campanha-$Stamp.txt"

function Write-Step([string]$Msg) {
    $line = "== $Msg =="
    Write-Host $line -ForegroundColor Cyan
    Add-Content -Path $Summary -Value $line
}

function Invoke-CampanhaStep {
    param(
        [Parameter(Mandatory = $true)][string]$Name,
        [Parameter(Mandatory = $true)][scriptblock]$Action
    )
    Write-Step $Name
    Push-Location $Root
    try {
        & $Action
        $code = $LASTEXITCODE
        if ($null -eq $code) { $code = 0 }
        $result = if ($code -eq 0) { "PASS" } else { "FAIL ($code)" }
        Add-Content -Path $Summary -Value "$Name : $result"
        if ($code -ne 0 -and $FailFast) {
            throw "Falha em: $Name (exit $code)"
        }
        return $code
    }
    finally {
        Pop-Location
    }
}

function Wait-GatewayUp {
    param([int]$TimeoutSec = 180)
    $deadline = (Get-Date).AddSeconds($TimeoutSec)
    while ((Get-Date) -lt $deadline) {
        try {
            $h = Invoke-RestMethod -Uri "http://localhost:3000/health" -TimeoutSec 5
            if ($h.status -eq "UP") {
                Write-Host "Gateway UP"
                return
            }
        }
        catch {}
        Start-Sleep -Seconds 3
    }
    throw "Gateway não respondeu UP em ${TimeoutSec}s"
}

function Invoke-RebootSeed {
    Write-Host "POST /reboot …"
    $null = Invoke-RestMethod `
        -Uri "http://localhost:3000/reboot" `
        -Method POST `
        -ContentType "application/json" `
        -Body "{}" `
        -TimeoutSec 120
}

New-Item -ItemType Directory -Force -Path $LogDir | Out-Null
"# Campanha BANTADS $Stamp" | Set-Content -Path $Summary -Encoding utf8
Add-Content -Path $Summary -Value "FromZero=$FromZero FailFast=$FailFast"

$exitCodes = @{}

if ($FromZero) {
    Invoke-CampanhaStep "docker compose down -v" {
        Set-Location $Root
        docker compose down -v
    } | Out-Null
    $exitCodes["docker_down"] = $LASTEXITCODE

    Invoke-CampanhaStep "docker compose up -d --build" {
        Set-Location $Root
        docker compose up -d --build
    } | Out-Null
    $exitCodes["docker_up"] = $LASTEXITCODE
    if ($exitCodes["docker_up"] -ne 0 -and $FailFast) { exit 1 }
}

Invoke-CampanhaStep "wait gateway + reboot inicial" {
    Wait-GatewayUp
    Invoke-RebootSeed
} | Out-Null

$exitCodes["test_bantads"] = Invoke-CampanhaStep "test_bantads-main pytest" {
    Set-Location (Join-Path $Root "test_bantads-main")
    Remove-Item cache.json -ErrorAction SilentlyContinue
    python -m pytest -v 2>&1 | Tee-Object -FilePath (Join-Path $LogDir "test_bantads-$Stamp.txt")
    exit $LASTEXITCODE
}

Invoke-CampanhaStep "reboot (pré contract-tests)" { Invoke-RebootSeed } | Out-Null

$venvPy = Join-Path $Root "backend\contract-tests\.venv\Scripts\python.exe"
if (-not (Test-Path $venvPy)) {
    $venvPy = "python"
}

$exitCodes["contract_tests"] = Invoke-CampanhaStep "backend/contract-tests pytest" {
    Set-Location (Join-Path $Root "backend\contract-tests")
    & $venvPy -m pytest -s -v 2>&1 | Tee-Object -FilePath (Join-Path $LogDir "contract-tests-$Stamp.txt")
    exit $LASTEXITCODE
}

$exitCodes["gateway"] = Invoke-CampanhaStep "backend/gateway npm test" {
    Set-Location (Join-Path $Root "backend\gateway")
    npm test 2>&1 | Tee-Object -FilePath (Join-Path $LogDir "gateway-$Stamp.txt")
    exit $LASTEXITCODE
}

$exitCodes["gradle"] = Invoke-CampanhaStep "backend/services gradlew test" {
    Set-Location (Join-Path $Root "backend\services")
    .\gradlew.bat --no-daemon --no-parallel --max-workers=1 test 2>&1 `
        | Tee-Object -FilePath (Join-Path $LogDir "gradle-$Stamp.txt")
    exit $LASTEXITCODE
}

if (-not $SkipFrontendUnit) {
    $exitCodes["frontend_unit"] = Invoke-CampanhaStep "frontend npm test" {
        Set-Location (Join-Path $Root "frontend")
        npm test -- --watch=false 2>&1 | Tee-Object -FilePath (Join-Path $LogDir "frontend-unit-$Stamp.txt")
        exit $LASTEXITCODE
    }
}

$exitCodes["frontend_build"] = Invoke-CampanhaStep "frontend npm run build" {
    Set-Location (Join-Path $Root "frontend")
    npm run build 2>&1 | Tee-Object -FilePath (Join-Path $LogDir "frontend-build-$Stamp.txt")
    exit $LASTEXITCODE
}

if (-not $SkipPlaywright) {
    Invoke-CampanhaStep "reboot (pré Playwright)" { Invoke-RebootSeed } | Out-Null
    $exitCodes["playwright"] = Invoke-CampanhaStep "e2e Playwright" {
        $env:CI = "true"
        Set-Location (Join-Path $Root "e2e")
        npx playwright test 2>&1 | Tee-Object -FilePath (Join-Path $LogDir "playwright-$Stamp.txt")
        exit $LASTEXITCODE
    }
}

if (-not $SkipHttpie) {
    Invoke-CampanhaStep "reboot (pré HTTPie E2E)" { Invoke-RebootSeed } | Out-Null
    $exitCodes["httpie"] = Invoke-CampanhaStep "httpie run-transacoes-e2e.py" {
        Set-Location (Join-Path $Root "backend\contract-tests")
        & $venvPy -m pip install -q -r requirements.txt
        & $venvPy -m pip install -q httpie
        Set-Location $Root
        & $venvPy (Join-Path $Root "httpie\run-transacoes-e2e.py") 2>&1 `
            | Tee-Object -FilePath (Join-Path $LogDir "httpie-e2e-$Stamp.txt")
        exit $LASTEXITCODE
    }
}

Write-Step "resumo"
$failed = 0
foreach ($kv in $exitCodes.GetEnumerator() | Sort-Object Name) {
    $line = "$($kv.Key): $($kv.Value)"
    Write-Host $line
    Add-Content -Path $Summary -Value $line
    if ($kv.Value -ne 0) { $failed++ }
}

Add-Content -Path $Summary -Value "Falhas: $failed"
Write-Host "Log da campanha: $Summary" -ForegroundColor Green
Write-Host "Logs por etapa: $LogDir"

if ($failed -gt 0) { exit 1 }
exit 0