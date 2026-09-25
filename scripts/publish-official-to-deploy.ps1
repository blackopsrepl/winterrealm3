param(
    [string]$RootDir = "$PSScriptRoot\..\official",
    [string]$DeployDir = "$PSScriptRoot\..\deploy",
    [switch]$SkipBuild
)

$ErrorActionPreference = "Stop"

$root = Resolve-Path -LiteralPath $RootDir
$deploy = Resolve-Path -LiteralPath $DeployDir
$repoRoot = Resolve-Path -LiteralPath (Join-Path $PSScriptRoot "..")

if (-not $SkipBuild) {
    powershell -NoProfile -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot "build-official.ps1") -RootDir $root.Path
    if ($LASTEXITCODE -ne 0) { throw "Official build failed." }
}

$loginZip = Get-ChildItem -LiteralPath (Join-Path $root "l2j-server-login\target") -Filter "l2j-server-login-*.zip" |
    Sort-Object LastWriteTime -Descending |
    Select-Object -First 1
$gameZip = Get-ChildItem -LiteralPath (Join-Path $root "l2j-server-game\target") -Filter "l2j-server-game-*.zip" |
    Sort-Object LastWriteTime -Descending |
    Select-Object -First 1
$datapackZip = Get-ChildItem -LiteralPath (Join-Path $root "l2j-server-datapack\target") -Filter "l2j-server-datapack-*.zip" |
    Sort-Object LastWriteTime -Descending |
    Select-Object -First 1

if (-not $loginZip) { throw "Login server zip was not found. Build official/l2j-server-login first." }
if (-not $gameZip) { throw "Game server zip was not found. Build official/l2j-server-game first." }
if (-not $datapackZip) { throw "Datapack zip was not found. Build official/l2j-server-datapack first." }

$tempRoot = Join-Path $env:TEMP ("l2j-publish-" + [guid]::NewGuid().ToString("N"))
New-Item -ItemType Directory -Force -Path $tempRoot | Out-Null

function Save-IfExists {
    param([string]$Path)
    if (Test-Path -LiteralPath $Path) {
        $relative = (Resolve-Path -LiteralPath $Path).Path.Substring($repoRoot.Path.Length).TrimStart("\")
        $target = Join-Path $tempRoot ("preserve\" + $relative)
        New-Item -ItemType Directory -Force -Path (Split-Path -Parent $target) | Out-Null
        Copy-Item -LiteralPath $Path -Destination $target -Force
    }
}

function Restore-IfSaved {
    param([string]$Path)
    $relative = $Path.Substring($repoRoot.Path.Length).TrimStart("\")
    $saved = Join-Path $tempRoot ("preserve\" + $relative)
    if (Test-Path -LiteralPath $saved) {
        New-Item -ItemType Directory -Force -Path (Split-Path -Parent $Path) | Out-Null
        Copy-Item -LiteralPath $saved -Destination $Path -Force
    }
}

function Clear-GeneratedDeployDir {
    param([string]$Path)
    if (Test-Path -LiteralPath $Path) {
        Get-ChildItem -LiteralPath $Path -Force |
            Where-Object { $_.Name -ne "log" } |
            Remove-Item -Recurse -Force
    } else {
        New-Item -ItemType Directory -Force -Path $Path | Out-Null
    }
}

try {
    Save-IfExists (Join-Path $deploy "login\config\database.properties")
    Save-IfExists (Join-Path $deploy "game\config\server.properties")
    Save-IfExists (Join-Path $deploy "game\config\ipconfig.xml")
    Save-IfExists (Join-Path $deploy "game\config\hexid.txt")

    $deployLogin = Join-Path $deploy "login"
    $deployGame = Join-Path $deploy "game"
    Clear-GeneratedDeployDir $deployLogin
    Clear-GeneratedDeployDir $deployGame

    tar -xf $loginZip.FullName -C $deployLogin
    if ($LASTEXITCODE -ne 0) { throw "Failed to extract $($loginZip.FullName)." }
    tar -xf $gameZip.FullName -C $deployGame
    if ($LASTEXITCODE -ne 0) { throw "Failed to extract $($gameZip.FullName)." }
    tar -xf $datapackZip.FullName -C $deployGame
    if ($LASTEXITCODE -ne 0) { throw "Failed to extract $($datapackZip.FullName)." }

    Restore-IfSaved (Join-Path $deploy "login\config\database.properties")
    Restore-IfSaved (Join-Path $deploy "game\config\server.properties")
    Restore-IfSaved (Join-Path $deploy "game\config\ipconfig.xml")
    Restore-IfSaved (Join-Path $deploy "game\config\hexid.txt")
} finally {
    if (Test-Path -LiteralPath $tempRoot) {
        Remove-Item -LiteralPath $tempRoot -Recurse -Force
    }
}

Write-Host "Published official build outputs to deploy/."
