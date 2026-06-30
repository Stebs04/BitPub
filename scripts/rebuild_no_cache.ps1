<#
.SYNOPSIS
    Script per la pulizia completa e rebuild del progetto BitPub senza l'uso di cache.
    
.DESCRIPTION
    Questo script compila tutti i microservizi (inclusi i simulatori), 
    forza la build delle immagini Docker ignorando la cache (--no-cache),
    e riavvia l'ambiente tramite docker-compose forzando la ricreazione dei container.
#>

Write-Host "=========================================="
Write-Host " Inizio Rebuild Completo BitPub (No Cache)"
Write-Host "=========================================="

Write-Host "`n[1/4] Arresto dei container attuali..."
docker-compose down

Write-Host "`n[2/4] Compilazione dei moduli Maven (salto i test)..."
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

# Build Simulators
Write-Host "--> Compilazione BitPub-Simulators..."
Push-Location .\BitPub-Simulators
mvn clean install -DskipTests
Pop-Location

Write-Host "`n[3/4] Build delle dipendenze per la Web-App..."
Push-Location .\BitPub-WebApp
npm install
Pop-Location

Write-Host "`n[4/4] Build delle immagini Docker SENZA CACHE e avvio..."
docker-compose build --no-cache
docker-compose up -d --force-recreate

Write-Host "`n=========================================="
Write-Host " Rebuild completato con successo!"
Write-Host "=========================================="
Write-Host "Verifica i log con: docker-compose logs -f"
Write-Host "=========================================="
