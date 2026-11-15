# BANTADS — E2E via HTTPie CLI (json-catalog + Rabbit + outbox)
$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
Set-Location $PSScriptRoot

$hasHttp = Get-Command http -ErrorAction SilentlyContinue
$hasModule = $false
if (-not $hasHttp) {
    python -m httpie --version 2>$null | Out-Null
    if ($LASTEXITCODE -eq 0) { $hasModule = $true }
}
if (-not $hasHttp -and -not $hasModule) {
    Write-Error "HTTPie não encontrado. Instale: pip install httpie"
}

Push-Location "$Root\backend\contract-tests"
python -m pip install -q -r requirements.txt
Pop-Location

python "$PSScriptRoot\run-transacoes-e2e.py" @args
exit $LASTEXITCODE
