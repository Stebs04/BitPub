# ==============================================================================
# Script: genera_certificati.ps1
# Descrizione: Rigenera TUTTI i certificati BitPub (CA + Broker + Client)
#              direttamente su Windows con OpenSSL.
#
# USO: Esegui dalla cartella BitPub-Security:
#      .\genera_certificati.ps1
#
# PREREQUISITI: OpenSSL installato e disponibile nel PATH
#
# FIX INCLUSI:
#   - Broker certificate con SAN (DNS:localhost, DNS:mosquitto, IP:127.0.0.1)
#     necessario per Java (PKIX path building failed fix)
#   - CA senza password per compatibilita' con Docker/automazione
#   - Chiave client convertita in PKCS8 per Paho MQTT Java
# ==============================================================================

$ErrorActionPreference = "Stop"

# --- CONFIGURAZIONE ---
$CERTS_DIR = "certs"
$CA_KEY    = "$CERTS_DIR\ca.key"
$CA_CRT    = "$CERTS_DIR\ca.crt"
$BROKER_KEY = "$CERTS_DIR\broker.key"
$BROKER_CRT = "$CERTS_DIR\broker.crt"
$BROKER_CSR = "$CERTS_DIR\broker.csr"
$CLIENT_KEY  = "$CERTS_DIR\client.key"
$CLIENT_CRT  = "$CERTS_DIR\client.crt"
$CLIENT_CSR  = "$CERTS_DIR\client.csr"
$CLIENT_PKCS8 = "$CERTS_DIR\client_pkcs8.key"

$CA_SUBJECT     = "/C=IT/ST=Piemonte/L=Vercelli/O=BitPub/OU=BitPub IoT Security/CN=BitPub Root CA"
$BROKER_SUBJECT = "/C=IT/ST=Piemonte/L=Vercelli/O=BitPub/OU=BitPub Broker/CN=localhost"
$CLIENT_SUBJECT = "/C=IT/ST=Piemonte/L=Vercelli/O=BitPub/OU=BitPub Edge/CN=SimulatoreClient"

# SAN critici per Java: include tutti i modi in cui Edge si connette al broker
$BROKER_SAN = "DNS:localhost,DNS:mosquitto,IP:127.0.0.1"

$DAYS_CA     = 3650
$DAYS_CERT   = 3650

# --- COLORI ---
function Write-Step  { param($msg) Write-Host "`n[STEP] $msg" -ForegroundColor Cyan }
function Write-OK    { param($msg) Write-Host "  [OK] $msg" -ForegroundColor Green }
function Write-Warn  { param($msg) Write-Host "  [!!] $msg" -ForegroundColor Yellow }
function Write-Err   { param($msg) Write-Host "  [ERRORE] $msg" -ForegroundColor Red }

# --- VERIFICA OPENSSL ---
Write-Host ""
Write-Host "======================================================" -ForegroundColor Magenta
Write-Host "   BitPub - Generazione Certificati SSL/TLS" -ForegroundColor Magenta
Write-Host "======================================================" -ForegroundColor Magenta
Write-Host ""

try {
    $ver = openssl version
    Write-OK "OpenSSL trovato: $ver"
} catch {
    Write-Err "OpenSSL non trovato nel PATH. Installalo e riprova."
    exit 1
}

# Crea cartella certs se non esiste
if (-not (Test-Path $CERTS_DIR)) {
    New-Item -ItemType Directory -Path $CERTS_DIR | Out-Null
    Write-OK "Cartella '$CERTS_DIR' creata."
}

# Avvisa se i certificati esistono gia' e li rimuove (erano read-only)
$existingCerts = @($CA_KEY, $CA_CRT, $BROKER_KEY, $BROKER_CRT, $BROKER_CSR, $CLIENT_KEY, $CLIENT_CRT, $CLIENT_CSR, $CLIENT_PKCS8, "$CERTS_DIR\ca.srl")
$anyExist = $existingCerts | Where-Object { Test-Path $_ }
if ($anyExist) {
    Write-Warn "Certificati esistenti trovati in '$CERTS_DIR\'. Verranno sovrascritti."
    $confirm = Read-Host "Continuare? (s/N)"
    if ($confirm -notmatch "^[sS]$") {
        Write-Host "Operazione annullata." -ForegroundColor Yellow
        exit 0
    }
    Write-Host "  Rimozione certificati vecchi (potrebbero essere read-only)..."
    foreach ($f in $existingCerts) {
        if (Test-Path $f) {
            # Forza rimozione anche se read-only
            Set-ItemProperty -Path $f -Name IsReadOnly -Value $false -ErrorAction SilentlyContinue
            Remove-Item $f -Force -ErrorAction SilentlyContinue
        }
    }
    Write-OK "Vecchi certificati rimossi."
}

# ============================================================
# FASE 1: CA ROOT
# ============================================================
Write-Step "Fase 1/3 - Generazione Certificate Authority (CA) Root"

Write-Host "  Generazione chiave CA (4096 bit)..."
openssl genrsa -out $CA_KEY 4096
Write-OK "Chiave CA generata: $CA_KEY"

