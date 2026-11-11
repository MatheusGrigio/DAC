# BANTADS — contract-tests (E2E) + gateway unit tests + relatório em RELATORIO-BACKEND-TESTES-FLUXOS.md
$ErrorActionPreference = "Stop"
$Root = $PSScriptRoot

Write-Host "==> Verificando frota Docker (WSL)..."
$health = try {
    (Invoke-WebRequest -Uri "http://localhost:3000/health" -UseBasicParsing -TimeoutSec 5).Content
} catch {
    $null
}
if (-not $health) {
    Write-Host "Subindo containers..."
    wsl -d Ubuntu -- bash -lc "cd /mnt/e/CODE/DAC-1 && docker compose up -d"
    $deadline = (Get-Date).AddMinutes(3)
    while ((Get-Date) -lt $deadline) {
        Start-Sleep -Seconds 5
        try {
            $h = Invoke-RestMethod -Uri "http://localhost:3000/health" -TimeoutSec 5
            if ($h.status -eq "UP") { break }
        } catch { }
    }
}

Push-Location "$Root\backend\contract-tests"
Write-Host "==> pytest contract-tests..."
python -m pytest -s -v
$pytestExit = $LASTEXITCODE
Pop-Location

Push-Location "$Root\backend\gateway"
Write-Host "==> npm test gateway..."
npm test
$npmExit = $LASTEXITCODE
Pop-Location

$report = "$Root\RELATORIO-BACKEND-TESTES-FLUXOS.md"
if (Test-Path $report) {
    $extra = @(
        "",
        "## Gateway npm test",
        "",
        "Exit code: **$npmExit** (0 = passou).",
        ""
    )
    Add-Content -Path $report -Value ($extra -join "`n") -Encoding utf8
}

if ($pytestExit -ne 0 -or $npmExit -ne 0) { exit 1 }
exit 0
