# Sincroniza o container email com MAIL_DEV do .env e roda a suíte pytest.
$ErrorActionPreference = "Stop"
$root = $PSScriptRoot

if (Test-Path (Join-Path $root ".env")) {
    docker compose -f (Join-Path $root "docker-compose.yml") up -d email --force-recreate | Out-Null
    $mailDev = docker compose -f (Join-Path $root "docker-compose.yml") exec -T email printenv MAIL_DEV 2>$null
    if ($mailDev -ne "true") {
        Write-Warning "MAIL_DEV no container email é '$mailDev'; para automatic_password use MAIL_DEV=true no .env"
    }
}

Push-Location (Join-Path $root "test_bantads-main")
try {
    python -m pytest @args
} finally {
    Pop-Location
}