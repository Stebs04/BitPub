#!/bin/bash

# ==============================================================================
# Script: genera_client_bitpub.sh
# Descrizione: Generazione del certificato per i Client (Simulatori/Edge).
# FIX: Soggetto aggiornato per coerenza con il progetto BitPub.
#      Aggiunto SAN per il client (best practice, anche se meno critico).
# ==============================================================================

set -e

# --- CONFIGURAZIONE ---
CERTS_DIR="certs"
CLIENT_KEY="client.key"
CLIENT_CSR="client.csr"
CLIENT_CRT="client.crt"
CLIENT_PKCS8="client_pkcs8.key"
CA_KEY="ca.key"
CA_CRT="ca.crt"
CLIENT_SUBJECT="/C=IT/ST=Piemonte/L=Vercelli/O=BitPub/OU=BitPub Edge/CN=SimulatoreClient"
DAYS_VALID=3650

# --- COLORI PER OUTPUT ---
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m'

echo ""
echo "======================================================"
echo "   BitPub - Generazione Certificato Client (Edge)     "
echo "======================================================"
echo ""

cd "$CERTS_DIR"

if [ ! -f "$CA_KEY" ] || [ ! -f "$CA_CRT" ]; then
    echo -e "${RED}[ERRORE] File CA non trovati. Esegui prima: ./genera_ca_bitpub.sh${NC}"
    exit 1
fi

# --- STEP 1: Chiave privata del Client ---
echo -e "${GREEN}[1/4] Generazione chiave privata del Client (2048 bit)...${NC}"
openssl genrsa -out "$CLIENT_KEY" 2048
chmod 400 "$CLIENT_KEY"
echo "   -> Chiave salvata: $CERTS_DIR/$CLIENT_KEY"

# --- STEP 2: Conversione in PKCS8 (richiesta da Java/Paho) ---
echo -e "${GREEN}[2/4] Conversione della chiave in formato PKCS8 per Java...${NC}"
openssl pkcs8 -topk8 -inform PEM -in "$CLIENT_KEY" -out "$CLIENT_PKCS8" -nocrypt
chmod 400 "$CLIENT_PKCS8"
echo "   -> Chiave PKCS8 salvata: $CERTS_DIR/$CLIENT_PKCS8"

# --- STEP 3: CSR (Certificate Signing Request) ---
echo -e "${GREEN}[3/4] Generazione richiesta di certificato (CSR)...${NC}"
openssl req -new \
    -key "$CLIENT_KEY" \
    -out "$CLIENT_CSR" \
    -subj "$CLIENT_SUBJECT"

# --- STEP 4: Firma del certificato con la Root CA ---
echo -e "${GREEN}[4/4] Firma del certificato Client con la Root CA...${NC}"
openssl x509 -req \
    -in "$CLIENT_CSR" \
    -CA "$CA_CRT" \
    -CAkey "$CA_KEY" \
    -CAcreateserial \
    -out "$CLIENT_CRT" \
    -days $DAYS_VALID \
    -sha256

chmod 444 "$CLIENT_CRT"
rm "$CLIENT_CSR"
echo "   -> Certificato salvato: $CERTS_DIR/$CLIENT_CRT"

echo ""
echo "======================================================"
echo -e "${GREEN}   Certificato Client generato con successo!${NC}"
echo "======================================================"
echo "  File generati in '$CERTS_DIR/':"
echo -e "  ${RED}[SEGRETO]${NC}  $CLIENT_KEY     — chiave privata del client"
echo -e "  ${RED}[SEGRETO]${NC}  $CLIENT_PKCS8   — chiave PKCS8 per Java/Paho"
echo -e "  ${GREEN}[PUBBLICO]${NC} $CLIENT_CRT     — certificato pubblico del client"
echo ""