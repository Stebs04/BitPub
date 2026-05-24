param (
    [string]$OutputFolder = "Docs\OpenAPI"
)

# Crea la directory di output se non esiste
if (-not (Test-Path -Path $OutputFolder)) {
    New-Item -ItemType Directory -Path $OutputFolder | Out-Null
}

$services = @(
    @{ Name = "auth-service"; Port = 8081; Dir = "services\auth-service" },
    @{ Name = "game-service"; Port = 8082; Dir = "services\game-service" },
    @{ Name = "tournament-service"; Port = 8083; Dir = "services\tournament-service" },
    @{ Name = "stats-service"; Port = 8084; Dir = "services\stats-service" }
)

Write-Host "Avvio esportazione OpenAPI per i microservizi BitPub..." -ForegroundColor Cyan

foreach ($service in $services) {
    Write-Host "`nGestione $($service.Name)..." -ForegroundColor Yellow
    
    # 1. Avvia il servizio in background
    $process = Start-Process -FilePath "mvn" -ArgumentList "spring-boot:run -pl $($service.Dir)" -WindowStyle Hidden -PassThru
    
    # 2. Attendi che l'endpoint OpenAPI sia disponibile (polling per max 60 secondi)
    $url = "http://localhost:$($service.Port)/v3/api-docs.yaml"
    $ready = $false
    $timeout = 60
    $stopwatch = [System.Diagnostics.Stopwatch]::StartNew()
    
    Write-Host "Attendo l'avvio di $($service.Name) sulla porta $($service.Port)..."
    while ($stopwatch.Elapsed.TotalSeconds -lt $timeout) {
        try {
            $response = Invoke-WebRequest -Uri $url -Method Get -ErrorAction Stop
            if ($response.StatusCode -eq 200) {
                $ready = $true
                break
            }
        } catch {
            Start-Sleep -Seconds 2
        }
    }
    
    # 3. Scarica lo YAML OpenAPI
    if ($ready) {
        $outFile = Join-Path $OutputFolder "$($service.Name)-openapi.yaml"
        Invoke-RestMethod -Uri $url -Method Get -OutFile $outFile
        Write-Host "YAML OpenAPI esportato con successo in $outFile" -ForegroundColor Green
    } else {
        Write-Host "Timeout! Impossibile raggiungere l'endpoint OpenAPI per $($service.Name)" -ForegroundColor Red
    }
    
    # 4. Termina il servizio
    Write-Host "Arresto $($service.Name)..."
    Stop-Process -Id $process.Id -Force
}

Write-Host "`nEsportazione completata! I file si trovano in $OutputFolder" -ForegroundColor Cyan
