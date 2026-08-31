#requires -Version 7.2
[CmdletBinding()]
param(
    [Parameter(Mandatory)][string]$SessionDirectory,
    [string]$OutputFile
)
$ErrorActionPreference = 'Stop'
$sessionRoot = (Resolve-Path -LiteralPath $SessionDirectory).Path
$metadata = Get-Content -Raw -LiteralPath (Join-Path $sessionRoot 'metadata.json') | ConvertFrom-Json
if ($metadata.SchemaVersion -ne 2 -or $metadata.Status -ne 'Complete') { throw 'Only a complete schema-v2 measurement batch can be aggregated. Legacy/partial files are not mixed automatically.' }
$records = @($metadata.Runs)
if (-not $records.Count -or $records.Count -ne $metadata.Protocol.PlannedRuns) { throw 'Run count does not match the batch protocol.' }
if (@($records.Run | Sort-Object -Unique).Count -ne $records.Count) { throw 'Duplicate run identifiers.' }
function Get-VerifiedFile {
    param([string]$Name,[string]$Hash)
    if (-not $Name -or $Name -ne [IO.Path]::GetFileName($Name) -or $Name.Contains('\') -or $Name.Contains('/') -or $Hash -notmatch '^[A-Fa-f0-9]{64}$') { throw 'Invalid evidence filename or SHA-256.' }
    $path = Join-Path $sessionRoot $Name
    if ((Get-FileHash -LiteralPath $path -Algorithm SHA256).Hash -ne $Hash) { throw "Evidence changed after the run: $Name" }
    return $path
}
function Get-Number {
    param($Data,[string]$Metric,[string]$Field)
    $value = $Data.metrics.$Metric.values.$Field
    if ($null -eq $value -or $value -is [string] -or $value -is [bool]) { throw "Missing or non-numeric metric: $Metric.$Field" }
    $number = [double]$value
    if ([double]::IsNaN($number) -or [double]::IsInfinity($number) -or $number -lt 0) { throw "Invalid metric: $Metric.$Field" }
    return $number
}
$runs = @(foreach ($record in $records) {
    if ($record.Status -ne 'Complete' -or $record.ExitCode -notin @(0,99)) { throw 'One or more runs did not finish normally.' }
    $data = Get-Content -Raw -LiteralPath (Get-VerifiedFile $record.SummaryFile $record.SummarySha256) | ConvertFrom-Json
    $metricsPath = Get-VerifiedFile $record.MetricsFile $record.MetricsSha256
    # ConvertFrom-Json can return DateTime on newer PowerShell versions. Casting
    # preserves its Kind; parsing its culture-formatted string would lose UTC.
    $start = [DateTimeOffset]$record.StartedAt
    $end = [DateTimeOffset]$record.FinishedAt
    if ($end -le $start) { throw 'Invalid run time window.' }
    $rows = @(Import-Csv -LiteralPath $metricsPath | Where-Object {
        $time=[DateTimeOffset]::Parse($_.Timestamp)
        $time -ge $start -and $time -le $end
    })
    if (@($rows | Where-Object { $_.Context -ne $metadata.Protocol.Context -or $_.Namespace -ne $metadata.Protocol.Namespace }).Count) { throw 'Metrics came from a different cluster or namespace.' }
    $samples = @($rows | Group-Object Timestamp | ForEach-Object {
        $running = @($_.Group | Where-Object Phase -eq 'Running')
        $available = @($running | Where-Object { $_.MetricsAvailable -eq 'True' -and $_.CpuMillicores -ne '' -and $_.MemoryMi -ne '' })
        [pscustomobject]@{
            RunningPods=$running.Count; ReadyPods=@($_.Group | Where-Object PodReady -eq 'True').Count
            Complete=($running.Count -gt 0 -and $available.Count -eq $running.Count)
            CpuMillicores=($available | ForEach-Object { [double]::Parse($_.CpuMillicores,[Globalization.CultureInfo]::InvariantCulture) } | Measure-Object -Sum).Sum
            MemoryMi=($available | ForEach-Object { [double]::Parse($_.MemoryMi,[Globalization.CultureInfo]::InvariantCulture) } | Measure-Object -Sum).Sum
        }
    })
    $completeSamples = @($samples | Where-Object Complete)
    if (-not $completeSamples.Count -and $metadata.Protocol.Purpose -eq 'formal') { throw 'No complete resource sample falls inside a formal run. Do not claim resource comparison results.' }
    $requests = Get-Number $data 'http_reqs' 'count'
    if ($requests -le 0) { throw 'A run contains no requests.' }
    $httpErrorRate = Get-Number $data 'http_req_failed' 'rate'
    $businessErrorRate = Get-Number $data 'business_errors' 'rate'
    if ($httpErrorRate -gt 1 -or $businessErrorRate -gt 1) { throw 'Error rate is outside [0,1].' }
    [pscustomobject][ordered]@{
        Run=$record.Run; File=$record.SummaryFile; ThresholdsPassed=[bool]$record.ThresholdsPassed
        Requests=$requests; RequestsPerSecond=(Get-Number $data 'http_reqs' 'rate')
        HttpErrorRatePercent=100*$httpErrorRate; BusinessErrorRatePercent=100*$businessErrorRate
        AverageMs=(Get-Number $data 'http_req_duration' 'avg'); P95Ms=(Get-Number $data 'http_req_duration' 'p(95)')
        ResourceSamples=$samples.Count; CompleteResourceSamples=$completeSamples.Count
        MeanNamespaceCpuMillicores=$(if ($completeSamples.Count) {($completeSamples.CpuMillicores | Measure-Object -Average).Average} else {$null})
        MeanNamespaceMemoryMi=$(if ($completeSamples.Count) {($completeSamples.MemoryMi | Measure-Object -Average).Average} else {$null})
        MinRunningPods=$(if ($samples.Count) {($samples.RunningPods | Measure-Object -Minimum).Minimum} else {$null})
        MaxRunningPods=$(if ($samples.Count) {($samples.RunningPods | Measure-Object -Maximum).Maximum} else {$null})
    }
})
$summary = [ordered]@{
    SessionId=$metadata.SessionId; GeneratedAt=(Get-Date).ToString('o'); Protocol=$metadata.Protocol
    RunCount=$runs.Count; Runs=$runs
    MeanOfRuns=[ordered]@{
        RequestsPerSecond=($runs.RequestsPerSecond | Measure-Object -Average).Average
        HttpErrorRatePercent=($runs.HttpErrorRatePercent | Measure-Object -Average).Average
        BusinessErrorRatePercent=($runs.BusinessErrorRatePercent | Measure-Object -Average).Average
        AverageMs=($runs.AverageMs | Measure-Object -Average).Average
        MeanOfRunP95Ms=($runs.P95Ms | Measure-Object -Average).Average
    }
    Notes=@(
        'MeanOfRunP95Ms is the arithmetic mean of per-run P95 values, NOT a pooled/global P95.',
        'Resource means sum all Running Pods in this namespace (including gateway, frontend and MySQL); incomplete samples are excluded, never zero-filled.',
        'Metrics Server values are sampled resource usage, not exact per-request usage. Review sample coverage and raw CSV.',
        'Complete means evidence exists, not that latency/error thresholds passed. Rehearsal batches are not formal comparison evidence.',
        'Matching batch parameters alone does not prove fair cross-architecture comparison; confirm dataset, total resource budget, machine load and service-owner baseline.'
    )
}
if (-not $OutputFile) { $OutputFile=Join-Path $sessionRoot 'aggregate.json' }
$outputPath=[IO.Path]::GetFullPath($OutputFile)
$protected=@('metadata.json') + @($records.SummaryFile) + @($records.MetricsFile) + @('warmup.json')
foreach ($name in $protected) {
    if ($outputPath -eq [IO.Path]::GetFullPath((Join-Path $sessionRoot $name))) { throw 'Aggregate output cannot overwrite raw evidence.' }
}
$summary | ConvertTo-Json -Depth 15 | Set-Content -LiteralPath $outputPath -Encoding utf8
$runs | Format-Table Run,RequestsPerSecond,AverageMs,P95Ms,HttpErrorRatePercent,BusinessErrorRatePercent,ThresholdsPassed -AutoSize | Out-Host
Write-Host "Saved batch-only aggregate: $outputPath"
