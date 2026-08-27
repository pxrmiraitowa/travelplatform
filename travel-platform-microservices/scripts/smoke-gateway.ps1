param(
    [string]$BaseUrl = "http://localhost:8000",
    [string]$Username = "demo",
    [string]$Password = "123456",
    [string]$AdminUsername = "admin",
    [string]$AdminPassword = "123456"
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

try {
    Invoke-JsonRequest -Method Get -Url "$BaseUrl/api/admin/dashboard" -Headers $headers
    throw "Admin route did not reject non-admin request"
} catch {
    if ($_.Exception.Response.StatusCode.value__ -ne 403) {
        throw
    }
    Write-Host "[OK] gateway rejects non-admin /api/admin/dashboard"
}

$adminLogin = Invoke-JsonRequest -Method Post -Url "$BaseUrl/api/auth/login" -Body @{
    username = $AdminUsername
    password = $AdminPassword
}
if ($adminLogin.code -ne 200 -or [string]::IsNullOrWhiteSpace($adminLogin.data.token)) {
    throw "Admin login failed or token missing"
}
Write-Host "[OK] admin auth route /api/auth/login"

$adminHeaders = @{ Authorization = "Bearer $($adminLogin.data.token)" }
$dashboard = Invoke-JsonRequest -Method Get -Url "$BaseUrl/api/admin/dashboard" -Headers $adminHeaders
if ($dashboard.code -ne 200 -or $null -eq $dashboard.data.userCount) {
    throw "Admin dashboard route failed"
}
Write-Host "[OK] admin dashboard route /api/admin/dashboard"

$adminFlights = Invoke-JsonRequest -Method Get -Url "$BaseUrl/api/admin/flights?pageNum=1&pageSize=1" -Headers $adminHeaders
if ($adminFlights.code -ne 200 -or $null -eq $adminFlights.data.records) {
    throw "Admin product route failed: /api/admin/flights"
}
Write-Host "[OK] admin product route /api/admin/flights"

$adminOrders = Invoke-JsonRequest -Method Get -Url "$BaseUrl/api/admin/orders?pageNum=1&pageSize=1" -Headers $adminHeaders
if ($adminOrders.code -ne 200 -or $null -eq $adminOrders.data.records) {
    throw "Admin order route failed: /api/admin/orders"
}
Write-Host "[OK] admin order route /api/admin/orders"

$adminShares = Invoke-JsonRequest -Method Get -Url "$BaseUrl/api/admin/shares?pageNum=1&pageSize=1" -Headers $adminHeaders
if ($adminShares.code -ne 200 -or $null -eq $adminShares.data.records) {
    throw "Admin content route failed: /api/admin/shares"
}
Write-Host "[OK] admin content route /api/admin/shares"

Write-Host "Gateway smoke test passed."
