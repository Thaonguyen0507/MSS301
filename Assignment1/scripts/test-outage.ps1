$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
Push-Location $root
try {
  $services = Get-Content results/services.json -Raw | ConvertFrom-Json
  $movie = $services | Where-Object service -eq 'movie-service'
  $process = Get-CimInstance Win32_Process -Filter "ProcessId = $($movie.pid)"
  if ($process.CommandLine -notlike '*movie-service-0.0.1-SNAPSHOT.jar*') { throw 'Movie Service PID does not match' }
  Stop-Process -Id $movie.pid
  try {
    & ./node_modules/.bin/newman.cmd run postman/BR14-Movie-Outage.postman_collection.json -e postman/FUCinema-Local.postman_environment.json --reporters cli,json,junit --reporter-json-export results/outage-newman-results.json --reporter-junit-export results/outage-newman-results.xml
    $exitCode = $LASTEXITCODE
  } finally {
    $java = if (Test-Path "$env:JAVA_HOME/bin/java.exe") { "$env:JAVA_HOME/bin/java.exe" } elseif (Test-Path 'C:/Program Files/Java/jdk-21/bin/java.exe') { 'C:/Program Files/Java/jdk-21/bin/java.exe' } else { 'java' }
    $jar = "$root/movie-service/target/movie-service-0.0.1-SNAPSHOT.jar"
    $p = Start-Process -FilePath $java -ArgumentList @('-Duser.timezone=Asia/Ho_Chi_Minh','-jar',$jar) -WorkingDirectory $root -WindowStyle Hidden -PassThru -RedirectStandardOutput "$root/results/movie-restart.log" -RedirectStandardError "$root/results/movie-restart.err.log"
    $movie.pid = $p.Id
    $services | ConvertTo-Json | Set-Content results/services.json -Encoding UTF8
  }
  if ($exitCode -ne 0) { throw 'Outage tests failed' }
} finally { Pop-Location }
