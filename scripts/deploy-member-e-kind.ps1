#requires -Version 7.2
[CmdletBinding()]
param(
    [switch]$Apply,
    [string]$Context = 'kind-travel-platform',
    [string]$ResumeDirectory
)
$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$namespace = 'travel-platform-micro-team'
$runKey = 'lab.travelplatform/deployment-run'
if ($Context -ne 'kind-travel-platform') { throw 'This script only operates on the explicitly reviewed local Kind cluster.' }
if (-not $Apply) {
    if ($ResumeDirectory) { throw 'Resume requires -Apply; no cluster change was made.' }
    & (Join-Path $PSScriptRoot 'prepare-member-e-kind.ps1') -Context $Context
    return
}
function Invoke-Kube {
    param([string[]]$Arguments, $InputObject)
    if ($null -ne $InputObject) {
        $json = $InputObject | ConvertTo-Json -Depth 80 -Compress
        $raw = $json | & kubectl --context $Context --namespace $namespace --request-timeout=30s @Arguments
    } else {
        $raw = & kubectl --context $Context --namespace $namespace --request-timeout=30s @Arguments
    }
    if ($LASTEXITCODE -ne 0) { throw "Cluster operation failed: $($Arguments -join ' ')" }
    return $raw
}
function Get-Object {
    param([string]$Kind, [string]$Name)
    $raw = Invoke-Kube @('get',$Kind,$Name,'--ignore-not-found','-o','json')
    if ($raw) { return (($raw -join "`n") | ConvertFrom-Json -AsHashtable) }
    return $null
}
function Save-State {
    param([string]$Phase)
    $state.Phase = $Phase
    $state.UpdatedAt = (Get-Date).ToString('o')
    $state | ConvertTo-Json -Depth 15 | Set-Content -LiteralPath (Join-Path $directory 'deployment-state.json') -Encoding utf8
    Write-Host "Deployment phase: $Phase"
}
function Get-ProtectedSnapshot {
    # Old namespaces are read-only throughout this script. Ignore Pod UIDs/replica counts,
    # which their existing controllers may legitimately change without our involvement.
    return @(foreach ($protected in @('travel-platform','travel-platform-micro')) {
        $raw = & kubectl --context $Context --namespace $protected --request-timeout=15s get deployment,statefulset,pvc -o json
        if ($LASTEXITCODE -ne 0) { throw 'Cannot capture protected environments.' }
        foreach ($item in (($raw -join "`n") | ConvertFrom-Json).items) {
            [ordered]@{
                Namespace=$protected; Kind=$item.kind; Name=$item.metadata.name; Uid=$item.metadata.uid
                Images=@($item.spec.template.spec.containers.image); Volume=$item.spec.volumeName
            }
        }
    })
}
$revision = (& git -C $root rev-parse HEAD)
& git -C $root diff --quiet HEAD -- travel-platform-microservices travel-platform-server travel-platform-web
if ($LASTEXITCODE -ne 0) { throw 'Business source changed; obtain a confirmed baseline before deployment.' }
$untracked = @(& git -C $root ls-files --others --exclude-standard -- travel-platform-microservices travel-platform-server travel-platform-web)
if ($LASTEXITCODE -ne 0 -or $untracked.Count) { throw 'Untracked business files would make the baseline ambiguous.' }
if ($ResumeDirectory) {
    $directory = (Resolve-Path -LiteralPath $ResumeDirectory).Path
    $allowed = [IO.Path]::GetFullPath((Join-Path $root 'artifacts/member-e')) + [IO.Path]::DirectorySeparatorChar
    if (-not $directory.StartsWith($allowed,[StringComparison]::OrdinalIgnoreCase)) { throw 'Resume evidence must be inside this repository artifacts/member-e directory.' }
    $state = Get-Content -Raw -LiteralPath (Join-Path $directory 'deployment-state.json') | ConvertFrom-Json -AsHashtable
    if ($state.Context -ne $Context -or $state.Namespace -ne $namespace -or $state.SourceRevision -ne $revision -or -not $state.RunId) { throw 'Resume identity mismatch.' }
    if ($state.Phase -eq 'Complete') { throw 'Deployment already completed. This is not a general upgrade/reinitialize command.' }
} else {
    $prepared = & (Join-Path $PSScriptRoot 'prepare-member-e-kind.ps1') -Context $Context
    $directory = $prepared.EvidenceDirectory
    $state = [ordered]@{
        RunId=[guid]::NewGuid().ToString(); Context=$Context; Namespace=$namespace
        SourceRevision=$revision; StartedAt=(Get-Date).ToString('o'); NamespaceUid=$null; SecretUid=$null
        BundleSha256=(Get-FileHash -LiteralPath (Join-Path $directory 'rendered.json') -Algorithm SHA256).Hash
        ProtectedBefore=@(Get-ProtectedSnapshot)
    }
    Save-State 'Prepared'
}
$manifestPath = Join-Path $directory 'rendered.json'
if ((Get-FileHash -LiteralPath $manifestPath -Algorithm SHA256).Hash -ne $state.BundleSha256) { throw 'Prepared bundle changed; refusing resume.' }
$bundle = Get-Content -Raw -LiteralPath $manifestPath | ConvertFrom-Json -AsHashtable
Import-Module (Join-Path $PSScriptRoot 'member-e/MemberEManifest.psm1') -Force
$checks = @(Test-MemberEManifest -Bundle $bundle -Revision $revision)
$allowedKinds = @('Namespace','ConfigMap','Service','PersistentVolumeClaim','StatefulSet','Deployment','Job','HorizontalPodAutoscaler')
if (@($bundle.items | Where-Object { $_.kind -notin $allowedKinds }).Count) { throw 'Unexpected resource type in deployment plan.' }
foreach ($kind in @('StatefulSet','Job','HorizontalPodAutoscaler')) {
    if (@($bundle.items | Where-Object kind -eq $kind).Count -ne 1) { throw "Unexpected $kind resource count." }
}
# Validate the exact Kind node before relying on its preloaded images. No Docker Hub
# fallback, TLS bypass, mutable business tag, or automatic rebuild is performed here.
$nodes = ((Invoke-Kube @('get','nodes','-o','json')) -join "`n") | ConvertFrom-Json
if (@($nodes.items).Count -ne 1 -or $nodes.items[0].metadata.name -ne 'travel-platform-control-plane' -or $nodes.items[0].metadata.labels.'kubernetes.io/arch' -ne 'amd64') { throw 'Unexpected node topology.' }
if (-not @($nodes.items[0].status.conditions | Where-Object { $_.type -eq 'Ready' -and $_.status -eq 'True' }).Count) { throw 'Node is not Ready.' }
$nodeLabel = & docker inspect travel-platform-control-plane --format '{{ index .Config.Labels "io.x-k8s.kind.cluster" }}'
if ($LASTEXITCODE -ne 0 -or $nodeLabel -ne 'travel-platform') { throw 'Docker node is not the expected Kind cluster.' }
$cached = @(& docker exec travel-platform-control-plane ctr --namespace k8s.io images ls -q)
if ($LASTEXITCODE -ne 0) { throw 'Cannot inspect node image cache.' }
$requiredImages = @($bundle.items | Where-Object { $_.kind -in @('Deployment','StatefulSet','Job') } | ForEach-Object { $_.spec.template.spec.containers[0].image })
foreach ($reference in $requiredImages) {
    $qualified = if ($reference -notmatch '/') { "docker.io/library/$reference" } else { $reference }
    if ($qualified -notin $cached) { throw "Load this image into the Kind node first: $qualified" }
}
$state.Images = $requiredImages
Save-State 'ImagesVerified'
try {
    $ns = Get-Object namespace $namespace
    if (-not $ns) {
        if ($state.NamespaceUid) { throw 'Previously created namespace disappeared. Do not silently create a new database.' }
        $nsSpec = $bundle.items | Where-Object kind -eq Namespace | Select-Object -First 1
        $nsSpec.metadata.annotations[$runKey] = $state.RunId
        Invoke-Kube @('create','-f','-') $nsSpec | Out-Host
        $ns = Get-Object namespace $namespace
    }
    if ($ns.metadata.annotations[$runKey] -ne $state.RunId -or ($state.NamespaceUid -and $state.NamespaceUid -ne $ns.metadata.uid)) { throw 'Existing namespace belongs to another deployment; no overwrite allowed.' }
    $state.NamespaceUid = $ns.metadata.uid
    Save-State 'NamespaceCreated'
    $secret = Get-Object secret 'travel-platform-secrets'
    if (-not $secret) {
        if ($state.SecretUid) { throw 'Credentials disappeared. Do not rotate them under an initialized database.' }
        # Independent credentials go to the API through stdin, never a command-line
        # argument, generated file, transcript, or kubectl last-applied annotation.
        $secretSpec = @{
            apiVersion='v1'; kind='Secret'; type='Opaque'
            metadata=@{name='travel-platform-secrets';namespace=$namespace;annotations=@{$runKey=$state.RunId}}
            stringData=@{
                'mysql-root-password'=[Convert]::ToHexString([Security.Cryptography.RandomNumberGenerator]::GetBytes(32))
                'jwt-secret'=[Convert]::ToHexString([Security.Cryptography.RandomNumberGenerator]::GetBytes(32))
            }
        }
        Invoke-Kube @('create','-f','-') $secretSpec | Out-Host
        $secretSpec = $null
        $secret = Get-Object secret 'travel-platform-secrets'
    }
    if ($secret.metadata.annotations[$runKey] -ne $state.RunId -or ($state.SecretUid -and $state.SecretUid -ne $secret.metadata.uid)) { throw 'Credential ownership mismatch.' }
    $state.SecretUid = $secret.metadata.uid
    $secret = $null
    Save-State 'CredentialsCreated'
    $infrastructure = @($bundle.items | Where-Object { $_.kind -notin @('Namespace','Deployment','Job','HorizontalPodAutoscaler') })
    Invoke-Kube @('apply','-f','-') @{apiVersion='v1';kind='List';items=$infrastructure} | Out-Host
    Save-State 'WaitingForMySQL'
    # The outer command runner yields while rollout waits; all waits are bounded.
    Invoke-Kube @('rollout','status','statefulset/mysql','--timeout=300s') | Out-Host
    $job = Get-Object job 'db-init'
    if (-not $job) {
        if ($state.JobUid) { throw 'Initializer disappeared; never rerun seed SQL automatically.' }
        $jobSpec = $bundle.items | Where-Object kind -eq Job | Select-Object -First 1
        $jobSpec.metadata.annotations[$runKey] = $state.RunId
        Invoke-Kube @('create','-f','-') $jobSpec | Out-Host
        $job = Get-Object job 'db-init'
    }
    if ($job.metadata.annotations[$runKey] -ne $state.RunId -or ($state.JobUid -and $state.JobUid -ne $job.metadata.uid)) { throw 'Initializer identity mismatch.' }
    $state.JobUid = $job.metadata.uid
    Save-State 'WaitingForInitialization'
    $deadline = (Get-Date).AddSeconds(650)
    while ($true) {
        $job = Get-Object job 'db-init'
        if (-not $job -or $job.metadata.uid -ne $state.JobUid) { throw 'Initializer changed during deployment.' }
        if ($job.status.failed -or @($job.status.conditions | Where-Object { $_.type -eq 'Failed' -and $_.status -eq 'True' }).Count) { throw 'Database initializer failed. Preserve its logs/data and inspect; this script will not retry SQL.' }
        if (@($job.status.conditions | Where-Object { $_.type -eq 'Complete' -and $_.status -eq 'True' }).Count) { break }
        if ((Get-Date) -gt $deadline) { throw 'Timed out waiting for database initialization.' }
        Start-Sleep -Seconds 3
    }
    Invoke-Kube @('logs','job/db-init') | Set-Content -LiteralPath (Join-Path $directory 'db-init.log') -Encoding utf8
    Save-State 'DatabaseInitialized'
    $applications = @($bundle.items | Where-Object kind -eq Deployment)
    Invoke-Kube @('apply','-f','-') @{apiVersion='v1';kind='List';items=$applications} | Out-Host
    Save-State 'WaitingForApplications'
    foreach ($deployment in $applications) {
        Invoke-Kube @('rollout','status',"deployment/$($deployment.metadata.name)",'--timeout=300s') | Out-Host
    }
    $autoscaling = @($bundle.items | Where-Object kind -eq HorizontalPodAutoscaler)
    Invoke-Kube @('apply','-f','-') @{apiVersion='v1';kind='List';items=$autoscaling} | Out-Host
    Invoke-Kube @('get','deployment,statefulset,pod,pvc,hpa','-o','json') | Set-Content -LiteralPath (Join-Path $directory 'deployed-resources.json') -Encoding utf8
    $state.ProtectedAfter = @(Get-ProtectedSnapshot)
    $before = $state.ProtectedBefore | ConvertTo-Json -Depth 10 -Compress
    $after = $state.ProtectedAfter | ConvertTo-Json -Depth 10 -Compress
    $state.ProtectedResourcesUnchanged = ($before -eq $after)
    if (-not $state.ProtectedResourcesUnchanged) { throw 'Protected environment identity/configuration changed; review before further experiments.' }
    Save-State 'Complete'
    [pscustomobject]@{Deployed=$true;Namespace=$namespace;SourceRevision=$revision;EvidenceDirectory=$directory;ProtectedResourcesUnchanged=$true}
} catch {
    $state.Failure = $_.Exception.Message
    $state.FailedPhase = $state.Phase
    Save-State 'NeedsInspection'
    Write-Warning "No namespace, credential, PVC, or database was deleted. Evidence: $directory"
    throw
}
