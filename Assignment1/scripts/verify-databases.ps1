$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
Push-Location $root
try {
  docker compose ps -a | Out-File results/docker-status.txt -Encoding utf8
  docker cp sqlserver/check.sql cinema-sqlserver:/check.sql
  docker exec cinema-sqlserver /opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P 'Fucinema@2026' -C -b -f 65001 -i /check.sql | Out-File results/sqlserver-check.txt -Encoding utf8
  if ($LASTEXITCODE -ne 0) { throw 'SQL Server verification failed' }
  docker cp scripts/mongo-check.js cinema-mongo:/check.js
  docker exec cinema-mongo mongosh -u root -p password --authenticationDatabase admin cinema_movie --quiet /check.js | Out-File results/mongodb-check.txt -Encoding utf8
  if ($LASTEXITCODE -ne 0) { throw 'MongoDB verification failed' }
  docker cp mysql/check.sql cinema-mysql:/check.sql
  docker exec -e MYSQL_PWD=mysql cinema-mysql sh -c 'mysql -uroot < /check.sql' 2>&1 | Out-File results/mysql-check.txt -Encoding utf8
  if ($LASTEXITCODE -ne 0) { throw 'MySQL verification failed' }
} finally { Pop-Location }
