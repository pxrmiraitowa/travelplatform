#requires -Version 7.2
[CmdletBinding()]
param([string]$Context='kind-travel-platform')
$ErrorActionPreference='Stop'
if (-not $Context.StartsWith('kind-')) { throw 'Only an explicit local Kind context is supported.' }
$root=(Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$namespace='travel-platform-micro-team'
$revision=(& git -C $root rev-parse HEAD)
if ($LASTEXITCODE -ne 0 -or $revision -notmatch '^[a-f0-9]{40}$') { throw 'Cannot resolve source revision.' }
# E adds deployment files only. Refuse silent changes to the supplied application/SQL baseline.
& git -C $root diff --quiet HEAD -- travel-platform-microservices travel-platform-server travel-platform-web
if ($LASTEXITCODE -ne 0) { throw 'Application or SQL source differs from the selected commit; obtain a confirmed baseline first.' }
$untracked=@(& git -C $root ls-files --others --exclude-standard -- travel-platform-microservices travel-platform-server travel-platform-web)
if ($LASTEXITCODE -ne 0 -or $untracked.Count) { throw 'Untracked application files would make this baseline ambiguous.' }
function Invoke-ReadKube {
    param([string[]]$Arguments)
    if ($Arguments[0] -notin @('get','kustomize','create') -or ($Arguments[0] -eq 'create' -and '--dry-run=client' -notin $Arguments)) { throw 'Preparation permits only read-only/client-render operations.' }
    $raw=& kubectl --context $Context --request-timeout=15s @Arguments
    if ($LASTEXITCODE -ne 0) { throw "Preparation check failed: kubectl $($Arguments -join ' ')" }
    return $raw
}
$nodes=((Invoke-ReadKube @('get','nodes','-o','json')) -join "`n") | ConvertFrom-Json
if (@($nodes.items).Count -ne 1 -or $nodes.items[0].metadata.labels.'kubernetes.io/arch' -ne 'amd64') { throw 'This local-path/RWO overlay is for one amd64 Kind node only.' }
$ready=@($nodes.items[0].status.conditions | Where-Object {$_.type -eq 'Ready' -and $_.status -eq 'True'})
if (-not $ready.Count) { throw 'Kind node is not Ready.' }
Invoke-ReadKube @('get','storageclass','standard') | Out-Host
$existing=Invoke-ReadKube @('get','namespace',$namespace,'--ignore-not-found','-o','name')
if ($existing) { throw 'The team namespace already exists. This fresh-baseline preparation refuses to initialize or overwrite an existing environment. Inspect its data before planning an upgrade.' }
$directory=Join-Path $root "artifacts/member-e/$($revision.Substring(0,7))/prepare-$(Get-Date -Format 'yyyyMMdd-HHmmss')-$([guid]::NewGuid().ToString('N').Substring(0,6))"
New-Item -ItemType Directory -Path $directory -Force | Out-Null
$yamlFile=Join-Path $directory 'rendered.yaml'
Invoke-ReadKube @('kustomize',(Join-Path $root 'deploy/member-e')) | Set-Content -LiteralPath $yamlFile -Encoding utf8
$jsonLines=Invoke-ReadKube @('create','--dry-run=client','-f',$yamlFile,'-o','jsonpath={@}{"\n"}')
$objects=@($jsonLines | Where-Object {$_.Trim()} | ForEach-Object {$_ | ConvertFrom-Json})
$runner=Join-Path $root 'travel-platform-microservices/tools/SqlRunner.java'
$schema=Join-Path $root 'travel-platform-server/src/main/resources/sql/schema.sql'
$data=Join-Path $root 'travel-platform-server/src/main/resources/sql/data-demo.sql'
$seed=((Invoke-ReadKube @('create','configmap','travel-platform-db-init',"-n",$namespace,
    "--from-file=SqlRunner.java=$runner","--from-file=schema.sql=$schema","--from-file=data-demo.sql=$data",'--dry-run=client','-o','json')) -join "`n") | ConvertFrom-Json
$bundle=[ordered]@{apiVersion='v1';kind='List';items=@($objects)+@($seed)}
Import-Module (Join-Path $PSScriptRoot 'member-e/MemberEManifest.psm1') -Force
$checks=@(Test-MemberEManifest -Bundle $bundle -Revision $revision)
$bundle | ConvertTo-Json -Depth 80 | Set-Content -LiteralPath (Join-Path $directory 'rendered.json') -Encoding utf8
# Stage files are preparation artifacts, not an instruction to apply everything together.
foreach ($stage in @('infrastructure','initialization','applications','autoscaling')) {
    $selected=switch ($stage) {
        'infrastructure' {@($bundle.items | Where-Object {$_.kind -notin @('Deployment','Job','HorizontalPodAutoscaler')})}
        'initialization' {@($bundle.items | Where-Object kind -eq Job)}
        'applications' {@($bundle.items | Where-Object kind -eq Deployment)}
        'autoscaling' {@($bundle.items | Where-Object kind -eq HorizontalPodAutoscaler)}
    }
    @{apiVersion='v1';kind='List';items=@($selected)} | ConvertTo-Json -Depth 80 | Set-Content -LiteralPath (Join-Path $directory "$stage.json") -Encoding utf8
}
$summary=[ordered]@{
    PreparedAt=(Get-Date).ToString('o');SourceRevision=$revision;SourceBranch=(& git -C $root branch --show-current)
    Context=$Context;Namespace=$namespace;ClusterChanged=$false;SqlExecuted=$false;SecretValuesWritten=$false
    Checks=$checks;ResourceCount=$bundle.items.Count
    Images=@($objects | Where-Object kind -eq Deployment | ForEach-Object {[ordered]@{Deployment=$_.metadata.name;Image=$_.spec.template.spec.containers[0].image}})
    SeedHashes=@($runner,$schema,$data | ForEach-Object {[ordered]@{File=[IO.Path]::GetRelativePath($root,$_);Sha256=(Get-FileHash -LiteralPath $_ -Algorithm SHA256).Hash}})
    Limitations=@(
        'Client rendering and structural checks only; this does not prove live deployment or HPA behavior.',
        'No Secret is generated. Future deployment must create independent random credentials and wait for MySQL, then initialization, then applications.',
        'The owner initializer normalizes demo credentials and initializes four databases. Never run it against an old lab database.',
        'The owner baseline uses root database access; this overlay does not claim per-service database privilege isolation.',
        'RWO local-path storage supports this one-node experiment, not cross-node shared storage or disaster recovery.'
    )
}
$summary | ConvertTo-Json -Depth 15 | Set-Content -LiteralPath (Join-Path $directory 'preflight.json') -Encoding utf8
[pscustomobject]@{Prepared=$true;ChecksPassed=$checks.Count;ResourceCount=$bundle.items.Count;Namespace=$namespace;ClusterChanged=$false;EvidenceDirectory=$directory}
