#requires -Version 7.2
[CmdletBinding()]
param([Parameter(Mandatory)][string]$PreparedDirectory)
$ErrorActionPreference='Stop'
$root=(Resolve-Path (Join-Path $PSScriptRoot '..')).Path
Import-Module (Join-Path $PSScriptRoot 'member-e/MemberEManifest.psm1') -Force
$metadata=Get-Content -Raw -LiteralPath (Join-Path $PreparedDirectory 'preflight.json') | ConvertFrom-Json
$json=Get-Content -Raw -LiteralPath (Join-Path $PreparedDirectory 'rendered.json')
$original=$json | ConvertFrom-Json
$positive=@(Test-MemberEManifest $original $metadata.SourceRevision)
$cases=@(
    @{Name='old namespace';Change={param($b) ($b.items | Where-Object kind -eq Deployment | Select-Object -First 1).metadata.namespace='travel-platform'}},
    @{Name='unversioned image';Change={param($b) ($b.items | Where-Object kind -eq Deployment | Select-Object -First 1).spec.template.spec.containers[0].image='app:latest'}},
    @{Name='missing CPU request';Change={param($b) ($b.items | Where-Object kind -eq Deployment | Select-Object -First 1).spec.template.spec.containers[0].resources.requests.cpu=$null}},
    @{Name='ephemeral MySQL';Change={param($b) ($b.items | Where-Object kind -eq StatefulSet).spec.template.spec.volumes=@([pscustomobject]@{name='mysql-data';emptyDir=@{}})}},
    @{Name='ephemeral product uploads';Change={param($b) ($b.items | Where-Object {$_.kind -eq 'Deployment' -and $_.metadata.name -eq 'product-service'}).spec.template.spec.volumes=@([pscustomobject]@{name='product-uploads';emptyDir=@{}})}},
    @{Name='wrong HPA target';Change={param($b) ($b.items | Where-Object kind -eq HorizontalPodAutoscaler).spec.scaleTargetRef.name='backend'}},
    @{Name='source revision drift';Change={param($b) ($b.items | Where-Object kind -eq Deployment | Select-Object -First 1).metadata.annotations.'lab.travelplatform/source-revision'='other-commit'}},
    @{Name='automatic SQL retry';Change={param($b) ($b.items | Where-Object kind -eq Job).spec.backoffLimit=2}},
    @{Name='duplicate environment name';Change={param($b) $c=($b.items | Where-Object kind -eq Deployment | Select-Object -First 1).spec.template.spec.containers[0]; $c.env += $c.env[0]}},
    @{Name='unreviewed resource kind';Change={param($b) $b.items += [pscustomobject]@{apiVersion='v1';kind='ResourceQuota';metadata=@{name='unreviewed';namespace='travel-platform-micro-team'}}}},
    @{Name='extra SQL initializer';Change={param($b) $job=($b.items | Where-Object kind -eq Job) | ConvertTo-Json -Depth 60 | ConvertFrom-Json; $job.metadata.name='unexpected-init'; $b.items += $job}}
)
$negative=@(foreach ($case in $cases) {
    $copy=$json | ConvertFrom-Json
    & $case.Change $copy
    $message=$null
    try { Test-MemberEManifest $copy $metadata.SourceRevision | Out-Null } catch {$message=$_.Exception.Message}
    if (-not $message) {throw "Unsafe manifest was accepted: $($case.Name)"}
    [pscustomobject]@{Name=$case.Name;Rejected=$true;Reason=$message}
})
$report=[ordered]@{ClusterChanged=$false;PositiveChecks=$positive.Count;NegativeChecks=$negative.Count;NegativeResults=$negative}
$report | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath (Join-Path $PreparedDirectory 'contract-tests.json') -Encoding utf8
Write-Host "$($positive.Count) manifest checks passed; $($negative.Count) unsafe variants rejected. No cluster write."
