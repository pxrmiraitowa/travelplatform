#requires -Version 7.2
[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$failures = [Collections.Generic.List[string]]::new()

function Add-DeliveryCheck {
    param([string]$Name,[bool]$Passed,[string]$Actual)
    $label = if ($Passed) { 'PASS' } else { 'FAIL' }
    Write-Host "[$label] $Name - $Actual"
    if (-not $Passed) { $failures.Add("${Name}: $Actual") }
}

$required = @(
    'README.md',
    '.env.example',
    '.github/workflows/ci-cd.yml',
    'deploy/member-e-benchmark/hpa.yaml',
    'docs/成员E-第一二阶段完成报告-20260831.md',
    'docs/成员E-第一二阶段完成报告-20260831.docx',
    'output/pdf/成员E-第一二阶段完成报告-20260831.pdf',
    'experiments/results/latest-evidence-index.json',
    'experiments/results/成员E-原始证据说明.md',
    'experiments/results/成员E-最新版原始证据-aece119-20260831.zip'
)
foreach ($relative in $required) {
    Add-DeliveryCheck "Required file $relative" (Test-Path -LiteralPath (Join-Path $root $relative)) 'present'
}

$conflictOutput = @(& git -C $root grep -n -I -E '^(<<<<<<<|=======|>>>>>>>)' -- .)
$conflictExit = $LASTEXITCODE
Add-DeliveryCheck 'No tracked Git conflict markers' ($conflictExit -eq 1) $(if ($conflictExit -eq 1) {'none'} elseif ($conflictOutput.Count) {$conflictOutput -join '; '} else {"git grep exit $conflictExit"})

$jsonFiles = @(
    'experiments/results/latest-evidence-index.json',
    'deploy/member-e-benchmark/protocol.json',
    'experiments/protocols/member-e-formal-comparison.json'
)
foreach ($relative in $jsonFiles) {
    try {
        Get-Content -Raw -LiteralPath (Join-Path $root $relative) | ConvertFrom-Json -AsHashtable | Out-Null
        Add-DeliveryCheck "JSON $relative" $true 'valid'
    } catch {
        Add-DeliveryCheck "JSON $relative" $false $_.Exception.Message
    }
}

$index = Get-Content -Raw -LiteralPath (Join-Path $root 'experiments/results/latest-evidence-index.json') | ConvertFrom-Json
$archive = Join-Path $root $index.deliveryArchive.file
if (Test-Path -LiteralPath $archive) {
    $archiveItem = Get-Item -LiteralPath $archive
    $archiveHash = (Get-FileHash -LiteralPath $archive -Algorithm SHA256).Hash
    Add-DeliveryCheck 'Evidence archive byte length' ($archiveItem.Length -eq [long]$index.deliveryArchive.bytes) "$($archiveItem.Length)"
    Add-DeliveryCheck 'Evidence archive SHA-256' ($archiveHash -eq [string]$index.deliveryArchive.sha256) $archiveHash
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $zip = [IO.Compression.ZipFile]::OpenRead($archive)
    try {
        Add-DeliveryCheck 'Evidence archive entry count' ($zip.Entries.Count -eq [int]$index.deliveryArchive.entryCount) "$($zip.Entries.Count)"
        foreach ($entry in $zip.Entries | Where-Object { -not [string]::IsNullOrEmpty($_.Name) }) {
            $stream = $entry.Open()
            try { $stream.CopyTo([IO.Stream]::Null) } finally { $stream.Dispose() }
        }
        Add-DeliveryCheck 'Evidence archive decompression' $true 'all file entries readable'
    } catch {
        Add-DeliveryCheck 'Evidence archive decompression' $false $_.Exception.Message
    } finally {
        $zip.Dispose()
    }
}

$parseFailures = [Collections.Generic.List[string]]::new()
$powerShellFiles = @(Get-ChildItem -LiteralPath (Join-Path $root 'scripts'),(Join-Path $root 'experiments/scripts') -Recurse -File | Where-Object Extension -in @('.ps1','.psm1'))
foreach ($file in $powerShellFiles) {
    $tokens = $null
    $errors = $null
    [Management.Automation.Language.Parser]::ParseFile($file.FullName,[ref]$tokens,[ref]$errors) | Out-Null
    foreach ($error in @($errors)) { $parseFailures.Add("$($file.Name):$($error.Extent.StartLineNumber) $($error.Message)") }
}
Add-DeliveryCheck 'PowerShell syntax' ($parseFailures.Count -eq 0) $(if ($parseFailures.Count) {$parseFailures -join '; '} else {"$($powerShellFiles.Count) files parsed"})

& git -C $root ls-files --error-unmatch .env *> $null
$envTracked = $LASTEXITCODE -eq 0
Add-DeliveryCheck 'Local .env is not tracked' (-not $envTracked) $(if ($envTracked) {'tracked'} else {'ignored; .env.example remains tracked'})

try {
    Import-Module (Join-Path $root 'experiments/scripts/LabEvidence.psm1') -Force
    $compatibility = Assert-LabSourceCompatibility -ProjectRoot $root -BaselineRevision ([string]$index.sourceRevision)
    $delta = @($compatibility.ApprovedNonRuntimeDelta)
    Add-DeliveryCheck 'Experiment source compatibility' $true $(if ($delta.Count) {"approved development-only delta: $($delta.Path -join ', ')"} else {'no application-source delta'})
} catch {
    Add-DeliveryCheck 'Experiment source compatibility' $false $_.Exception.Message
}

$report = Get-Content -Raw -LiteralPath (Join-Path $root 'docs/成员E-第一二阶段完成报告-20260831.md')
$requiredReportValues = @('311.06','324.37','52.56','49.33','283.93','260.69','1121.03','1302.50','794.40','1272.73','279A7B1C9FA94820D9BD86A93D3C2CE788D1ED4737006ACE868CC36566EF4D41')
$missingValues = @($requiredReportValues | Where-Object { -not $report.Contains($_) })
Add-DeliveryCheck 'Report key evidence values' ($missingValues.Count -eq 0) $(if ($missingValues.Count) {"missing: $($missingValues -join ', ')"} else {'all present'})

if ($failures.Count -gt 0) {
    Write-Host "Delivery verification failed: $($failures.Count) check(s)."
    exit 1
}
Write-Host 'Member E delivery verification passed.'
