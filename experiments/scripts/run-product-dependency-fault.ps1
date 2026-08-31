#requires -Version 7.2
[CmdletBinding()]
param(
    [switch]$AllowDisruption,
    [string]$Context = 'kind-travel-platform',
    [string]$Namespace = 'travel-platform-bench-micro',
    [ValidateRange(1024,65535)][int]$GatewayPort = 18000,
    [string]$ResultDirectory
)

$ErrorActionPreference = 'Stop'
Import-Module (Join-Path $PSScriptRoot 'LabHttp.psm1') -Force

$expectedContext = 'kind-travel-platform'
$expectedNamespace = 'travel-platform-bench-micro'
$expectedSourceRevision = 'aece11988e2364289625c0de2c75b18444c55b8d'
$expectedMessage = '商品服务暂不可用，请稍后重试'
$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path

if (-not $AllowDisruption) {
    throw 'This experiment temporarily scales one reviewed product-service Deployment to zero. Re-run with -AllowDisruption after reviewing the target.'
}
if ($Context -ne $expectedContext -or $Namespace -ne $expectedNamespace) {
    throw 'Only the reviewed local benchmark microservice environment is allowed.'
}
if ((& kubectl config current-context) -ne $Context) {
    throw 'The active kubectl context is not the reviewed local Kind cluster.'
}
& git -C $projectRoot diff --quiet $expectedSourceRevision HEAD -- travel-platform-microservices travel-platform-server travel-platform-web
if ($LASTEXITCODE -ne 0) { throw 'Application runtime inputs changed after the tested source revision; rebuild before this experiment.' }
& git -C $projectRoot diff --quiet -- travel-platform-microservices
if ($LASTEXITCODE -ne 0) { throw 'The microservice source has uncommitted changes; refusing to mix them into fault evidence.' }

function Invoke-Kube {
    param([Parameter(Mandatory)][string[]]$Arguments)
    $output = & kubectl --context $Context --namespace $Namespace --request-timeout=30s @Arguments
    if ($LASTEXITCODE -ne 0) { throw "kubectl failed: $($Arguments -join ' ')" }
    return $output
}

function Read-Kube {
    param([Parameter(Mandatory)][string[]]$Arguments)
    return ((Invoke-Kube $Arguments) -join "`n") | ConvertFrom-Json
}

function Get-Ready {
    param([object]$Deployment)
    if ($null -eq $Deployment.status.readyReplicas) { return 0 }
    return [int]$Deployment.status.readyReplicas
}

function Get-Health {
    param([int]$Port,[string]$Service)
    $response = Invoke-LabRequest -BaseUrl "http://127.0.0.1:$Port" -Path '/api/public/health' -TimeoutSeconds 10
    return [ordered]@{
        Service=$Service; HttpStatus=$response.HttpStatus; Code=$response.Code
        Status=$response.Data.status; ElapsedMs=$response.ElapsedMs
        Passed=($response.HttpStatus -eq 200 -and $response.Code -eq 200 -and $response.Data.status -eq 'UP')
    }
}

function Wait-HttpReady {
    param([int]$Port,[string]$Service,[Diagnostics.Process]$Process)
    $deadline = (Get-Date).AddSeconds(45)
    do {
        if ($Process.HasExited) { throw "$Service port-forward exited before it became ready." }
        try {
            $health = Get-Health -Port $Port -Service $Service
            if ($health.Passed) { return $health }
        } catch {
            if ((Get-Date) -ge $deadline) { throw }
        }
        if ((Get-Date) -ge $deadline) { throw "$Service did not become reachable through its local port-forward." }
        Start-Sleep -Seconds 1
    } while ($true)
}

$namespaceObject = Read-Kube @('get','namespace',$Namespace,'-o','json')
if ($namespaceObject.metadata.annotations.'lab.travelplatform/source-revision' -ne $expectedSourceRevision) {
    throw 'The namespace does not belong to the tested source revision.'
}
$deploymentsBefore = Read-Kube @('get','deployments','-o','json')
$expectedDeployments = @('content-trip-service','frontend','gateway-service','order-service','product-service','user-service')
if (@($deploymentsBefore.items).Count -ne $expectedDeployments.Count) { throw 'Unexpected Deployment count in the benchmark namespace.' }
foreach ($name in $expectedDeployments) {
    $item = @($deploymentsBefore.items | Where-Object { $_.metadata.name -eq $name })
    if ($item.Count -ne 1 -or $item[0].spec.replicas -ne 1 -or (Get-Ready $item[0]) -ne 1 -or
        $item[0].metadata.annotations.'lab.travelplatform/source-revision' -ne $expectedSourceRevision) {
        throw "Deployment is not the reviewed ready single-replica baseline: $name"
    }
}
if (@((Read-Kube @('get','hpa','-o','json')).items).Count -ne 0) { throw 'Fault evidence requires the fixed-replica benchmark namespace without HPA.' }

