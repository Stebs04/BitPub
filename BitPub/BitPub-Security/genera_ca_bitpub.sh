#!/bin/bash

# ==============================================================================
# Script: genera_ca_bitpub.sh
# Descrizione: Generazione della Certificate Authority (CA) root per BitPub.
# Autore: Stefano
# Scopo: Creare la chiave privata e il certificato pubblico di livello "Root",
#        necessari per firmare e autorizzare tutti i futuri certificati
#        (es. per Mosquitto e gli Edge Node).
# ==============================================================================

set -e  # Interrompe lo script immediatamente se un comando fallisce

# --- CONFIGURAZIONE ---
CERTS_DIR="certs"
CA_KEY="ca.key"
CA_CRT="ca.crt"
CA_SUBJECT="//C=IT/ST=Piemonte/L=Vercelli/O=BitPub/OU=BitPub IoT Security/CN=BitPub Root CA"
KEY_BITS=4096       # 4096 bit: più sicuro di 2048 per una CA root
DAYS_VALID=3650     # 10 anni

# --- COLORI PER OUTPUT ---
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo ""
echo "======================================================"
echo "   BitPub - Generazione Certificate Authority (CA)   "
echo "======================================================"
echo ""

# Verifica che OpenSSL sia installato
if ! command -v openssl &> /dev/null; then
    echo -e "${RED}[ERRORE] OpenSSL non trovato. Installalo prima di continuare.${NC}"
    exit 1
fi

# Crea la directory e proteggila subito (solo il proprietario può accedervi)
mkdir -p "$CERTS_DIR"
chmod 700 "$CERTS_DIR"
cd "$CERTS_DIR"

# Avvisa se i file esistono già, evitando sovrascritture accidentali
if [ -f "$CA_KEY" ] || [ -f "$CA_CRT" ]; then
    echo -e "${YELLOW}[ATTENZIONE] I file CA esistono già in '$CERTS_DIR/'.${NC}"
    read -p "Vuoi sovrascriverli? (s/N): " CONFIRM
    if [[ "$CONFIRM" != "s" && "$CONFIRM" != "S" ]]; then
        echo "Operazione annullata."
        exit 0
    fi
fi

# --- STEP 1: Chiave privata della CA ---
echo -e "${GREEN}[1/2] Generazione chiave privata CA ($KEY_BITS bit)...${NC}"

# '-aes256': cifra la chiave privata con AES-256 (richiede una passphrase).
# Rimuovi '-aes256' se vuoi automazione senza password (meno sicuro).
openssl genrsa -aes256 -out "$CA_KEY" $KEY_BITS

# Permessi restrittivi: solo il proprietario può leggere la chiave privata
chmod 400 "$CA_KEY"
echo "   -> Chiave salvata: $CERTS_DIR/$CA_KEY (sola lettura, proprietario)"

# --- STEP 2: Certificato pubblico auto-firmato ---
echo -e "${GREEN}[2/2] Generazione certificato pubblico CA ($DAYS_VALID giorni)...${NC}"

openssl req -new -x509 \
    -days $DAYS_VALID \
    -key "$CA_KEY" \
    -out "$CA_CRT" \
    -subj "$CA_SUBJECT" \
    -extensions v3_ca \
    -addext "basicConstraints=critical,CA:TRUE" \
    -addext "keyUsage=critical,keyCertSign,cRLSign" \
    -addext "subjectKeyIdentifier=hash"

chmod 444 "$CA_CRT"
echo "   -> Certificato salvato: $CERTS_DIR/$CA_CRT (sola lettura)"

# --- RIEPILOGO ---
echo ""
echo "======================================================"
echo -e "${GREEN}   Operazione completata con successo!${NC}"
echo "======================================================"
echo ""
echo "  File generati in '$CERTS_DIR/':"
echo ""
echo -e "  ${RED}[SEGRETO]${NC}  $CA_KEY  — chiave privata, NON condividere mai"
echo -e "  ${GREEN}[PUBBLICO]${NC} $CA_CRT  — da distribuire a Mosquitto e agli Edge Node"
echo ""
echo "  Verifica il certificato con:"
echo "  openssl x509 -in $CERTS_DIR/$CA_CRT -text -noout"
echo ""