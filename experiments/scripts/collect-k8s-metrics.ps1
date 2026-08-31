#requires -Version 7.2
[CmdletBinding()]
param(
    [string]$Namespace = 'travel-platform',
    [string]$WorkloadName = 'backend',
    [string]$Context = 'kind-travel-platform',
    [ValidateRange(5, 7200)][int]$DurationSeconds = 180,
    [ValidateRange(1, 30)][int]$IntervalSeconds = 5,
    [string]$OutputFile
)
$ErrorActionPreference = 'Stop'
Import-Module (Join-Path $PSScriptRoot 'LabEvidence.psm1') -Force
if (-not $OutputFile) {
    $OutputFile = Join-Path $PSScriptRoot "../results/k8s-metrics-$(Get-Date -Format 'yyyyMMdd-HHmmss')-$([guid]::NewGuid().ToString('N').Substring(0,6)).csv"
}
$OutputFile = [IO.Path]::GetFullPath($OutputFile)
if (Test-Path -LiteralPath $OutputFile) { throw 'Refusing to mix samples with an existing CSV.' }
New-Item -ItemType Directory -Path (Split-Path -Parent $OutputFile) -Force | Out-Null
$deadline = (Get-Date).AddSeconds($DurationSeconds)
$rowCount = 0
while ((Get-Date) -lt $deadline) {
    $timestamp = (Get-Date).ToString('o')
    $pods = ((Invoke-LabKube $Context $Namespace @('get','pods','-o','json')) -join "`n") | ConvertFrom-Json
    $replicaSets = ((Invoke-LabKube $Context $Namespace @('get','replicasets','-o','json')) -join "`n") | ConvertFrom-Json
    $deployment = ((Invoke-LabKube $Context $Namespace @('get','deployment',$WorkloadName,'-o','json')) -join "`n") | ConvertFrom-Json
    $hpas = ((Invoke-LabKube $Context $Namespace @('get','hpa','-o','json')) -join "`n") | ConvertFrom-Json
    # Match the target, not an assumed HPA name. An absent HPA is valid for fixed-replica tests.
    $hpa = @($hpas.items | Where-Object { $_.spec.scaleTargetRef.kind -eq 'Deployment' -and $_.spec.scaleTargetRef.name -eq $WorkloadName })
    if ($hpa.Count -gt 1) { throw 'Multiple HPAs target the observed Deployment.' }
    $topByPod = @{}
    try {
        $top = Invoke-LabKube $Context $Namespace @('top','pods','--no-headers')
        foreach ($line in $top) {
            $columns = $line.Trim() -split '\s+'
            if ($columns.Count -ge 3) { $topByPod[$columns[0]] = @($columns[1],$columns[2]) }
        }
    } catch { Write-Warning 'Metrics unavailable for this sample; missing values remain blank, not zero.' }
    $rows = @(foreach ($pod in $pods.items) {
        $owner = $pod.metadata.ownerReferences | Where-Object controller | Select-Object -First 1
        $kind = $owner.kind
        $name = $owner.name
        if ($kind -eq 'ReplicaSet') {
            $rs = $replicaSets.items | Where-Object { $_.metadata.uid -eq $owner.uid } | Select-Object -First 1
            $parent = $rs.metadata.ownerReferences | Where-Object controller | Select-Object -First 1
            if ($parent) { $kind=$parent.kind; $name=$parent.name }
        }
        $values = $topByPod[$pod.metadata.name]
        $hasMetrics = $null -ne $values
        $ready = @($pod.status.conditions | Where-Object { $_.type -eq 'Ready' -and $_.status -eq 'True' }).Count -gt 0
        [pscustomobject][ordered]@{
            Timestamp=$timestamp; Context=$Context; Namespace=$Namespace
            WorkloadKind=$kind; Workload=$name; Pod=$pod.metadata.name; PodUid=$pod.metadata.uid
            Phase=$pod.status.phase; PodReady=$ready; MetricsAvailable=$hasMetrics
            Cpu=$(if ($hasMetrics) {$values[0]} else {$null})
            Memory=$(if ($hasMetrics) {$values[1]} else {$null})
            CpuMillicores=$(if ($hasMetrics) {Convert-LabCpuMilli $values[0]} else {$null})
            MemoryMi=$(if ($hasMetrics) {Convert-LabMemoryMi $values[1]} else {$null})
            ObservedWorkload=$WorkloadName; ObservedReadyReplicas=[int]$deployment.status.readyReplicas
            ObservedDesiredReplicas=$deployment.spec.replicas; HpaPresent=($hpa.Count -eq 1)
            HpaCurrentReplicas=$(if ($hpa.Count) {$hpa[0].status.currentReplicas} else {$null})
            HpaDesiredReplicas=$(if ($hpa.Count) {$hpa[0].status.desiredReplicas} else {$null})
        }
    })
    if ($rows.Count) {
        # Flush each sample so cancellation preserves evidence already collected.
        $rows | Export-Csv -LiteralPath $OutputFile -NoTypeInformation -Encoding utf8 -Append
        $rowCount += $rows.Count
    }
    $remaining = ($deadline - (Get-Date)).TotalSeconds
    if ($remaining -gt 0) { Start-Sleep -Milliseconds ([int]([math]::Min($IntervalSeconds,$remaining)*1000)) }
}
Write-Host "Saved $rowCount metric rows to $OutputFile"
