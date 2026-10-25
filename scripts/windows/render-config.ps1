# Renders the host-specific server configuration into deploy\generated.
# Mirrors scripts/linux/render-config.sh.
param(
    [string]$HostIp = $env:L2J_HOST_IP,
    [string]$LanCidr = $env:L2J_LAN_CIDR
)

$ErrorActionPreference = "Stop"
. (Join-Path $PSScriptRoot "common.ps1")

$templateDir = Join-Path $RootDir "deploy\templates"
$generatedDir = Join-Path $RootDir "deploy\generated"

function Get-NetworkPrefix {
    param([string]$Ip, [int]$PrefixLength)
    # Byte-wise masking avoids Windows PowerShell's signed 32-bit hex literals.
    $bytes = ([System.Net.IPAddress]::Parse($Ip)).GetAddressBytes()
    $octets = @()
    for ($i = 0; $i -lt 4; $i++) {
        $bits = [Math]::Max(0, [Math]::Min(8, $PrefixLength - (8 * $i)))
        $mask = (255 -shl (8 - $bits)) -band 255
        $octets += ([int]$bytes[$i] -band $mask)
    }
    return "{0}.{1}.{2}.{3}/{4}" -f $octets[0], $octets[1], $octets[2], $octets[3], $PrefixLength
}

if (-not $HostIp -or -not $LanCidr) {
    # The adapter with a default gateway is the one on the home network;
    # Docker and WSL virtual adapters have none.
    $config = Get-NetIPConfiguration |
        Where-Object { $_.IPv4DefaultGateway -and $_.NetAdapter.Status -eq "Up" } |
        Select-Object -First 1
    if ($config) {
        $address = $config.IPv4Address | Select-Object -First 1
        if (-not $HostIp) { $HostIp = $address.IPAddress }
        if (-not $LanCidr) { $LanCidr = Get-NetworkPrefix -Ip $address.IPAddress -PrefixLength $address.PrefixLength }
    }
}

if (-not $HostIp -or -not $LanCidr) {
    Write-Host "Could not detect this computer's home-network address." -ForegroundColor Red
    Write-Host "Find it with 'ipconfig' (IPv4 Address), then start with, for example:"
    Write-Host "  start-windows.cmd -HostIp 192.168.1.50 -LanCidr 192.168.1.0/24"
    throw "Home-network address not found."
}

$cidrParts = $LanCidr.Split("/")
if ($cidrParts.Count -eq 2) {
    $LanCidr = Get-NetworkPrefix -Ip $cidrParts[0] -PrefixLength ([int]$cidrParts[1])
}

# Keep the game server's registration key across restarts.
$hexid = Join-Path $generatedDir "game\config\hexid.txt"
$savedHexid = $null
if (Test-Path -LiteralPath $hexid) { $savedHexid = [System.IO.File]::ReadAllBytes($hexid) }

if (Test-Path -LiteralPath $generatedDir) { Remove-Item -LiteralPath $generatedDir -Recurse -Force }
New-Item -ItemType Directory -Force -Path (Join-Path $generatedDir "login") | Out-Null
New-Item -ItemType Directory -Force -Path (Join-Path $generatedDir "game") | Out-Null
Copy-Item -LiteralPath (Join-Path $RootDir "deploy\login\config") -Destination (Join-Path $generatedDir "login\config") -Recurse
Copy-Item -LiteralPath (Join-Path $RootDir "deploy\game\config") -Destination (Join-Path $generatedDir "game\config") -Recurse
foreach ($stale in @("game\config\hexid.txt", "game\config\ipconfig.xml")) {
    $path = Join-Path $generatedDir $stale
    if (Test-Path -LiteralPath $path) { Remove-Item -LiteralPath $path -Force }
}
if ($savedHexid) { [System.IO.File]::WriteAllBytes($hexid, $savedHexid) }

# Overlay each template, filled in. Write UTF-8 without a byte-order mark:
# Java property files would read one as part of the first key.
$utf8 = New-Object System.Text.UTF8Encoding $false
$templateRoot = (Resolve-Path -LiteralPath $templateDir).Path
Get-ChildItem -LiteralPath $templateRoot -Recurse -File | ForEach-Object {
    $relative = $_.FullName.Substring($templateRoot.Length).TrimStart("\", "/")
    $target = Join-Path $generatedDir $relative
    New-Item -ItemType Directory -Force -Path (Split-Path -Parent $target) | Out-Null
    $text = [System.IO.File]::ReadAllText($_.FullName)
    $text = $text.Replace("__L2J_HOST_IP__", $HostIp).Replace("__L2J_LAN_CIDR__", $LanCidr)
    [System.IO.File]::WriteAllText($target, $text, $utf8)
}

$hostEnv = "L2J_HOST_IP=$HostIp`nL2J_LAN_CIDR=$LanCidr`nL2J_BIND_IP=0.0.0.0`n"
[System.IO.File]::WriteAllText((Join-Path $generatedDir "host.env"), $hostEnv, $utf8)

Write-Host "Prepared Winterrealm3 configuration:"
Write-Host "  Host IP:  $HostIp"
Write-Host "  LAN CIDR: $LanCidr"
