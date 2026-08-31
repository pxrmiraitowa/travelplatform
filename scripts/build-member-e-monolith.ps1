#requires -Version 7.2
[CmdletBinding()]
param()
$ErrorActionPreference='Stop'
$root=(Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$revision=(& git -C $root rev-parse HEAD)
$protocol=Get-Content -Raw (Join-Path $root 'deploy/member-e-benchmark/protocol.json') | ConvertFrom-Json -AsHashtable
if ($revision -ne $protocol.sourceRevision) { throw 'Review the pinned benchmark runtime before building a different source revision.' }
& git -C $root diff --quiet HEAD -- travel-platform-server
if ($LASTEXITCODE -ne 0) { throw 'Monolith source and tests must match the pinned source revision.' }
$fixturePath=Join-Path $root 'travel-platform-server/src/test/resources/sql/data-test.sql'
$extra=@(& git -C $root ls-files --others --exclude-standard -- travel-platform-server)
if ($LASTEXITCODE -ne 0 -or $extra.Count) { throw 'Unexpected untracked monolith source files.' }
$id=[guid]::NewGuid().ToString('N').Substring(0,8)
$shortRevision=$revision.Substring(0,7)
$directory=Join-Path $root "artifacts/member-e/$shortRevision/monolith-build-$(Get-Date -Format yyyyMMdd-HHmmss)-$id"
New-Item -ItemType Directory -Path $directory -Force | Out-Null
$network="member-e-build-$id"
$database="member-e-unit-db-$id"
$createdNetwork=$false; $databaseId=$null
$oldEnv=@{}
foreach ($name in @('MYSQL_ROOT_PASSWORD','SPRING_DATASOURCE_URL','SPRING_DATASOURCE_USERNAME','SPRING_DATASOURCE_PASSWORD')) { $oldEnv[$name]=[Environment]::GetEnvironmentVariable($name,'Process') }
$record=[ordered]@{SourceRevision=$revision;StartedAt=(Get-Date).ToString('o');Status='Running';Directory=$directory;TestsSkipped=$false;TemporaryDatabase=$database;ProductionDatabaseTouched=$false;ProductionSourceModified=$false;TestFixturePatch='None; the current test fixture is committed in the pinned source revision';TestFixtureSha256=(Get-FileHash -LiteralPath $fixturePath).Hash}
try {
    docker network create --label "lab.travelplatform.build=$id" $network | Out-Host
    if ($LASTEXITCODE -ne 0) { throw 'Cannot create isolated test network.' }
    $createdNetwork=$true
    $env:MYSQL_ROOT_PASSWORD=[Convert]::ToHexString([Security.Cryptography.RandomNumberGenerator]::GetBytes(32))
    $env:SPRING_DATASOURCE_PASSWORD=$env:MYSQL_ROOT_PASSWORD
    $env:SPRING_DATASOURCE_USERNAME='root'
    $env:SPRING_DATASOURCE_URL="jdbc:mysql://${database}:3306/travel_platform_it?createDatabaseIfNotExist=true&useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false"
    $databaseId=(& docker run -d --rm --pull=never --name $database --network $network --label "lab.travelplatform.build=$id" --env MYSQL_ROOT_PASSWORD mysql:8.4-kind-amd64)
    if ($LASTEXITCODE -ne 0 -or $databaseId -notmatch '^[0-9a-f]{64}$') { throw 'Cannot start isolated MySQL.' }
    $deadline=(Get-Date).AddSeconds(120)
    do {
        & docker exec $databaseId sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysqladmin ping -h 127.0.0.1 -uroot --silent' *> $null
        if ($LASTEXITCODE -eq 0) { break }
        if ((Get-Date) -gt $deadline) { throw 'Temporary MySQL did not become ready.' }
        Start-Sleep -Seconds 2
    } while ($true)
    $source=Join-Path $root 'travel-platform-server'
    & docker run --rm --pull=never --network $network --volume "${source}:/workspace" --volume travel-e-maven-cache:/root/.m2 --workdir /workspace `
        --env SPRING_DATASOURCE_URL --env SPRING_DATASOURCE_USERNAME --env SPRING_DATASOURCE_PASSWORD maven:3.9-eclipse-temurin-17 mvn -B clean package 2>&1 |
        Tee-Object -FilePath (Join-Path $directory 'maven-package.log') | Out-Host
    $record.MavenExitCode=$LASTEXITCODE
    # Preserve counts for failed runs too, without copying MockMvc credentials or
    # response bodies into the shareable build record. clean prevents stale XML.
    $reports=Join-Path $source 'target/surefire-reports'
    $record.TestReports=@(if (Test-Path -LiteralPath $reports) {
        Get-ChildItem -LiteralPath $reports -Filter 'TEST-*.xml' | ForEach-Object {
            [xml]$xml=Get-Content -Raw $_.FullName
            [ordered]@{Name=$xml.testsuite.name;Tests=[int]$xml.testsuite.tests;Failures=[int]$xml.testsuite.failures;Errors=[int]$xml.testsuite.errors;Skipped=[int]$xml.testsuite.skipped}
        }
    })
    if ($record.MavenExitCode -ne 0 -or -not $record.TestReports.Count -or @($record.TestReports | Where-Object {$_.Failures -gt 0 -or $_.Errors -gt 0 -or $_.Skipped -gt 0}).Count) { throw 'Monolith tests/build failed. Do not package by skipping tests.' }
    $jar=Join-Path $source 'target/travel-platform-server-0.0.1-SNAPSHOT.jar'
    $hash=(Get-FileHash -LiteralPath $jar -Algorithm SHA256).Hash.ToLowerInvariant()
    $reference="travel-platform-benchmark-monolith:$shortRevision-$($hash.Substring(0,12))"
    & docker image inspect $reference *> $null
    if ($LASTEXITCODE -eq 0) { throw 'That content-tagged benchmark image already exists; inspect it instead of overwriting.' }
    $context=Join-Path $directory 'image-context'
    New-Item -ItemType Directory -Path $context | Out-Null
    Copy-Item -LiteralPath $jar -Destination (Join-Path $context 'app.jar')
    Copy-Item -LiteralPath (Join-Path $root 'deploy/member-e-benchmark/monolith.Dockerfile') -Destination (Join-Path $context 'Dockerfile')
    & docker build --pull=false --network=none --build-arg "SOURCE_REVISION=$revision" --tag $reference $context 2>&1 | Tee-Object -FilePath (Join-Path $directory 'image-build.log') | Out-Host
    if ($LASTEXITCODE -ne 0) { throw 'Monolith packaging failed.' }
    $record.Image=$reference
    $record.ImageId=(& docker image inspect $reference --format '{{.Id}}')
    if ($LASTEXITCODE -ne 0) { throw 'Cannot record built image identity.' }
    $record.JarSha256=$hash
    $record.RuntimeImage='ghcr.io/pxrmiraitowa/travel-platform-product-service@sha256:97a44668096f1da4092731807d08cda81030630b5377f03ae8de253b72f46c27'
    $record.Status='BuiltAndTested'
} catch {
    $record.Status='Failed'; $record.Error=$_.Exception.Message
    throw
} finally {
    # Stop only the exact ephemeral container created by this invocation. --rm also
    # removes its anonymous test-data volume. No pre-existing database is addressed.
    if ($databaseId) {
        $label=(& docker inspect $databaseId --format '{{ index .Config.Labels "lab.travelplatform.build" }}' 2>$null)
        if ($LASTEXITCODE -eq 0 -and $label -eq $id) { & docker stop --timeout 15 $databaseId | Out-Host }
    }
    if ($createdNetwork) { & docker network rm $network | Out-Host }
    foreach ($name in $oldEnv.Keys) { [Environment]::SetEnvironmentVariable($name,$oldEnv[$name],'Process') }
    $record.FinishedAt=(Get-Date).ToString('o')
    $record | ConvertTo-Json -Depth 12 | Set-Content -LiteralPath (Join-Path $directory 'build.json') -Encoding utf8
    Write-Host "Monolith build evidence: $directory"
}
