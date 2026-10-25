# Starts the Winterrealm3 world on a Windows PC with Docker Desktop.
# Double-click start-windows.cmd in the project folder to run it.
param(
    [string]$HostIp = $env:L2J_HOST_IP,
    [string]$LanCidr = $env:L2J_LAN_CIDR
)

$ErrorActionPreference = "Stop"
. (Join-Path $PSScriptRoot "common.ps1")

function Confirm-FirewallRule {
    # Family PCs reach the world through Windows Defender Firewall, which
    # blocks inbound connections until a rule allows them.
    if (Get-NetFirewallRule -DisplayName $FirewallRuleName -ErrorAction SilentlyContinue) { return }

    Write-Host ""
    Write-Host "Windows Firewall must allow the other PCs at home to reach ports 2106 and 7777."
    Write-Host "Windows will ask for permission once; choose Yes."
    $command = "New-NetFirewallRule -DisplayName '$FirewallRuleName' -Direction Inbound -Protocol TCP -LocalPort 2106,7777 -Action Allow -Profile Private | Out-Null"
    try {
        $process = Start-Process -FilePath "powershell.exe" -Verb RunAs -Wait -PassThru `
            -ArgumentList "-NoProfile", "-Command", $command
    } catch {
        $process = $null
    }
    if ($process -and $process.ExitCode -eq 0) {
        Write-Host "Firewall rule added."
    } else {
        Write-Host "The firewall rule was not added. Other PCs cannot join until it exists;" -ForegroundColor Yellow
        Write-Host "run start-windows.cmd again and choose Yes when Windows asks." -ForegroundColor Yellow
    }
}

function Show-NetworkProfileWarning {
    param([string]$Ip)
    $address = Get-NetIPAddress -IPAddress $Ip -ErrorAction SilentlyContinue | Select-Object -First 1
    if (-not $address) { return }
    $connection = Get-NetConnectionProfile -InterfaceIndex $address.InterfaceIndex -ErrorAction SilentlyContinue
    if ($connection -and $connection.NetworkCategory -eq "Public") {
        Write-Host ""
        Write-Host "This PC treats the network '$($connection.Name)' as Public, so other PCs are blocked." -ForegroundColor Yellow
        Write-Host "For your home network: Settings > Network & internet > (Wi-Fi or Ethernet) >" -ForegroundColor Yellow
        Write-Host "your network > Network profile type > Private." -ForegroundColor Yellow
    }
}

Assert-DockerReady

# Containers read their configuration at startup, so a running world is
# stopped before it is re-rendered. Characters live in the database volume.
$running = Get-NativeOutput { docker compose -f $ComposeFile -p $ProjectName ps -q }
if ($running) {
    Write-Host "Stopping the running world before restarting it..."
    Invoke-Compose down
}

& (Join-Path $PSScriptRoot "render-config.ps1") -HostIp $HostIp -LanCidr $LanCidr
$hostEnv = @{}
Get-Content -LiteralPath (Join-Path $RootDir "deploy\generated\host.env") | ForEach-Object {
    $pair = $_.Split("=", 2)
    if ($pair.Count -eq 2) { $hostEnv[$pair[0]] = $pair[1] }
}
$HostIp = $hostEnv["L2J_HOST_IP"]

foreach ($dir in @("deploy\login\log", "deploy\game\log", "deploy\game\data\geodata", "deploy\game\data\pathnode")) {
    New-Item -ItemType Directory -Force -Path (Join-Path $RootDir $dir) | Out-Null
}

Confirm-FirewallRule
Show-NetworkProfileWarning -Ip $HostIp

# Start the services one at a time so a failure names the service that
# caused it. The first start downloads the server images and imports the
# game database, which takes several minutes.
# Compile login, game and datapack from official/. Later starts reuse the
# cached layers and rebuild only what changed.
Write-Host ""
Write-Host "Building the server from source. The first build downloads its tools and takes several minutes."
Invoke-Compose build l2j-login

Write-Host ""
Write-Host "Starting the database. The first start imports game data."
Invoke-Compose up -d l2j-db
Wait-Healthy -Container $DbContainer -Label "the database" -TimeoutSeconds 1200
Invoke-Compose up -d l2j-login
Wait-Healthy -Container $LoginContainer -Label "the login server" -TimeoutSeconds 300
Invoke-Compose up -d l2j-game
Wait-Healthy -Container $GameContainer -Label "the game world to load" -TimeoutSeconds 1200
Invoke-Compose ps

Write-Host ""
Write-Host "Winterrealm3 is running." -ForegroundColor Green
Write-Host ""
Write-Host "Point each player's game client at:"
Write-Host "  Other PCs at home: $HostIp"
Write-Host "  This PC:           127.0.0.1"
Write-Host "  Ports:             2106 (login) and 7777 (game)"
Write-Host ""
Write-Host "Stop the world with stop-windows.cmd. Characters are kept."
