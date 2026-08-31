#requires -Version 7.2
[CmdletBinding()]
param([Parameter(Mandatory)][string]$PreparedDirectory)
$ErrorActionPreference='Stop'
Import-Module (Join-Path $PSScriptRoot 'member-e/MemberEComparison.psm1') -Force
$directory=(Resolve-Path -LiteralPath $PreparedDirectory).Path
$protocol=Get-Content -Raw (Join-Path $directory 'protocol.json') | ConvertFrom-Json -AsHashtable
$prepared=Get-Content -Raw (Join-Path $directory 'prepared.json') | ConvertFrom-Json -AsHashtable
$bundles=@{}
foreach ($variant in @('monolith','microservices')) {
    $bundles[$variant]=Get-Content -Raw (Join-Path $directory "$variant.json") | ConvertFrom-Json -AsHashtable
}
$microImages=@{}
foreach ($deployment in @($bundles.microservices.items | Where-Object kind -eq Deployment)) { $microImages[$deployment.metadata.name]=$deployment.spec.template.spec.containers[0].image }
$checks=@(Test-ComparisonPair $bundles $protocol $prepared.MonolithImage $microImages)
$negative=[Collections.Generic.List[object]]::new()
function Test-Rejection([string]$Name,[scriptblock]$Mutate) {
    $b=$bundles | ConvertTo-Json -Depth 80 | ConvertFrom-Json -AsHashtable
    $p=$protocol | ConvertTo-Json -Depth 15 | ConvertFrom-Json -AsHashtable
    & $Mutate $b $p
    $rejected=$false
    try { Test-ComparisonPair $b $p $prepared.MonolithImage $microImages | Out-Null }
    catch {
        # A null-reference/parse failure is not evidence that the contract worked.
        if ($_.Exception.Message -notlike 'Comparison contract failed:*') { throw }
        $rejected=$true
    }
    if (-not $rejected) { throw "Unsafe synthetic configuration was accepted: $Name" }
    $negative.Add(@{Name=$Name;Rejected=$true;Synthetic=$true})
}
Test-Rejection 'reuse an existing namespace' {param($b,$p) $p.namespaces.monolith='travel-platform'}
Test-Rejection 'inject an HPA' {param($b,$p) $b.monolith.items+=@{kind='HorizontalPodAutoscaler';metadata=@{name='backend';namespace=$p.namespaces.monolith}}}
Test-Rejection 'increase one replica' {param($b,$p) ($b.microservices.items | Where-Object kind -eq Deployment)[0].spec.replicas=2}
Test-Rejection 'increase one CPU limit' {param($b,$p) ($b.monolith.items | Where-Object kind -eq Deployment)[0].spec.template.spec.containers[0].resources.limits.cpu='8'}
Test-Rejection 'change only one seed payload' {param($b,$p) ($b.monolith.items | Where-Object {$_.metadata.name -eq 'travel-platform-db-init'}).data.'data-demo.sql'+="`n-- synthetic mismatch"}
Test-Rejection 'route the monolith through the microservice gateway' {param($b,$p) $c=($b.monolith.items | Where-Object {$_.metadata.name -eq 'benchmark-nginx'}); $c.data.'default.conf'=$c.data.'default.conf'.Replace('http://backend:8080','http://gateway-service:8000')}
Test-Rejection 'permit API writes' {param($b,$p) $c=($b.microservices.items | Where-Object {$_.metadata.name -eq 'benchmark-nginx'}); $c.data.'default.conf'=$c.data.'default.conf'.Replace('limit_except GET { deny all; }','')}
Test-Rejection 'enable SQL reseeding on monolith restart' {param($b,$p) $c=($b.monolith.items | Where-Object {$_.kind -eq 'Deployment' -and $_.metadata.name -eq 'backend'}).spec.template.spec.containers[0]; ($c.env | Where-Object name -eq SPRING_SQL_INIT_MODE).value='always'}
Test-Rejection 'enable verbose SQL in one architecture' {param($b,$p) ($b.monolith.items | Where-Object {$_.metadata.name -eq 'member-e-runtime'}).data.SPRING_APPLICATION_JSON='{"mybatis-plus":{"configuration":{"log-impl":"org.apache.ibatis.logging.stdout.StdOutImpl"}}}'}
Test-Rejection 'mix source revisions' {param($b,$p) $b.microservices.items[0].metadata.annotations.'lab.travelplatform/source-revision'='synthetic-other-source'}
Test-Rejection 'substitute an untested monolith image' {param($b,$p) ($b.monolith.items | Where-Object {$_.kind -eq 'Deployment' -and $_.metadata.name -eq 'backend'}).spec.template.spec.containers[0].image='synthetic/untested:latest'}
Test-Rejection 'use a mutable microservice image tag' {param($b,$p) ($b.microservices.items | Where-Object kind -eq Deployment)[0].spec.template.spec.containers[0].image='synthetic/untested:latest'}
Test-Rejection 'run only two measurements per architecture' {param($b,$p) $p.order=@('monolith','microservices','monolith','microservices')}
Test-Rejection 'claim team approval automatically' {param($b,$p) $p.reviewStatus='team-approved'}
Test-Rejection 'open a NodePort' {param($b,$p) ($b.monolith.items | Where-Object kind -eq Service)[0].spec.type='NodePort'}
Test-Rejection 'duplicate environment variable names' {param($b,$p) $c=($b.monolith.items | Where-Object {$_.kind -eq 'Deployment' -and $_.metadata.name -eq 'backend'}).spec.template.spec.containers[0]; $c.env+=@{name='SERVER_PORT';value='8080'}}

$hashChecks=@(
    @{Name='property order does not change the hash';Passed=((Get-ComparisonHash ([ordered]@{b=2;a=1})) -eq (Get-ComparisonHash ([ordered]@{a=1;b=2})))},
    @{Name='nested property order does not change the hash';Passed=((Get-ComparisonHash @{data=@{b=2;a=1}}) -eq (Get-ComparisonHash @{data=@{a=1;b=2}}))},
    @{Name='a changed value changes the hash';Passed=((Get-ComparisonHash @{a=1}) -ne (Get-ComparisonHash @{a=2}))},
    @{Name='array order changes the hash';Passed=((Get-ComparisonHash @(1,2)) -ne (Get-ComparisonHash @(2,1)))},
    @{Name='an empty array differs from null';Passed=((Get-ComparisonHash @{data=@()}) -ne (Get-ComparisonHash @{data=$null}))}
)
if (@($hashChecks | Where-Object {-not $_.Passed}).Count) { throw 'Canonical hashing regression.' }
$report=@{CheckedAt=(Get-Date).ToString('o');Scope='SYNTHETIC configuration tests only; no load or cluster writes';Checks=$checks;NegativeCases=$negative.ToArray();HashChecks=$hashChecks;ReadyForDeployment=$prepared.ReadyForDeployment;SourceRevision=$protocol.sourceRevision}
$report | ConvertTo-Json -Depth 15 | Set-Content -LiteralPath (Join-Path $directory 'comparison-contract-tests.json') -Encoding utf8
[pscustomobject]@{PositiveChecks=$checks.Count;UnsafeConfigurationsRejected=$negative.Count;HashChecks=$hashChecks.Count;ClusterChanged=$false;ReadyForDeployment=$prepared.ReadyForDeployment}
