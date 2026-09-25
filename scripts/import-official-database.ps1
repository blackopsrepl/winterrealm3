param(
    [string]$RootDir = "$PSScriptRoot\..\official",
    [string]$Container = "winterrealm3-db",
    [string]$RootPassword = "l2jroot"
)

$ErrorActionPreference = "Stop"
$root = Resolve-Path -LiteralPath $RootDir
$datapackSql = Join-Path $root "l2j-server-datapack\src\main\resources\sql"
$loginSql = Join-Path $root "l2j-server-login\src\main\resources\sql"

docker exec $Container sh -lc "mysqladmin ping -uroot -p$RootPassword --silent"
docker exec $Container sh -lc "mysql -uroot -p$RootPassword -e 'CREATE DATABASE IF NOT EXISTS l2jls DEFAULT CHARACTER SET utf8;'"
if ($LASTEXITCODE -ne 0) { throw "Failed to create l2jls database." }
docker exec $Container sh -lc "mysql -uroot -p$RootPassword -e 'CREATE DATABASE IF NOT EXISTS l2jdb DEFAULT CHARACTER SET utf8;'"
if ($LASTEXITCODE -ne 0) { throw "Failed to create l2jdb database." }
docker exec $Container sh -lc "mysql -uroot -p$RootPassword -e `"CREATE USER IF NOT EXISTS 'l2j'@'%' IDENTIFIED BY 'l2jserver2019'; GRANT ALL PRIVILEGES ON l2jls.* TO 'l2j'@'%'; GRANT ALL PRIVILEGES ON l2jdb.* TO 'l2j'@'%'; FLUSH PRIVILEGES;`""
if ($LASTEXITCODE -ne 0) { throw "Failed to grant l2j database permissions." }

if (Test-Path $loginSql) {
    Get-ChildItem -LiteralPath $loginSql -Filter "*.sql" | Sort-Object Name | ForEach-Object {
        Write-Host "Importing login $($_.Name)"
        docker cp $_.FullName "${Container}:/tmp/$($_.Name)"
        docker exec $Container sh -lc "mysql -uroot -p$RootPassword l2jls < /tmp/$($_.Name)"
        if ($LASTEXITCODE -ne 0) { throw "Failed to import login $($_.Name)." }
    }
}

Get-ChildItem -LiteralPath $datapackSql -Filter "*.sql" | Sort-Object Name | ForEach-Object {
    Write-Host "Importing game $($_.Name)"
    docker cp $_.FullName "${Container}:/tmp/$($_.Name)"
    docker exec $Container sh -lc "mysql -uroot -p$RootPassword l2jdb < /tmp/$($_.Name)"
    if ($LASTEXITCODE -ne 0) { throw "Failed to import game $($_.Name)." }
}

Write-Host "Database import complete."
