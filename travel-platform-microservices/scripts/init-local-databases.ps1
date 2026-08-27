param(
    [string]$MysqlUrl = "jdbc:mysql://localhost:3306/?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false",
    [string]$MysqlUsername = "root",
    [string]$MysqlPassword = "123456",
    [string]$JavaHome = "C:\Program Files\Java\jdk-24",
    [string]$JdbcJar = "$env:USERPROFILE\.m2\repository\com\mysql\mysql-connector-j\8.3.0\mysql-connector-j-8.3.0.jar"
)

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
$RepoRoot = Split-Path -Parent $Root
$Runner = Join-Path $Root "tools\SqlRunner.java"
$BuildDir = Join-Path $Root "build\sql-runner"
$SchemaSql = Join-Path $RepoRoot "travel-platform-server\src\main\resources\sql\schema.sql"
$DataSql = Join-Path $RepoRoot "travel-platform-server\src\main\resources\sql\data-demo.sql"
$Java = Join-Path $JavaHome "bin\java.exe"
$Javac = Join-Path $JavaHome "bin\javac.exe"

if (-not (Test-Path $JdbcJar)) {
    throw "MySQL JDBC driver not found: $JdbcJar. Run Maven once to download dependencies."
}
if (-not (Test-Path $SchemaSql)) {
    throw "Schema SQL not found: $SchemaSql"
}
if (-not (Test-Path $DataSql)) {
    throw "Demo data SQL not found: $DataSql"
}

New-Item -ItemType Directory -Force -Path $BuildDir | Out-Null
$LocalJdbcJar = Join-Path $BuildDir (Split-Path $JdbcJar -Leaf)
Copy-Item -LiteralPath $JdbcJar -Destination $LocalJdbcJar -Force
$CompileErr = Join-Path $BuildDir "javac.err.log"
$PreviousErrorActionPreference = $ErrorActionPreference
$ErrorActionPreference = "Continue"
& $Javac -encoding UTF-8 -cp $LocalJdbcJar -d $BuildDir $Runner 2> $CompileErr
$CompileExitCode = $LASTEXITCODE
$ErrorActionPreference = $PreviousErrorActionPreference
if ($CompileExitCode -ne 0) {
    Get-Content -Path $CompileErr
    throw "Failed to compile SQL runner."
}
& $Java -cp "$BuildDir;$LocalJdbcJar" SqlRunner $MysqlUrl $MysqlUsername $MysqlPassword $SchemaSql $DataSql
