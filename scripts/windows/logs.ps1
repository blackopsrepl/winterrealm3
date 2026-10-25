# Follows the server logs. Pass a service name (l2j-db, l2j-login, l2j-game)
# to narrow the output; press Ctrl+C to stop following.
$ErrorActionPreference = "Stop"
. (Join-Path $PSScriptRoot "common.ps1")

Assert-DockerReady
Invoke-Compose logs -f --tail 200 @args
