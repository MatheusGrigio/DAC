############################################################
#  START BACKEND TESTE
############################################################

$startTime = Get-Date

function Section($text) {
    Write-Host ""
    Write-Host "==================================================" -ForegroundColor DarkGray
    Write-Host $text -ForegroundColor Cyan
    Write-Host "==================================================" -ForegroundColor DarkGray
}

function Success($text) {
    Write-Host "✅ $text" -ForegroundColor Green
}

function WarningMsg($text) {
    Write-Host "⚠️  $text" -ForegroundColor Yellow
}

function ErrorMsg($text) {
    Write-Host ""
    Write-Host "❌ $text" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "🚀  START SCRIPT" -ForegroundColor Yellow
Write-Host ""

############################################################
# 1 - Docker
############################################################

Section "[1/9] Verificando Docker..."

$docker = Get-Command docker -ErrorAction SilentlyContinue

if (-not $docker) {
    ErrorMsg "Docker não encontrado."
}

Success "Docker encontrado."

############################################################
# 2 - Docker Desktop
############################################################

Section "[2/9] Verificando Docker Desktop..."

docker ps > $null 2>&1

if ($LASTEXITCODE -ne 0) {
    ErrorMsg "Docker Desktop não está rodando."
}

Success "Docker Desktop está rodando."

############################################################
# 3 - docker-compose.yml
############################################################

Section "[3/9] Procurando docker-compose.yml..."

$composeFile = Join-Path $PSScriptRoot "docker-compose.yml"

if (!(Test-Path $composeFile)) {
    ErrorMsg "Arquivo docker-compose.yml não encontrado.`n$composeFile"
}

Success "docker-compose.yml encontrado."

############################################################
# 4 - Compose
############################################################

Section "[4/9] Detectando Docker Compose..."

$legacy = Get-Command docker-compose -ErrorAction SilentlyContinue

if ($legacy) {

    $UseLegacy = $true
    Success "Utilizando docker-compose"

}
else {

    $UseLegacy = $false
    Success "Utilizando docker compose"

}

############################################################
# 5 - Build
############################################################

Section "[5/9] Build das imagens..."

$buildStart = Get-Date

if ($UseLegacy) {

    docker-compose -f $composeFile build

}
else {

    docker compose -f $composeFile build

}

if ($LASTEXITCODE -ne 0) {
    ErrorMsg "Erro durante o build."
}

$buildTime = (Get-Date) - $buildStart

Success "Build concluído em $([math]::Round($buildTime.TotalSeconds,2)) segundos."

############################################################
# 6 - Up
############################################################

Section "[6/9] Subindo containers..."

$upStart = Get-Date

if ($UseLegacy) {

    docker-compose -f $composeFile up -d

}
else {

    docker compose -f $composeFile up -d

}

if ($LASTEXITCODE -ne 0) {
    ErrorMsg "Erro ao subir os containers."
}

$upTime = (Get-Date) - $upStart

Success "Containers iniciados em $([math]::Round($upTime.TotalSeconds,2)) segundos."

############################################################
# 7 - Espera
############################################################

Section "[7/9] Aguardando inicialização..."

for ($i=5; $i -ge 1; $i--) {

    Write-Host "Iniciando serviços... $i"

    Start-Sleep 1

}

Success "Serviços iniciados."

############################################################
# 8 - Status
############################################################

Section "[8/9] Containers ativos"

docker ps

############################################################
# 9 - Finalização
############################################################

Section "[9/9] Finalização"

$total = (Get-Date) - $startTime

Success "Ambiente iniciado com sucesso."

Write-Host ""
Write-Host "Tempo total: $([math]::Round($total.TotalSeconds,2)) segundos" -ForegroundColor Yellow

Write-Host ""
Write-Host "Comandos úteis:" -ForegroundColor Cyan
Write-Host ""

if ($UseLegacy) {

    Write-Host "Ver logs:"
    Write-Host "docker-compose logs -f"
    Write-Host ""

    Write-Host "Status:"
    Write-Host "docker-compose ps"
    Write-Host ""

    Write-Host "Parar:"
    Write-Host "docker-compose down"
    Write-Host ""

    Write-Host "Rebuild:"
    Write-Host "docker-compose up -d --build"

}
else {

    Write-Host "Ver logs:"
    Write-Host "docker compose logs -f"
    Write-Host ""

    Write-Host "Status:"
    Write-Host "docker compose ps"
    Write-Host ""

    Write-Host "Parar:"
    Write-Host "docker compose down"
    Write-Host ""

    Write-Host "Rebuild:"
    Write-Host "docker compose up -d --build"

}

Write-Host ""
Write-Host "==========================================" -ForegroundColor Green
Write-Host "          RODANDO"
Write-Host "==========================================" -ForegroundColor Green
Write-Host ""

exit 0