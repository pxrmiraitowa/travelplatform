# Shared by local integration experiments. Never persist Authorization or login data.
function Assert-LabUrl {
    param([string]$BaseUrl)
    $uri = [uri]$BaseUrl
    if ($uri.Scheme -ne 'http' -or -not $uri.IsLoopback -or $uri.UserInfo -or $uri.Query -or $uri.Fragment) {
        throw 'These write experiments accept only a local HTTP port-forward URL.'
    }
}

function Invoke-LabRequest {
    param(
        [Parameter(Mandatory)][string]$BaseUrl,
        [Parameter(Mandatory)][string]$Path,
        [string]$Method = 'GET',
        [hashtable]$Headers = @{},
        [object]$Body,
        [int]$TimeoutSeconds = 15
    )
    Assert-LabUrl $BaseUrl
    if (-not $Path.StartsWith('/api/')) { throw 'Expected an application API path.' }
    $request = @{Uri=$BaseUrl.TrimEnd('/') + $Path; Method=$Method; Headers=$Headers;
        TimeoutSec=$TimeoutSeconds; SkipHttpErrorCheck=$true; MaximumRedirection=0}
    if ($PSBoundParameters.ContainsKey('Body')) {
        $request.ContentType = 'application/json; charset=utf-8'
        $request.Body = $Body | ConvertTo-Json -Depth 12 -Compress
    }
    $timer = [Diagnostics.Stopwatch]::StartNew()
    $response = Invoke-WebRequest @request
    $timer.Stop()
    $envelope = $null
    try { $envelope = $response.Content | ConvertFrom-Json } catch { }
    [pscustomobject]@{
        HttpStatus=[int]$response.StatusCode; Code=$envelope.code; Message=$envelope.message
        Data=$envelope.data; ElapsedMs=[math]::Round($timer.Elapsed.TotalMilliseconds, 2)
    }
}

function Get-LabData {
    param([object]$Response, [string]$Operation)
    if ($Response.HttpStatus -ne 200 -or $Response.Code -ne 200) {
        throw "$Operation failed (HTTP $($Response.HttpStatus), business $($Response.Code)): $($Response.Message)"
    }
    return $Response.Data
}

function Add-LabCheck {
    param(
        [Parameter(Mandatory)][AllowEmptyCollection()][System.Collections.Generic.List[object]]$Checks,
        [string]$Name, [bool]$Passed, [object]$Expected, [object]$Actual
    )
    $Checks.Add([pscustomobject]@{Name=$Name; Passed=$Passed; Expected=$Expected; Actual=$Actual; At=(Get-Date).ToString('o')})
    $label = if ($Passed) { 'PASS' } else { 'FAIL' }
    Write-Host "[$label] $Name"
}

Export-ModuleMember -Function Assert-LabUrl, Invoke-LabRequest, Get-LabData, Add-LabCheck
