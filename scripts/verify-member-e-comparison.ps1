#requires -Version 7.2
[CmdletBinding()]
param([Parameter(Mandatory)][string]$PreparedDirectory)
$ErrorActionPreference='Stop'
$directory=(Resolve-Path -LiteralPath $PreparedDirectory).Path
$prepared=Get-Content -Raw (Join-Path $directory 'prepared.json') | ConvertFrom-Json -AsHashtable
$protocol=Get-Content -Raw (Join-Path $directory 'protocol.json') | ConvertFrom-Json -AsHashtable
if (-not $prepared.ReadyForDeployment) { throw 'The prepared pair was not backed by tested images.' }
Import-Module (Join-Path $PSScriptRoot 'member-e/MemberEComparison.psm1') -Force
$forwards=@()
try {
    foreach ($variant in @('monolith','microservices')) {
        $namespace=$protocol.namespaces[$variant];$port=[int]$protocol.ports[$variant]
        $listener=[Net.NetworkInformation.IPGlobalProperties]::GetIPGlobalProperties().GetActiveTcpListeners() | Where-Object Port -eq $port
        if ($listener) { throw "Local port $port is already in use." }
        $stdout=Join-Path $directory "$variant-port-forward.stdout.log";$stderr=Join-Path $directory "$variant-port-forward.stderr.log"
        $process=Start-Process kubectl -WindowStyle Hidden -PassThru -RedirectStandardOutput $stdout -RedirectStandardError $stderr -ArgumentList @('--context',$protocol.context,'-n',$namespace,'port-forward','service/frontend',"${port}:80",'--address=127.0.0.1')
        $forwards+=@{Variant=$variant;Process=$process;Port=$port}
    }
    $responses=@{};$writes=@{}
    foreach ($forward in $forwards) {
        $deadline=(Get-Date).AddSeconds(60);$url="http://127.0.0.1:$($forward.Port)$($protocol.datasetCheckPath)"
        do {
            if ($forward.Process.HasExited) { throw "$($forward.Variant) port-forward exited early." }
            try { $response=Invoke-WebRequest -Uri $url -TimeoutSec 5;break } catch { if ((Get-Date) -gt $deadline) {throw};Start-Sleep -Seconds 1 }
        } while ($true)
        $body=$response.Content | ConvertFrom-Json -AsHashtable
        if ($response.StatusCode -ne 200 -or $body.code -ne 200 -or -not $body.data.records.Count) { throw "$($forward.Variant) dataset query failed." }
        $responses[$forward.Variant]=@{HttpStatus=$response.StatusCode;BusinessCode=$body.code;Records=$body.data.records.Count;Total=$body.data.total;DataSha256=(Get-ComparisonHash $body.data)}
        $write=Invoke-WebRequest -Method Post -Uri "http://127.0.0.1:$($forward.Port)/api/public/flights" -SkipHttpErrorCheck -TimeoutSec 5
        if ($write.StatusCode -ne 403) { throw "$($forward.Variant) frontend did not reject a write request." }
        $writes[$forward.Variant]=@{Method='POST';HttpStatus=$write.StatusCode;Rejected=$true}
    }
    if ($responses.monolith.DataSha256 -ne $responses.microservices.DataSha256 -or $responses.monolith.Total -ne $responses.microservices.Total) { throw 'The two flight-query datasets differ.' }
    $clusters=@{}
    foreach ($variant in @('monolith','microservices')) {
        $namespace=$protocol.namespaces[$variant]
        $raw=& kubectl --context $protocol.context -n $namespace get deployment,statefulset,hpa,pod,pvc,job -o json
        if ($LASTEXITCODE -ne 0) { throw "Cannot inspect $namespace." }
        $items=@(($raw | ConvertFrom-Json -AsHashtable).items)
        $workloads=@($items | Where-Object {$_.kind -in @('Deployment','StatefulSet')})
        $pods=@($items | Where-Object kind -eq Pod)
        $budget=Get-ComparisonBudget $items
        if (@($workloads | Where-Object {$_.spec.replicas -ne 1 -or $_.status.readyReplicas -ne 1}).Count -or @($items | Where-Object kind -eq HorizontalPodAutoscaler).Count -or @($pods | Where-Object {$_.status.phase -notin @('Running','Succeeded')}).Count) { throw "$namespace is not a fixed, ready baseline." }
        foreach ($key in $protocol.budgetPerVariant.Keys) { if ($budget[$key] -ne $protocol.budgetPerVariant[$key]) { throw "$namespace runtime budget mismatch: $key" } }
        $clusters[$variant]=@{Namespace=$namespace;Workloads=$workloads.Count;RunningOrCompletedPods=$pods.Count;BoundClaims=@($items | Where-Object {$_.kind -eq 'PersistentVolumeClaim' -and $_.status.phase -eq 'Bound'}).Count;HpaCount=0;Budget=$budget;Images=@($workloads | ForEach-Object {$_.spec.template.spec.containers[0].image} | Sort-Object)}
    }
    $nodeTop=& kubectl --context $protocol.context top node --no-headers
    $report=[ordered]@{CheckedAt=(Get-Date).ToString('o');SourceRevision=$protocol.sourceRevision;DatasetPath=$protocol.datasetCheckPath;Responses=$responses;DatasetIdentical=$true;WriteRequests=$writes;Clusters=$clusters;BudgetsEqual=((Get-ComparisonHash $clusters.monolith.Budget) -eq (Get-ComparisonHash $clusters.microservices.Budget));FixedReplicas=$true;HpaEnabled=$false;NodeTop=$nodeTop;FormalPerformanceRuns=0;SecretValuesIncluded=$false}
    $report | ConvertTo-Json -Depth 15 | Set-Content -LiteralPath (Join-Path $directory 'runtime-verification.json') -Encoding utf8
    [pscustomobject]@{DatasetIdentical=$report.DatasetIdentical;Records=$responses.monolith.Records;Total=$responses.monolith.Total;BudgetsEqual=$report.BudgetsEqual;FixedReplicas=$report.FixedReplicas;WritesRejected=$true;FormalPerformanceRuns=0}
} finally {
    foreach ($forward in $forwards) {
        if (-not $forward.Process.HasExited) { Stop-Process -Id $forward.Process.Id -Force }
    }
}
