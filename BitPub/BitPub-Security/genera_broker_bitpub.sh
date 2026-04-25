#!/bin/bash

# ==============================================================================
# Script: genera_broker_bitpub.sh
# Descrizione: Generazione del certificato per il broker Mosquitto.
# Autore: Tuo Partner di Programmazione
# Scopo: Creare la chiave e il certificato per Mosquitto, firmandoli con la Root CA.
# ==============================================================================

set -e

# --- CONFIGURAZIONE ---
CERTS_DIR="certs"
BROKER_KEY="broker.key"
BROKER_CSR="broker.csr"
BROKER_CRT="broker.crt"
CA_KEY="ca.key"
CA_CRT="ca.crt"
# Il Common Name (CN) di solito dovrebbe corrispondere al nome host del server.
# Nel nostro caso Docker, usiamo il nome del container o localhost.
BROKER_SUBJECT="//C=IT/ST=Piemonte/L=Vercelli/O=BitPub/OU=BitPub Broker/CN=localhost"
DAYS_VALID=365      # Validità di 1 anno per il server

# --- COLORI PER OUTPUT ---
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m'

echo ""
echo "======================================================"
echo "   BitPub - Generazione Certificato Broker Mosquitto  "
echo "======================================================"
echo ""

cd "$CERTS_DIR"

# Verifica che la CA esista
if [ ! -f "$CA_KEY" ] || [ ! -f "$CA_CRT" ]; then
    echo -e "${RED}[ERRORE] File CA ($CA_KEY o $CA_CRT) non trovati in $CERTS_DIR/. Esegui prima genera_ca_bitpub.sh.${NC}"
    exit 1
fi

# --- STEP 1: Chiave privata del Broker ---
echo -e "${GREEN}[1/3] Generazione chiave privata del Broker (2048 bit)...${NC}"
# Nota: NON usiamo -aes256 qui. Se mettessimo una password, Mosquitto si bloccherebbe
# all'avvio chiedendola, impedendo l'avvio automatico del container Docker.
openssl genrsa -out "$BROKER_KEY" 2048
chmod 400 "$BROKER_KEY"

# --- STEP 2: Richiesta di firma (CSR) ---
echo -e "${GREEN}[2/3] Generazione richiesta di certificato (CSR)...${NC}"
openssl req -new \
    -key "$BROKER_KEY" \
    -out "$BROKER_CSR" \
    -subj "$BROKER_SUBJECT"

# --- STEP 3: Firma del certificato con la Root CA ---
echo -e "${GREEN}[3/3] Firma del certificato del Broker con la Root CA...${NC}"
# -CAcreateserial crea un file .srl per tenere traccia dei certificati firmati
openssl x509 -req \
    -in "$BROKER_CSR" \
    -CA "$CA_CRT" \
    -CAkey "$CA_KEY" \
    -CAcreateserial \
    -out "$BROKER_CRT" \
    -days $DAYS_VALID \
    -sha256

chmod 444 "$BROKER_CRT"

# Pulizia: il file CSR non serve più dopo la firma
rm "$BROKER_CSR"

echo ""
echo "======================================================"
echo -e "${GREEN}   Certificato del Broker generato con successo!${NC}"
echo "======================================================"
echo "  File generati in '$CERTS_DIR/':"
echo -e "  ${RED}[SEGRETO]${NC}  $BROKER_KEY  — chiave privata del broker"
echo -e "  ${GREEN}[PUBBLICO]${NC} $BROKER_CRT  — certificato pubblico del broker"
echo ""