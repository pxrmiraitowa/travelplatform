#requires -Version 7.2
[CmdletBinding()]
param(
    [string]$BaseUrl = 'http://localhost:8088',
    [ValidateSet('travel-platform','travel-platform-micro','travel-platform-micro-team')][string]$Namespace = 'travel-platform',
    [ValidateSet('mysql')][string]$StatefulSetName = 'mysql',
    [string]$Context = 'kind-travel-platform',
    [string]$HealthPath = '/api/public/health',
    [string]$BusinessPath = '/api/public/flights',
    [ValidateRange(10,300)][int]$RecoveryTimeoutSeconds = 180,
    [string]$OutputFile,
    [switch]$AllowDisruption
)
$ErrorActionPreference = 'Stop'
# All opt-in and target checks precede both HTTP and Kubernetes requests.
if (-not $AllowDisruption) { throw 'This experiment stops the local MySQL temporarily. Explicit -AllowDisruption is required.' }
Import-Module (Join-Path $PSScriptRoot 'LabHttp.psm1') -Force
Import-Module (Join-Path $PSScriptRoot 'LabEvidence.psm1') -Force
Assert-LabUrl $BaseUrl
if (([uri]$BaseUrl).AbsolutePath -ne '/' -or -not $Context.StartsWith('kind-')) { throw 'Use a local origin and an explicitly selected Kind context.' }
foreach ($path in @($HealthPath,$BusinessPath)) {
    if (-not $path.StartsWith('/api/public/')) { throw 'Only public-read API probes are supported.' }
}
if (-not $OutputFile) {
    $OutputFile=Join-Path $PSScriptRoot "../results/faults/$(Get-Date -Format 'yyyyMMdd-HHmmss')-$([guid]::NewGuid().ToString('N').Substring(0,6))-mysql-dependency.json"
}
$OutputFile=[IO.Path]::GetFullPath($OutputFile)
if (Test-Path -LiteralPath $OutputFile) { throw 'Refusing to overwrite existing fault evidence.' }
New-Item -ItemType Directory -Path (Split-Path -Parent $OutputFile) -Force | Out-Null
function Read-StatefulSet {
    return (((Invoke-LabKube $Context $Namespace @('get','statefulset',$StatefulSetName,'-o','json')) -join "`n") | ConvertFrom-Json)
}
function Read-OwnedPods {
    $list=((Invoke-LabKube $Context $Namespace @('get','pods','-o','json')) -join "`n") | ConvertFrom-Json
    return @($list.items | Where-Object { @($_.metadata.ownerReferences | Where-Object { $_.controller -and $_.uid -eq $result.OriginalUid }).Count -gt 0 })
}
function Invoke-Probe {
    param([string]$Path)
    try {
        $response=Invoke-LabRequest -BaseUrl $BaseUrl -Path $Path -TimeoutSeconds 8
        return [ordered]@{Path=$Path;HttpStatus=$response.HttpStatus;ApplicationCode=$response.Code;DurationMs=$response.ElapsedMs;
            Succeeded=($response.HttpStatus -eq 200 -and $response.Code -eq 200)}
    } catch { return [ordered]@{Path=$Path;Succeeded=$false;Error=$_.Exception.Message} }
}
function Read-Probes {
    return [ordered]@{Health=(Invoke-Probe $HealthPath);Business=(Invoke-Probe $BusinessPath)}
}
function Test-PvcIdentity {
    foreach ($saved in $result.Pvcs) {
        $pvc=((Invoke-LabKube $Context $Namespace @('get','pvc',$saved.Name,'-o','json')) -join "`n") | ConvertFrom-Json
        if ($pvc.metadata.uid -ne $saved.Uid -or $pvc.metadata.deletionTimestamp -or $pvc.status.phase -ne 'Bound') { throw "PVC identity/state changed: $($saved.Name). Manual inspection required." }
    }
}
$result=[ordered]@{
    StartedAt=(Get-Date).ToString('o');FinishedAt=$null;Context=$Context;Namespace=$Namespace;StatefulSet=$StatefulSetName
    OriginalUid=$null;OriginalReplicas=$null;Pvcs=@();Before=$null;During=$null;After=$null
    ScaleAttempted=$false;FaultObserved=$false;MysqlRestored=$false;PvcIdentityPreserved=$false
    Error=$null;RestoreError=$null;ManualRecovery=$null
}
try {
    $initial=Read-StatefulSet
    $result.OriginalUid=$initial.metadata.uid
    $result.OriginalReplicas=[int]$initial.spec.replicas
    $result.ManualRecovery="First verify StatefulSet UID '$($result.OriginalUid)' and PVC identities. Only then: kubectl --context $Context -n $Namespace scale statefulset/$StatefulSetName --replicas=$($result.OriginalReplicas)"
    if ($initial.metadata.deletionTimestamp -or $result.OriginalReplicas -lt 1 -or $initial.status.readyReplicas -ne $result.OriginalReplicas) { throw 'MySQL must be fully ready before fault injection.' }
    if ($initial.spec.persistentVolumeClaimRetentionPolicy.whenScaled -eq 'Delete') { throw 'Scaling down would delete PVCs; this fault experiment refuses that retention policy.' }
    $pods=@(Read-OwnedPods)
    if ($pods.Count -ne $result.OriginalReplicas) { throw 'Unexpected MySQL Pod count; wait until the StatefulSet is stable.' }
    $claims=@($pods.spec.volumes.persistentVolumeClaim.claimName | Where-Object { $_ } | Sort-Object -Unique)
    if (-not $claims.Count) { throw 'No persistent volume claim found. Refusing to stop an ephemeral database.' }
    $result.Pvcs=@(foreach ($claim in $claims) {
        $pvc=((Invoke-LabKube $Context $Namespace @('get','pvc',$claim,'-o','json')) -join "`n") | ConvertFrom-Json
        if ($pvc.status.phase -ne 'Bound' -or $pvc.metadata.deletionTimestamp) { throw 'Database PVC is not stable and Bound.' }
        [ordered]@{Name=$claim;Uid=$pvc.metadata.uid}
    })
    $result.Before=Read-Probes
    if (-not $result.Before.Health.Succeeded -or -not $result.Before.Business.Succeeded) { throw 'Baseline probes failed. MySQL has NOT been stopped.' }
    # If the API response is lost after applying the change, finally still inspects the actual state.
    $result.ScaleAttempted=$true
    Invoke-LabKube $Context $Namespace @('scale','statefulset',$StatefulSetName,'--replicas=0',
        "--current-replicas=$($result.OriginalReplicas)","--resource-version=$($initial.metadata.resourceVersion)") | Out-Host
    $stopDeadline=(Get-Date).AddSeconds(90)
    while (@(Read-OwnedPods).Count -gt 0) {
        if ((Get-Date) -ge $stopDeadline) { throw 'MySQL Pods did not terminate in time.' }
        Start-Sleep -Seconds 2
    }
    $result.During=Read-Probes
    $result.FaultObserved=-not $result.During.Business.Succeeded
    if (-not $result.FaultObserved) { throw 'Business API still succeeded with MySQL stopped. Check caching and whether this URL points at the selected namespace; do not claim the fault was verified.' }
} catch {
    $result.Error=$_.Exception.Message
} finally {
    if ($result.ScaleAttempted) {
        try {
            $current=Read-StatefulSet
            if ($current.metadata.uid -ne $result.OriginalUid -or $current.metadata.deletionTimestamp) { throw 'StatefulSet was replaced/deleted. Refusing to modify the new object.' }
            Test-PvcIdentity
            if ($current.spec.replicas -eq 0) {
                Invoke-LabKube $Context $Namespace @('scale','statefulset',$StatefulSetName,
                    "--replicas=$($result.OriginalReplicas)",'--current-replicas=0',"--resource-version=$($current.metadata.resourceVersion)") | Out-Host
            } elseif ($current.spec.replicas -ne $result.OriginalReplicas) {
                throw 'Replica count was changed by another actor. Refusing to overwrite their setting.'
            }
            $deadline=(Get-Date).AddSeconds($RecoveryTimeoutSeconds)
            do {
                $current=Read-StatefulSet
                if ($current.metadata.uid -ne $result.OriginalUid -or $current.spec.replicas -ne $result.OriginalReplicas) { throw 'StatefulSet identity or desired replicas changed during recovery.' }
                if ($current.status.readyReplicas -eq $result.OriginalReplicas) {
                    $result.After=Read-Probes
                    if ($result.After.Health.Succeeded -and $result.After.Business.Succeeded) {
                        Test-PvcIdentity
                        $result.PvcIdentityPreserved=$true
                        $result.MysqlRestored=$true
                        break
                    }
                }
                Start-Sleep -Seconds 2
            } while ((Get-Date) -lt $deadline)
            if (-not $result.MysqlRestored) { throw 'MySQL desired replicas were restored, but readiness/business recovery did not finish before the deadline.' }
        } catch { $result.RestoreError=$_.Exception.Message }
    }
    $result.FinishedAt=(Get-Date).ToString('o')
    $result | ConvertTo-Json -Depth 12 | Set-Content -LiteralPath $OutputFile -Encoding utf8
    Write-Host "Saved fault evidence: $OutputFile"
}
if ($result.Error -or $result.RestoreError -or -not $result.FaultObserved -or -not $result.MysqlRestored) {
    throw "Experiment not verified. Error=$($result.Error); RestoreError=$($result.RestoreError). Inspect $OutputFile before any manual action."
}
Write-Host 'Verified: healthy baseline, business failure during MySQL outage, original replicas and PVC identities preserved, business recovery.'
