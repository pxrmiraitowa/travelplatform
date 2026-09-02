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
function Assert-LabSourceCompatibility {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory)][string]$ProjectRoot,
        [Parameter(Mandatory)][string]$BaselineRevision,
        [string]$RepositoryHead = 'HEAD'
    )
    $reviewedBaseline = 'aece11988e2364289625c0de2c75b18444c55b8d'
    $approvedPath = 'travel-platform-web/vite.config.js'
    $approvedNormalizedSha256 = '10522FA4CFDF9FE712792AC1017408B8F065ABA9FE1B3B76D88D0E0886734C23'
    if ($BaselineRevision -ne $reviewedBaseline) {
        throw 'The requested baseline is not the reviewed member E experiment revision.'
    }
    $runtimePaths = @('travel-platform-microservices','travel-platform-server','travel-platform-web')
    $changes = @(& git -C $ProjectRoot diff --name-only $BaselineRevision $RepositoryHead -- @runtimePaths)
    if ($LASTEXITCODE -ne 0) { throw 'Cannot compare the reviewed application source with the repository head.' }
    $unexpected = @($changes | Where-Object { $_ -ne $approvedPath })
    if ($unexpected.Count -gt 0) {
        throw "Application runtime inputs changed after the reviewed source revision: $($unexpected -join ', ')"
    }
    $approvedDelta = @()
    if ($approvedPath -in $changes) {
        $content = (Get-Content -Raw -LiteralPath (Join-Path $ProjectRoot $approvedPath)) -replace "`r`n","`n"
        $bytes = [Text.Encoding]::UTF8.GetBytes($content)
        $actualHash = [Convert]::ToHexString([Security.Cryptography.SHA256]::HashData($bytes))
        if ($actualHash -ne $approvedNormalizedSha256) {
            throw 'The post-experiment Vite configuration is not the reviewed development-proxy-only change.'
        }
        $approvedDelta = @([ordered]@{
            Path=$approvedPath
            Scope='Vite development-server proxy default only; Docker/Kubernetes production traffic uses Nginx and is unchanged.'
            NormalizedSha256=$actualHash
        })
    }
    $workingChanges = @(& git -C $ProjectRoot diff --name-only HEAD -- @runtimePaths)
    if ($LASTEXITCODE -ne 0 -or $workingChanges.Count -gt 0) {
        throw 'Application runtime inputs have uncommitted changes; refusing an ambiguous experiment baseline.'
    }
    $untracked = @(& git -C $ProjectRoot ls-files --others --exclude-standard -- @runtimePaths)
    if ($LASTEXITCODE -ne 0 -or $untracked.Count -gt 0) {
        throw 'Application runtime inputs contain untracked files; refusing an ambiguous experiment baseline.'
    }
    return [pscustomobject]@{
        BaselineRevision=$BaselineRevision
        RepositoryHead=(& git -C $ProjectRoot rev-parse $RepositoryHead)
        ApprovedNonRuntimeDelta=$approvedDelta
    }
}
Export-ModuleMember -Function Invoke-LabKube,Get-LabSnapshot,Convert-LabCpuMilli,Convert-LabMemoryMi,Get-LabSeconds,Get-LabConfigFingerprint,Assert-LabFixedScale,Assert-LabSourceCompatibility
