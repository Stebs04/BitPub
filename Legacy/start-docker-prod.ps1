# BitPub - Production Startup Script
Write-Host "=== BitPub Production Startup ===" -ForegroundColor Green
Write-Host "Starting all services in detached mode with production overrides..." -ForegroundColor Cyan

# Carica le variabili d'ambiente di produzione
$envFile = ".env.prod"
if (Test-Path $envFile) {
    Get-Content $envFile | Foreach-Object {
        if ($_ -match '^(.*?)=(.*)$') {
            [Environment]::SetEnvironmentVariable($matches[1], $matches[2], "Process")
        }
    }
}

# Costruisci e avvia
docker-compose -f docker-compose.yml -f docker-compose.prod.yml --env-file .env.prod up -d --build

Write-Host "Production environment started successfully!" -ForegroundColor Green
Write-Host "API Gateway is available on port 80." -ForegroundColor Yellow
Write-Host "To monitor logs: docker-compose logs -f" -ForegroundColor Yellow
