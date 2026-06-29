#!/bin/bash

# ==============================================================================
# Script: genera_ca_bitpub.sh
# Descrizione: Generazione della Certificate Authority (CA) root per BitPub.
# Scopo: Creare la chiave privata e il certificato pubblico di livello "Root",
#        necessari per firmare e autorizzare tutti i futuri certificati
#        (es. per Mosquitto e gli Edge Node).
# FIX: Chiave CA senza password per compatibilità con script automatici.
#      Soggetto aggiornato per coerenza con il progetto BitPub.
# ==============================================================================

set -e

# --- CONFIGURAZIONE ---
CERTS_DIR="certs"
CA_KEY="ca.key"
CA_CRT="ca.crt"
CA_SUBJECT="/C=IT/ST=Piemonte/L=Vercelli/O=BitPub/OU=BitPub IoT Security/CN=BitPub Root CA"
KEY_BITS=4096
DAYS_VALID=3650

# --- COLORI PER OUTPUT ---
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

echo ""
echo "======================================================"
echo "   BitPub - Generazione Certificate Authority (CA)   "
echo "======================================================"
echo ""

if ! command -v openssl &> /dev/null; then
    echo -e "${RED}[ERRORE] OpenSSL non trovato. Installalo prima di continuare.${NC}"
    exit 1
fi

mkdir -p "$CERTS_DIR"
chmod 700 "$CERTS_DIR"
cd "$CERTS_DIR"

if [ -f "$CA_KEY" ] || [ -f "$CA_CRT" ]; then
    echo -e "${YELLOW}[ATTENZIONE] I file CA esistono già in '$CERTS_DIR/'.${NC}"
    read -p "Vuoi sovrascriverli? (s/N): " CONFIRM
    if [[ "$CONFIRM" != "s" && "$CONFIRM" != "S" ]]; then
        echo "Operazione annullata."
        exit 0
    fi
fi

# --- STEP 1: Chiave privata della CA (senza password per automazione) ---
echo -e "${GREEN}[1/2] Generazione chiave privata CA ($KEY_BITS bit)...${NC}"
openssl genrsa -out "$CA_KEY" $KEY_BITS
chmod 400 "$CA_KEY"
echo "   -> Chiave salvata: $CERTS_DIR/$CA_KEY"

# --- STEP 2: Certificato pubblico auto-firmato ---
echo -e "${GREEN}[2/2] Generazione certificato pubblico CA ($DAYS_VALID giorni)...${NC}"
openssl req -new -x509 \
    -days $DAYS_VALID \
    -key "$CA_KEY" \
    -out "$CA_CRT" \
    -subj "$CA_SUBJECT" \
    -addext "basicConstraints=critical,CA:TRUE" \
    -addext "keyUsage=critical,keyCertSign,cRLSign" \
    -addext "subjectKeyIdentifier=hash"

chmod 444 "$CA_CRT"
echo "   -> Certificato salvato: $CERTS_DIR/$CA_CRT"

echo ""
echo "======================================================"
echo -e "${GREEN}   Operazione completata con successo!${NC}"
echo "======================================================"
echo ""
echo "  File generati in '$CERTS_DIR/':"
echo -e "  ${RED}[SEGRETO]${NC}  $CA_KEY  — chiave privata, NON condividere mai"
echo -e "  ${GREEN}[PUBBLICO]${NC} $CA_CRT  — da distribuire a Mosquitto e agli Edge Node"
echo ""
echo "  Verifica il certificato con:"
echo "  openssl x509 -in $CERTS_DIR/$CA_CRT -text -noout"
echo ""