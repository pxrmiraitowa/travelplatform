#requires -Version 7.2
# Shared read-only evidence helpers. Never collect Secret objects or container environment values.
function Invoke-LabKube {
    param([string]$Context,[string]$Namespace,[string[]]$Arguments)
    if (-not $Context.StartsWith('kind-')) { throw 'Only an explicitly selected local Kind context is supported.' }
    if ($Namespace -notin @('travel-platform','travel-platform-micro','travel-platform-micro-team','travel-platform-bench-monolith','travel-platform-bench-micro')) { throw 'Namespace is outside the travel-platform lab.' }
    $result = & kubectl --context $Context --request-timeout=10s -n $Namespace @Arguments
    if ($LASTEXITCODE -ne 0) { throw "kubectl failed: $($Arguments -join ' ')" }
    return $result
}
function Get-LabSnapshot {
    param([string]$Context,[string]$Namespace)
    $workloads = ((Invoke-LabKube $Context $Namespace @('get','deployments,statefulsets','-o','json')) -join "`n") | ConvertFrom-Json
    $hpas = ((Invoke-LabKube $Context $Namespace @('get','hpa','-o','json')) -join "`n") | ConvertFrom-Json
    $pods = ((Invoke-LabKube $Context $Namespace @('get','pods','-o','json')) -join "`n") | ConvertFrom-Json
    return [ordered]@{
        At=(Get-Date).ToString('o'); Context=$Context; Namespace=$Namespace
        Workloads=@($workloads.items | Sort-Object kind,{$_.metadata.name} | ForEach-Object {
            [ordered]@{Kind=$_.kind;Name=$_.metadata.name;Uid=$_.metadata.uid;Replicas=$_.spec.replicas;
                Ready=$_.status.readyReplicas;
                Containers=@($_.spec.template.spec.containers | ForEach-Object {
                    [ordered]@{Name=$_.name;Image=$_.image;Resources=$_.resources}
                })}
        })
        Hpas=@($hpas.items | ForEach-Object {
            [ordered]@{Name=$_.metadata.name;Target=$_.spec.scaleTargetRef.name;Min=$_.spec.minReplicas;
                Max=$_.spec.maxReplicas;Metrics=$_.spec.metrics;Behavior=$_.spec.behavior}
        })
        PodImages=@($pods.items | ForEach-Object {
            [ordered]@{Pod=$_.metadata.name;Uid=$_.metadata.uid;Node=$_.spec.nodeName;
                Images=@($_.status.containerStatuses | ForEach-Object {
                    [ordered]@{Name=$_.name;Image=$_.image;ImageId=$_.imageID}
                })}
        })
    }
}
function Convert-LabCpuMilli {
    param([string]$Value)
    if ($Value -notmatch '^(\d+(?:\.\d+)?)(n|u|m)?$') { throw "Unsupported CPU quantity: $Value" }
    $number=[double]::Parse($Matches[1],[Globalization.CultureInfo]::InvariantCulture)
    return $number * $(switch ($Matches[2]) { 'n' {0.000001} 'u' {0.001} 'm' {1} default {1000} })
}
function Convert-LabMemoryMi {
    param([string]$Value)
    if ($Value -cnotmatch '^(\d+(?:\.\d+)?)(Ki|Mi|Gi|Ti|K|M|G|T)?$') { throw "Unsupported memory quantity: $Value" }
    $number=[double]::Parse($Matches[1],[Globalization.CultureInfo]::InvariantCulture)
    return $number * $(switch -CaseSensitive ($Matches[2]) {
        'Ki' {1/1024} 'Mi' {1} 'Gi' {1024} 'Ti' {1048576}
        'K' {1e3/1MB} 'M' {1e6/1MB} 'G' {1e9/1MB} 'T' {1e12/1MB} default {1/1MB}
    })
}
function Get-LabSeconds {
    param([string]$Duration,[switch]$AllowZero)
    if ($Duration -notmatch '^(\d+)(s|m)$') { throw 'Duration must be a whole number followed by s or m.' }
    $seconds=[int]$Matches[1] * $(if ($Matches[2] -eq 'm') {60} else {1})
    if ($seconds -gt 3600 -or $seconds -lt $(if ($AllowZero) {0} else {1})) { throw 'Duration is outside the supported range (up to one hour).' }
    return $seconds
}
function Get-LabConfigFingerprint {
    param($Snapshot,[ValidateSet('fixed','hpa')][string]$ScaleMode)
    $config = [ordered]@{
        Workloads=@($Snapshot.Workloads | ForEach-Object {
            [ordered]@{Kind=$_.Kind;Name=$_.Name;Uid=$_.Uid;Containers=$_.Containers;
                Replicas=$(if ($ScaleMode -eq 'fixed') {$_.Replicas} else {$null})}
        })
        Hpas=@($Snapshot.Hpas | Sort-Object Name)
    }
    $bytes = [Text.Encoding]::UTF8.GetBytes(($config | ConvertTo-Json -Depth 30 -Compress))
    return [Convert]::ToHexString([Security.Cryptography.SHA256]::HashData($bytes))
}
function Assert-LabFixedScale {
    param($Snapshot)
    foreach ($hpa in $Snapshot.Hpas) {
        $target = @($Snapshot.Workloads | Where-Object { $_.Name -eq $hpa.Target -and $_.Kind -eq 'Deployment' })
        if ($target.Count -ne 1 -or $hpa.Min -ne $hpa.Max -or $target[0].Replicas -ne $hpa.Min) {
            throw 'Fixed-replica measurements require no active autoscaling (remove the HPA or pin min=max=current first). No cluster setting was changed.'
        }
    }
}
Export-ModuleMember -Function Invoke-LabKube,Get-LabSnapshot,Convert-LabCpuMilli,Convert-LabMemoryMi,Get-LabSeconds,Get-LabConfigFingerprint,Assert-LabFixedScale
