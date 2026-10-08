$root = Split-Path $PSScriptRoot -Parent
$realm = Get-Content "$root/api-gateway/docker/keycloak/realms/spring-microservices-realm.json" -Raw | ConvertFrom-Json
$client = $realm.clients | Where-Object serviceAccountsEnabled | Select-Object -First 1
$result = Invoke-RestMethod -Method Post -Uri "http://localhost:8181/realms/$($realm.realm)/protocol/openid-connect/token" -ContentType 'application/x-www-form-urlencoded' -Body @{ grant_type='client_credentials'; client_id=$client.clientId; client_secret=$client.secret }
$result.access_token
