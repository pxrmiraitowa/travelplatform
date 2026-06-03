$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$backendDir = Join-Path $projectRoot 'travel-platform-server'
$frontendDir = Join-Path $projectRoot 'travel-platform-web'

function Test-RequiredPath {
    param(
        [string]$Path,
        [string]$Label
    )

    if (-not (Test-Path -LiteralPath $Path)) {
        throw "$Label directory not found: $Path"
    }
}

function Test-RequiredCommand {
    param(
        [string]$CommandName,
        [string]$InstallHint
    )

    if (-not (Get-Command $CommandName -ErrorAction SilentlyContinue)) {
        throw "Missing command '$CommandName'. $InstallHint"
    }
}

function Start-DevWindow {
    param(
        [string]$Title,
        [string]$WorkingDirectory,
        [string]$Command
    )

    $startupCommand = @"
Set-Location -LiteralPath '$WorkingDirectory'
$host.UI.RawUI.WindowTitle = '$Title'
Write-Host 'Starting $Title...' -ForegroundColor Cyan
$Command
"@

    Start-Process -FilePath 'powershell.exe' -ArgumentList @(
        '-NoExit',
        '-ExecutionPolicy', 'Bypass',
        '-Command', $startupCommand
    ) | Out-Null
}

Test-RequiredPath -Path $backendDir -Label 'Backend'
Test-RequiredPath -Path $frontendDir -Label 'Frontend'
Test-RequiredCommand -CommandName 'mvn' -InstallHint 'Please install Maven 3.9+ and add it to PATH.'
Test-RequiredCommand -CommandName 'npm' -InstallHint 'Please install Node.js 18+ and add npm to PATH.'

if (-not (Test-Path -LiteralPath (Join-Path $frontendDir 'node_modules'))) {
    Write-Warning "Frontend dependencies are missing. Run 'npm install' in travel-platform-web first."
}

Start-DevWindow -Title 'Travel Platform Backend' -WorkingDirectory $backendDir -Command 'mvn spring-boot:run'
Start-Sleep -Seconds 2
Start-DevWindow -Title 'Travel Platform Frontend' -WorkingDirectory $frontendDir -Command 'npm run dev'

Write-Host ''
Write-Host 'Travel Platform dev services are launching in separate windows.' -ForegroundColor Green
Write-Host 'Backend:  http://localhost:8080' -ForegroundColor Yellow
Write-Host 'Frontend: http://localhost:5173' -ForegroundColor Yellow
Write-Host 'Swagger:  http://localhost:8080/swagger-ui.html' -ForegroundColor Yellow
