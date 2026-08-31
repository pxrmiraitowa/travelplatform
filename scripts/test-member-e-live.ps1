#requires -Version 7.2
[CmdletBinding()]
param(
    [switch]$AllowPodReplacement,
    [switch]$KeepFrontend,
    [string]$Context = 'kind-travel-platform'
)
$ErrorActionPreference = 'Stop'
if ($Context -ne 'kind-travel-platform') { throw 'Only the reviewed local Kind cluster is allowed.' }
$root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$namespace = 'travel-platform-micro-team'
$revision = (& git -C $root rev-parse HEAD)
$expectedVersion = "sha-$($revision.Substring(0,12))"
$base = 'http://127.0.0.1:8090'
function Invoke-Kube {
    param([string[]]$Arguments)
    $raw = & kubectl --context $Context --namespace $namespace --request-timeout=20s @Arguments
    if ($LASTEXITCODE -ne 0) { throw "Local acceptance operation failed: $($Arguments -join ' ')" }
    return $raw
}
function Read-Kube {
    param([string[]]$Arguments)
    return ((Invoke-Kube $Arguments) -join "`n") | ConvertFrom-Json
}
$ns = Read-Kube @('get','namespace',$namespace,'-o','json')
if ($ns.metadata.annotations.'lab.travelplatform/source-revision' -ne $revision -or -not $ns.metadata.annotations.'lab.travelplatform/deployment-run') { throw 'Namespace does not match this member-E deployment.' }
$deployments = Read-Kube @('get','deployment','-o','json')
if (@($deployments.items).Count -ne 6 -or @($deployments.items | Where-Object { $_.metadata.annotations.'lab.travelplatform/source-revision' -ne $revision -or $_.status.readyReplicas -lt 1 }).Count) { throw 'All six confirmed applications must be ready.' }
& git -C $root diff --quiet HEAD -- travel-platform-microservices travel-platform-server travel-platform-web
if ($LASTEXITCODE -ne 0) { throw 'Refusing tests against a silently modified business baseline.' }
$ports = @(8090,8000,8101,8102,8103,8104)
$occupied = @([Net.NetworkInformation.IPGlobalProperties]::GetIPGlobalProperties().GetActiveTcpListeners() | Where-Object { $_.Port -in $ports })
if ($occupied.Count) { throw "Acceptance needs unused local ports: $(@($occupied.Port | Sort-Object -Unique) -join ', '). No existing listener was stopped." }
$directory = Join-Path $root "artifacts/member-e/$($revision.Substring(0,7))/live-$(Get-Date -Format 'yyyyMMdd-HHmmss')-$([guid]::NewGuid().ToString('N').Substring(0,6))"
New-Item -ItemType Directory -Path $directory -Force | Out-Null
$result = [ordered]@{
    StartedAt=(Get-Date).ToString('o'); Context=$Context; Namespace=$namespace; NamespaceUid=$ns.metadata.uid
    SourceRevision=$revision; BaseUrl=$base; NodeVersion=(& node --version); Status='Running'
    OwnerTestFilesUnchanged=$true; PodReplacementAuthorized=[bool]$AllowPodReplacement
    Versions=@(); UploadRecovery=@(); ForwardProcesses=@()
    DataEffects='Owner API scenarios create demo users, closed/completed orders and uploaded images in the new test database only. No real payment or AI API key is configured.'
}
function Save-Result { $result | ConvertTo-Json -Depth 20 | Set-Content -LiteralPath (Join-Path $directory 'acceptance.json') -Encoding utf8 }
$forwards = [Collections.Generic.List[object]]::new()
$client = [Net.Http.HttpClient]::new()
$client.Timeout = [TimeSpan]::FromSeconds(15)
$previousBase = $env:API_BASE_URL
$completed = $false
function Read-Api {
    param([string]$Path,[string]$Origin=$base)
    $response = Invoke-RestMethod -Uri "$Origin$Path" -TimeoutSec 15
    if ($response.code -ne 200) { throw "Business response rejected: $Path" }
    return $response
}
function New-Login {
    param([string]$Username)
    $response = Invoke-RestMethod -Method Post -Uri "$base/api/auth/login" -ContentType 'application/json' -Body (@{username=$Username;password='123456'} | ConvertTo-Json -Compress) -TimeoutSec 15
    if ($response.code -ne 200 -or -not $response.data.token) { throw "Demo login failed: $Username" }
    return $response.data.token
}
function Get-BytesHash {
    param([string]$Path)
    if ($Path -notmatch '^/api/public/(product-uploads|uploads)/') { throw 'Unexpected upload URL; refuse external request.' }
    $bytes = $client.GetByteArrayAsync("$base$Path").GetAwaiter().GetResult()
    return [Convert]::ToHexString([Security.Cryptography.SHA256]::HashData($bytes))
}
function Upload-TestPng {
    param([string]$Path,[string]$Token)
    # Same one-pixel fixture as the unchanged owner's API regression suite.
    [byte[]]$png = @(0x89,0x50,0x4e,0x47,0x0d,0x0a,0x1a,0x0a,0x00,0x00,0x00,0x0d,0x49,0x48,0x44,0x52,0x00,0x00,0x00,0x01,0x00,0x00,0x00,0x01,0x08,0x06,0x00,0x00,0x00,0x1f,0x15,0xc4,0x89,0x00,0x00,0x00,0x0d,0x49,0x44,0x41,0x54,0x78,0x9c,0x63,0xf8,0xff,0xff,0x3f,0x00,0x05,0xfe,0x02,0xfe,0xdc,0xcc,0x59,0xe7,0x00,0x00,0x00,0x00,0x49,0x45,0x4e,0x44,0xae,0x42,0x60,0x82)
    $multipart = [Net.Http.MultipartFormDataContent]::new()
    $part = [Net.Http.ByteArrayContent]::new($png)
    $part.Headers.ContentType = [Net.Http.Headers.MediaTypeHeaderValue]::new('image/png')
    $multipart.Add($part,'file','member-e-persistence.png')
    $request = [Net.Http.HttpRequestMessage]::new([Net.Http.HttpMethod]::Post,"$base$Path")
    $request.Headers.Authorization = [Net.Http.Headers.AuthenticationHeaderValue]::new('Bearer',$Token)
    $request.Content = $multipart
    try {
        $response = $client.SendAsync($request).GetAwaiter().GetResult()
        try {
            $response.EnsureSuccessStatusCode() | Out-Null
            $payload = $response.Content.ReadAsStringAsync().GetAwaiter().GetResult() | ConvertFrom-Json
            if ($payload.code -ne 200 -or -not $payload.data.url) { throw 'Upload returned no successful file URL.' }
            return [pscustomobject]@{Url=$payload.data.url;ExpectedSha256=[Convert]::ToHexString([Security.Cryptography.SHA256]::HashData($png))}
        } finally { $response.Dispose() }
    } finally { $request.Dispose() }
}
try {
    $services = @(
        @{Name='frontend';Local=8090;Remote=80}, @{Name='gateway-service';Local=8000;Remote=8000},
        @{Name='user-service';Local=8101;Remote=8101}, @{Name='product-service';Local=8102;Remote=8102},
        @{Name='order-service';Local=8103;Remote=8103}, @{Name='content-trip-service';Local=8104;Remote=8104}
    )
    foreach ($service in $services) {
        $arguments = @('--context',$Context,'--namespace',$namespace,'port-forward',"service/$($service.Name)","$($service.Local):$($service.Remote)",'--address=127.0.0.1')
        $process = Start-Process -FilePath (Get-Command kubectl).Source -ArgumentList $arguments -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $directory "$($service.Name)-forward.log") -RedirectStandardError (Join-Path $directory "$($service.Name)-forward.err")
        $entry = [pscustomobject]@{Service=$service.Name;Process=$process;Pid=$process.Id;StartTime=$process.StartTime.ToUniversalTime().ToString('o');Port=$service.Local}
        $forwards.Add($entry)
    }
    $result.ForwardProcesses = @($forwards | Select-Object Service,Pid,StartTime,Port)
    Save-Result
    $deadline = (Get-Date).AddSeconds(45)
    do {
        if (@($forwards | Where-Object { $_.Process.HasExited }).Count) { throw 'One of our port-forwards exited; see its error log.' }
        try { $health = Read-Api '/api/public/health'; if ($health.data.status -eq 'UP') { break } } catch { if ((Get-Date) -gt $deadline) { throw } }
        if ((Get-Date) -gt $deadline) { throw 'Frontend gateway did not become reachable.' }
        Start-Sleep -Seconds 1
    } while ($true)
    $page = Invoke-WebRequest -Uri "$base/" -TimeoutSec 15
    if ($page.StatusCode -ne 200 -or $page.Content -notmatch '<div id="app">') { throw 'Frontend HTML entry did not load.' }
    $result.FrontendHtmlStatus = $page.StatusCode
    foreach ($service in $services | Where-Object Name -ne frontend) {
        $origin = "http://127.0.0.1:$($service.Local)"
        $health = Read-Api '/api/public/health' $origin
        if ($health.data.status -ne 'UP') { throw "Service not healthy: $($service.Name)" }
        if ($service.Name -ne 'gateway-service') {
            $version = Read-Api '/api/public/version' $origin
            if ($version.data.version -ne $expectedVersion) { throw "Wrong live version: $($service.Name)" }
            $result.Versions += @{Service=$service.Name;Version=$version.data.version}
        }
    }
    Save-Result
    $env:API_BASE_URL = "$base/api"
    Push-Location (Join-Path $root 'travel-platform-web')
    try {
        foreach ($suite in @('api','e2e')) {
            Write-Host "Running unchanged owner suite: $suite"
            & npm.cmd run "test:$suite" -- --maxWorkers=1 --reporter=default --reporter=json "--outputFile=$(Join-Path $directory "$suite-results.json")" 2>&1 | Tee-Object -FilePath (Join-Path $directory "$suite-console.log") | Out-Host
            $exitCode = $LASTEXITCODE
            $result["${suite}ExitCode"] = $exitCode
            Save-Result
            if ($exitCode -ne 0) { throw "Owner $suite regression failed. Preserve evidence and return the issue to its owner; do not patch business code here." }
        }
    } finally { Pop-Location }
    if ($AllowPodReplacement) {
        $adminToken = New-Login 'admin'
        $userToken = New-Login 'demo_user'
        foreach ($target in @(
            @{Deployment='product-service';Claim='product-uploads';Path='/api/admin/media/upload';Token=$adminToken},
            @{Deployment='content-trip-service';Claim='content-uploads';Path='/api/shares/upload';Token=$userToken}
        )) {
            $upload = Upload-TestPng $target.Path $target.Token
            $beforeHash = Get-BytesHash $upload.Url
            if ($beforeHash -ne $upload.ExpectedSha256) { throw 'Uploaded file differs from the fixture before replacement.' }
            $deployment = Read-Kube @('get','deployment',$target.Deployment,'-o','json')
            $claim = Read-Kube @('get','pvc',$target.Claim,'-o','json')
            $replicasets = Read-Kube @('get','replicaset','-o','json')
            $ownedRs = @($replicasets.items | Where-Object { $deployment.metadata.uid -in $_.metadata.ownerReferences.uid })
            $pods = Read-Kube @('get','pod','-o','json')
            $ownedPods = @($pods.items | Where-Object { $_.metadata.ownerReferences.uid -in $ownedRs.metadata.uid -and -not $_.metadata.deletionTimestamp })
            if ($deployment.spec.replicas -ne 1 -or $ownedPods.Count -ne 1) { throw 'Persistence demonstration needs a stable single replica; wait for HPA cooldown and run a new acceptance session.' }
            $old = $ownedPods[0]
            $checked = Read-Kube @('get','pod',$old.metadata.name,'-o','json')
            if ($checked.metadata.uid -ne $old.metadata.uid) { throw 'Pod identity changed before the scoped replacement.' }
            $started = Get-Date
            $recovery = [ordered]@{Deployment=$target.Deployment;Url=$upload.Url;BeforeSha256=$beforeHash;PvcUidBefore=$claim.metadata.uid;OldPod=$old.metadata.name;OldPodUid=$old.metadata.uid;DeletedAt=$started.ToString('o');Status='Replacing'}
            $result.UploadRecovery += $recovery
            Save-Result
            Write-Host "Replacing one explicitly owned test Pod: $($old.metadata.name)"
            Invoke-Kube @('delete','pod',$old.metadata.name,'--wait=false') | Out-Host
            $deadline = (Get-Date).AddSeconds(240)
            do {
                $pods = Read-Kube @('get','pod','-o','json')
                $replacement = @($pods.items | Where-Object {
                    $_.metadata.ownerReferences.uid -in $ownedRs.metadata.uid -and $_.metadata.uid -ne $old.metadata.uid -and -not $_.metadata.deletionTimestamp -and
                    @($_.status.conditions | Where-Object { $_.type -eq 'Ready' -and $_.status -eq 'True' }).Count
                })
                if ($replacement.Count) { break }
                if ((Get-Date) -gt $deadline) { throw 'Replacement Pod did not become Ready within 240 seconds.' }
                Start-Sleep -Seconds 2
            } while ($true)
            $claimAfter = Read-Kube @('get','pvc',$target.Claim,'-o','json')
            if ($claimAfter.metadata.uid -ne $claim.metadata.uid -or $claimAfter.spec.volumeName -ne $claim.spec.volumeName) { throw 'Persistent storage identity changed.' }
            # Service endpoint propagation may trail readiness by a few seconds.
            $readDeadline = (Get-Date).AddSeconds(30)
            do {
                try { $afterHash = Get-BytesHash $upload.Url; break } catch { if ((Get-Date) -gt $readDeadline) { throw }; Start-Sleep -Seconds 2 }
            } while ($true)
            if ($afterHash -ne $beforeHash) { throw 'Upload content did not survive Pod replacement.' }
            $recovery.NewPod = $replacement[0].metadata.name
            $recovery.NewPodUid = $replacement[0].metadata.uid
            $recovery.PvcUidAfter = $claimAfter.metadata.uid
            $recovery.AfterSha256 = $afterHash
            $recovery.RecoveredAt = (Get-Date).ToString('o')
            $recovery.ElapsedSeconds = [math]::Round(((Get-Date)-$started).TotalSeconds,2)
            $recovery.Status = 'Passed'
            Save-Result
        }
        $adminToken = $null; $userToken = $null
    }
    Invoke-Kube @('get','deployment,pod,pvc,hpa','-o','json') | Set-Content -LiteralPath (Join-Path $directory 'resources-after.json') -Encoding utf8
    $result.Status = 'Passed'
    $completed = $true
} catch {
    $result.Status = 'Failed'
    $result.Error = $_.Exception.Message
    throw
} finally {
    $env:API_BASE_URL = $previousBase
    $client.Dispose()
    foreach ($entry in $forwards) {
        if ($completed -and $KeepFrontend -and $entry.Service -eq 'frontend') { continue }
        # Only terminate process objects created by this invocation, not listeners
        # found by port number. Never stop the user's existing forwarding sessions.
        if (-not $entry.Process.HasExited) { $entry.Process.Kill(); $entry.Process.WaitForExit(5000) | Out-Null }
    }
    $result.FrontendForwardKept = ($completed -and [bool]$KeepFrontend)
    $result.FinishedAt = (Get-Date).ToString('o')
    Save-Result
    Write-Host "Acceptance evidence: $directory"
}
