#requires -Version 7.2
[CmdletBinding()]
param([Parameter(Mandatory)][string]$PreparedDirectory,[switch]$Apply)
$ErrorActionPreference='Stop'
$directory=(Resolve-Path -LiteralPath $PreparedDirectory).Path
$prepared=Get-Content -Raw (Join-Path $directory 'prepared.json') | ConvertFrom-Json -AsHashtable
$protocol=Get-Content -Raw (Join-Path $directory 'protocol.json') | ConvertFrom-Json -AsHashtable
if (-not $prepared.ReadyForDeployment -or $prepared.PlanOnly -or $prepared.SourceRevision -ne $protocol.sourceRevision) { throw 'Only a tested, non-plan comparison bundle can be deployed.' }
foreach ($entry in $prepared.Manifests) {
    if ((Get-FileHash -LiteralPath (Join-Path $directory $entry.File)).Hash -ne $entry.Sha256) { throw "Prepared manifest changed: $($entry.File)" }
}
if (-not $Apply) {
    [pscustomobject]@{WouldCreate=@($protocol.namespaces.Values);Context=$protocol.context;SourceRevision=$protocol.sourceRevision;ClusterChanged=$false;ApplyRequired=$true}
    exit 0
}
$existing=@(& kubectl --context $protocol.context --request-timeout=10s get namespace @($protocol.namespaces.Values) --ignore-not-found -o name)
if ($LASTEXITCODE -ne 0) { throw 'Cannot inspect target namespaces.' }
if ($existing.Count) { throw "A comparison namespace already exists: $($existing -join ', '). Refusing to overwrite or reinitialize it." }
$images=@($prepared.MonolithImage)+@($prepared.MicroserviceImages.Image)
if ($images.Count -ne 7 -or @($images | Sort-Object -Unique).Count -ne 7) { throw 'Expected seven unique tested images.' }
$runId=$prepared.RunId
$node='travel-platform-control-plane'
$PSNativeCommandUseErrorActionPreference=$true
foreach ($image in $images) {
    $null=& docker image inspect $image
    if ($LASTEXITCODE -ne 0) { throw "Local image is missing: $image" }
    # PowerShell 7 preserves native byte streams, so no host/container archive
    # file is needed. This also avoids Docker Desktop 29's silent docker-cp issue.
    & docker save $image | & docker exec -i $node ctr --namespace k8s.io images import --platform linux/amd64 - | Out-Host
    if ($LASTEXITCODE -ne 0) { throw "Cannot import image into the Kind node: $image" }
    $nodeImages=@(& docker exec $node ctr --namespace k8s.io images list -q)
    if ($LASTEXITCODE -ne 0 -or $nodeImages -notcontains "docker.io/library/$image") { throw "Imported image reference was not found: $image" }
}
    $state=[ordered]@{RunId=$runId;StartedAt=(Get-Date).ToString('o');SourceRevision=$protocol.sourceRevision;Context=$protocol.context;Namespaces=@();Images=$images;SecretValuesRecorded=$false;Status='Deploying'}
    foreach ($variant in @('monolith','microservices')) {
        $namespace=$protocol.namespaces[$variant]
        $bundle=Get-Content -Raw (Join-Path $directory "$variant.json") | ConvertFrom-Json -AsHashtable
        $namespaceItem=@($bundle.items | Where-Object kind -eq Namespace)
        if ($namespaceItem.Count -ne 1 -or $namespaceItem[0].metadata.name -ne $namespace) { throw "Invalid Namespace item for $variant." }
        $namespaceItem[0] | ConvertTo-Json -Depth 20 | & kubectl --context $protocol.context apply -f - | Out-Host
        if ($LASTEXITCODE -ne 0) { throw "Cannot create namespace $namespace." }
        $mysqlPassword=[Convert]::ToHexString([Security.Cryptography.RandomNumberGenerator]::GetBytes(32))
        $jwtSecret=[Convert]::ToHexString([Security.Cryptography.RandomNumberGenerator]::GetBytes(64))
        $secret=[ordered]@{apiVersion='v1';kind='Secret';metadata=@{name='travel-platform-secrets';namespace=$namespace;annotations=@{'lab.travelplatform/source-revision'=$protocol.sourceRevision;'lab.travelplatform/benchmark-run'=$runId}};type='Opaque';data=@{'mysql-root-password'=[Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($mysqlPassword));'jwt-secret'=[Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($jwtSecret))}}
        $secret | ConvertTo-Json -Depth 10 -Compress | & kubectl --context $protocol.context apply -f - | Out-Null
        if ($LASTEXITCODE -ne 0) { throw "Cannot create secret in $namespace." }
        $infrastructure=@($bundle.items | Where-Object {$_.kind -in @('ConfigMap','Service','PersistentVolumeClaim','StatefulSet')})
        @{apiVersion='v1';kind='List';items=$infrastructure} | ConvertTo-Json -Depth 80 | & kubectl --context $protocol.context apply -f - | Out-Host
        if ($LASTEXITCODE -ne 0) { throw "Cannot create infrastructure in $namespace." }
        & kubectl --context $protocol.context -n $namespace rollout status statefulset/mysql --timeout=300s | Out-Host
        if ($LASTEXITCODE -ne 0) { throw "MySQL did not become ready in $namespace." }
        $job=@($bundle.items | Where-Object kind -eq Job)
        if ($job.Count -ne 1) { throw "Expected one initializer in $namespace." }
        $job[0] | ConvertTo-Json -Depth 80 | & kubectl --context $protocol.context apply -f - | Out-Host
        if ($LASTEXITCODE -ne 0) { throw "Cannot start initializer in $namespace." }
        & kubectl --context $protocol.context -n $namespace wait --for=condition=complete job/db-init --timeout=600s | Out-Host
        if ($LASTEXITCODE -ne 0) { throw "Database initialization failed in $namespace; keep the evidence and do not retry blindly." }
        $deployments=@($bundle.items | Where-Object kind -eq Deployment)
        @{apiVersion='v1';kind='List';items=$deployments} | ConvertTo-Json -Depth 80 | & kubectl --context $protocol.context apply -f - | Out-Host
        if ($LASTEXITCODE -ne 0) { throw "Cannot create applications in $namespace." }
        foreach ($deployment in $deployments) {
            & kubectl --context $protocol.context -n $namespace rollout status "deployment/$($deployment.metadata.name)" --timeout=300s | Out-Host
            if ($LASTEXITCODE -ne 0) { throw "$namespace/$($deployment.metadata.name) did not become ready." }
        }
        $snapshot=(& kubectl --context $protocol.context -n $namespace get deployment,statefulset,pod,pvc,job -o json) | ConvertFrom-Json -AsHashtable
        if ($LASTEXITCODE -ne 0) { throw "Cannot record $namespace." }
        $state.Namespaces+=@{Variant=$variant;Namespace=$namespace;Resources=@($snapshot.items | ForEach-Object {@{Kind=$_.kind;Name=$_.metadata.name;Uid=$_.metadata.uid;ReadyReplicas=$_.status.readyReplicas;Phase=$_.status.phase;VolumeName=$_.spec.volumeName;Images=@($_.spec.template.spec.containers.image)}})}
    }
    $state.Status='Ready';$state.FinishedAt=(Get-Date).ToString('o')
    $state | ConvertTo-Json -Depth 20 | Set-Content -LiteralPath (Join-Path $directory 'deployment-state.json') -Encoding utf8
[pscustomobject]@{Status=$state.Status;Namespaces=$state.Namespaces.Namespace;ImagesImported=$images.Count;SecretValuesRecorded=$false;Directory=$directory}