$ports = [ordered]@{
    'gateway-service'=$GatewayPort
    'user-service'=($GatewayPort + 1)
    'product-service'=($GatewayPort + 2)
    'order-service'=($GatewayPort + 3)
    'content-trip-service'=($GatewayPort + 4)
}
$occupied = @([Net.NetworkInformation.IPGlobalProperties]::GetIPGlobalProperties().GetActiveTcpListeners() |
    Where-Object { $_.Address.ToString() -in @('127.0.0.1','0.0.0.0','::','::1') -and $_.Port -in $ports.Values })
if ($occupied.Count) { throw "Required local ports are already in use: $(@($occupied.Port | Sort-Object -Unique) -join ', ')" }

$artifactRoot = if ($ResultDirectory) { [IO.Path]::GetFullPath($ResultDirectory) } else {
    Join-Path $projectRoot "artifacts/member-e/$($expectedSourceRevision.Substring(0,7))"
}
$sessionId = "dependency-fault-$(Get-Date -Format 'yyyyMMdd-HHmmss')-$([guid]::NewGuid().ToString('N').Substring(0,8))"
$sessionDirectory = Join-Path $artifactRoot $sessionId
New-Item -ItemType Directory -Path $sessionDirectory -Force | Out-Null
$resultFile = Join-Path $sessionDirectory 'fault-experiment.json'
$result = [ordered]@{
    SchemaVersion=1; SessionId=$sessionId; Status='Running'; StartedAt=(Get-Date).ToString('o'); FinishedAt=$null; Error=$null
    Context=$Context; Namespace=$Namespace; NamespaceUid=$namespaceObject.metadata.uid; SourceRevision=$expectedSourceRevision
    Fault=[ordered]@{Type='dependency-unavailable'; Target='Deployment/product-service'; OriginalReplicas=1; Injected=$false; Restored=$false}
    Expected=[ordered]@{BusinessCode=500; Message=$expectedMessage; MaximumResponseMs=8000; OtherServicesRemainHealthy=$true}
    Before=[ordered]@{}; During=[ordered]@{}; After=[ordered]@{}; Checks=[Collections.Generic.List[object]]::new()
    LogFiles=[Collections.Generic.List[object]]::new()
    SecretValuesIncluded=$false; LoginTokenPersisted=$false
}
function Save-Result { $result | ConvertTo-Json -Depth 40 | Set-Content -LiteralPath $resultFile -Encoding utf8 }
function Save-ServiceLog {
    param([Parameter(Mandatory)][string]$Service,[Parameter(Mandatory)][ValidateSet('before','during','after')][string]$Phase)
    $log = (Invoke-Kube @('logs',"deployment/$Service",'--since=10m','--tail=300')) -join "`n"
    $sanitized = $log -replace '(?i)Bearer\s+[^\s,;]+','Bearer [REDACTED]'
    $name = "$Phase-$Service.log"
    $path = Join-Path $sessionDirectory $name
    Set-Content -LiteralPath $path -Value $sanitized -Encoding utf8
    $result.LogFiles.Add([ordered]@{Phase=$Phase;Service=$Service;File=$name;Sha256=(Get-FileHash -LiteralPath $path -Algorithm SHA256).Hash;Sanitized=$true})
}
function Add-Check {
    param([string]$Name,[bool]$Passed,[object]$Expected,[object]$Actual)
    $result.Checks.Add([ordered]@{Name=$Name;Passed=$Passed;Expected=$Expected;Actual=$Actual;At=(Get-Date).ToString('o')})
    Write-Host "[$(if ($Passed) {'PASS'} else {'FAIL'})] $Name"
}

