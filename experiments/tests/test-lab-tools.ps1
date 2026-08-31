#requires -Version 7.2
# Offline contract tests. kubectl and HTTP are replaced with in-process fakes.
[CmdletBinding()]
param([string]$OutputDirectory)
$ErrorActionPreference='Stop'
$repo=(Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
$scripts=Join-Path $repo 'experiments/scripts'
if (-not $OutputDirectory) { $OutputDirectory=Join-Path $repo "artifacts/member-e/tools-tests/$(Get-Date -Format 'yyyyMMdd-HHmmss')-$([guid]::NewGuid().ToString('N').Substring(0,6))" }
New-Item -ItemType Directory -Path $OutputDirectory -Force | Out-Null
$checks=[Collections.Generic.List[object]]::new()
function Check {
    param([string]$Name,[scriptblock]$Body)
    try { & $Body; $checks.Add([pscustomobject]@{Name=$Name;Passed=$true}); Write-Host "[PASS] $Name" }
    catch { $checks.Add([pscustomobject]@{Name=$Name;Passed=$false;Error=$_.Exception.Message}); Write-Host "[FAIL] $Name : $($_.Exception.Message)" }
}
function Assert-Equal {
    param($Actual,$Expected)
    if ($Actual -cne $Expected) { throw "Expected '$Expected', got '$Actual'." }
}
function Assert-Throws {
    param([scriptblock]$Body,[string]$Pattern='*')
    $caught=$null
    try { & $Body | Out-Null } catch { $caught=$_.Exception.Message }
    if (-not $caught -or $caught -notlike $Pattern) { throw "Expected error matching '$Pattern'; got '$caught'." }
}
foreach ($name in @('kubectl','docker','Invoke-WebRequest')) {
    if (Test-Path "Function:global:$name") { throw "An existing global function named $name would be shadowed; use a fresh PowerShell session." }
}
function global:docker { throw 'OFFLINE TEST: Docker must never be invoked.' }
function global:kubectl {
    $global:LabToolFake.Calls.Add(($args -join ' '))
    $global:LASTEXITCODE=0
    $a=@($args)
    if ($a[0] -ne '--context' -or $a[1] -ne 'kind-travel-platform' -or $a[3] -ne '-n' -or $a[4] -notin @('travel-platform','travel-platform-micro-team')) {
        throw 'All fake Kubernetes calls must pin the exact context and namespace.'
    }
    $op=$a[5]; $type=$a[6]
    if ($op -eq 'scale') {
        $global:LabToolFake.Scales.Add(($a -join ' '))
        if ($global:LabToolFake.Mode -eq 'ScaleRejected' -and $a -contains '--replicas=0') { $global:LASTEXITCODE=1; return }
        $replicaArgument=$a | Where-Object { $_ -match '^--replicas=' } | Select-Object -First 1
        $global:LabToolFake.Replicas=[int]($replicaArgument -replace '^--replicas=','')
        $global:LabToolFake.Injected=$global:LabToolFake.Replicas -eq 0
        if ($global:LabToolFake.Mode -eq 'LostResponse' -and $global:LabToolFake.Injected) { $global:LASTEXITCODE=1 }
        return 'scaled (fake)'
    }
    if ($op -eq 'top') {
        if ($global:LabToolFake.Mode -eq 'MetricsMissing') { $global:LASTEXITCODE=1; return }
        return @('backend-fake 12m 128Mi','frontend-fake 2m 16Mi','mysql-0 7m 256Mi')
    }
    $uid='mysql-uid'
    $replicas=$global:LabToolFake.Replicas
    if ($global:LabToolFake.Injected -and $global:LabToolFake.Mode -eq 'Replaced') { $uid='someone-elses-mysql' }
    if ($global:LabToolFake.Injected -and $global:LabToolFake.Mode -eq 'ConcurrentScale') { $replicas=3 }
    $mysql=@{kind='StatefulSet';metadata=@{name='mysql';uid=$uid;resourceVersion='42'};
        spec=@{replicas=$replicas;persistentVolumeClaimRetentionPolicy=@{whenScaled=$(if ($global:LabToolFake.Mode -eq 'DeletePvc') {'Delete'} else {'Retain'})}};status=@{readyReplicas=$replicas}}
    if ($op -eq 'get' -and $type -eq 'statefulset') { return ($mysql | ConvertTo-Json -Depth 15 -Compress) }
    if ($op -eq 'get' -and $type -eq 'pvc') {
        return (@{metadata=@{name='mysql-data';uid='pvc-uid'};status=@{phase='Bound'}} | ConvertTo-Json -Compress)
    }
    if ($op -eq 'get' -and $type -eq 'pods') {
        $items=@(for ($i=0;$i -lt $global:LabToolFake.Replicas;$i++) {
            @{metadata=@{name="mysql-$i";uid="pod-$i";ownerReferences=@(@{controller=$true;kind='StatefulSet';name='mysql';uid='mysql-uid'})};
                spec=@{volumes=@(@{persistentVolumeClaim=@{claimName='mysql-data'}})};status=@{phase='Running';conditions=@(@{type='Ready';status='True'})}}
        })
        if ($global:LabToolFake.Mode -in @('Metrics','MetricsMissing')) {
            $items+=@(
                @{metadata=@{name='backend-fake';uid='b-pod';ownerReferences=@(@{controller=$true;kind='ReplicaSet';name='backend-rs';uid='b-rs'})};status=@{phase='Running';conditions=@(@{type='Ready';status='True'})}},
                @{metadata=@{name='frontend-fake';uid='f-pod';ownerReferences=@(@{controller=$true;kind='ReplicaSet';name='frontend-rs';uid='f-rs'})};status=@{phase='Running';conditions=@(@{type='Ready';status='True'})}}
            )
        }
        return (@{items=$items} | ConvertTo-Json -Depth 20 -Compress)
    }
    if ($op -eq 'get' -and $type -eq 'replicasets') {
        return (@{items=@(
            @{metadata=@{uid='b-rs';ownerReferences=@(@{controller=$true;kind='Deployment';name='backend'})}},
            @{metadata=@{uid='f-rs';ownerReferences=@(@{controller=$true;kind='Deployment';name='frontend'})}}
        )} | ConvertTo-Json -Depth 12 -Compress)
    }
    if ($op -eq 'get' -and $type -eq 'deployment') { return '{"spec":{"replicas":1},"status":{"readyReplicas":1}}' }
    if ($op -eq 'get' -and $type -eq 'hpa') { return '{"items":[]}' }
    throw "Unexpected fake kubectl invocation: $($args -join ' ')"
}
function global:Invoke-WebRequest {
    param($Uri,$Method,$Headers,$TimeoutSec,$SkipHttpErrorCheck,$MaximumRedirection,$ContentType,$Body)
    $global:LabToolFake.HttpCalls++
    $ok=$global:LabToolFake.Mode -ne 'BaselineFailed'
    if ($global:LabToolFake.Injected -and "$Uri" -like '*/flights' -and $global:LabToolFake.Mode -ne 'FaultNotObserved') { $ok=$false }
    return [pscustomobject]@{StatusCode=$(if ($ok) {200} else {503});Content=$(if ($ok) {'{"code":200,"data":[]}'} else {'{"code":503}'})}
}
function Reset-Fake {
    param([string]$Mode='Normal',[int]$Replicas=1)
    $global:LabToolFake=@{Mode=$Mode;Replicas=$Replicas;Injected=$false;Calls=[Collections.Generic.List[string]]::new();Scales=[Collections.Generic.List[string]]::new();HttpCalls=0}
}
try {
    Reset-Fake
    Import-Module (Join-Path $scripts 'LabEvidence.psm1') -Force
    foreach ($case in @(@('500m',500),@('0.5',500),@('12000000n',12),@('15000u',15))) {
        Check "CPU quantity $($case[0])" { Assert-Equal (Convert-LabCpuMilli $case[0]) $case[1] }
    }
    foreach ($case in @(@('128Mi',128),@('1Gi',1024),@('1024Ki',1),@('1048576',1))) {
        Check "Memory quantity $($case[0])" { Assert-Equal (Convert-LabMemoryMi $case[0]) $case[1] }
    }
    Check 'Missing metrics are not interpreted as zero' { Assert-Throws {Convert-LabCpuMilli '<unknown>'} }
    Check 'Duration validation' { Assert-Equal (Get-LabSeconds '2m') 120; Assert-Equal (Get-LabSeconds '0s' -AllowZero) 0; Assert-Throws {Get-LabSeconds '0s'} }
    Check 'Kubernetes rejects foreign context before invoking CLI' {
        Assert-Throws {Invoke-LabKube production travel-platform @('get','pods')}
        Assert-Equal $global:LabToolFake.Calls.Count 0
    }
    Check 'Fixed baseline rejects active HPA' {
        Assert-Throws {Assert-LabFixedScale @{Workloads=@(@{Name='backend';Kind='Deployment';Replicas=1});Hpas=@(@{Target='backend';Min=1;Max=5})}}
        Assert-LabFixedScale @{Workloads=@(@{Name='backend';Kind='Deployment';Replicas=1});Hpas=@(@{Target='backend';Min=1;Max=1})}
    }
    Check 'Performance rejects remote targets before any calls' {
        Assert-Throws {& (Join-Path $scripts 'run-performance.ps1') -BaseUrl 'https://example.com'}
        Assert-Equal $global:LabToolFake.Calls.Count 0
    }
    Check 'Formal measurements require team confirmation' {
        Assert-Throws {& (Join-Path $scripts 'run-performance.ps1') -Purpose formal} '*TeamBaselineConfirmed*'
        Assert-Equal $global:LabToolFake.Calls.Count 0
    }
    Check 'New team namespace is allowed and stays explicitly selected' {
        Invoke-LabKube kind-travel-platform travel-platform-micro-team @('get','pods','-o','json') | Out-Null
        if ($global:LabToolFake.Calls[0] -notlike '*-n travel-platform-micro-team get pods*') {throw 'Namespace was not pinned.'}
        Reset-Fake
    }
    Check 'Performance rejects mismatched variant and namespace' {
        Assert-Throws {& (Join-Path $scripts 'run-performance.ps1') -Variant monolith -Namespace travel-platform-micro-team} '*disagree*'
        Assert-Equal $global:LabToolFake.Calls.Count 0
    }
    Check 'New deployment rejects foreign context without a cluster call' {
        Assert-Throws {& (Join-Path $repo 'scripts/deploy-member-e-kind.ps1') -Apply -Context production}
        Assert-Equal $global:LabToolFake.Calls.Count 0
    }
    Check 'Live acceptance rejects foreign context before HTTP or Kubernetes calls' {
        Assert-Throws {& (Join-Path $repo 'scripts/test-member-e-live.ps1') -Context production}
        Assert-Equal $global:LabToolFake.Calls.Count 0
        Assert-Equal $global:LabToolFake.HttpCalls 0
    }
    Check 'HPA rehearsal rejects out-of-bounds load before invoking tools' {
        Assert-Throws {& (Join-Path $repo 'scripts/run-member-e-hpa.ps1') -VirtualUsers 61}
        Assert-Equal $global:LabToolFake.Calls.Count 0
    }
    Check 'MySQL fault requires explicit disruption opt-in' {
        Assert-Throws {& (Join-Path $scripts 'run-mysql-fault.ps1')} '*AllowDisruption*'
        Assert-Equal $global:LabToolFake.Calls.Count 0
        Assert-Equal $global:LabToolFake.HttpCalls 0
    }
    foreach ($mode in @('Normal','BaselineFailed','DeletePvc','Replaced','ConcurrentScale','LostResponse','ScaleRejected','FaultNotObserved')) {
        Check "MySQL safety scenario: $mode (fake only)" {
            Reset-Fake $mode 2
            $file=Join-Path $OutputDirectory "$mode-fault-SYNTHETIC.json"
            $invoke={& (Join-Path $scripts 'run-mysql-fault.ps1') -AllowDisruption -OutputFile $file}
            if ($mode -eq 'Normal') { & $invoke } else { Assert-Throws $invoke '*Experiment not verified*' }
            $evidence=Get-Content -Raw $file | ConvertFrom-Json
            $expectedScales=switch ($mode) {'BaselineFailed' {0} 'DeletePvc' {0} 'Replaced' {1} 'ConcurrentScale' {1} 'ScaleRejected' {1} default {2}}
            Assert-Equal $global:LabToolFake.Scales.Count $expectedScales
            if ($expectedScales -eq 2) {
                if ($global:LabToolFake.Scales[1] -notlike '*--replicas=2*--current-replicas=0*--resource-version=42*') { throw 'Original replicas/preconditions were not preserved.' }
                Assert-Equal $evidence.MysqlRestored $true
                Assert-Equal $evidence.PvcIdentityPreserved $true
            }
            if ($mode -eq 'Normal') { Assert-Equal $evidence.FaultObserved $true }
            if ($mode -eq 'BaselineFailed') { Assert-Equal $evidence.ScaleAttempted $false }
            if ($mode -in @('Replaced','ConcurrentScale')) { if (-not $evidence.RestoreError) {throw 'Concurrent change was not reported.'} }
        }
    }
    foreach ($mode in @('Metrics','MetricsMissing')) {
        Check "Metrics attribution and missing HPA: $mode" {
            Reset-Fake $mode
            $csv=Join-Path $OutputDirectory "$mode-SYNTHETIC.csv"
            & (Join-Path $scripts 'collect-k8s-metrics.ps1') -DurationSeconds 5 -IntervalSeconds 5 -OutputFile $csv
            $rows=@(Import-Csv $csv)
            Assert-Equal $rows.Count 3
            Assert-Equal ($rows | Where-Object Pod -eq 'frontend-fake').Workload 'frontend'
            Assert-Equal ($rows | Where-Object Pod -eq 'mysql-0').WorkloadKind 'StatefulSet'
            Assert-Equal ($rows | Where-Object Pod -eq 'backend-fake').HpaPresent 'False'
            if ($mode -eq 'Metrics') { Assert-Equal ($rows | Where-Object Pod -eq 'backend-fake').CpuMillicores '12' }
            else { Assert-Equal ($rows | Where-Object Pod -eq 'backend-fake').CpuMillicores ''; Assert-Equal $rows[0].MetricsAvailable 'False' }
        }
    }
    $batch=Join-Path $OutputDirectory 'SYNTHETIC-batch-not-experiment'
    New-Item -ItemType Directory -Path $batch -Force | Out-Null
    $records=@(for ($i=1;$i -le 3;$i++) {
        $data=@{metrics=@{http_reqs=@{values=@{count=100*$i;rate=10*$i}};http_req_failed=@{values=@{rate=0}};business_errors=@{values=@{rate=0}};http_req_duration=@{values=@{avg=20*$i;'p(95)'=40*$i}}}}
        $file=Join-Path $batch "run-$i-summary.json"
        $data | ConvertTo-Json -Depth 10 | Set-Content $file
        $csv=Join-Path $batch "run-$i-metrics.csv"
        @(
            [pscustomobject]@{Timestamp='2026-08-28T01:00:05Z';Context='kind-travel-platform';Namespace='travel-platform';Phase='Running';PodReady=$true;MetricsAvailable=$true;CpuMillicores=10;MemoryMi=100},
            [pscustomobject]@{Timestamp='2026-08-28T01:00:05Z';Context='kind-travel-platform';Namespace='travel-platform';Phase='Running';PodReady=$true;MetricsAvailable=$true;CpuMillicores=5;MemoryMi=200}
        ) | Export-Csv $csv -NoTypeInformation
        @{Run=$i;Status='Complete';ExitCode=$(if ($i -eq 3) {99} else {0});ThresholdsPassed=($i -ne 3);
            StartedAt='2026-08-28T01:00:00Z';FinishedAt='2026-08-28T01:00:10Z';
            SummaryFile=(Split-Path $file -Leaf);SummarySha256=(Get-FileHash $file).Hash;MetricsFile=(Split-Path $csv -Leaf);MetricsSha256=(Get-FileHash $csv).Hash}
    })
    $meta=@{SchemaVersion=2;SessionId='SYNTHETIC';Status='Complete';Protocol=@{Purpose='rehearsal';PlannedRuns=3;Context='kind-travel-platform';Namespace='travel-platform'};Runs=$records}
    function Save-TestMetadata {$meta | ConvertTo-Json -Depth 12 | Set-Content (Join-Path $batch 'metadata.json')}
    Save-TestMetadata
    Check 'Aggregation uses only this batch; preserves threshold failures and names mean P95 correctly' {
        '{"invalid":"orphan and warm-up must be ignored"}' | Set-Content (Join-Path $batch 'warmup.json')
        & (Join-Path $scripts 'summarize-performance.ps1') -SessionDirectory $batch
        $aggregate=Get-Content -Raw (Join-Path $batch 'aggregate.json') | ConvertFrom-Json
        Assert-Equal $aggregate.RunCount 3
        Assert-Equal $aggregate.MeanOfRuns.RequestsPerSecond 20
        Assert-Equal $aggregate.MeanOfRuns.MeanOfRunP95Ms 80
        Assert-Equal $aggregate.Runs[2].ThresholdsPassed $false
        Assert-Equal $aggregate.Runs[0].MeanNamespaceCpuMillicores 15
        Assert-Equal $aggregate.Runs[0].MeanNamespaceMemoryMi 300
    }
    Check 'Aggregation rejects raw-evidence overwrite' {
        Assert-Throws {& (Join-Path $scripts 'summarize-performance.ps1') -SessionDirectory $batch -OutputFile (Join-Path $batch 'metadata.json')} '*overwrite raw evidence*'
    }
    Check 'Aggregation rejects partial batch' {
        $meta.Status='Failed'; Save-TestMetadata
        Assert-Throws {& (Join-Path $scripts 'summarize-performance.ps1') -SessionDirectory $batch} '*complete schema-v2*'
        $meta.Status='Complete'; Save-TestMetadata
    }
    Check 'Aggregation rejects altered raw data' {
        Add-Content (Join-Path $batch 'run-1-summary.json') ' '
        Assert-Throws {& (Join-Path $scripts 'summarize-performance.ps1') -SessionDirectory $batch} '*Evidence changed*'
        $records[0].SummarySha256=(Get-FileHash (Join-Path $batch 'run-1-summary.json')).Hash
        Save-TestMetadata
    }
    Check 'Aggregation rejects missing metrics rather than manufacturing zeros' {
        $file=Join-Path $batch 'run-1-summary.json'
        $invalid=Get-Content -Raw $file | ConvertFrom-Json
        $invalid.metrics.http_req_duration.values.PSObject.Properties.Remove('p(95)')
        $invalid | ConvertTo-Json -Depth 10 | Set-Content $file
        $records[0].SummarySha256=(Get-FileHash $file).Hash; Save-TestMetadata
        Assert-Throws {& (Join-Path $scripts 'summarize-performance.ps1') -SessionDirectory $batch} '*Missing or non-numeric*'
    }
} finally {
    Remove-Item Function:global:kubectl,Function:global:docker,Function:global:Invoke-WebRequest
    Remove-Variable LabToolFake -Scope Global -ErrorAction SilentlyContinue
    $report=[ordered]@{SyntheticOnly=$true;ClusterWrites=0;Checks=$checks;Passed=@($checks | Where-Object Passed).Count;Failed=@($checks | Where-Object {-not $_.Passed}).Count}
    $report | ConvertTo-Json -Depth 6 | Set-Content (Join-Path $OutputDirectory 'test-report.json')
    Write-Host "Offline test report: $OutputDirectory"
}
if ($report.Failed) { throw "$($report.Failed) offline tests failed." }
