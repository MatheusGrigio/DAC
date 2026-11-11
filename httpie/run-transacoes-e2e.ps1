# BANTADS — E2E via HTTPie CLI (json-catalog + Rabbit + outbox)
$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
Set-Location $PSScriptRoot

if (-not (Get-Command http -ErrorAction SilentlyContinue)) {
    Write-Error "HTTPie CLI (http) não está no PATH."
}

Push-Location "$Root\backend\contract-tests"
python -m pip install -q -r requirements.txt
Pop-Location

python "$PSScriptRoot\run-transacoes-e2e.py" @args
exit $LASTEXITCODE
