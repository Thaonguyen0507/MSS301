$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
New-Item "$root/results" -ItemType Directory -Force | Out-Null
Push-Location $root
try {
  docker compose up -d
  if ($LASTEXITCODE -ne 0) { throw 'Infrastructure startup failed' }
  $java = if (Test-Path "$env:JAVA_HOME/bin/java.exe") { "$env:JAVA_HOME/bin/java.exe" } elseif (Test-Path 'C:/Program Files/Java/jdk-21/bin/java.exe') { 'C:/Program Files/Java/jdk-21/bin/java.exe' } else { 'java' }
  $processes = @()
  foreach ($service in @('inventory-service','product-service','order-service','api-gateway')) {
    $jar = "$root/$service/target/$service-0.0.1-SNAPSHOT.jar"
    if (!(Test-Path $jar)) { throw "Missing $jar; build project first" }
    $p = Start-Process -FilePath $java -ArgumentList @('-jar',$jar) -WorkingDirectory $root -WindowStyle Hidden -PassThru -RedirectStandardOutput "$root/results/$service.log" -RedirectStandardError "$root/results/$service.err.log"
    $processes += @{ service = $service; pid = $p.Id }
  }
  $processes | ConvertTo-Json | Set-Content "$root/results/services.json" -Encoding UTF8
  $ready = $false
  for ($i=0; $i -lt 90; $i++) {
    try {
      foreach ($service in @('product-service','order-service','inventory-service')) { $null = Invoke-RestMethod "http://localhost:9000/aggregate/$service/v3/api-docs" }
      $null = Invoke-RestMethod 'http://localhost:8181/realms/spring-microservices-realm'
      $ready = $true; break
    } catch { Start-Sleep -Seconds 2 }
  }
  if (!$ready) { throw 'Services did not become ready; inspect results/*.log' }
  Write-Host 'Swagger ready: http://localhost:9000/swagger-ui.html'
} finally { Pop-Location }
