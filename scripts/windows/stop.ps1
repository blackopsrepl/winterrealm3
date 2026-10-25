# Stops the Winterrealm3 world. Characters stay in the database volume.
$ErrorActionPreference = "Stop"
. (Join-Path $PSScriptRoot "common.ps1")

Assert-DockerReady
Invoke-Compose down
Write-Host "Winterrealm3 stopped. Characters and progress are kept." -ForegroundColor Green
