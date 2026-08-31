#requires -Version 7.2
[CmdletBinding()]
param([string]$MonolithBuildDirectory,[string]$MicroserviceBuildDirectory,[switch]$PlanOnly)
$ErrorActionPreference='Stop'
$root=(Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$protocol=Get-Content -Raw (Join-Path $root 'deploy/member-e-benchmark/protocol.json') | ConvertFrom-Json -AsHashtable
if ($PlanOnly) {
    # This mode exercises configuration only. Never manufacture a passing build or
    # deployable image; the output explicitly keeps ReadyForDeployment=false.
    $build=@{Status='NotBuilt-PlanOnly';Image='travel-platform-benchmark-monolith:unbuilt-plan-only';ImageId=$null}
    $microBuild=@{Status='NotBuilt-PlanOnly';Images=@()}
    $microImages=@{}
    foreach ($component in @('user-service','product-service','order-service','content-trip-service','gateway-service','frontend')) { $microImages[$component]="travel-platform-benchmark-${component}:unbuilt-plan-only" }
} else {
    if (-not $MonolithBuildDirectory -or -not $MicroserviceBuildDirectory) { throw 'Supply both successful build directories, or explicitly select -PlanOnly.' }
    $build=Get-Content -Raw (Join-Path $MonolithBuildDirectory 'build.json') | ConvertFrom-Json -AsHashtable
    if ($build.Status -ne 'BuiltAndTested' -or $build.SourceRevision -ne $protocol.sourceRevision -or $build.TestsSkipped -or -not $build.TestReports.Count -or @($build.TestReports | Where-Object {$_.Failures -gt 0 -or $_.Errors -gt 0 -or $_.Skipped -gt 0}).Count) { throw 'A tested, matching monolith build is required. Failed tests cannot become deployment evidence.' }
    $imageId=(& docker image inspect $build.Image --format '{{.Id}}')
    if ($LASTEXITCODE -ne 0 -or $imageId -ne $build.ImageId) { throw 'Recorded monolith image identity does not match the local image.' }
    $microBuild=Get-Content -Raw (Join-Path $MicroserviceBuildDirectory 'build.json') | ConvertFrom-Json -AsHashtable
    if ($microBuild.Status -ne 'BuiltAndTested' -or $microBuild.SourceRevision -ne $protocol.sourceRevision -or $microBuild.TestsSkipped -or -not $microBuild.TestReports.Count -or @($microBuild.TestReports | Where-Object {$_.Failures -gt 0 -or $_.Errors -gt 0 -or $_.Skipped -gt 0}).Count) { throw 'A tested, matching microservice build is required.' }
    $expectedComponents=@('content-trip-service','frontend','gateway-service','order-service','product-service','user-service')
    if ((@($microBuild.Images.Component | Sort-Object) -join ',') -ne ($expectedComponents -join ',')) { throw 'The microservice build record must contain exactly six expected images.' }
    $microImages=@{}
    foreach ($entry in $microBuild.Images) {
        $actual=(& docker image inspect $entry.Image --format '{{.Id}}')
        if ($LASTEXITCODE -ne 0 -or $actual -ne $entry.ImageId) { throw "Recorded image identity mismatch for $($entry.Component)." }
        $microImages[$entry.Component]=$entry.Image
    }
}
if ((& git -C $root rev-parse HEAD) -ne $protocol.sourceRevision) { throw 'Source revision changed.' }
& git -C $root diff --quiet HEAD -- travel-platform-server travel-platform-microservices travel-platform-web
if ($LASTEXITCODE -ne 0) { throw 'Application source or tests changed after the pinned revision.' }
$extra=@(& git -C $root ls-files --others --exclude-standard -- travel-platform-server travel-platform-microservices travel-platform-web)
if ($LASTEXITCODE -ne 0 -or $extra.Count) { throw 'Untracked business source would make the baseline ambiguous.' }
$id=[guid]::NewGuid().ToString('N').Substring(0,8)
$directory=Join-Path $root "artifacts/member-e/$($protocol.sourceRevision.Substring(0,7))/comparison-$(Get-Date -Format yyyyMMdd-HHmmss)-$id"
New-Item -ItemType Directory -Path $directory -Force | Out-Null
$baseYaml=Join-Path $directory 'member-e-base.yaml'
& kubectl --context $protocol.context kustomize (Join-Path $root 'deploy/member-e') | Set-Content -LiteralPath $baseYaml -Encoding utf8
if ($LASTEXITCODE -ne 0) { throw 'Cannot render member-E base.' }
$lines=& kubectl --context $protocol.context create --dry-run=client -f $baseYaml -o 'jsonpath={@}{"\n"}'
if ($LASTEXITCODE -ne 0) { throw 'Cannot parse rendered resources.' }
$baseItems=@($lines | Where-Object {$_.Trim()} | ForEach-Object {$_ | ConvertFrom-Json -AsHashtable})
$runner=Join-Path $root 'travel-platform-microservices/tools/SqlRunner.java'
$schema=Join-Path $root 'travel-platform-server/src/main/resources/sql/schema.sql'
$data=Join-Path $root 'travel-platform-server/src/main/resources/sql/data-demo.sql'
$seed=@{apiVersion='v1';kind='ConfigMap';metadata=@{name='travel-platform-db-init';namespace='travel-platform-micro-team';annotations=@{'lab.travelplatform/source-revision'=$protocol.sourceRevision}};data=@{'SqlRunner.java'=(Get-Content -Raw $runner);'schema.sql'=(Get-Content -Raw $schema);'data-demo.sql'=(Get-Content -Raw $data)}}
$baseBundle=@{apiVersion='v1';kind='List';items=@($baseItems)+@($seed)}
Import-Module (Join-Path $PSScriptRoot 'member-e/MemberEManifest.psm1') -Force
$baseRevision=(@($baseItems | Where-Object kind -eq Namespace)[0]).metadata.annotations.'lab.travelplatform/source-revision'
Test-MemberEManifest $baseBundle $baseRevision | Out-Null
Import-Module (Join-Path $PSScriptRoot 'member-e/MemberEComparison.psm1') -Force
$bundles=[ordered]@{}
$nginx=Get-Content -Raw (Join-Path $root 'travel-platform-web/nginx.conf')
if (-not $nginx.Contains('proxy_pass http://gateway-service:8000;')) { throw 'Owner frontend proxy changed; review the benchmark path.' }
$nginx=$nginx.Replace('location /api/ {',"location /api/ {`n        limit_except GET { deny all; }`n        access_log off;")
foreach ($variant in @('monolith','microservices')) {
    $ns=$protocol.namespaces[$variant]
    $copy=($baseBundle | ConvertTo-Json -Depth 80 | ConvertFrom-Json -AsHashtable)
    $items=@($copy.items | Where-Object kind -ne HorizontalPodAutoscaler)
    foreach ($item in $items) {
        if ($item.kind -eq 'Namespace') { $item.metadata.name=$ns } else { $item.metadata.namespace=$ns }
        $item.metadata.annotations['lab.travelplatform/source-revision']=$protocol.sourceRevision
        $item.metadata.annotations['lab.travelplatform/benchmark-run']=$id
        if ($item.kind -in @('Deployment','StatefulSet','Job')) {
            $item.spec.template.metadata.annotations['lab.travelplatform/source-revision']=$protocol.sourceRevision
            $item.spec.template.metadata.annotations['lab.travelplatform/benchmark-run']=$id
        }
    }
    foreach ($deployment in @($items | Where-Object kind -eq Deployment)) {
        $deployment.spec.replicas=1
        $deployment.spec.template.spec.containers[0].image=$microImages[$deployment.metadata.name]
        $deployment.spec.template.spec.containers[0].imagePullPolicy='Never'
        if ($deployment.metadata.name -ne 'frontend') {
            $deployment.spec.template.spec.containers[0].resources=@{requests=@{cpu=$protocol.javaPerMicroservice.cpuRequest;memory=$protocol.javaPerMicroservice.memoryRequest};limits=@{cpu=$protocol.javaPerMicroservice.cpuLimit;memory=$protocol.javaPerMicroservice.memoryLimit}}
        }
    }
    $runtime=@($items | Where-Object {$_.kind -eq 'ConfigMap' -and $_.metadata.name -eq 'member-e-runtime'})[0]
    $runtime.data.SPRING_APPLICATION_JSON='{"mybatis-plus":{"configuration":{"log-impl":"org.apache.ibatis.logging.nologging.NoLoggingImpl"}}}'
    $variantNginx=$nginx
    if ($variant -eq 'monolith') {
        $backend=@($items | Where-Object {$_.kind -eq 'Deployment' -and $_.metadata.name -eq 'product-service'})[0]
        $backend.metadata.name='backend'
        $backend.metadata.labels.'app.kubernetes.io/name'='backend'
        $backend.spec.selector.matchLabels.'app.kubernetes.io/name'='backend'
        $backend.spec.template.metadata.labels.'app.kubernetes.io/name'='backend'
        $container=$backend.spec.template.spec.containers[0]
        $container.name='backend'; $container.image=$build.Image; $container.imagePullPolicy='Never'
        $container.ports[0].containerPort=8080
        $container.resources=@{requests=@{cpu=$protocol.javaMonolith.cpuRequest;memory=$protocol.javaMonolith.memoryRequest};limits=@{cpu=$protocol.javaMonolith.cpuLimit;memory=$protocol.javaMonolith.memoryLimit}}
        $container.env=@(
            @{name='SERVER_PORT';value='8080'},
            @{name='SPRING_DATASOURCE_URL';value='jdbc:mysql://mysql:3306/travel_product?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false'},
            @{name='SPRING_DATASOURCE_USERNAME';value='root'},
            @{name='SPRING_DATASOURCE_PASSWORD';valueFrom=@{secretKeyRef=@{name='travel-platform-secrets';key='mysql-root-password'}}},
            @{name='SECURITY_JWT_SECRET';valueFrom=@{secretKeyRef=@{name='travel-platform-secrets';key='jwt-secret'}}},
            @{name='SPRING_SQL_INIT_MODE';value='never'},@{name='TRAVEL_UPLOAD_DIR';value='/app/uploads'},@{name='TZ';value='Asia/Shanghai'}
        )
        $backendService=@($items | Where-Object {$_.kind -eq 'Service' -and $_.metadata.name -eq 'product-service'})[0]
        $backendService.metadata.name='backend'; $backendService.metadata.labels.'app.kubernetes.io/name'='backend'
        $backendService.spec.selector.'app.kubernetes.io/name'='backend'; $backendService.spec.ports[0].port=8080
        $items=@($items | Where-Object {
            ($_.kind -ne 'Deployment' -or $_.metadata.name -in @('backend','frontend')) -and
            ($_.kind -ne 'Service' -or $_.metadata.name -in @('backend','frontend','mysql')) -and
            ($_.kind -ne 'PersistentVolumeClaim' -or $_.metadata.name -ne 'content-uploads')
        })
        $variantNginx=$nginx.Replace('http://gateway-service:8000','http://backend:8080')
    }
    $items+=@{apiVersion='v1';kind='ConfigMap';metadata=@{name='benchmark-nginx';namespace=$ns;annotations=@{'lab.travelplatform/source-revision'=$protocol.sourceRevision;'lab.travelplatform/benchmark-run'=$id}};data=@{'default.conf'=$variantNginx}}
    $frontend=@($items | Where-Object {$_.kind -eq 'Deployment' -and $_.metadata.name -eq 'frontend'})[0]
    $frontend.spec.template.spec.volumes=@(@{name='nginx-config';configMap=@{name='benchmark-nginx'}})
    $frontend.spec.template.spec.containers[0].volumeMounts=@(@{name='nginx-config';mountPath='/etc/nginx/conf.d/default.conf';subPath='default.conf';readOnly=$true})
    $bundles[$variant]=@{apiVersion='v1';kind='List';items=$items}
    $bundles[$variant] | ConvertTo-Json -Depth 80 | Set-Content (Join-Path $directory "$variant.json") -Encoding utf8
}
$checks=@(Test-ComparisonPair $bundles $protocol $build.Image $microImages)
$protocol | ConvertTo-Json -Depth 15 | Set-Content (Join-Path $directory 'protocol.json') -Encoding utf8
$record=[ordered]@{
    RunId=$id;PreparedAt=(Get-Date).ToString('o');SourceRevision=$protocol.sourceRevision;ClusterChanged=$false;TeamReviewStatus='awaiting-team-review'
    MonolithBuildDirectory=$(if ($MonolithBuildDirectory) {(Resolve-Path $MonolithBuildDirectory).Path} else {$null});MonolithImage=$build.Image;MonolithImageId=$build.ImageId
    MicroserviceBuildDirectory=$(if ($MicroserviceBuildDirectory) {(Resolve-Path $MicroserviceBuildDirectory).Path} else {$null});MicroserviceImages=$microBuild.Images;Checks=$checks
    ReadyForDeployment=(-not $PlanOnly);BuildStatus=$build.Status;PlanOnly=[bool]$PlanOnly
    ProtocolSha256=(Get-FileHash (Join-Path $directory 'protocol.json')).Hash
    Manifests=@(foreach ($variant in $bundles.Keys) {@{Variant=$variant;File="$variant.json";Sha256=(Get-FileHash (Join-Path $directory "$variant.json")).Hash;Budget=(Get-ComparisonBudget $bundles[$variant].items)}})
}
$record | ConvertTo-Json -Depth 15 | Set-Content (Join-Path $directory 'prepared.json') -Encoding utf8
[pscustomobject]@{Directory=$directory;Checks=$checks.Count;ClusterChanged=$false;BudgetsEqual=$true;ReadyForDeployment=(-not $PlanOnly)}
