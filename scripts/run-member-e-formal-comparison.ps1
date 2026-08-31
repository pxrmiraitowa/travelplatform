#requires -Version 7.2
[CmdletBinding()]
param(
    [Parameter(Mandatory)][string]$PreparedDirectory,
    [string]$ProtocolFile = 'experiments/protocols/member-e-formal-comparison.json',
    [string]$DockerK6Image = 'grafana/k6:2.1.0',
    [string]$ResultDirectory
)

$ErrorActionPreference='Stop'
$projectRoot=(Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$preparedRoot=(Resolve-Path -LiteralPath (Join-Path $projectRoot $PreparedDirectory)).Path
$protocolPath=(Resolve-Path -LiteralPath (Join-Path $projectRoot $ProtocolFile)).Path
$protocol=Get-Content -Raw -LiteralPath $protocolPath | ConvertFrom-Json -AsHashtable
$prepared=Get-Content -Raw -LiteralPath (Join-Path $preparedRoot 'prepared.json') | ConvertFrom-Json -AsHashtable
$deploymentState=Get-Content -Raw -LiteralPath (Join-Path $preparedRoot 'deployment-state.json') | ConvertFrom-Json -AsHashtable
$runtimeVerification=Get-Content -Raw -LiteralPath (Join-Path $preparedRoot 'runtime-verification.json') | ConvertFrom-Json -AsHashtable
Import-Module (Join-Path $PSScriptRoot 'member-e/MemberEComparison.psm1') -Force
Import-Module (Join-Path $projectRoot 'experiments/scripts/LabEvidence.psm1') -Force

if ($protocol.schemaVersion -ne 1 -or $protocol.context -ne 'kind-travel-platform') { throw 'Unexpected formal comparison protocol.' }
if ((& kubectl config current-context) -ne $protocol.context) { throw 'The active kubectl context is not the reviewed local Kind cluster.' }
if (-not $prepared.ReadyForDeployment -or $deploymentState.Status -ne 'Ready' -or -not $runtimeVerification.DatasetIdentical -or -not $runtimeVerification.BudgetsEqual) {
    throw 'The prepared comparison pair is not backed by verified builds, deployment state and identical data.'
}
if ($prepared.SourceRevision -ne $protocol.sourceRevision -or $deploymentState.SourceRevision -ne $protocol.sourceRevision) { throw 'Prepared artifacts and formal protocol use different source revisions.' }
if (@($protocol.endpoints).Count -ne 3 -or (@($protocol.endpoints.name | Sort-Object -Unique) -join ',') -ne 'flights,hotels,tours') { throw 'Formal protocol must contain the three reviewed catalog endpoints.' }
if (@($protocol.order).Count -ne 6 -or @($protocol.order | Where-Object {$_ -eq 'monolith'}).Count -ne 3 -or @($protocol.order | Where-Object {$_ -eq 'microservices'}).Count -ne 3) { throw 'Formal protocol must schedule three measurements per architecture.' }
if ($protocol.hpaEnabled -or $protocol.replicasPerDeployment -ne 1) { throw 'Formal architecture comparison must use fixed single replicas.' }
if ($protocol.virtualUsers -ne 30 -or $protocol.warmupSecondsPerRun -ne 30 -or $protocol.measurementSecondsPerRun -ne 60) { throw 'Unexpected workload parameters; create a new reviewed protocol instead of silently changing them.' }

& git -C $projectRoot diff --quiet $protocol.sourceRevision HEAD -- travel-platform-microservices travel-platform-server travel-platform-web deploy .github
if ($LASTEXITCODE -ne 0) { throw 'Runtime inputs changed after the tested source revision; rebuild the pair before measuring.' }
& git -C $projectRoot diff --quiet -- travel-platform-microservices travel-platform-web
if ($LASTEXITCODE -ne 0) { throw 'Uncommitted application changes would make the runtime baseline ambiguous.' }
& docker image inspect $DockerK6Image *> $null
if ($LASTEXITCODE -ne 0) { throw "The pinned local k6 image is missing: $DockerK6Image" }

$k6Script=Join-Path $projectRoot 'experiments/k6/public-catalog-comparison.js'
$collectorScript=Join-Path $projectRoot 'experiments/scripts/collect-k8s-metrics.ps1'
$artifactRoot=if ($ResultDirectory) {[IO.Path]::GetFullPath($ResultDirectory)} else {Join-Path $projectRoot "artifacts/member-e/$($protocol.sourceRevision.Substring(0,7))"}
$sessionId="formal-comparison-$(Get-Date -Format 'yyyyMMdd-HHmmss')-$([guid]::NewGuid().ToString('N').Substring(0,8))"
$sessionDirectory=Join-Path $artifactRoot $sessionId
if (Test-Path -LiteralPath $sessionDirectory) { throw 'Refusing to overwrite an existing formal comparison session.' }
New-Item -ItemType Directory -Path $sessionDirectory -Force | Out-Null
$metadataFile=Join-Path $sessionDirectory 'metadata.json'
$metadata=[ordered]@{
    SchemaVersion=1;SessionId=$sessionId;Status='Running';StartedAt=(Get-Date).ToString('o');FinishedAt=$null;Error=$null
    SourceRevision=$protocol.sourceRevision;RepositoryHead=(& git -C $projectRoot rev-parse HEAD);PreparedDirectory=$preparedRoot
    Protocol=$protocol;ProtocolSha256=(Get-FileHash $protocolPath -Algorithm SHA256).Hash
    K6=[ordered]@{Image=$DockerK6Image;ImageId=(& docker image inspect $DockerK6Image --format '{{.Id}}');ScriptSha256=(Get-FileHash $k6Script -Algorithm SHA256).Hash}
    Preflight=$null;Before=$null;After=$null;Runs=[Collections.Generic.List[object]]::new();SecretValuesIncluded=$false
}
function Save-Metadata { $metadata | ConvertTo-Json -Depth 50 | Set-Content -LiteralPath $metadataFile -Encoding utf8 }
function Invoke-KubeJson {
    param([string]$Namespace,[string[]]$Arguments)
    $raw=& kubectl --context $protocol.context -n $Namespace --request-timeout=30s @Arguments
    if ($LASTEXITCODE -ne 0) { throw "Cannot inspect ${Namespace}: $($Arguments -join ' ')" }
    return (($raw -join "`n") | ConvertFrom-Json -AsHashtable)
}
function Assert-RuntimePair {
    $snapshots=[ordered]@{}
    foreach ($variant in @('monolith','microservices')) {
        $namespace=$protocol.namespaces[$variant]
        $items=@((Invoke-KubeJson $namespace @('get','deployment,statefulset,hpa,pod,pvc','-o','json')).items)
        $workloads=@($items | Where-Object {$_.kind -in @('Deployment','StatefulSet')})
        $hpas=@($items | Where-Object kind -eq 'HorizontalPodAutoscaler')
        $pods=@($items | Where-Object kind -eq 'Pod')
        $claims=@($items | Where-Object kind -eq 'PersistentVolumeClaim')
        if ($hpas.Count -ne 0 -or @($workloads | Where-Object {$_.spec.replicas -ne 1 -or $_.status.readyReplicas -ne 1}).Count -ne 0) { throw "$namespace is not a ready, fixed-replica baseline." }
        if (@($pods | Where-Object {$_.status.phase -notin @('Running','Succeeded')}).Count -ne 0) { throw "$namespace contains an unexpected Pod phase." }
        $budget=Get-ComparisonBudget $items
        foreach ($key in $protocol.budgetPerVariant.Keys) {
            if ([double]$budget[$key] -ne [double]$protocol.budgetPerVariant[$key]) { throw "$namespace resource budget changed: $key" }
        }
        $snapshots[$variant]=[ordered]@{
            Namespace=$namespace;Workloads=@($workloads | Sort-Object {$_.metadata.name} | ForEach-Object {[ordered]@{Kind=$_.kind;Name=$_.metadata.name;Uid=$_.metadata.uid;Replicas=$_.spec.replicas;Ready=$_.status.readyReplicas;Image=$_.spec.template.spec.containers[0].image}})
            Pods=@($pods | Sort-Object {$_.metadata.name} | ForEach-Object {[ordered]@{Name=$_.metadata.name;Uid=$_.metadata.uid;Phase=$_.status.phase;Ready=@($_.status.conditions | Where-Object {$_.type -eq 'Ready' -and $_.status -eq 'True'}).Count -gt 0}})
            PersistentClaims=@($claims | Sort-Object {$_.metadata.name} | ForEach-Object {[ordered]@{Name=$_.metadata.name;Uid=$_.metadata.uid;VolumeName=$_.spec.volumeName;Phase=$_.status.phase}})
            HpaCount=0;Budget=$budget
        }
    }
    if ((Get-ComparisonHash $snapshots.monolith.Budget) -ne (Get-ComparisonHash $snapshots.microservices.Budget)) { throw 'Aggregate resource budgets are no longer equal.' }
    return $snapshots
}
function Wait-Forward {
    param($Forward)
    $deadline=(Get-Date).AddSeconds(45)
    do {
        if ($Forward.Process.HasExited) { throw "$($Forward.Variant) port-forward exited; inspect its stderr log." }
        try {
            $response=Invoke-WebRequest -Uri "http://127.0.0.1:$($Forward.Port)/api/public/health" -TimeoutSec 5
            $body=$response.Content | ConvertFrom-Json
            if ($response.StatusCode -eq 200 -and $body.code -eq 200) { return }
        } catch { if ((Get-Date) -ge $deadline) { throw } }
        if ((Get-Date) -ge $deadline) { throw "$($Forward.Variant) frontend did not become reachable." }
        Start-Sleep -Seconds 1
    } while ($true)
}
function Invoke-K6 {
    param([string]$Variant,[int]$Port,[string]$Duration,[string]$SummaryName,[string]$RunLabel)
    $summaryHost=Join-Path $sessionDirectory $SummaryName
    $summaryContainer="/results/$SummaryName"
    $sleepText=([double]$protocol.sleepSeconds).ToString([Globalization.CultureInfo]::InvariantCulture)
    $options=@('run','-e',"BASE_URL=http://host.docker.internal:$Port",'-e',"VUS=$($protocol.virtualUsers)",'-e',"DURATION=$Duration",
        '-e',"SLEEP_SECONDS=$sleepText",'-e',"SUMMARY_FILE=$summaryContainer",'-e',"SESSION_ID=$sessionId",'-e',"RUN_LABEL=$RunLabel",'-e',"VARIANT=$Variant")
    & docker run --rm --pull=never --volume "${k6Script}:/test/public-catalog-comparison.js:ro" --volume "${sessionDirectory}:/results" $DockerK6Image @options /test/public-catalog-comparison.js | Out-Host
    $exitCode=$LASTEXITCODE
    if ($exitCode -notin @(0,99)) { throw "k6 failed with exit code $exitCode." }
    if (-not (Test-Path -LiteralPath $summaryHost)) { throw "k6 did not create $SummaryName." }
    return [ordered]@{File=$SummaryName;Sha256=(Get-FileHash $summaryHost -Algorithm SHA256).Hash;ExitCode=$exitCode;ThresholdsPassed=($exitCode -eq 0)}
}
function Get-Number {
    param($Summary,[string]$Metric,[string]$Field)
    $metricObject=$Summary.metrics.$Metric
    if ($null -eq $metricObject) { throw "Missing k6 metric: $Metric" }
    $value=$metricObject.values.$Field
    if ($null -eq $value -or $value -is [string] -or $value -is [bool]) { throw "Missing numeric k6 metric: $Metric.$Field" }
    return [double]$value
}
function Measure-ResourceCsv {
    param([string]$Path,[DateTimeOffset]$StartedAt,[DateTimeOffset]$FinishedAt)
    $rows=@(Import-Csv -LiteralPath $Path | Where-Object {
        $timestamp=[DateTimeOffset]::Parse($_.Timestamp)
        $timestamp -ge $StartedAt -and $timestamp -le $FinishedAt
    })
    $samples=@($rows | Group-Object Timestamp | ForEach-Object {
        $running=@($_.Group | Where-Object Phase -eq 'Running')
        $available=@($running | Where-Object {$_.MetricsAvailable -eq 'True' -and $_.CpuMillicores -ne '' -and $_.MemoryMi -ne ''})
        [pscustomobject]@{
            Timestamp=$_.Name;RunningPods=$running.Count;ReadyPods=@($running | Where-Object PodReady -eq 'True').Count
            Complete=($running.Count -gt 0 -and $available.Count -eq $running.Count)
            CpuMillicores=($available | ForEach-Object {[double]::Parse($_.CpuMillicores,[Globalization.CultureInfo]::InvariantCulture)} | Measure-Object -Sum).Sum
            MemoryMi=($available | ForEach-Object {[double]::Parse($_.MemoryMi,[Globalization.CultureInfo]::InvariantCulture)} | Measure-Object -Sum).Sum
        }
    })
    $complete=@($samples | Where-Object Complete)
    if ($complete.Count -lt 6) { throw "Too few complete resource samples in formal run: $($complete.Count)" }
    return [ordered]@{
        Samples=$samples.Count;CompleteSamples=$complete.Count
        MeanCpuMillicores=($complete.CpuMillicores | Measure-Object -Average).Average
        MaxCpuMillicores=($complete.CpuMillicores | Measure-Object -Maximum).Maximum
        MeanMemoryMi=($complete.MemoryMi | Measure-Object -Average).Average
        MaxMemoryMi=($complete.MemoryMi | Measure-Object -Maximum).Maximum
        MinRunningPods=($complete.RunningPods | Measure-Object -Minimum).Minimum
        MaxRunningPods=($complete.RunningPods | Measure-Object -Maximum).Maximum
    }
}

$forwards=[Collections.Generic.List[object]]::new()
$completed=$false
Save-Metadata
try {
    $metadata.Before=Assert-RuntimePair
    $requiredPorts=@([int]$protocol.ports.monolith,[int]$protocol.ports.microservices)
    $listeners=@([Net.NetworkInformation.IPGlobalProperties]::GetIPGlobalProperties().GetActiveTcpListeners() | Where-Object {$_.Port -in $requiredPorts})
    if ($listeners.Count) { throw "Formal comparison ports are already in use: $(@($listeners.Port | Sort-Object -Unique) -join ', ')" }
    foreach ($variant in @('monolith','microservices')) {
        $port=[int]$protocol.ports[$variant]
        $arguments=@('--context',$protocol.context,'--namespace',$protocol.namespaces[$variant],'port-forward','service/frontend',"${port}:80",'--address=127.0.0.1')
        $process=Start-Process -FilePath (Get-Command kubectl).Source -ArgumentList $arguments -WindowStyle Hidden -PassThru `
            -RedirectStandardOutput (Join-Path $sessionDirectory "$variant-forward.log") `
            -RedirectStandardError (Join-Path $sessionDirectory "$variant-forward.err")
        $forward=[pscustomobject]@{Variant=$variant;Port=$port;Process=$process}
        $forwards.Add($forward)
    }
    foreach ($forward in $forwards) { Wait-Forward $forward }

    $datasets=[ordered]@{}
    foreach ($endpoint in $protocol.endpoints) {
        $pair=[ordered]@{}
        foreach ($variant in @('monolith','microservices')) {
            $port=[int]$protocol.ports[$variant]
            $response=Invoke-WebRequest -Uri "http://127.0.0.1:$port$($endpoint.datasetPath)" -TimeoutSec 15
            $body=$response.Content | ConvertFrom-Json -AsHashtable
            if ($response.StatusCode -ne 200 -or $body.code -ne 200 -or -not $body.data.records.Count) { throw "$variant $($endpoint.name) dataset preflight failed." }
            $pair[$variant]=[ordered]@{HttpStatus=$response.StatusCode;BusinessCode=$body.code;Records=$body.data.records.Count;Total=$body.data.total;DataSha256=(Get-ComparisonHash $body.data)}
        }
        if ($pair.monolith.DataSha256 -ne $pair.microservices.DataSha256 -or $pair.monolith.Total -ne $pair.microservices.Total) { throw "$($endpoint.name) data differs between architectures." }
        $datasets[$endpoint.name]=$pair
    }
    $metadata.Preflight=[ordered]@{At=(Get-Date).ToString('o');Datasets=$datasets;AllIdentical=$true;NodeTop=((& kubectl --context $protocol.context top node --no-headers) -join "`n")}
    Save-Metadata

    $occurrences=@{monolith=0;microservices=0}
    for ($index=0;$index -lt $protocol.order.Count;$index++) {
        $variant=[string]$protocol.order[$index]
        $occurrences[$variant]++
        $runNumber=$index+1
        $variantRun=$occurrences[$variant]
        $record=[ordered]@{
            Sequence=$runNumber;Variant=$variant;VariantRun=$variantRun;Status='Running';StartedAt=$null;FinishedAt=$null
            Warmup=$null;Summary=$null;MetricsFile=("run-{0:D2}-{1}-metrics.csv" -f $runNumber,$variant);MetricsSha256=$null
            NodeTopBefore=((& kubectl --context $protocol.context top node --no-headers) -join "`n");ResourceSummary=$null;Error=$null
        }
        $metadata.Runs.Add($record)
        Save-Metadata
        Write-Host "Warm-up for sequence $runNumber/6: $variant (excluded from formal metrics)."
        $record.Warmup=Invoke-K6 -Variant $variant -Port ([int]$protocol.ports[$variant]) -Duration "$($protocol.warmupSecondsPerRun)s" -SummaryName ("run-{0:D2}-{1}-warmup.json" -f $runNumber,$variant) -RunLabel "warmup-$runNumber"
        Save-Metadata

        $metricsPath=Join-Path $sessionDirectory $record.MetricsFile
        $workload=if ($variant -eq 'monolith') {'backend'} else {'product-service'}
        $job=Start-Job -ScriptBlock {
            param($Script,$Context,$Namespace,$Workload,$Seconds,$Interval,$Output)
            & $Script -Context $Context -Namespace $Namespace -WorkloadName $Workload -DurationSeconds $Seconds -IntervalSeconds $Interval -OutputFile $Output
        } -ArgumentList $collectorScript,$protocol.context,$protocol.namespaces[$variant],$workload,($protocol.measurementSecondsPerRun+30),$protocol.metricIntervalSeconds,$metricsPath
        try {
            $deadline=(Get-Date).AddSeconds(30)
            while (-not (Test-Path -LiteralPath $metricsPath) -and $job.State -eq 'Running' -and (Get-Date) -lt $deadline) { Start-Sleep -Seconds 1 }
            if (-not (Test-Path -LiteralPath $metricsPath)) { throw 'Resource collector did not start; formal measurement was not started.' }
            $record.StartedAt=(Get-Date).ToString('o')
            Save-Metadata
            Write-Host "Formal measurement sequence $runNumber/6: $variant run $variantRun/3."
            $record.Summary=Invoke-K6 -Variant $variant -Port ([int]$protocol.ports[$variant]) -Duration "$($protocol.measurementSecondsPerRun)s" -SummaryName ("run-{0:D2}-{1}-summary.json" -f $runNumber,$variant) -RunLabel "$runNumber"
            $record.FinishedAt=(Get-Date).ToString('o')
            $record.Status='Complete'
        } catch {
            $record.Status='Failed';$record.Error=$_.Exception.Message
            throw
        } finally {
            if (-not $record.FinishedAt) {$record.FinishedAt=(Get-Date).ToString('o')}
            Stop-Job -Job $job -ErrorAction SilentlyContinue
            Receive-Job -Job $job -ErrorAction Continue | Out-Host
            Remove-Job -Job $job -Force
            if (Test-Path -LiteralPath $metricsPath) {$record.MetricsSha256=(Get-FileHash $metricsPath -Algorithm SHA256).Hash}
            Save-Metadata
        }
        $record.ResourceSummary=Measure-ResourceCsv -Path $metricsPath -StartedAt ([DateTimeOffset]$record.StartedAt) -FinishedAt ([DateTimeOffset]$record.FinishedAt)
        Save-Metadata
        if ($runNumber -lt $protocol.order.Count -and $protocol.delaySecondsBetweenRuns -gt 0) { Start-Sleep -Seconds $protocol.delaySecondsBetweenRuns }
    }
    $metadata.After=Assert-RuntimePair
    foreach ($variant in @('monolith','microservices')) {
        $before=@($metadata.Before[$variant].Workloads | ForEach-Object {"$($_.Name):$($_.Uid):$($_.Replicas):$($_.Image)"}) -join '|'
        $after=@($metadata.After[$variant].Workloads | ForEach-Object {"$($_.Name):$($_.Uid):$($_.Replicas):$($_.Image)"}) -join '|'
        if ($before -ne $after) { throw "$variant workload identity or configuration changed during formal measurements." }
    }

    $runResults=@()
    foreach ($record in $metadata.Runs) {
        if ($record.Status -ne 'Complete') { throw 'A formal run is incomplete.' }
        $summaryPath=Join-Path $sessionDirectory $record.Summary.File
        if ((Get-FileHash $summaryPath -Algorithm SHA256).Hash -ne $record.Summary.Sha256) { throw 'A raw k6 summary changed before aggregation.' }
        $summary=Get-Content -Raw -LiteralPath $summaryPath | ConvertFrom-Json
        $endpointResults=[ordered]@{}
        foreach ($endpoint in $protocol.endpoints) {
            $name=[string]$endpoint.name
            $endpointResults[$name]=[ordered]@{
                Requests=Get-Number $summary "${name}_requests" 'count'
                ErrorRatePercent=100*(Get-Number $summary "${name}_errors" 'rate')
                AverageMs=Get-Number $summary "${name}_duration" 'avg'
                P95Ms=Get-Number $summary "${name}_duration" 'p(95)'
            }
        }
        $runResults += [pscustomobject][ordered]@{
            Sequence=$record.Sequence;Variant=$record.Variant;VariantRun=$record.VariantRun;ThresholdsPassed=[bool]$record.Summary.ThresholdsPassed
            Requests=Get-Number $summary 'http_reqs' 'count';RequestsPerSecond=Get-Number $summary 'http_reqs' 'rate'
            HttpErrorRatePercent=100*(Get-Number $summary 'http_req_failed' 'rate');BusinessErrorRatePercent=100*(Get-Number $summary 'business_errors' 'rate')
            AverageMs=Get-Number $summary 'http_req_duration' 'avg';P95Ms=Get-Number $summary 'http_req_duration' 'p(95)'
            Endpoints=$endpointResults;Resources=$record.ResourceSummary
        }
    }
    $variants=[ordered]@{}
    foreach ($variant in @('monolith','microservices')) {
        $subset=@($runResults | Where-Object Variant -eq $variant)
        if ($subset.Count -ne 3) { throw "$variant does not have exactly three complete formal runs." }
        $perEndpoint=[ordered]@{}
        foreach ($endpoint in $protocol.endpoints) {
            $name=[string]$endpoint.name
            $items=@($subset | ForEach-Object {$_.Endpoints[$name]})
            $perEndpoint[$name]=[ordered]@{
                MeanRequests=($items.Requests | Measure-Object -Average).Average
                MeanErrorRatePercent=($items.ErrorRatePercent | Measure-Object -Average).Average
                MeanAverageMs=($items.AverageMs | Measure-Object -Average).Average
                MeanOfRunP95Ms=($items.P95Ms | Measure-Object -Average).Average
            }
        }
        $variants[$variant]=[ordered]@{
            Runs=3;AllThresholdsPassed=@($subset | Where-Object {-not $_.ThresholdsPassed}).Count -eq 0
            MeanRequestsPerSecond=($subset.RequestsPerSecond | Measure-Object -Average).Average
            MeanHttpErrorRatePercent=($subset.HttpErrorRatePercent | Measure-Object -Average).Average
            MeanBusinessErrorRatePercent=($subset.BusinessErrorRatePercent | Measure-Object -Average).Average
            MeanAverageMs=($subset.AverageMs | Measure-Object -Average).Average
            MeanOfRunP95Ms=($subset.P95Ms | Measure-Object -Average).Average
            MeanNamespaceCpuMillicores=($subset.Resources.MeanCpuMillicores | Measure-Object -Average).Average
            MeanNamespaceMemoryMi=($subset.Resources.MeanMemoryMi | Measure-Object -Average).Average
            Endpoints=$perEndpoint
        }
    }
    $report=[ordered]@{
        SchemaVersion=1;SessionId=$sessionId;GeneratedAt=(Get-Date).ToString('o');Status='Complete'
        SourceRevision=$protocol.sourceRevision;RepositoryHead=$metadata.RepositoryHead;Protocol=$protocol
        DataIdentical=$metadata.Preflight.AllIdentical;Runs=$runResults;Variants=$variants
        Interpretation=[ordered]@{
            ThroughputDifferencePercent=100*($variants.microservices.MeanRequestsPerSecond-$variants.monolith.MeanRequestsPerSecond)/$variants.monolith.MeanRequestsPerSecond
            AverageLatencyDifferencePercent=100*($variants.microservices.MeanAverageMs-$variants.monolith.MeanAverageMs)/$variants.monolith.MeanAverageMs
            P95DifferencePercent=100*($variants.microservices.MeanOfRunP95Ms-$variants.monolith.MeanOfRunP95Ms)/$variants.monolith.MeanOfRunP95Ms
        }
        Notes=@(
            'Each architecture has three independent 60-second measured runs; every run follows a separate 30-second warm-up.',
            'The alternating order reduces time-order bias. Both variants use one Kind node, identical catalog data, fixed one-replica workloads and equal aggregate requests/limits.',
            'Each k6 iteration selects flights, hotels or tours in round-robin order, so all three interfaces share the same concurrency and timing window.',
            'MeanOfRunP95Ms is the arithmetic mean of three per-run P95 values, not a pooled percentile.',
            'Namespace CPU and memory sum all Running Pods in that variant, including frontend and MySQL. Metrics Server samples every five seconds.',
            'These results describe this local protocol only; they are not maximum-capacity results and do not prove universal architecture superiority.'
        );SecretValuesIncluded=$false
    }
    $reportPath=Join-Path $sessionDirectory 'comparison-report.json'
    $report | ConvertTo-Json -Depth 40 | Set-Content -LiteralPath $reportPath -Encoding utf8
    $metadata.Status='Complete';$metadata.ReportFile='comparison-report.json';$metadata.ReportSha256=(Get-FileHash $reportPath -Algorithm SHA256).Hash
    $completed=$true
} catch {
    $metadata.Status='Failed';$metadata.Error=$_.Exception.Message
    throw
} finally {
    foreach ($forward in $forwards) {
        if (-not $forward.Process.HasExited) {$forward.Process.Kill();$forward.Process.WaitForExit(5000) | Out-Null}
    }
    $metadata.FinishedAt=(Get-Date).ToString('o')
    Save-Metadata
    Write-Host "Formal comparison evidence: $sessionDirectory"
}

if (-not $completed) { throw 'Formal comparison did not complete.' }
[pscustomobject]@{
    Session=$sessionId
    MonolithQps=[math]::Round($variants.monolith.MeanRequestsPerSecond,2)
    MicroservicesQps=[math]::Round($variants.microservices.MeanRequestsPerSecond,2)
    MonolithAverageMs=[math]::Round($variants.monolith.MeanAverageMs,2)
    MicroservicesAverageMs=[math]::Round($variants.microservices.MeanAverageMs,2)
    MonolithP95Ms=[math]::Round($variants.monolith.MeanOfRunP95Ms,2)
    MicroservicesP95Ms=[math]::Round($variants.microservices.MeanOfRunP95Ms,2)
}
