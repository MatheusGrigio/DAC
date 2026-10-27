# Compile BANTADS apps one at a time to keep RAM low (backend MSs + gateway + frontend).
# Usage: .\compile-services.ps1
#        .\compile-services.ps1 --test
$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$services = Join-Path $root "backend\services"
$gateway = Join-Path $root "backend\gateway"
$frontend = Join-Path $root "frontend"
$withTest = $args -contains "--test"
$gradlew = Join-Path $services "gradlew.bat"
$gradleOpts = @("--no-daemon", "--no-parallel", "--max-workers=1")

function Invoke-Gradle {
    param([Parameter(Mandatory = $true)][string[]]$GradleArgs)
    Push-Location $services
    try {
        & $gradlew @gradleOpts @GradleArgs
        if ($LASTEXITCODE -ne 0) {
            throw "Gradle failed: $($GradleArgs -join ' ')"
        }
    }
    finally {
        Pop-Location
    }
}

function Invoke-NpmBuild {
    param(
        [Parameter(Mandatory = $true)][string]$Name,
        [Parameter(Mandatory = $true)][string]$Dir
    )
    $packageJson = Join-Path $Dir "package.json"
    if (-not (Test-Path $packageJson)) {
        Write-Host "==> $Name (skipped, not present yet)"
        return
    }
    Write-Host "==> $Name"
    Push-Location $Dir
    try {
        if (-not (Test-Path "node_modules")) {
            npm ci
            if ($LASTEXITCODE -ne 0) { throw "npm ci failed ($Name)" }
        }
        npm run build
        if ($LASTEXITCODE -ne 0) { throw "$Name build failed" }
        if ($withTest) {
            $pkg = Get-Content "package.json" -Raw | ConvertFrom-Json
            if ($null -ne $pkg.scripts.test) {
                npm test -- --watch=false
                if ($LASTEXITCODE -ne 0) { throw "$Name tests failed" }
            }
        }
    }
    finally {
        Pop-Location
    }
}

Write-Host "==> stopping leftover Gradle daemon"
Push-Location $services
try {
    & $gradlew --stop | Out-Null
}
catch {
    # ignore missing daemon
}
finally {
    Pop-Location
}

Write-Host "==> shared"
Invoke-Gradle -GradleArgs @(":shared:jar")
if ($withTest) {
    Invoke-Gradle -GradleArgs @(":shared:test")
}

foreach ($module in @("auth", "cliente", "gerente", "conta", "saga", "email")) {
    Write-Host "==> $module"
    Invoke-Gradle -GradleArgs @(":${module}:bootJar")
    if ($withTest) {
        Invoke-Gradle -GradleArgs @(":${module}:test")
    }
}

Invoke-NpmBuild -Name "gateway" -Dir $gateway
Invoke-NpmBuild -Name "frontend" -Dir $frontend

Write-Host "==> done (sequential compile)"
