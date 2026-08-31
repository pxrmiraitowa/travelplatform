#requires -Version 7.2
# Validate the member-E overlay without changing the cluster or executing SQL.
function Test-MemberEManifest {
    param([Parameter(Mandatory)]$Bundle,[Parameter(Mandatory)][string]$Revision)
    $namespace='travel-platform-micro-team'
    $items=@($Bundle.items)
    $checks=[Collections.Generic.List[object]]::new()
    function Assert-Contract {
        param([string]$Name,[bool]$Passed)
        if (-not $Passed) { throw "Member E manifest contract failed: $Name" }
        $checks.Add([pscustomobject]@{Name=$Name;Passed=$true})
    }
    function Find-Resource {
        param([string]$Kind,[string]$Name)
        $matches=@($items | Where-Object {$_.kind -eq $Kind -and $_.metadata.name -eq $Name})
        if ($matches.Count -ne 1) { throw "Expected one $Kind/$Name." }
        return $matches[0]
    }
    $ns=Find-Resource Namespace $namespace
    Assert-Contract 'Only the independent team namespace is declared' (@($items | Where-Object kind -eq Namespace).Count -eq 1)
    Assert-Contract 'Every namespaced object stays out of old environments' (@($items | Where-Object {$_.kind -ne 'Namespace' -and $_.metadata.namespace -ne $namespace}).Count -eq 0)
    Assert-Contract 'Only reviewed namespaced resource types are permitted' (@($items | Where-Object { $_.kind -notin @('Namespace','ConfigMap','Service','PersistentVolumeClaim','StatefulSet','Deployment','Job','HorizontalPodAutoscaler') }).Count -eq 0)
    Assert-Contract 'Exactly one database, initializer and autoscaler' (@($items | Where-Object kind -eq StatefulSet).Count -eq 1 -and @($items | Where-Object kind -eq Job).Count -eq 1 -and @($items | Where-Object kind -eq HorizontalPodAutoscaler).Count -eq 1)
    $deployments=@($items | Where-Object kind -eq Deployment)
    $expected=@('content-trip-service','frontend','gateway-service','order-service','product-service','user-service')
    Assert-Contract 'Exactly the six expected application Deployments' ((@($deployments.metadata.name | Sort-Object) -join ',') -eq ($expected -join ','))
    $duplicateEnv = @($deployments | ForEach-Object { $_.spec.template.spec.containers | ForEach-Object { $_.env | Group-Object name | Where-Object Count -gt 1 } })
    Assert-Contract 'Explicit application environment variable names are unique' ($duplicateEnv.Count -eq 0)
    foreach ($deployment in $deployments) {
        $name=$deployment.metadata.name
        $containers=@($deployment.spec.template.spec.containers)
        $revisionPrefix=[regex]::Escape($Revision.Substring(0,7))
        $expectedImage="^travel-platform-benchmark-(?:user-service|product-service|order-service|content-trip-service|gateway-service|frontend):$revisionPrefix-[0-9a-f]{12}$"
        Assert-Contract "$name has one versioned local image and cannot pull a substitute" ($containers.Count -eq 1 -and $containers[0].image -match $expectedImage -and $containers[0].imagePullPolicy -eq 'Never')
        $container=$containers[0]
        Assert-Contract "$name has requests, limits and all three probes" ([bool]($container.resources.requests.cpu -and $container.resources.requests.memory -and $container.resources.limits.cpu -and $container.resources.limits.memory -and $container.startupProbe -and $container.readinessProbe -and $container.livenessProbe))
        Assert-Contract "$name carries the selected source revision and runtime config" ($deployment.metadata.annotations.'lab.travelplatform/source-revision' -eq $Revision -and 'member-e-runtime' -in @($container.envFrom.configMapRef.name))
    }
    $runtime=Find-Resource ConfigMap member-e-runtime
    Assert-Contract 'Runtime version matches checkout' ($runtime.data.SERVICE_VERSION -eq "sha-$($Revision.Substring(0,12))")
    $frontend=Find-Resource Service frontend
    Assert-Contract 'Frontend avoids host NodePort collisions' ($frontend.spec.type -eq 'ClusterIP' -and -not $frontend.spec.ports[0].nodePort)
    $claims=@($items | Where-Object kind -eq PersistentVolumeClaim)
    Assert-Contract 'Three named persistent claims' ((@($claims.metadata.name | Sort-Object) -join ',') -eq 'content-uploads,mysql-data,product-uploads')
    Assert-Contract 'Claims use local single-node RWO storage' (@($claims | Where-Object {$_.spec.storageClassName -ne 'standard' -or 'ReadWriteOnce' -notin $_.spec.accessModes -or -not $_.spec.resources.requests.storage}).Count -eq 0)
    $mysql=Find-Resource StatefulSet mysql
    $mysqlVolume=@($mysql.spec.template.spec.volumes | Where-Object name -eq mysql-data)
    Assert-Contract 'MySQL data survives Pod replacement' ($mysqlVolume.Count -eq 1 -and $mysqlVolume[0].persistentVolumeClaim.claimName -eq 'mysql-data' -and -not $mysqlVolume[0].emptyDir -and @($mysql.spec.template.spec.containers[0].volumeMounts | Where-Object {$_.name -eq 'mysql-data' -and $_.mountPath -eq '/var/lib/mysql'}).Count -eq 1)
    Assert-Contract 'MySQL has requests and limits' ([bool]($mysql.spec.template.spec.containers[0].resources.requests.cpu -and $mysql.spec.template.spec.containers[0].resources.requests.memory -and $mysql.spec.template.spec.containers[0].resources.limits.cpu -and $mysql.spec.template.spec.containers[0].resources.limits.memory))
    foreach ($pair in @(@('product-service','product-uploads'),@('content-trip-service','content-uploads'))) {
        $deployment=Find-Resource Deployment $pair[0]
        $volumes=@($deployment.spec.template.spec.volumes | Where-Object {$_.persistentVolumeClaim.claimName -eq $pair[1]})
        $mounts=@($deployment.spec.template.spec.containers[0].volumeMounts | Where-Object {$_.mountPath -eq '/app/uploads' -and $_.name -eq $volumes[0].name})
        Assert-Contract "$($pair[0]) uploads use its own writable claim" ($volumes.Count -eq 1 -and $mounts.Count -eq 1 -and $deployment.spec.template.spec.securityContext.fsGroup -eq 10001)
    }
    $hpa=Find-Resource HorizontalPodAutoscaler product-service
    Assert-Contract 'HPA targets product CPU with the planned bounds' ($hpa.spec.scaleTargetRef.kind -eq 'Deployment' -and $hpa.spec.scaleTargetRef.name -eq 'product-service' -and $hpa.spec.minReplicas -eq 1 -and $hpa.spec.maxReplicas -eq 5 -and $hpa.spec.metrics[0].resource.name -eq 'cpu' -and $hpa.spec.metrics[0].resource.target.averageUtilization -eq 60)
    $job=Find-Resource Job db-init
    Assert-Contract 'Initialization does not retry business SQL automatically' ($job.spec.backoffLimit -eq 0 -and $job.spec.activeDeadlineSeconds -eq 600)
    $seed=Find-Resource ConfigMap travel-platform-db-init
    Assert-Contract 'Initialization uses the owner-provided runner and two seed files' ([bool]($seed.data.'SqlRunner.java' -and $seed.data.'schema.sql' -and $seed.data.'data-demo.sql'))
    Assert-Contract 'Rendered evidence contains no Secret values' (@($items | Where-Object kind -eq Secret).Count -eq 0)
    return $checks.ToArray()
}
Export-ModuleMember -Function Test-MemberEManifest
