param(
    [string]$MysqlUser = "root",
    [string]$MysqlPassword = $env:MYSQL_PASSWORD,
    [string]$MysqlHost = "127.0.0.1",
    [string]$Scale = "medium"
)

$ErrorActionPreference = "Stop"
$Dir = Split-Path -Parent $MyInvocation.MyCommand.Path

function Invoke-MysqlFile {
    param(
        [string]$File,
        [string]$Database = ""
    )
    $dbArg = if ($Database) { " $Database" } else { "" }
    $cmd = "mysql -h $MysqlHost -u $MysqlUser -p$MysqlPassword --default-character-set=utf8mb4$dbArg < `"$File`""
    cmd /c $cmd
    if ($LASTEXITCODE -ne 0) { throw "mysql failed: $File" }
}

Write-Host "==> [1/4] Create database chatbi_bench"
Invoke-MysqlFile -File "$Dir\00-init.sql"

Write-Host "==> [2/4] Apply schema (32 tables)"
Invoke-MysqlFile -File "$Dir\01-schema.sql" -Database "chatbi_bench"

Write-Host "==> [3/4] Seed dimensions"
Invoke-MysqlFile -File "$Dir\02-seed-dimensions.sql" -Database "chatbi_bench"

Write-Host "==> [4/4] Generate & import facts (scale=$Scale)"
python "$Dir\generate_facts.py" --scale $Scale
Invoke-MysqlFile -File "$Dir\03-seed-facts.sql" -Database "chatbi_bench"

Write-Host ""
Write-Host "Done! Database: chatbi_bench"
Write-Host "Connect in ChatBI: host=$MysqlHost, db=chatbi_bench, user=$MysqlUser"
