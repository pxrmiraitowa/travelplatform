#requires -Version 7.2
[CmdletBinding()]
param(
    [string]$BaseUrl = 'http://localhost:8088',
    [string]$TargetPath = '/api/public/flights',
    [ValidateRange(1,20)][int]$Runs = 3,
    [ValidateRange(1,1000)][int]$VirtualUsers = 30,
    [string]$Duration = '60s',
    [string]$WarmupDuration = '30s',
    [ValidateRange(0.01,60)][double]$SleepSeconds = 0.1,
    [ValidateSet('monolith','microservices')][string]$Variant = 'monolith',
    [ValidateSet('travel-platform','travel-platform-micro','travel-platform-micro-team','travel-platform-bench-monolith','travel-platform-bench-micro')][string]$Namespace,
    [ValidateRange(0,60)][int]$DelayBetweenRunsSeconds = 10,
    [string]$ResultDirectory,
    [string]$DockerK6Image = 'grafana/k6:2.1.0',
    [string]$Context = 'kind-travel-platform',
    [ValidateSet('rehearsal','formal')][string]$Purpose = 'rehearsal',
    [ValidateSet('fixed','hpa')][string]$ScaleMode = 'hpa',
    [string]$BaselineId = 'unconfirmed',
    [string]$DatasetId = 'unconfirmed',
    [switch]$TeamBaselineConfirmed
)
$ErrorActionPreference = 'Stop'
Import-Module (Join-Path $PSScriptRoot 'LabHttp.psm1') -Force
Import-Module (Join-Path $PSScriptRoot 'LabEvidence.psm1') -Force
Assert-LabUrl $BaseUrl
$baseUri = [uri]$BaseUrl
if ($baseUri.AbsolutePath -ne '/') { throw 'BaseUrl must contain only the local origin, without a path.' }
if (-not $TargetPath.StartsWith('/api/public/') -or $TargetPath.Contains('#')) { throw 'Only local public-read API paths are supported.' }
$durationSeconds = Get-LabSeconds $Duration
$warmupSeconds = Get-LabSeconds $WarmupDuration -AllowZero
if ($Purpose -eq 'formal' -and (-not $TeamBaselineConfirmed -or $Runs -lt 3 -or $BaselineId -eq 'unconfirmed' -or $DatasetId -eq 'unconfirmed' -or -not $BaselineId.Trim() -or -not $DatasetId.Trim())) {
    throw 'Formal runs require >=3 repetitions, BaselineId, DatasetId and explicit TeamBaselineConfirmed. This is a human confirmation, not an automatic code approval.'
}
$namespace = if ($Namespace) {$Namespace} elseif ($Variant -eq 'monolith') {'travel-platform'} else {'travel-platform-micro'}
if (($Variant -eq 'monolith' -and $namespace -ne 'travel-platform') -or ($Variant -eq 'microservices' -and $namespace -eq 'travel-platform')) {
    throw 'Variant and Namespace disagree. Explicitly select the intended monolith or microservice environment.'
}
$workload = if ($Variant -eq 'monolith') {'backend'} else {'product-service'}
$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
$testScript = Join-Path $projectRoot 'experiments/k6/public-read.js'
$nativeK6 = Get-Command k6 -ErrorAction SilentlyContinue
if (-not $nativeK6 -and -not (Get-Command docker -ErrorAction SilentlyContinue)) { throw 'Install k6 or start Docker Desktop first.' }
$before = Get-LabSnapshot $Context $namespace
if (-not $before.Workloads.Count -or @($before.Workloads | Where-Object { $_.Replicas -lt 1 -or $_.Ready -ne $_.Replicas }).Count) {
    throw 'All application workloads must be ready before a measurement batch starts.'
}
if ($ScaleMode -eq 'fixed') { Assert-LabFixedScale $before }
$configFingerprint = Get-LabConfigFingerprint $before $ScaleMode
$probe = Invoke-LabRequest -BaseUrl $BaseUrl -Path $TargetPath -TimeoutSeconds 10
if ($probe.HttpStatus -ne 200 -or $probe.Code -ne 200) { throw 'The target API failed the preflight business check.' }
$engine = if ($nativeK6) { (& k6 version) -join ' ' } else { (& docker run --rm --pull=never $DockerK6Image version) -join ' ' }
if ($LASTEXITCODE -ne 0) { throw 'Cannot start k6. For Docker, pull the specified image explicitly before running this script.' }
$resultRoot = if ($ResultDirectory) { [IO.Path]::GetFullPath($ResultDirectory) } else { Join-Path $projectRoot "experiments/results/$Variant" }
$sessionId = "$(Get-Date -Format 'yyyyMMdd-HHmmss')-$([guid]::NewGuid().ToString('N').Substring(0,8))"
$sessionDirectory = Join-Path $resultRoot $sessionId
New-Item -ItemType Directory -Path $sessionDirectory -Force | Out-Null
$metadataFile = Join-Path $sessionDirectory 'metadata.json'
$metadata = [ordered]@{
    SchemaVersion=2; SessionId=$sessionId; Status='Running'; StartedAt=(Get-Date).ToString('o'); FinishedAt=$null; Error=$null
    Protocol=[ordered]@{
        Variant=$Variant; Purpose=$Purpose; BaselineId=$BaselineId; DatasetId=$DatasetId; TeamBaselineConfirmed=[bool]$TeamBaselineConfirmed
        BaseUrl=$BaseUrl; TargetPath=$TargetPath; VirtualUsers=$VirtualUsers; DurationSeconds=$durationSeconds
        WarmupSeconds=$warmupSeconds; SleepSeconds=$SleepSeconds; DelayBetweenRunsSeconds=$DelayBetweenRunsSeconds
        PlannedRuns=$Runs; ScaleMode=$ScaleMode; Context=$Context; Namespace=$namespace; ObservedWorkload=$workload
        K6Version=$engine; K6DockerImage=$(if ($nativeK6) {$null} else {$DockerK6Image})
        ScriptSha256=(Get-FileHash -LiteralPath $testScript -Algorithm SHA256).Hash
    }
    ConfigFingerprint=$configFingerprint; Before=$before; After=$null; Warmup=$null
    Runs=[Collections.Generic.List[object]]::new()
}
function Save-Metadata { $metadata | ConvertTo-Json -Depth 40 | Set-Content -LiteralPath $metadataFile -Encoding utf8 }
function Invoke-Scenario {
    param([string]$RunDuration,[string]$SummaryName,[string]$RunLabel)
    $summaryFile = Join-Path $sessionDirectory $SummaryName
    $effectiveUrl = $BaseUrl
    if (-not $nativeK6) {
        $builder = [UriBuilder]::new($BaseUrl)
        $builder.Host = 'host.docker.internal'
        $effectiveUrl = $builder.Uri.AbsoluteUri.TrimEnd('/')
    }
    $summaryDestination = if ($nativeK6) {$summaryFile} else {"/results/$SummaryName"}
    $sleepText = $SleepSeconds.ToString([Globalization.CultureInfo]::InvariantCulture)
    $options = @('run','-e',"BASE_URL=$effectiveUrl",'-e',"TARGET_PATH=$TargetPath",'-e',"VUS=$VirtualUsers",
        '-e',"DURATION=$RunDuration",'-e',"SLEEP_SECONDS=$sleepText",'-e',"SUMMARY_FILE=$summaryDestination",
        '-e',"SESSION_ID=$sessionId",'-e',"RUN_LABEL=$RunLabel",'-e',"VARIANT=$Variant")
    if ($nativeK6) { & k6 @options $testScript | Out-Host }
    else {
        & docker run --rm --pull=never --volume "${testScript}:/test/public-read.js:ro" --volume "${sessionDirectory}:/results" $DockerK6Image @options /test/public-read.js | Out-Host
    }
    $exitCode = $LASTEXITCODE
    # Threshold violations are valid measurements, not permission to discard an inconvenient result.
    if ($exitCode -notin @(0,99)) { throw "k6 execution failed (exit $exitCode)." }
    if (-not (Test-Path -LiteralPath $summaryFile)) { throw 'k6 did not produce its raw summary.' }
    return [ordered]@{SummaryFile=$SummaryName; SummarySha256=(Get-FileHash $summaryFile -Algorithm SHA256).Hash; ExitCode=$exitCode; ThresholdsPassed=($exitCode -eq 0)}
}
Save-Metadata
try {
    if ($warmupSeconds -gt 0) {
        Write-Host "Warm-up only: $WarmupDuration (excluded from measurement runs)."
        $metadata.Warmup = Invoke-Scenario $WarmupDuration 'warmup.json' 'warmup'
        Save-Metadata
    }
    for ($run=1; $run -le $Runs; $run++) {
        $snapshot = Get-LabSnapshot $Context $namespace
        if ((Get-LabConfigFingerprint $snapshot $ScaleMode) -ne $configFingerprint) { throw 'Workload configuration changed within this batch. Start a new batch.' }
        $record = [ordered]@{Run=$run;Status='Running';StartedAt=$null;FinishedAt=$null;SummaryFile=$null;SummarySha256=$null;ExitCode=$null;ThresholdsPassed=$null;MetricsFile="run-$run-metrics.csv";MetricsSha256=$null}
        $metadata.Runs.Add($record)
        Save-Metadata
        $metricsFile = Join-Path $sessionDirectory $record.MetricsFile
        $collector = Join-Path $PSScriptRoot 'collect-k8s-metrics.ps1'
        $job = Start-Job -ScriptBlock {
            param($Script,$Context,$Namespace,$Workload,$Seconds,$Output)
            & $Script -Context $Context -Namespace $Namespace -WorkloadName $Workload -DurationSeconds $Seconds -IntervalSeconds 5 -OutputFile $Output
        } -ArgumentList $collector,$Context,$namespace,$workload,($durationSeconds+60),$metricsFile
        try {
            $readyDeadline = (Get-Date).AddSeconds(30)
            while (-not (Test-Path -LiteralPath $metricsFile) -and $job.State -eq 'Running' -and (Get-Date) -lt $readyDeadline) { Start-Sleep -Seconds 1 }
            if (-not (Test-Path -LiteralPath $metricsFile)) { throw 'Metrics collector failed to start; measurement was not started.' }
            $record.StartedAt = (Get-Date).ToString('o')
            Save-Metadata
            Write-Host "Starting $Purpose $Variant run $run/$Runs. Session: $sessionId"
            $outcome = Invoke-Scenario $Duration "run-$run-summary.json" "$run"
            $record.FinishedAt = (Get-Date).ToString('o')
            foreach ($key in $outcome.Keys) { $record[$key]=$outcome[$key] }
            if ($job.State -eq 'Failed') { throw 'Metrics collector failed during the measurement.' }
            $record.Status='Complete'
        } finally {
            if (-not $record.FinishedAt) { $record.FinishedAt=(Get-Date).ToString('o') }
            Stop-Job -Job $job
            Receive-Job -Job $job -ErrorAction Continue | Out-Host
            Remove-Job -Job $job
            if (Test-Path -LiteralPath $metricsFile) { $record.MetricsSha256=(Get-FileHash $metricsFile -Algorithm SHA256).Hash }
            Save-Metadata
        }
        if ($run -lt $Runs -and $DelayBetweenRunsSeconds) { Start-Sleep -Seconds $DelayBetweenRunsSeconds }
    }
    $metadata.After = Get-LabSnapshot $Context $namespace
    if ((Get-LabConfigFingerprint $metadata.After $ScaleMode) -ne $configFingerprint) { throw 'Workload configuration changed during the batch; do not aggregate it as a consistent protocol.' }
    $metadata.Status='Complete'
} catch {
    $metadata.Status='Failed'
    $metadata.Error=$_.Exception.Message
    throw
} finally {
    $metadata.FinishedAt=(Get-Date).ToString('o')
    Save-Metadata
    Write-Host "Batch evidence: $sessionDirectory"
}
Write-Host 'Complete means all runs were recorded; inspect ThresholdsPassed separately. Rehearsal results are not formal comparison evidence.'
