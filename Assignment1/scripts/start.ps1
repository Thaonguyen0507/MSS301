$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
New-Item "$root/results" -ItemType Directory -Force | Out-Null
& "$PSScriptRoot/stop.ps1"
Push-Location $root
try {
  docker compose up -d
  if ($LASTEXITCODE -ne 0) { throw 'Database startup failed' }
  $java = if (Test-Path "$env:JAVA_HOME/bin/java.exe") { "$env:JAVA_HOME/bin/java.exe" } elseif (Test-Path 'C:/Program Files/Java/jdk-21/bin/java.exe') { 'C:/Program Files/Java/jdk-21/bin/java.exe' } else { 'java' }
  $processes = @()
  foreach ($service in @('customer-service','movie-service','booking-service','api-gateway')) {
    $jar = "$root/$service/target/$service-0.0.1-SNAPSHOT.jar"
    if (!(Test-Path $jar)) { throw "Missing $jar; run scripts/build.ps1 first" }
    $p = Start-Process -FilePath $java -ArgumentList @('-Duser.timezone=Asia/Ho_Chi_Minh','-jar', $jar) -WorkingDirectory $root -WindowStyle Hidden -PassThru -RedirectStandardOutput "$root/results/$service.log" -RedirectStandardError "$root/results/$service.err.log"
    $processes += @{ service = $service; pid = $p.Id }
  }
  $processes | ConvertTo-Json | Set-Content "$root/results/services.json" -Encoding UTF8
  $ready = $false
  for ($i = 0; $i -lt 90; $i++) {
    try { $null = Invoke-RestMethod -Method Post -Uri 'http://localhost:9000/api/auth/login' -ContentType 'application/json' -Body '{"email":"admin@fucinema.com","password":"@@abc123@@"}'; $null = Invoke-RestMethod 'http://localhost:9000/api/movies'; $null = Invoke-RestMethod 'http://localhost:9000/api/bookings/showtimes/66f300000000000000000001/seats'; $ready = $true; break } catch { Start-Sleep -Seconds 2 }
  }
  if (!$ready) { throw 'Services did not become ready; inspect results/*.log' }
  Write-Host 'Ready: http://localhost:9000'
} finally { Pop-Location }
