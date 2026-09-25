param(
    [string]$RootDir = "$PSScriptRoot\..\official",
    [string]$JavaHome = "C:\Program Files\Eclipse Adoptium\jdk-25.0.3.9-hotspot"
)

$ErrorActionPreference = "Stop"
$root = Resolve-Path -LiteralPath $RootDir
$jdk = Resolve-Path -LiteralPath $JavaHome
$env:JAVA_HOME = $jdk.Path
$env:Path = "$($jdk.Path)\bin;$env:Path"

foreach ($module in @("l2j-server-login", "l2j-server-game", "l2j-server-datapack")) {
    Write-Host "Building $module with Java at $env:JAVA_HOME"
    Push-Location (Join-Path $root $module)
    try {
        .\mvnw.cmd install
    } finally {
        Pop-Location
    }
}

Write-Host "Build artifacts:"
Get-ChildItem -LiteralPath $root -Recurse -Filter "*.zip" |
    Where-Object { $_.FullName -like "*\target\*" } |
    Select-Object -ExpandProperty FullName
