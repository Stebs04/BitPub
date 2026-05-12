#!/bin/bash

# ==============================================================================
# Script: genera_client_bitpub.sh
# Descrizione: Generazione del certificato per i Client (Simulatori/Edge).
# Autore: Tuo Partner di Programmazione
# ==============================================================================

set -e

# --- CONFIGURAZIONE ---
CERTS_DIR="certs"
CLIENT_KEY="client.key"
CLIENT_CSR="client.csr"
CLIENT_CRT="client.crt"
CA_KEY="ca.key"
CA_CRT="ca.crt"
# CN (Common Name) identifica il client.
CLIENT_SUBJECT="//C=IT/ST=Piemonte/L=Vercelli/O=BitPub/OU=BitPub Edge/CN=SimulatoreClient"
DAYS_VALID=365

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
    echo -e "${RED}[ERRORE] File CA non trovati. Assicurati di essere nella cartella giusta.${NC}"
    exit 1
fi

# --- STEP 1: Chiave privata del Client ---
echo -e "${GREEN}[1/3] Generazione chiave privata del Client (2048 bit)...${NC}"
openssl genrsa -out "$CLIENT_KEY" 2048
chmod 400 "$CLIENT_KEY"

echo -e "${GREEN}[+] Conversione della chiave in formato PKCS8 per Java...${NC}"
openssl pkcs8 -topk8 -inform PEM -in "$CLIENT_KEY" -out "client_pkcs8.key" -nocrypt
chmod 400 "client_pkcs8.key"

# --- STEP 2: Richiesta di firma (CSR) ---
echo -e "${GREEN}[2/3] Generazione richiesta di certificato (CSR)...${NC}"
openssl req -new \
    -key "$CLIENT_KEY" \
    -out "$CLIENT_CSR" \
    -subj "$CLIENT_SUBJECT"

# --- STEP 3: Firma del certificato con la Root CA ---
echo -e "${GREEN}[3/3] Firma del certificato Client con la Root CA...${NC}"
# Ti chiederà la password della ca.key!
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

echo ""
echo "======================================================"
echo -e "${GREEN}   Certificato Client generato con successo!${NC}"
echo "======================================================"
echo "  File generati in '$CERTS_DIR/':"
echo -e "  ${RED}[SEGRETO]${NC}  $CLIENT_KEY  — chiave privata del client"
echo -e "  ${GREEN}[PUBBLICO]${NC} $CLIENT_CRT  — certificato pubblico del client"
echo ""