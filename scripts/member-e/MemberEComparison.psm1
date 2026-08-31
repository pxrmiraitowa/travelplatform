#requires -Version 7.2
Import-Module (Join-Path $PSScriptRoot '../../experiments/scripts/LabEvidence.psm1') -Force
function Get-ComparisonBudget {
    param($Items)
    $total=[ordered]@{cpuRequestMillicores=0.0;memoryRequestMi=0.0;cpuLimitMillicores=0.0;memoryLimitMi=0.0}
    foreach ($workload in @($Items | Where-Object {$_.kind -in @('Deployment','StatefulSet')})) {
        foreach ($container in $workload.spec.template.spec.containers) {
            $r=$container.resources
            $total.cpuRequestMillicores += (Convert-LabCpuMilli $r.requests.cpu)*$workload.spec.replicas
            $total.memoryRequestMi += (Convert-LabMemoryMi $r.requests.memory)*$workload.spec.replicas
            $total.cpuLimitMillicores += (Convert-LabCpuMilli $r.limits.cpu)*$workload.spec.replicas
            $total.memoryLimitMi += (Convert-LabMemoryMi $r.limits.memory)*$workload.spec.replicas
        }
    }
    return $total
}
function Get-ComparisonHash {
    param($Value)
    function Sort-Value($InputValue) {
        if ($null -eq $InputValue) { return $null }
        if ($InputValue -is [Collections.IDictionary]) {
            $sorted=[ordered]@{}
            foreach ($key in @($InputValue.Keys | Sort-Object)) { $sorted[$key]=Sort-Value $InputValue[$key] }
            return $sorted
        }
        if ($InputValue -is [array]) { return ,@(foreach ($item in $InputValue) { Sort-Value $item }) }
        return $InputValue
    }
    $canonical=Sort-Value $Value | ConvertTo-Json -Depth 80 -Compress
    return [Convert]::ToHexString([Security.Cryptography.SHA256]::HashData([Text.Encoding]::UTF8.GetBytes($canonical)))
}
function Test-ComparisonPair {
    param($Bundles,$Protocol,[string]$MonolithImage,[hashtable]$MicroserviceImages)
    $checks=[Collections.Generic.List[object]]::new()
    function Assert-Pair([string]$Name,[bool]$Passed) {
        if (-not $Passed) { throw "Comparison contract failed: $Name" }
        $checks.Add([pscustomobject]@{Name=$Name;Passed=$true})
    }
    Assert-Pair 'fixed local pair and source' ($Protocol.context -eq 'kind-travel-platform' -and $Protocol.namespaces.monolith -eq 'travel-platform-bench-monolith' -and $Protocol.namespaces.microservices -eq 'travel-platform-bench-micro' -and $Protocol.sourceRevision -match '^[a-f0-9]{40}$')
    Assert-Pair 'three measurements per architecture in the declared order with fixed replicas' (@($Protocol.order | Where-Object {$_ -eq 'monolith'}).Count -eq 3 -and @($Protocol.order | Where-Object {$_ -eq 'microservices'}).Count -eq 3 -and @($Protocol.order).Count -eq 6 -and -not $Protocol.hpaEnabled -and $Protocol.replicasPerDeployment -eq 1)
    Assert-Pair 'bounded identical public-read load and no claimed team approval' ($Protocol.virtualUsers -ge 1 -and $Protocol.virtualUsers -le 60 -and $Protocol.measurementSecondsPerRun -ge 30 -and $Protocol.measurementSecondsPerRun -le 180 -and $Protocol.warmupSecondsPerRun -ge 30 -and $Protocol.warmupSecondsPerRun -le 120 -and $Protocol.sleepSeconds -ge 0.01 -and $Protocol.sleepSeconds -le 1 -and $Protocol.delaySecondsBetweenRuns -ge 0 -and $Protocol.delaySecondsBetweenRuns -le 60 -and $Protocol.targetPath -eq '/api/public/flights?pageNum=1&pageSize=10' -and $Protocol.reviewStatus -eq 'awaiting-team-review')
    $seeds=@(); $frontImages=@(); $mysqlImages=@()
    foreach ($variant in @('monolith','microservices')) {
        $namespace=$Protocol.namespaces[$variant]
        $items=@($Bundles[$variant].items)
        Assert-Pair "$variant has only its new namespace and allowed resource kinds" (@($items | Where-Object kind -eq Namespace).Count -eq 1 -and @($items | Where-Object {($_.kind -eq 'Namespace' -and $_.metadata.name -ne $namespace) -or ($_.kind -ne 'Namespace' -and $_.metadata.namespace -ne $namespace) -or $_.kind -notin @('Namespace','ConfigMap','Service','PersistentVolumeClaim','StatefulSet','Deployment','Job')}).Count -eq 0)
        $deployments=@($items | Where-Object kind -eq Deployment)
        $expected=if ($variant -eq 'monolith') {@('backend','frontend')} else {@('content-trip-service','frontend','gateway-service','order-service','product-service','user-service')}
        Assert-Pair "$variant has the expected fixed-replica workload set" ((@($deployments.metadata.name | Sort-Object) -join ',') -eq ($expected -join ',') -and @($items | Where-Object { $_.kind -in @('Deployment','StatefulSet') -and $_.spec.replicas -ne 1 }).Count -eq 0)
        Assert-Pair "$variant declares the same source revision" (@($items | Where-Object {$_.metadata.annotations.'lab.travelplatform/source-revision' -ne $Protocol.sourceRevision}).Count -eq 0 -and @($items | Where-Object {$_.kind -in @('Deployment','StatefulSet','Job') -and $_.spec.template.metadata.annotations.'lab.travelplatform/source-revision' -ne $Protocol.sourceRevision}).Count -eq 0)
        $duplicates=@($deployments | ForEach-Object { $_.spec.template.spec.containers | ForEach-Object { $_.env | Group-Object name | Where-Object Count -gt 1 } })
        Assert-Pair "$variant has probes, resource bounds and unique env names" ($duplicates.Count -eq 0 -and @($deployments | Where-Object {-not $_.spec.template.spec.containers[0].startupProbe -or -not $_.spec.template.spec.containers[0].readinessProbe -or -not $_.spec.template.spec.containers[0].livenessProbe}).Count -eq 0)
        $budget=Get-ComparisonBudget $items
        foreach ($key in $Protocol.budgetPerVariant.Keys) { Assert-Pair "$variant budget $key" ($budget[$key] -eq $Protocol.budgetPerVariant[$key]) }
        $mysql=@($items | Where-Object kind -eq StatefulSet)
        Assert-Pair "$variant database is a single persistent MySQL" ($mysql.Count -eq 1 -and $mysql[0].metadata.name -eq 'mysql' -and $mysql[0].spec.template.spec.volumes[0].persistentVolumeClaim.claimName -eq 'mysql-data')
        $mysqlImages+= $mysql[0].spec.template.spec.containers[0].image
        $job=@($items | Where-Object kind -eq Job)
        Assert-Pair "$variant initializer is bounded and not automatically retried" ($job.Count -eq 1 -and $job[0].metadata.name -eq 'db-init' -and $job[0].spec.backoffLimit -eq 0 -and $job[0].spec.activeDeadlineSeconds -eq 600)
        $seed=@($items | Where-Object {$_.kind -eq 'ConfigMap' -and $_.metadata.name -eq 'travel-platform-db-init'})
        Assert-Pair "$variant has exactly the owner initializer and seed" ($seed.Count -eq 1 -and [bool]$seed[0].data.'SqlRunner.java' -and [bool]$seed[0].data.'schema.sql' -and [bool]$seed[0].data.'data-demo.sql')
        $seeds+=Get-ComparisonHash $seed[0].data
        $frontend=@($deployments | Where-Object {$_.metadata.name -eq 'frontend'})[0]
        $frontImages+=$frontend.spec.template.spec.containers[0].image
        $nginx=@($items | Where-Object {$_.kind -eq 'ConfigMap' -and $_.metadata.name -eq 'benchmark-nginx'})[0].data.'default.conf'
        $upstream=if ($variant -eq 'monolith') {'http://backend:8080'} else {'http://gateway-service:8000'}
        Assert-Pair "$variant frontend uses its correct API path and denies writes" ($nginx.Contains("proxy_pass $upstream;") -and $nginx.Contains('limit_except GET { deny all; }') -and $nginx.Contains('access_log off;') -and @($frontend.spec.template.spec.volumes | Where-Object {$_.configMap.name -eq 'benchmark-nginx'}).Count -eq 1)
        Assert-Pair "$variant services do not open host or NodePorts" (@($items | Where-Object {$_.kind -eq 'Service' -and ($_.spec.type -notin @($null,'ClusterIP') -or @($_.spec.ports | Where-Object nodePort).Count)}).Count -eq 0)
        $runtime=@($items | Where-Object {$_.kind -eq 'ConfigMap' -and $_.metadata.name -eq 'member-e-runtime'})[0]
        $logging=$runtime.data.SPRING_APPLICATION_JSON | ConvertFrom-Json -AsHashtable
        Assert-Pair "$variant uses the same explicit SQL logging and JVM settings" ($logging.'mybatis-plus'.configuration.'log-impl' -eq 'org.apache.ibatis.logging.nologging.NoLoggingImpl' -and $runtime.data.JAVA_TOOL_OPTIONS -eq '-XX:MaxRAMPercentage=75.0')
        if ($variant -eq 'monolith') {
            $backend=@($deployments | Where-Object {$_.metadata.name -eq 'backend'})[0].spec.template.spec.containers[0]
            Assert-Pair 'monolith image reference matches the plan and app restart cannot reinitialize data' ($backend.image -eq $MonolithImage -and @($backend.env | Where-Object {$_.name -eq 'SPRING_SQL_INIT_MODE' -and $_.value -eq 'never'}).Count -eq 1 -and @($backend.env | Where-Object {$_.name -eq 'SPRING_DATASOURCE_URL' -and $_.value -like 'jdbc:mysql://mysql:3306/travel_product?*'}).Count -eq 1)
        } else {
            Assert-Pair 'six tested microservice/frontend images match the build record' ($MicroserviceImages.Count -eq 6 -and @($deployments | Where-Object {$_.spec.template.spec.containers[0].image -ne $MicroserviceImages[$_.metadata.name] -or $_.spec.template.spec.containers[0].imagePullPolicy -ne 'Never'}).Count -eq 0)
        }
    }
    Assert-Pair 'identical initialization payload in the two environments' ($seeds[0] -eq $seeds[1])
    Assert-Pair 'identical frontend and database images' ($frontImages[0] -eq $frontImages[1] -and $mysqlImages[0] -eq $mysqlImages[1])
    return $checks.ToArray()
}
Export-ModuleMember -Function Get-ComparisonBudget,Get-ComparisonHash,Test-ComparisonPair