$forwards = [Collections.Generic.List[object]]::new()
$scaled = $false
$token = $null
Save-Result
try {
    $remotePorts = @{'gateway-service'=8000;'user-service'=8101;'product-service'=8102;'order-service'=8103;'content-trip-service'=8104}
    foreach ($service in $ports.Keys) {
        $arguments = @('--context',$Context,'--namespace',$Namespace,'port-forward',"service/$service","$($ports[$service]):$($remotePorts[$service])",'--address=127.0.0.1')
        $process = Start-Process -FilePath (Get-Command kubectl).Source -ArgumentList $arguments -WindowStyle Hidden -PassThru `
            -RedirectStandardOutput (Join-Path $sessionDirectory "$service-forward.log") `
            -RedirectStandardError (Join-Path $sessionDirectory "$service-forward.err")
        $forwards.Add([pscustomobject]@{Service=$service;Port=$ports[$service];Process=$process})
    }
    $healthBefore = @()
    foreach ($entry in $forwards) { $healthBefore += Wait-HttpReady -Port $entry.Port -Service $entry.Service -Process $entry.Process }

    $login = Invoke-LabRequest -BaseUrl "http://127.0.0.1:$GatewayPort" -Path '/api/auth/login' -Method POST `
        -Body @{username='demo_user';password='123456'} -TimeoutSeconds 10
    $token = $login.Data.token
    Add-Check 'Demo user login works before fault injection' ($login.HttpStatus -eq 200 -and $login.Code -eq 200 -and -not [string]::IsNullOrWhiteSpace($token)) 'HTTP 200, business 200, non-empty token' "HTTP $($login.HttpStatus), business $($login.Code), tokenPresent=$(-not [string]::IsNullOrWhiteSpace($token))"
    if ([string]::IsNullOrWhiteSpace($token)) { throw 'Login produced no token.' }
    $headers = @{Authorization="Bearer $token"}
    $requestBody = @{destination='上海';totalDays=1;startDate='2030-08-03';preferences=@('城市观光')}
    $baselinePreview = Invoke-LabRequest -BaseUrl "http://127.0.0.1:$GatewayPort" -Path '/api/trip-plans/ai-preview' -Method POST -Headers $headers -Body $requestBody -TimeoutSeconds 15
    $baselineDays = @($baselinePreview.Data.days).Count
    Add-Check 'Trip preview works before dependency fault' ($baselinePreview.HttpStatus -eq 200 -and $baselinePreview.Code -eq 200 -and $baselineDays -gt 0) 'HTTP 200, business 200, at least one day' "HTTP $($baselinePreview.HttpStatus), business $($baselinePreview.Code), days=$baselineDays"
    $pvcBefore = Read-Kube @('get','pvc','-o','json')
    $result.Before = [ordered]@{
        Health=$healthBefore
        Preview=[ordered]@{HttpStatus=$baselinePreview.HttpStatus;Code=$baselinePreview.Code;Message=$baselinePreview.Message;Days=$baselineDays;ElapsedMs=$baselinePreview.ElapsedMs}
        Deployments=@($deploymentsBefore.items | Sort-Object {$_.metadata.name} | ForEach-Object {[ordered]@{Name=$_.metadata.name;Uid=$_.metadata.uid;Replicas=$_.spec.replicas;Ready=(Get-Ready $_)}})
        PersistentClaims=@($pvcBefore.items | Sort-Object {$_.metadata.name} | ForEach-Object {[ordered]@{Name=$_.metadata.name;Uid=$_.metadata.uid;VolumeName=$_.spec.volumeName;Phase=$_.status.phase}})
    }
    foreach ($deploymentName in @('content-trip-service','gateway-service')) { Save-ServiceLog -Service $deploymentName -Phase before }
    Save-Result

    $productBefore = @($deploymentsBefore.items | Where-Object { $_.metadata.name -eq 'product-service' })[0]
    Invoke-Kube @('scale','deployment/product-service','--replicas=0') | Out-Host
    $scaled = $true
    $result.Fault.Injected = $true
    $result.Fault.InjectedAt = (Get-Date).ToString('o')
    Save-Result
    $deadline = (Get-Date).AddSeconds(90)
    do {
        $product = Read-Kube @('get','deployment','product-service','-o','json')
        $endpointSlices = Read-Kube @('get','endpointslices','-l','kubernetes.io/service-name=product-service','-o','json')
        $addressCount = @($endpointSlices.items.endpoints | Where-Object { $_.conditions.ready -eq $true }).Count
        if ($product.spec.replicas -eq 0 -and (Get-Ready $product) -eq 0 -and $addressCount -eq 0) { break }
        if ((Get-Date) -ge $deadline) { throw 'Product service did not reach the intended unavailable state.' }
        Start-Sleep -Seconds 1
    } while ($true)

    $faultPreview = Invoke-LabRequest -BaseUrl "http://127.0.0.1:$GatewayPort" -Path '/api/trip-plans/ai-preview' -Method POST -Headers $headers -Body $requestBody -TimeoutSeconds 10
    $healthDuring = @()
    foreach ($service in @('gateway-service','user-service','order-service','content-trip-service')) {
        $healthDuring += Get-Health -Port $ports[$service] -Service $service
    }
    Add-Check 'Unavailable dependency returns the designed business message' ($faultPreview.Code -eq 500 -and $faultPreview.Message -eq $expectedMessage) "business 500, $expectedMessage" "HTTP $($faultPreview.HttpStatus), business $($faultPreview.Code), $($faultPreview.Message)"
    Add-Check 'Dependency failure response is bounded' ($faultPreview.ElapsedMs -le 8000) '<= 8000 ms' "$($faultPreview.ElapsedMs) ms"
    Add-Check 'Gateway, user, order and content services stay healthy' (@($healthDuring | Where-Object {-not $_.Passed}).Count -eq 0) 'all four services UP' (($healthDuring | ForEach-Object {"$($_.Service)=$($_.Status)"}) -join ', ')
    $result.During = [ordered]@{
        Product=[ordered]@{DeploymentUid=$product.metadata.uid;DesiredReplicas=$product.spec.replicas;ReadyReplicas=(Get-Ready $product);EndpointAddresses=$addressCount}
        Preview=[ordered]@{HttpStatus=$faultPreview.HttpStatus;Code=$faultPreview.Code;Message=$faultPreview.Message;ElapsedMs=$faultPreview.ElapsedMs}
        OtherServiceHealth=$healthDuring
    }
    foreach ($deploymentName in @('content-trip-service','gateway-service')) { Save-ServiceLog -Service $deploymentName -Phase during }
    Save-Result
} catch {
    $result.Status='Failed'
    $result.Error=$_.Exception.Message
    throw
} finally {
    $token=$null
    if ($scaled) {
        try {
            $current = Read-Kube @('get','deployment','product-service','-o','json')
            if ($current.metadata.uid -ne $productBefore.metadata.uid) { throw 'Product Deployment identity changed; refusing to scale an unexpected replacement.' }
            Invoke-Kube @('scale','deployment/product-service','--replicas=1') | Out-Host
            Invoke-Kube @('rollout','status','deployment/product-service','--timeout=240s') | Out-Host
            $result.Fault.Restored=$true
            $result.Fault.RestoredAt=(Get-Date).ToString('o')
        } catch {
            $result.Fault.RestoreError=$_.Exception.Message
            $result.Status='Failed'
            if (-not $result.Error) { $result.Error='Automatic restoration failed; inspect Fault.RestoreError.' }
        }
    }
    if ($result.Fault.Restored) {
        try {
            $deploymentsAfter = Read-Kube @('get','deployments','-o','json')
            $pvcAfter = Read-Kube @('get','pvc','-o','json')
            $healthAfter=@()
            foreach ($entry in $forwards | Where-Object Service -ne 'product-service') { $healthAfter += Get-Health -Port $entry.Port -Service $entry.Service }
            $productForward = @($forwards | Where-Object Service -eq 'product-service')[0]
            # A Service port-forward is tied to the original Pod selected when it
            # starts. Always replace it after the dependency Pod is recreated.
            if (-not $productForward.Process.HasExited) {
                $productForward.Process.Kill()
                $productForward.Process.WaitForExit(5000) | Out-Null
            }
            $arguments = @('--context',$Context,'--namespace',$Namespace,'port-forward','service/product-service',"$($ports['product-service']):8102",'--address=127.0.0.1')
            $replacementForward = Start-Process -FilePath (Get-Command kubectl).Source -ArgumentList $arguments -WindowStyle Hidden -PassThru `
                -RedirectStandardOutput (Join-Path $sessionDirectory 'product-service-recovery-forward.log') `
                -RedirectStandardError (Join-Path $sessionDirectory 'product-service-recovery-forward.err')
            $productForward=[pscustomobject]@{Service='product-service';Port=$ports['product-service'];Process=$replacementForward}
            $forwards.Add($productForward)
            $healthAfter += Wait-HttpReady -Port $productForward.Port -Service 'product-service' -Process $productForward.Process
            $loginAfter = Invoke-LabRequest -BaseUrl "http://127.0.0.1:$GatewayPort" -Path '/api/auth/login' -Method POST -Body @{username='demo_user';password='123456'} -TimeoutSeconds 10
            $recoveryToken=$loginAfter.Data.token
            $recoveryPreview = Invoke-LabRequest -BaseUrl "http://127.0.0.1:$GatewayPort" -Path '/api/trip-plans/ai-preview' -Method POST -Headers @{Authorization="Bearer $recoveryToken"} -Body @{destination='上海';totalDays=1;startDate='2030-08-03';preferences=@('城市观光')} -TimeoutSeconds 15
            $recoveryToken=$null
            $recoveryDays=@($recoveryPreview.Data.days).Count
            $uidsUnchanged = @($deploymentsAfter.items | Where-Object {
                $currentName = $_.metadata.name
                $before = @($deploymentsBefore.items | Where-Object { $_.metadata.name -eq $currentName })[0]
                $_.metadata.uid -ne $before.metadata.uid
            }).Count -eq 0
            $claimsUnchanged = @($pvcAfter.items | Where-Object {
                $currentName = $_.metadata.name
                $before = @($pvcBefore.items | Where-Object { $_.metadata.name -eq $currentName })[0]
                $_.metadata.uid -ne $before.metadata.uid -or $_.spec.volumeName -ne $before.spec.volumeName
            }).Count -eq 0
            Add-Check 'Product service is restored and trip preview recovers' ($recoveryPreview.HttpStatus -eq 200 -and $recoveryPreview.Code -eq 200 -and $recoveryDays -gt 0) 'HTTP 200, business 200, at least one day' "HTTP $($recoveryPreview.HttpStatus), business $($recoveryPreview.Code), days=$recoveryDays"
            Add-Check 'All five microservices are healthy after recovery' (@($healthAfter | Where-Object {-not $_.Passed}).Count -eq 0) 'all services UP' (($healthAfter | ForEach-Object {"$($_.Service)=$($_.Status)"}) -join ', ')
            Add-Check 'Deployment and persistent-volume identities are unchanged' ($uidsUnchanged -and $claimsUnchanged) 'all UIDs and PVC volumes unchanged' "deployments=$uidsUnchanged, claims=$claimsUnchanged"
            foreach ($deploymentName in @('product-service','content-trip-service','gateway-service')) { Save-ServiceLog -Service $deploymentName -Phase after }
            $result.After=[ordered]@{
                Health=$healthAfter
                Preview=[ordered]@{HttpStatus=$recoveryPreview.HttpStatus;Code=$recoveryPreview.Code;Message=$recoveryPreview.Message;Days=$recoveryDays;ElapsedMs=$recoveryPreview.ElapsedMs}
                Deployments=@($deploymentsAfter.items | Sort-Object {$_.metadata.name} | ForEach-Object {[ordered]@{Name=$_.metadata.name;Uid=$_.metadata.uid;Replicas=$_.spec.replicas;Ready=(Get-Ready $_)}})
                PersistentClaims=@($pvcAfter.items | Sort-Object {$_.metadata.name} | ForEach-Object {[ordered]@{Name=$_.metadata.name;Uid=$_.metadata.uid;VolumeName=$_.spec.volumeName;Phase=$_.status.phase}})
            }
            if ($result.Status -ne 'Failed' -and @($result.Checks | Where-Object {-not $_.Passed}).Count -eq 0) { $result.Status='Passed' }
            elseif (@($result.Checks | Where-Object {-not $_.Passed}).Count -gt 0) { $result.Status='Failed'; if (-not $result.Error) {$result.Error='One or more acceptance checks failed.'} }
        } catch {
            $result.Status='Failed'
            if (-not $result.Error) { $result.Error=$_.Exception.Message }
        }
    }
    foreach ($entry in $forwards) {
        if (-not $entry.Process.HasExited) { $entry.Process.Kill(); $entry.Process.WaitForExit(5000) | Out-Null }
    }
    $result.FinishedAt=(Get-Date).ToString('o')
    Save-Result
    Write-Host "Fault experiment evidence: $sessionDirectory"
}

if ($result.Status -ne 'Passed') { throw "Fault experiment did not pass: $($result.Error)" }
