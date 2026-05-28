#!/bin/bash

# ==============================================================================
# Script: genera_broker_bitpub.sh
# Descrizione: Generazione del certificato per il broker Mosquitto.
# Scopo: Creare la chiave e il certificato per Mosquitto, firmandoli con la Root CA.
#
# FIX CRITICO: Aggiunta estensione SAN (Subject Alternative Name) con:
#   - DNS:localhost     -> per connessioni locali con hostname
#   - DNS:mosquitto     -> per connessioni intra-Docker (nome container)
#   - IP:127.0.0.1      -> per connessioni locali con IP (Java verifica il SAN!)
#
# Java (dalla versione 7+) verifica il SAN invece del CN per l'hostname.
# Senza SAN, Java lancia: PKIX path building failed / certificate_unknown
# ==============================================================================

set -e

# --- CONFIGURAZIONE ---
CERTS_DIR="certs"
BROKER_KEY="broker.key"
BROKER_CSR="broker.csr"
BROKER_CRT="broker.crt"
CA_KEY="ca.key"
CA_CRT="ca.crt"
BROKER_SUBJECT="/C=IT/ST=Piemonte/L=Vercelli/O=BitPub/OU=BitPub Broker/CN=localhost"
DAYS_VALID=3650

# SAN critici per Java: include localhost, 127.0.0.1 e il nome del container Docker
BROKER_SAN="DNS:localhost,DNS:mosquitto,IP:127.0.0.1"

# --- COLORI PER OUTPUT ---
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

echo ""
echo "======================================================"
echo "   BitPub - Generazione Certificato Broker Mosquitto  "
echo "======================================================"
echo ""

cd "$CERTS_DIR"

if [ ! -f "$CA_KEY" ] || [ ! -f "$CA_CRT" ]; then
    echo -e "${RED}[ERRORE] File CA ($CA_KEY o $CA_CRT) non trovati in $CERTS_DIR/.${NC}"
    echo "         Esegui prima: ./genera_ca_bitpub.sh"
    exit 1
fi

# --- STEP 1: Chiave privata del Broker ---
echo -e "${GREEN}[1/3] Generazione chiave privata del Broker (2048 bit)...${NC}"
# NON usiamo -aes256: Mosquitto legge la chiave all'avvio e non può chiedere password
openssl genrsa -out "$BROKER_KEY" 2048
chmod 400 "$BROKER_KEY"
echo "   -> Chiave salvata: $CERTS_DIR/$BROKER_KEY"

# --- STEP 2: CSR (Certificate Signing Request) con SAN ---
echo -e "${GREEN}[2/3] Generazione richiesta di certificato (CSR) con SAN...${NC}"
echo "   -> SAN configurati: $BROKER_SAN"
openssl req -new \
    -key "$BROKER_KEY" \
    -out "$BROKER_CSR" \
    -subj "$BROKER_SUBJECT" \
    -addext "subjectAltName=$BROKER_SAN"

# --- STEP 3: Firma del certificato con la Root CA (copia estensioni SAN) ---
echo -e "${GREEN}[3/3] Firma del certificato del Broker con la Root CA...${NC}"
echo -e "${YELLOW}   [NOTA] -copy_extensions copy assicura che il SAN venga incluso nel cert finale.${NC}"
openssl x509 -req \
    -in "$BROKER_CSR" \
    -CA "$CA_CRT" \
    -CAkey "$CA_KEY" \
    -CAcreateserial \
    -out "$BROKER_CRT" \
    -days $DAYS_VALID \
    -sha256 \
    -copy_extensions copy

chmod 444 "$BROKER_CRT"
rm "$BROKER_CSR"
echo "   -> Certificato salvato: $CERTS_DIR/$BROKER_CRT"

echo ""
echo "======================================================"
echo -e "${GREEN}   Certificato del Broker generato con successo!${NC}"
echo "======================================================"
echo "  File generati in '$CERTS_DIR/':"
echo -e "  ${RED}[SEGRETO]${NC}  $BROKER_KEY  — chiave privata del broker"
echo -e "  ${GREEN}[PUBBLICO]${NC} $BROKER_CRT  — certificato pubblico del broker"
echo ""
echo "  Verifica SAN con:"
echo "  openssl x509 -in $CERTS_DIR/$BROKER_CRT -text -noout | grep -A2 'Subject Alternative'"
echo ""