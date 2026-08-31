#requires -Version 7.2
[CmdletBinding()]
param()
$ErrorActionPreference='Stop'
$root=(Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$protocol=Get-Content -Raw (Join-Path $root 'deploy/member-e-benchmark/protocol.json') | ConvertFrom-Json -AsHashtable
$revision=(& git -C $root rev-parse HEAD)
if ($revision -ne $protocol.sourceRevision) { throw 'Source revision does not match the reviewed protocol.' }
& git -C $root diff --quiet HEAD -- travel-platform-microservices travel-platform-web
if ($LASTEXITCODE -ne 0) { throw 'Microservice or frontend source has local changes.' }
$extra=@(& git -C $root ls-files --others --exclude-standard -- travel-platform-microservices travel-platform-web)
if ($LASTEXITCODE -ne 0 -or $extra.Count) { throw 'Unexpected untracked microservice or frontend source.' }
$shortRevision=$revision.Substring(0,7)
$id=[guid]::NewGuid().ToString('N').Substring(0,8)
$directory=Join-Path $root "artifacts/member-e/$shortRevision/microservices-build-$(Get-Date -Format yyyyMMdd-HHmmss)-$id"
New-Item -ItemType Directory -Path $directory -Force | Out-Null
$context=Join-Path $directory 'image-context'
$record=[ordered]@{SourceRevision=$revision;StartedAt=(Get-Date).ToString('o');Status='Running';TestsSkipped=$false;ProductionSourceModified=$false;Directory=$directory;Images=@()}
try {
    $source=Join-Path $root 'travel-platform-microservices'
    & docker run --rm --pull=never --volume "${source}:/workspace" --volume travel-e-maven-cache:/root/.m2 --workdir /workspace `
        maven:3.9-eclipse-temurin-17 mvn -B clean package 2>&1 | Tee-Object -FilePath (Join-Path $directory 'maven-package.log') | Out-Host
    $record.MavenExitCode=$LASTEXITCODE
    $record.TestReports=@(Get-ChildItem -LiteralPath $source -Recurse -Filter 'TEST-*.xml' | Where-Object {$_.FullName -match '[\\/]target[\\/]surefire-reports[\\/]'} | ForEach-Object {
        [xml]$xml=Get-Content -Raw $_.FullName
        [ordered]@{Module=$_.Directory.Parent.Parent.Name;Name=$xml.testsuite.name;Tests=[int]$xml.testsuite.tests;Failures=[int]$xml.testsuite.failures;Errors=[int]$xml.testsuite.errors;Skipped=[int]$xml.testsuite.skipped}
    })
    if ($record.MavenExitCode -ne 0 -or -not $record.TestReports.Count -or @($record.TestReports | Where-Object {$_.Failures -gt 0 -or $_.Errors -gt 0 -or $_.Skipped -gt 0}).Count) { throw 'Microservice tests/build failed. Do not package by skipping tests.' }
    New-Item -ItemType Directory -Path $context | Out-Null
    Copy-Item -LiteralPath (Join-Path $root 'deploy/member-e-benchmark/microservice.Dockerfile') -Destination (Join-Path $context 'Dockerfile')
    foreach ($service in @('user-service','product-service','order-service','content-trip-service','gateway-service')) {
        $jars=@(Get-ChildItem -LiteralPath (Join-Path $source "$service/target") -Filter '*.jar' | Where-Object {$_.Name -notlike '*.original'})
        if ($jars.Count -ne 1) { throw "Expected exactly one packaged JAR for $service." }
        $jarHash=(Get-FileHash -LiteralPath $jars[0].FullName -Algorithm SHA256).Hash.ToLowerInvariant()
        $reference="travel-platform-benchmark-${service}:$shortRevision-$($jarHash.Substring(0,12))"
        & docker image inspect $reference *> $null
        if ($LASTEXITCODE -eq 0) { throw "Refusing to overwrite existing content tag $reference." }
        Copy-Item -LiteralPath $jars[0].FullName -Destination (Join-Path $context 'app.jar') -Force
        & docker build --pull=false --network=none --build-arg "SOURCE_REVISION=$revision" --build-arg "SERVICE_NAME=$service" --tag $reference $context 2>&1 | Tee-Object -FilePath (Join-Path $directory "$service-image-build.log") | Out-Host
        if ($LASTEXITCODE -ne 0) { throw "Image build failed for $service." }
        $record.Images+=@{Component=$service;Image=$reference;ImageId=(& docker image inspect $reference --format '{{.Id}}');JarSha256=$jarHash}
    }
    $web=Join-Path $root 'travel-platform-web'
    & docker run --rm --pull=never --volume "${web}:/workspace" --volume travel-e-node-modules:/workspace/node_modules --volume travel-e-npm-cache:/root/.npm --workdir /workspace `
        node:22-alpine sh -c 'npm ci && npm run test:unit && npm run build' 2>&1 | Tee-Object -FilePath (Join-Path $directory 'frontend-test-build.log') | Out-Host
    if ($LASTEXITCODE -ne 0) { throw 'Frontend install, unit tests or build failed.' }
    $treeHash=(& git -C $root rev-parse 'HEAD:travel-platform-web')
    $frontReference="travel-platform-benchmark-frontend:$shortRevision-$($treeHash.Substring(0,12))"
    & docker image inspect $frontReference *> $null
    if ($LASTEXITCODE -eq 0) { throw "Refusing to overwrite existing content tag $frontReference." }
    & docker build --pull=false --tag $frontReference $web 2>&1 | Tee-Object -FilePath (Join-Path $directory 'frontend-image-build.log') | Out-Host
    if ($LASTEXITCODE -ne 0) { throw 'Frontend image build failed.' }
    $record.Images+=@{Component='frontend';Image=$frontReference;ImageId=(& docker image inspect $frontReference --format '{{.Id}}');SourceTree=$treeHash}
    $record.Status='BuiltAndTested'
} catch {
    $record.Status='Failed';$record.Error=$_.Exception.Message
    throw
} finally {
    if (Test-Path -LiteralPath $context) {
        $resolvedContext=(Resolve-Path -LiteralPath $context).Path
        $resolvedDirectory=(Resolve-Path -LiteralPath $directory).Path
        if (-not $resolvedContext.StartsWith($resolvedDirectory+[IO.Path]::DirectorySeparatorChar)) { throw 'Refusing to remove image context outside the build artifact directory.' }
        Remove-Item -LiteralPath $resolvedContext -Recurse -Force
    }
    $record.FinishedAt=(Get-Date).ToString('o')
    $record | ConvertTo-Json -Depth 15 | Set-Content -LiteralPath (Join-Path $directory 'build.json') -Encoding utf8
    Write-Host "Microservice build evidence: $directory"
}
