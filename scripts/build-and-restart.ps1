# The stack runs an image built from official/, so rebuilding and restarting
# is exactly what the start helper does. Kept for existing muscle memory.
$ErrorActionPreference = "Stop"

& (Join-Path $PSScriptRoot "windows\start.ps1") @args
