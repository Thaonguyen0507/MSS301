param([string]$Maven = "")
$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
if (!(Test-Path "$env:JAVA_HOME/bin/java.exe")) {
  if (Test-Path 'C:/Program Files/Java/jdk-21/bin/java.exe') { $env:JAVA_HOME = 'C:/Program Files/Java/jdk-21' }
}
Push-Location $root
try {
  if ($Maven) { & $Maven -B -f pom.xml clean package -DskipTests }
  elseif (Get-Command mvn -ErrorAction SilentlyContinue) { & mvn -B -f pom.xml clean package -DskipTests }
  else { & ./customer-service/mvnw.cmd -B -f pom.xml clean package -DskipTests }
  if ($LASTEXITCODE -ne 0) { throw 'Build failed' }
} finally { Pop-Location }
