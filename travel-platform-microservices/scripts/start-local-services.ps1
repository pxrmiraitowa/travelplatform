param(
    [string]$Maven = "C:\Program Files\apache-maven-3.9.11\bin\mvn.cmd",
    [string]$JavaHome = "C:\Program Files\Java\jdk-24"
)

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
$LogDir = Join-Path $Root "logs"
New-Item -ItemType Directory -Force -Path $LogDir | Out-Null

$env:JAVA_HOME = $JavaHome
$env:Path = "$env:JAVA_HOME\bin;$env:Path"

$services = @(
    @{ Name = "user-service"; Port = 8101 },
    @{ Name = "product-service"; Port = 8102 },
    @{ Name = "order-service"; Port = 8103 },
    @{ Name = "content-trip-service"; Port = 8104 },
    @{ Name = "gateway-service"; Port = 8000 }
)

foreach ($service in $services) {
    $out = Join-Path $LogDir "$($service.Name).out.log"
    $err = Join-Path $LogDir "$($service.Name).err.log"
    if (Test-Path $out) { Remove-Item -LiteralPath $out -Force }
    if (Test-Path $err) { Remove-Item -LiteralPath $err -Force }

    $command = "Set-Location '$Root'; `$env:JAVA_HOME='$JavaHome'; `$env:Path=`"`$env:JAVA_HOME\bin;`$env:Path`"; & '$Maven' -pl $($service.Name) spring-boot:run"
    $process = Start-Process -FilePath "powershell.exe" `
        -ArgumentList "-NoProfile", "-Command", $command `
        -WindowStyle Hidden `
        -RedirectStandardOutput $out `
        -RedirectStandardError $err `
        -PassThru

    Write-Host "Started $($service.Name) on port $($service.Port), pid=$($process.Id)"
}

Write-Host "Logs are in $LogDir"
Write-Host "Run scripts\smoke-gateway.ps1 after the services finish starting."
