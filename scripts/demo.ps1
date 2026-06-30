<#
.SYNOPSIS
    Script per la demo finale del progetto BitPub.
    
.DESCRIPTION
    Questo script si occupa di compilare tutti i microservizi (saltando i test per velocizzare la build),
    costruire le immagini Docker e avviare l'intero ambiente tramite docker-compose.
#>

Write-Host "=========================================="
Write-Host " Inizio Deploy Demo BitPub"
Write-Host "=========================================="

Write-Host "`n[1/3] Compilazione dei moduli Maven (salto i test)..."
# Build Common
Write-Host "--> Compilazione BitPub-Common..."
Push-Location .\BitPub-Common
mvn clean install -DskipTests
Pop-Location

# Build Cloud
Write-Host "--> Compilazione BitPub-Cloud..."
Push-Location .\BitPub-Cloud
mvn clean install -DskipTests
Pop-Location

# Build Edge
Write-Host "--> Compilazione BitPub-Edge..."
Push-Location .\BitPub-Edge
mvn clean install -DskipTests
Pop-Location

Write-Host "`n[2/3] Build delle dipendenze per la Web-App..."
Push-Location .\BitPub-WebApp
npm install
Pop-Location

Write-Host "`n[3/3] Avvio di tutti i container tramite Docker Compose..."
docker-compose up --build -d

Write-Host "`n=========================================="
Write-Host " Deploy completato con successo!"
Write-Host "=========================================="
Write-Host "Verifica i log con: docker-compose logs -f"
Write-Host "Per arrestare l'ambiente: docker-compose down"
Write-Host "=========================================="
