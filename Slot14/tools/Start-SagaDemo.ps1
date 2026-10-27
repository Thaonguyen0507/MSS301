param(
    [switch]$Build,
    [ValidateRange(1024,65535)][int]$OrderPort = 8081,
    [ValidateRange(1024,65535)][int]$InventoryPort = 8082,
    [ValidateRange(1024,65535)][int]$PaymentPort = 8083
)
$ErrorActionPreference = 'Stop'
$slotRoot = Split-Path -Parent $PSScriptRoot
$demoRoot = Join-Path $slotRoot 'saga-demo'
$evidenceRoot = Join-Path $slotRoot 'test-evidence'
$javaInfo = New-Object System.Diagnostics.ProcessStartInfo
$javaInfo.FileName = (Get-Command java).Source
$javaInfo.Arguments = '-XshowSettings:properties -version'
$javaInfo.UseShellExecute = $false
$javaInfo.CreateNoWindow = $true
$javaInfo.RedirectStandardError = $true
$javaInfo.RedirectStandardOutput = $true
$javaProbe = [System.Diagnostics.Process]::Start($javaInfo)
$javaSettings = $javaProbe.StandardError.ReadToEnd()
$javaProbe.WaitForExit()
$javaHomeLine = $javaSettings -split "`n" | Where-Object { $_ -match '^\s*java.home\s*=' } | Select-Object -First 1
if (!$javaHomeLine) { throw 'Cannot resolve java.home' }
$javaExecutable = Join-Path (($javaHomeLine -split '=',2)[1].Trim()) 'bin\java.exe'
New-Item -ItemType Directory -Force -Path $evidenceRoot | Out-Null
Push-Location $demoRoot
try {
    docker compose up -d mysql sqlserver mongodb
    if ($LASTEXITCODE -ne 0) { throw 'Cannot start databases' }
    $deadline = (Get-Date).AddMinutes(4)
    do {
        $states = docker compose ps --format json | ForEach-Object { $_ | ConvertFrom-Json }
        if ($LASTEXITCODE -ne 0) { throw 'Cannot inspect databases' }
        $ready = @($states | Where-Object { $_.Service -in @('mysql','sqlserver','mongodb') -and $_.Health -eq 'healthy' }).Count -eq 3
        if ($ready) { break }
        if ((Get-Date) -gt $deadline) { throw 'Databases did not become healthy; inspect docker compose logs' }
        Start-Sleep -Seconds 3
    } while ($true)
    $ports = @($OrderPort,$InventoryPort,$PaymentPort)
    if (@($ports | Select-Object -Unique).Count -ne 3) { throw 'Each service requires a distinct port' }
    $busy = Get-NetTCPConnection -State Listen -ErrorAction SilentlyContinue | Where-Object { $_.LocalPort -in $ports }
    if ($busy) { throw 'A required port is in use. Stop-SagaDemo.ps1 only stops processes started by this script.' }
    $processes = @()
    foreach ($service in @('inventory-service','payment-service','order-service')) {
        $serviceRoot = Join-Path $demoRoot $service
        if ($Build) {
            Push-Location $serviceRoot
            try { & .\mvnw.cmd -B -DskipTests package; if ($LASTEXITCODE -ne 0) { throw "Build failed for $service" } }
            finally { Pop-Location }
        }
        $jar = Join-Path $serviceRoot "target\$service-0.0.1-SNAPSHOT.jar"
        if (!(Test-Path -LiteralPath $jar)) { throw "Missing jar: run with -Build" }
        $port = switch ($service) { 'order-service' { $OrderPort }; 'inventory-service' { $InventoryPort }; 'payment-service' { $PaymentPort } }
        $javaArgs = @('-Xmx256m','-Dfile.encoding=UTF-8','-Duser.timezone=Asia/Ho_Chi_Minh','-jar',$jar,"--server.port=$port","--logging.file.name=$evidenceRoot\$service.application.log",'--logging.charset.file=UTF-8')
        if ($service -eq 'order-service') {
            $javaArgs += "--services.inventory.url=http://localhost:$InventoryPort"
            $javaArgs += "--services.payment.url=http://localhost:$PaymentPort"
        }
        $process = Start-Process -FilePath $javaExecutable -ArgumentList $javaArgs -WorkingDirectory $serviceRoot -WindowStyle Hidden -RedirectStandardOutput (Join-Path $evidenceRoot "$service.stdout.log") -RedirectStandardError (Join-Path $evidenceRoot "$service.stderr.log") -PassThru
        $processes += [pscustomobject]@{service=$service;pid=$process.Id;jar=$jar}
        $processes | ConvertTo-Json | Set-Content -Encoding utf8 (Join-Path $evidenceRoot 'processes.json')
    }
    $endpoints = @("http://localhost:$InventoryPort/api/inventory/products","http://localhost:$PaymentPort/api/payments/wallets","http://localhost:$OrderPort/api/orders")
    foreach ($endpoint in $endpoints) {
        $deadline = (Get-Date).AddMinutes(2)
        while ($true) {
            try { $response = Invoke-WebRequest -UseBasicParsing -Uri $endpoint -TimeoutSec 3; if ($response.StatusCode -eq 200) { break } } catch {}
            if ((Get-Date) -gt $deadline) { throw "Service not ready: $endpoint. Inspect test-evidence logs." }
            Start-Sleep -Seconds 2
        }
    }
    Write-Host "Saga ready: order $OrderPort, inventory $InventoryPort, payment $PaymentPort"
} finally { Pop-Location }
