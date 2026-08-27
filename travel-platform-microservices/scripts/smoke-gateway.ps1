param(
    [string]$BaseUrl = "http://localhost:8000",
    [string]$Username = "demo",
    [string]$Password = "123456"
)

$ErrorActionPreference = "Stop"

function Invoke-JsonRequest {
    param(
        [string]$Method,
        [string]$Url,
        [object]$Body = $null,
        [hashtable]$Headers = @{}
    )

    $params = @{
        Method = $Method
        Uri = $Url
        Headers = $Headers
    }
    if ($null -ne $Body) {
        $params.ContentType = "application/json"
        $params.Body = ($Body | ConvertTo-Json -Depth 10)
    }
    Invoke-RestMethod @params
}

Write-Host "Gateway smoke test: $BaseUrl"

$flights = Invoke-JsonRequest -Method Get -Url "$BaseUrl/api/public/flights?pageNum=1&pageSize=1"
if ($flights.code -ne 200) {
    throw "Public product route failed: /api/public/flights"
}
Write-Host "[OK] product route /api/public/flights"

try {
    Invoke-JsonRequest -Method Get -Url "$BaseUrl/api/orders"
    throw "Protected route did not reject anonymous request"
} catch {
    if ($_.Exception.Response.StatusCode.value__ -ne 401) {
        throw
    }
    Write-Host "[OK] gateway rejects anonymous /api/orders"
}

$login = Invoke-JsonRequest -Method Post -Url "$BaseUrl/api/auth/login" -Body @{
    username = $Username
    password = $Password
}
if ($login.code -ne 200 -or [string]::IsNullOrWhiteSpace($login.data.token)) {
    throw "Login route failed or token missing"
}
Write-Host "[OK] auth route /api/auth/login"

$headers = @{ Authorization = "Bearer $($login.data.token)" }
$me = Invoke-JsonRequest -Method Get -Url "$BaseUrl/api/users/me" -Headers $headers
if ($me.code -ne 200 -or $null -eq $me.data.id) {
    throw "Authenticated user route failed: /api/users/me"
}
Write-Host "[OK] authenticated route /api/users/me"

Write-Host "Gateway smoke test passed."
