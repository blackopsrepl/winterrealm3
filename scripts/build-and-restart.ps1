param(
    [switch]$NoBuild
)

$ErrorActionPreference = "Stop"

if ($NoBuild) {
    powershell -NoProfile -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot "publish-official-to-deploy.ps1") -SkipBuild
} else {
    powershell -NoProfile -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot "publish-official-to-deploy.ps1")
}
if ($LASTEXITCODE -ne 0) { throw "Publish failed." }

docker compose up -d --force-recreate l2j-login l2j-game
if ($LASTEXITCODE -ne 0) { throw "Docker restart failed." }

docker compose ps