Write-Host "  Generazione certificato CA auto-firmato ($DAYS_CA giorni)..."
openssl req -new -x509 `
    -days $DAYS_CA `
    -key $CA_KEY `
    -out $CA_CRT `
    -subj $CA_SUBJECT `
    -addext "basicConstraints=critical,CA:TRUE" `
    -addext "keyUsage=critical,keyCertSign,cRLSign" `
    -addext "subjectKeyIdentifier=hash"
Write-OK "Certificato CA generato: $CA_CRT"

# ============================================================
# FASE 2: CERTIFICATO BROKER (con SAN per Java)
# ============================================================
Write-Step "Fase 2/3 - Generazione Certificato Broker Mosquitto (con SAN)"
Write-Warn "SAN configurati: $BROKER_SAN"
Write-Host "  (necessari per Java: senza SAN -> PKIX path building failed)"

Write-Host "  Generazione chiave broker (2048 bit)..."
openssl genrsa -out $BROKER_KEY 2048
Write-OK "Chiave broker generata: $BROKER_KEY"

Write-Host "  Generazione CSR broker con SAN..."
openssl req -new `
    -key $BROKER_KEY `
    -out $BROKER_CSR `
    -subj $BROKER_SUBJECT `
    -addext "subjectAltName=$BROKER_SAN"

Write-Host "  Firma del certificato broker con la CA (copia SAN con -copy_extensions copy)..."
openssl x509 -req `
    -in $BROKER_CSR `
    -CA $CA_CRT `
    -CAkey $CA_KEY `
    -CAcreateserial `
    -out $BROKER_CRT `
    -days $DAYS_CERT `
    -sha256 `
    -copy_extensions copy

Remove-Item $BROKER_CSR -ErrorAction SilentlyContinue
Write-OK "Certificato broker generato: $BROKER_CRT"

# ============================================================
# FASE 3: CERTIFICATO CLIENT (Edge Node / Simulatori)
# ============================================================
Write-Step "Fase 3/3 - Generazione Certificato Client (Edge Node)"

Write-Host "  Generazione chiave client (2048 bit)..."
openssl genrsa -out $CLIENT_KEY 2048
Write-OK "Chiave client generata: $CLIENT_KEY"

Write-Host "  Conversione chiave in PKCS8 (richiesta da Java/Paho MQTT)..."
openssl pkcs8 -topk8 -inform PEM -in $CLIENT_KEY -out $CLIENT_PKCS8 -nocrypt
Write-OK "Chiave PKCS8 generata: $CLIENT_PKCS8"

Write-Host "  Generazione CSR client..."
openssl req -new `
    -key $CLIENT_KEY `
    -out $CLIENT_CSR `
    -subj $CLIENT_SUBJECT

Write-Host "  Firma del certificato client con la CA..."
openssl x509 -req `
    -in $CLIENT_CSR `
    -CA $CA_CRT `
    -CAkey $CA_KEY `
    -CAcreateserial `
    -out $CLIENT_CRT `
    -days $DAYS_CERT `
    -sha256

Remove-Item $CLIENT_CSR -ErrorAction SilentlyContinue
Write-OK "Certificato client generato: $CLIENT_CRT"

# ============================================================
# VERIFICA FINALE
# ============================================================
Write-Step "Verifica certificati generati"

Write-Host "  Verifica CA:"
openssl x509 -in $CA_CRT -noout -subject -dates
Write-Host ""
Write-Host "  Verifica Broker (SAN):"
openssl x509 -in $BROKER_CRT -noout -subject -ext subjectAltName
Write-Host ""
Write-Host "  Verifica Client:"
openssl x509 -in $CLIENT_CRT -noout -subject -dates

Write-Host ""
Write-Host "======================================================" -ForegroundColor Green
Write-Host "   Tutti i certificati generati con successo!" -ForegroundColor Green
Write-Host "======================================================" -ForegroundColor Green
Write-Host ""
Write-Host "File in '$CERTS_DIR\':" -ForegroundColor White
Write-Host "  [SEGRETO]  ca.key           - chiave privata CA" -ForegroundColor Red
Write-Host "  [PUBBLICO] ca.crt           - certificato CA" -ForegroundColor Green
Write-Host "  [SEGRETO]  broker.key       - chiave privata broker" -ForegroundColor Red
Write-Host "  [PUBBLICO] broker.crt       - certificato broker (con SAN)" -ForegroundColor Green
Write-Host "  [SEGRETO]  client.key       - chiave privata client" -ForegroundColor Red
Write-Host "  [SEGRETO]  client_pkcs8.key - chiave PKCS8 per Java" -ForegroundColor Red
Write-Host "  [PUBBLICO] client.crt       - certificato client" -ForegroundColor Green
Write-Host ""
Write-Host "PROSSIMI PASSI:" -ForegroundColor Yellow
Write-Host "  1. Riavvia il container Docker Mosquitto:"
Write-Host "     docker-compose -f docker-compose.yml -f docker-compose.dev.yml restart mosquitto"
Write-Host "  2. Verifica connessione SSL:"
Write-Host "     openssl s_client -connect 127.0.0.1:8883 -CAfile $CA_CRT"
Write-Host "  3. Avvia BitPub-Edge:"
Write-Host "     cd BitPub\BitPub-Edge && mvn exec:java"
Write-Host ""
