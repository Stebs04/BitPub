# BitPub - Development Startup Script
Write-Host "=== BitPub Development Startup ===" -ForegroundColor Green
Write-Host "Starting all services with local development overrides..." -ForegroundColor Cyan

# Avvia i servizi esponendo tutte le porte dei container localmente
docker-compose -f docker-compose.yml -f docker-compose.dev.yml --env-file .env up -d --build

Write-Host "Development environment started successfully!" -ForegroundColor Green
Write-Host "Eureka Dashboard:  http://localhost:8761" -ForegroundColor Cyan
Write-Host "API Gateway:       http://localhost:8080" -ForegroundColor Cyan
Write-Host "Grafana:           http://localhost:3000" -ForegroundColor Cyan
Write-Host "To monitor logs: docker-compose logs -f" -ForegroundColor Yellow
