param(
    [string]$RootDir = "$PSScriptRoot\..\official",
    [string]$JavaHome = ""
)

$ErrorActionPreference = "Stop"
$root = Resolve-Path -LiteralPath $RootDir

# Use an explicit -JavaHome, then JAVA_HOME, then the newest Temurin 25 JDK
# installed in its default location.
if (-not $JavaHome) { $JavaHome = $env:JAVA_HOME }
if (-not $JavaHome) {
    $temurin = Get-ChildItem -Path "$env:ProgramFiles\Eclipse Adoptium" -Directory -Filter "jdk-25*" -ErrorAction SilentlyContinue |
        Sort-Object Name -Descending | Select-Object -First 1
    if ($temurin) { $JavaHome = $temurin.FullName }
}
if (-not $JavaHome -or -not (Test-Path -LiteralPath (Join-Path $JavaHome "bin\java.exe"))) {
    throw "Java 25 JDK not found. Install it with 'winget install EclipseAdoptium.Temurin.25.JDK', open a new PowerShell window, and run this script again."
}

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
