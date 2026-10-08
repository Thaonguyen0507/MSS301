$root = Split-Path $PSScriptRoot -Parent
if (Test-Path "$root/results/services.json") {
  foreach ($p in (Get-Content "$root/results/services.json" -Raw | ConvertFrom-Json)) {
    $proc = Get-CimInstance Win32_Process -Filter "ProcessId = $($p.pid)" -ErrorAction SilentlyContinue
    if ($proc -and $proc.CommandLine -like "*$($p.service)-0.0.1-SNAPSHOT.jar*") { Stop-Process -Id $p.pid }
  }
}
