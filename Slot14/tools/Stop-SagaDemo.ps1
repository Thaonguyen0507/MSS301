param([switch]$StopDatabases)
$ErrorActionPreference='Stop'
$slotRoot=Split-Path -Parent $PSScriptRoot
$demoRoot=[IO.Path]::GetFullPath((Join-Path $slotRoot 'saga-demo'))
$processFile=Join-Path $slotRoot 'test-evidence\processes.json'
if (Test-Path -LiteralPath $processFile) {
    foreach ($item in (Get-Content -LiteralPath $processFile -Raw | ConvertFrom-Json)) {
        $process=Get-CimInstance Win32_Process -Filter "ProcessId = $($item.pid)"
        if ($process -and $process.CommandLine.Contains($demoRoot)) {
            Stop-Process -Id $item.pid
            Write-Host "Stopped $($item.service) PID $($item.pid)"
        }
    }
}
if ($StopDatabases) {
    Push-Location $demoRoot
    try { docker compose stop mysql sqlserver mongodb } finally { Pop-Location }
}
