# Shared by the Windows helpers; dot-source it, do not run it.
# Compatible with Windows PowerShell 5.1, which every Windows 10/11 PC has.

$RootDir = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
$ComposeFile = Join-Path $RootDir "docker-compose.yml"
$ProjectName = "winterrealm3"
$DbContainer = "winterrealm3-db"
$LoginContainer = "winterrealm3-login"
$GameContainer = "winterrealm3-game"
$FirewallRuleName = "Winterrealm3 game server"

# Windows PowerShell turns a native program's stderr into a terminating error
# when it is redirected under "Stop". Probes run with "Continue" instead.
function Test-NativeSuccess {
    param([scriptblock]$Command)
    $previous = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    try {
        & $Command *> $null
        return ($LASTEXITCODE -eq 0)
    } finally {
        $ErrorActionPreference = $previous
    }
}

function Get-NativeOutput {
    param([scriptblock]$Command)
    $previous = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    try {
        $output = & $Command 2> $null
        if ($LASTEXITCODE -ne 0) { return "" }
        return (($output | Out-String).Trim())
    } finally {
        $ErrorActionPreference = $previous
    }
}

function Invoke-Compose {
    & docker compose -f $ComposeFile -p $ProjectName @args
    if ($LASTEXITCODE -ne 0) { throw "docker compose $($args -join ' ') failed." }
}

function Assert-DockerReady {
    if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
        Write-Host ""
        Write-Host "Docker Desktop is not installed." -ForegroundColor Red
        Write-Host "Install it from https://www.docker.com/products/docker-desktop/"
        Write-Host "restart the PC if the installer asks, open Docker Desktop once,"
        Write-Host "then run this helper again."
        throw "Docker Desktop is required."
    }

    if (Test-NativeSuccess { docker info }) { return }

    $desktop = Join-Path $env:ProgramFiles "Docker\Docker\Docker Desktop.exe"
    if (Test-Path -LiteralPath $desktop) {
        Write-Host "Starting Docker Desktop. The first start can take a couple of minutes..."
        Start-Process -FilePath $desktop | Out-Null
    } else {
        Write-Host "Open Docker Desktop from the Start menu and wait until it says the engine is running."
    }

    $deadline = (Get-Date).AddMinutes(5)
    while ((Get-Date) -lt $deadline) {
        Start-Sleep -Seconds 5
        if (Test-NativeSuccess { docker info }) {
            Write-Host "Docker is running."
            return
        }
    }
    throw "Docker did not become ready. Open Docker Desktop, accept its terms if asked, wait for 'Engine running', then try again."
}

function Wait-Healthy {
    param([string]$Container, [string]$Label, [int]$TimeoutSeconds)
    Write-Host "Waiting for $Label..."
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ($true) {
        $health = Get-NativeOutput { docker inspect -f "{{.State.Health.Status}}" $Container }
        if ($health -eq "healthy") { return }
        $state = Get-NativeOutput { docker inspect -f "{{.State.Status}}" $Container }
        if ($health -eq "unhealthy" -or $state -eq "exited") {
            Write-Host "$Label failed. Recent log:" -ForegroundColor Red
            $previous = $ErrorActionPreference
            $ErrorActionPreference = "Continue"
            & docker logs --tail 40 $Container
            $ErrorActionPreference = $previous
            throw "$Label failed to start."
        }
        if ((Get-Date) -gt $deadline) {
            throw "Timed out waiting for $Label. See the log with: logs-windows.cmd"
        }
        Start-Sleep -Seconds 3
    }
}
