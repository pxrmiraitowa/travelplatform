#requires -Version 7.2
[CmdletBinding()]
param(
    [ValidateRange(1,60)][int]$VirtualUsers = 30,
    [ValidateRange(30,180)][int]$LoadSeconds = 90,
    [ValidateRange(0.01,1)][double]$SleepSeconds = 0.05,
    [ValidateRange(1024,65535)][int]$LocalPort = 8090,
    [string]$Namespace = 'travel-platform-bench-micro'
)
$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$context = 'kind-travel-platform'
$expectedNamespace = 'travel-platform-bench-micro'
if ($Namespace -ne $expectedNamespace) { throw 'Only the reviewed latest benchmark microservice namespace is allowed.' }
$namespace = $Namespace
$baseUrl = "http://127.0.0.1:$LocalPort"
<<<<<<< HEAD
$revision = (& git -C $root rev-parse HEAD)
=======
$protocolPath = Join-Path $root 'deploy/member-e-benchmark/protocol.json'
$protocol = Get-Content -Raw -LiteralPath $protocolPath | ConvertFrom-Json
$revision = [string]$protocol.sourceRevision
$repositoryHead = (& git -C $root rev-parse HEAD)
if ($LASTEXITCODE -ne 0 -or $revision -notmatch '^[a-f0-9]{40}$') { throw 'The reviewed application source revision is invalid.' }
& git -C $root diff --quiet $revision $repositoryHead -- travel-platform-microservices travel-platform-server travel-platform-web
if ($LASTEXITCODE -ne 0) { throw 'Application runtime inputs changed after the reviewed source revision; rebuild before this experiment.' }
& git -C $root diff --quiet HEAD -- travel-platform-microservices travel-platform-server travel-platform-web
if ($LASTEXITCODE -ne 0) { throw 'Application runtime inputs have uncommitted changes; refusing to mix them into HPA evidence.' }
$untrackedRuntimeInputs = @(& git -C $root ls-files --others --exclude-standard -- travel-platform-microservices travel-platform-server travel-platform-web)
if ($LASTEXITCODE -ne 0 -or $untrackedRuntimeInputs.Count -gt 0) { throw 'Application runtime inputs contain untracked files; refusing to mix them into HPA evidence.' }
>>>>>>> github/codex/microservices-ci-integration
Import-Module (Join-Path $root 'experiments/scripts/LabEvidence.psm1') -Force
if ((& kubectl config current-context) -ne $context) { throw 'The active kubectl context is not the reviewed local Kind cluster.' }
$namespaceRevision = (& kubectl --context $context get namespace $namespace -o 'jsonpath={.metadata.annotations.lab\.travelplatform/source-revision}')
if ($LASTEXITCODE -ne 0 -or $namespaceRevision -ne $revision) { throw 'The benchmark namespace does not match the checked-out source revision.' }
function Read-Resource {
    param([string]$Kind,[string]$Name)
    return ((Invoke-LabKube $context $namespace @('get',$Kind,$Name,'-o','json')) -join "`n") | ConvertFrom-Json
}
$hpa = Read-Resource hpa product-service
$deployment = Read-Resource deployment product-service
if ($hpa.metadata.annotations.'lab.travelplatform/source-revision' -ne $revision -or $deployment.metadata.annotations.'lab.travelplatform/source-revision' -ne $revision) { throw 'Live resource revision does not match this baseline.' }
if ($hpa.spec.minReplicas -ne 1 -or $hpa.spec.maxReplicas -ne 5 -or $hpa.spec.metrics[0].resource.target.averageUtilization -ne 60) { throw 'Unexpected HPA experiment configuration.' }
$directory = Join-Path $root "artifacts/member-e/$($revision.Substring(0,7))/hpa-$(Get-Date -Format 'yyyyMMdd-HHmmss')-$([guid]::NewGuid().ToString('N').Substring(0,6))"
New-Item -ItemType Directory -Path $directory -Force | Out-Null
$timeline = Join-Path $directory 'timeline.csv'
$report = [ordered]@{
<<<<<<< HEAD
    StartedAt=(Get-Date).ToString('o'); SourceRevision=$revision; Context=$context; Namespace=$namespace; BaseUrl=$baseUrl
=======
    StartedAt=(Get-Date).ToString('o'); SourceRevision=$revision; RepositoryHead=$repositoryHead; Context=$context; Namespace=$namespace; BaseUrl=$baseUrl
>>>>>>> github/codex/microservices-ci-integration
    HpaUid=$hpa.metadata.uid; DeploymentUid=$deployment.metadata.uid; Purpose='formal-hpa-verification'; Status='Running'
    VirtualUsers=$VirtualUsers; LoadSeconds=$LoadSeconds; SleepSeconds=$SleepSeconds
    TargetPath='/api/public/flights'; BaselineStableSeconds=30; CooldownTimeoutSeconds=360
    ReplicaCountsManuallyChanged=$false; CpuRequestsManuallyChanged=$false; Before=(Get-LabSnapshot $context $namespace)
    Samples=[Collections.Generic.List[object]]::new()
}
function Save-Report { $report | ConvertTo-Json -Depth 40 | Set-Content -LiteralPath (Join-Path $directory 'hpa-report.json') -Encoding utf8 }
function Sample-State {
    $currentHpa = Read-Resource hpa product-service
    $currentDeployment = Read-Resource deployment product-service
    if ($currentHpa.metadata.uid -ne $report.HpaUid -or $currentDeployment.metadata.uid -ne $report.DeploymentUid) { throw 'Observed HPA or Deployment was replaced during the experiment.' }
    $cpu = @($currentHpa.status.currentMetrics | Where-Object { $_.type -eq 'Resource' -and $_.resource.name -eq 'cpu' })
    $sample = [pscustomobject]@{
        Timestamp=(Get-Date).ToString('o'); Desired=[int]$currentDeployment.spec.replicas; Ready=[int]$currentDeployment.status.readyReplicas
        HpaCurrent=$currentHpa.status.currentReplicas; HpaDesired=$currentHpa.status.desiredReplicas
        CpuUtilization=$(if ($cpu.Count) {$cpu[0].resource.current.averageUtilization} else {$null})
    }
    $report.Samples.Add($sample)
    return $sample
}
$collector = Join-Path $root 'experiments/scripts/collect-k8s-metrics.ps1'
$job = $null
$forward = $null
Save-Report
try {
    $listener=[Net.NetworkInformation.IPGlobalProperties]::GetIPGlobalProperties().GetActiveTcpListeners() | Where-Object Port -eq $LocalPort
    if ($listener) { throw "Local port $LocalPort is already in use." }
    $forward=Start-Process -FilePath (Get-Command kubectl).Source -WindowStyle Hidden -PassThru `
        -RedirectStandardOutput (Join-Path $directory 'frontend-forward.log') `
        -RedirectStandardError (Join-Path $directory 'frontend-forward.err') `
        -ArgumentList @('--context',$context,'--namespace',$namespace,'port-forward','service/frontend',"${LocalPort}:80",'--address=127.0.0.1')
    $deadline=(Get-Date).AddSeconds(60)
    do {
        if ($forward.HasExited) { throw 'Frontend port-forward exited before the HPA experiment started.' }
        try {
            $health=Invoke-WebRequest -Uri "$baseUrl/api/public/health" -TimeoutSec 5
            $body=$health.Content | ConvertFrom-Json
            if ($health.StatusCode -eq 200 -and $body.code -eq 200) { break }
        } catch { if ((Get-Date) -ge $deadline) { throw } }
        if ((Get-Date) -ge $deadline) { throw 'Frontend did not become reachable for the HPA experiment.' }
        Start-Sleep -Seconds 1
    } while ($true)
    $job = Start-Job -ScriptBlock {
        param($Script,$Context,$Namespace,$File)
        & $Script -Context $Context -Namespace $Namespace -WorkloadName product-service -DurationSeconds 1200 -IntervalSeconds 5 -OutputFile $File
    } -ArgumentList $collector,$context,$namespace,$timeline
    Write-Host 'Waiting for 30 seconds of a measured, single-replica idle baseline.'
    $deadline = (Get-Date).AddSeconds(300)
    $idleSince = $null
    do {
        $sample = Sample-State
        $idle = $sample.Desired -eq 1 -and $sample.Ready -eq 1 -and $null -ne $sample.CpuUtilization -and $sample.CpuUtilization -lt 60
        if ($idle) { if (-not $idleSince) { $idleSince = Get-Date } } else { $idleSince = $null }
        Save-Report
        if ($idleSince -and ((Get-Date)-$idleSince).TotalSeconds -ge 30) { break }
        if ((Get-Date) -gt $deadline) { throw 'A stable idle baseline was not reached; load was not started.' }
        Start-Sleep -Seconds 5
    } while ($true)
    if (-not (Test-Path -LiteralPath $timeline) -or $job.State -ne 'Running') { throw 'Full experiment metric collector is not running.' }
    $report.LoadStartedAt = (Get-Date).ToString('o')
    Save-Report
    & (Join-Path $root 'experiments/scripts/run-performance.ps1') -BaseUrl $baseUrl -Variant microservices -Namespace $namespace -Context $context `
        -Runs 1 -VirtualUsers $VirtualUsers -Duration "${LoadSeconds}s" -WarmupDuration 0s -SleepSeconds $SleepSeconds `
        -Purpose rehearsal -ScaleMode hpa -BaselineId $revision -DatasetId "demo-$($revision.Substring(0,7))" -ResultDirectory (Join-Path $directory 'load')
    $report.LoadFinishedAt = (Get-Date).ToString('o')
    $metadataFiles = @(Get-ChildItem -Path (Join-Path $directory 'load') -Filter metadata.json -Recurse -File)
    if ($metadataFiles.Count -ne 1) { throw 'Expected exactly one isolated load session.' }
    $metadata = Get-Content -Raw $metadataFiles[0].FullName | ConvertFrom-Json
    if ($metadata.Status -ne 'Complete') { throw 'Load session did not complete; retain all evidence.' }
    $report.PerformanceSession = $metadataFiles[0].Directory.FullName
    $report.LoadThresholdsPassed = [bool]$metadata.Runs[0].ThresholdsPassed
    $deadline = (Get-Date).AddSeconds(360)
    $idleSince = $null
    Write-Host 'Load stopped. Observing natural scale-down without manually setting replicas.'
    do {
        $sample = Sample-State
        if ($sample.Desired -eq 1 -and $sample.Ready -eq 1 -and $sample.HpaDesired -eq 1) {
            if (-not $idleSince) { $idleSince=Get-Date }
            if (((Get-Date)-$idleSince).TotalSeconds -ge 30) { break }
        } else { $idleSince = $null }
        Save-Report
        if ((Get-Date) -gt $deadline) { throw 'Natural cooldown did not reach a stable single replica before the deadline.' }
        Start-Sleep -Seconds 5
    } while ($true)
    $report.SingleReplicaRestoredAt = (Get-Date).ToString('o')
    if ($job.State -ne 'Running') { throw 'The full experiment collector stopped early; do not claim complete cooldown evidence.' }
    $rows = @(Import-Csv -LiteralPath $timeline | Where-Object Workload -eq product-service)
    $latestSample = $rows | Select-Object -Last 1
    if (-not $latestSample -or ((Get-Date).ToUniversalTime() - ([datetimeoffset]$latestSample.Timestamp).UtcDateTime).TotalSeconds -gt 20) { throw 'Cooldown metric evidence is stale or missing.' }
    $during = @($rows | Where-Object { [datetimeoffset]$_.Timestamp -ge [datetimeoffset]$metadata.Runs[0].StartedAt -and [datetimeoffset]$_.Timestamp -le [datetimeoffset]$metadata.Runs[0].FinishedAt })
    if (-not $during.Count) { throw 'No product-service samples were captured during the load run.' }
    $report.MaxDesiredReplicasDuringLoad = [int](($during | Measure-Object ObservedDesiredReplicas -Maximum).Maximum)
    $report.MaxReadyReplicasDuringLoad = [int](($during | Measure-Object ObservedReadyReplicas -Maximum).Maximum)
    $report.ScaleUpObservedDuringLoad = ($report.MaxDesiredReplicasDuringLoad -gt 1 -and $report.MaxReadyReplicasDuringLoad -gt 1)
    $report.After = Get-LabSnapshot $context $namespace
    $report.ConfigurationUnchanged = ((Get-LabConfigFingerprint $report.Before hpa) -eq (Get-LabConfigFingerprint $report.After hpa))
    if (-not $report.ConfigurationUnchanged) { throw 'Experiment configuration changed during observation.' }
    Invoke-LabKube $context $namespace @('get','events','--field-selector=involvedObject.kind=HorizontalPodAutoscaler','-o','json') | Set-Content -LiteralPath (Join-Path $directory 'hpa-events.json') -Encoding utf8
    $report.Status = if ($report.ScaleUpObservedDuringLoad) {'Verified'} else {'ScaleUpNotObserved'}
} catch {
    $report.Status = 'Failed'
    $report.Error = $_.Exception.Message
    throw
} finally {
    if ($job) {
        Stop-Job -Job $job
        Receive-Job -Job $job -ErrorAction Continue | Out-Host
        Remove-Job -Job $job
    }
    if ($forward -and -not $forward.HasExited) { $forward.Kill(); $forward.WaitForExit(5000) | Out-Null }
    if (Test-Path -LiteralPath $timeline) { $report.TimelineSha256=(Get-FileHash -LiteralPath $timeline -Algorithm SHA256).Hash }
    $report.FinishedAt = (Get-Date).ToString('o')
    Save-Report
    Write-Host "HPA observation: $($report.Status). Evidence: $directory"
}
