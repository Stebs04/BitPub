# BitPub - Startup Script
# Avvia i servizi in finestre PowerShell separate nell'ordine corretto.
# Prerequisito: Docker in esecuzione con 'docker-compose -f docker-compose.yml -f docker-compose.dev.yml --env-file .env up -d postgres mosquitto'

$projectRoot = $PSScriptRoot

# Funzione per aprire una nuova finestra PowerShell e avviare un servizio
function Start-Service {
    param (
        [string]$ServiceName,
        [string]$ModulePath
    )
    Write-Host "Starting $ServiceName from $ModulePath..." -ForegroundColor Cyan
    Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$projectRoot'; mvn spring-boot:run -pl $ModulePath"
}

Write-Host "=== BitPub Microservices Startup ===" -ForegroundColor Green
Write-Host "Make sure Docker is running with: docker-compose -f docker-compose.yml -f docker-compose.dev.yml --env-file .env up -d postgres mosquitto" -ForegroundColor Yellow
Start-Sleep -Seconds 2

# 1. Discovery Server first (tutti gli altri dipendono da Eureka)
Start-Service -ServiceName "Discovery Server (Eureka :8761)" -ModulePath "infra/discovery-server"
Write-Host "Waiting 15s for Eureka to start..." -ForegroundColor Yellow
Start-Sleep -Seconds 15

# 2. Config Server
Start-Service -ServiceName "Config Server (:8888)" -ModulePath "infra/config-server"
Start-Sleep -Seconds 8

# 3. Core Services (in parallelo)
Start-Service -ServiceName "Auth Service (:8081)"         -ModulePath "services/auth-service"
Start-Service -ServiceName "Game Service (:8082)"         -ModulePath "services/game-service"
Start-Service -ServiceName "Tournament Service (:8083)"   -ModulePath "services/tournament-service"
Start-Service -ServiceName "Stats Service (:8084)"        -ModulePath "services/stats-service"
Start-Service -ServiceName "Edge-Sync Service (:8085)"    -ModulePath "services/edge-sync-service"
Start-Sleep -Seconds 20

# 4. API Gateway last (ha bisogno che i servizi siano su Eureka)
Start-Service -ServiceName "API Gateway (:8080)" -ModulePath "infra/api-gateway"

Write-Host ""
Write-Host "=== All services started! ===" -ForegroundColor Green
Write-Host "Eureka Dashboard:  http://localhost:8761" -ForegroundColor Cyan
Write-Host "API Gateway:       http://localhost:8080" -ForegroundColor Cyan
Write-Host "Grafana:           http://localhost:3000  (admin/admin)" -ForegroundColor Cyan
Write-Host "Prometheus:        http://localhost:9090" -ForegroundColor Cyan
