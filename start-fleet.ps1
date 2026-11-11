# Abre WSL com frota (build + up -d) e mantém a sessão viva para os testes no Windows.
$repo = $PSScriptRoot
Write-Host "Iniciando frota BANTADS no WSL (deixe a janela Ubuntu aberta)..."
Start-Process wt.exe -ArgumentList @(
  "wsl", "-d", "Ubuntu", "--", "bash", "/mnt/e/CODE/DAC-1/start-hold.sh"
)
Write-Host "Aguardando gateway..."
Start-Sleep -Seconds 5
for ($i = 0; $i -lt 60; $i++) {
  try {
    $h = Invoke-RestMethod -Uri "http://localhost:3000/health" -TimeoutSec 3
    if ($h.status -eq "UP") {
      Write-Host "OK: $($h | ConvertTo-Json -Compress)"
      Write-Host "Rode: cd backend\contract-tests; python -m pytest -s -v"
      exit 0
    }
  } catch { }
  Start-Sleep -Seconds 3
}
Write-Host "Gateway ainda nao responde. Confira a janela WSL (build pode demorar na primeira vez)."
