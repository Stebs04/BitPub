#!/bin/bash
# Specifica al sistema operativo di utilizzare l'interprete Bash per eseguire questo script.

# ==============================================================================
# Script: genera_ca_bitpub.sh
# Descrizione: Generazione della Certificate Authority (CA) root per BitPub.
# Autore: Stefano
# Scopo: Creare la chiave privata e il certificato pubblico di livello "Root",
#        necessari per firmare e autorizzare tutti i futuri certificati
#        (es. per Mosquitto e gli Edge Node).
# ==============================================================================

# Stampa a terminale un messaggio visivo per indicare l'inizio dell'esecuzione.
echo "--- Inizio Creazione della Certificate Authority (CA) per BitPub ---"

# Crea la directory 'certs' per mantenere l'output ordinato.
# Il flag '-p' previene errori nel caso in cui la cartella esista già.
mkdir -p certs

# Cambia la directory di lavoro corrente, posizionandosi all'interno di 'certs'.
# Tutti i file generati dai comandi successivi verranno salvati qui.
cd certs

# Comunica all'utente l'inizio del primo processo logico: la creazione della chiave.
echo "1. Generazione della chiave privata della CA (ca.key)..."

# Invoca OpenSSL per generare una chiave privata basata sull'algoritmo RSA.
# '-out ca.key': definisce il nome del file di output.
# '2048': imposta la lunghezza della chiave a 2048 bit (standard di sicurezza attuale).
# NOTA CRITICA: Questo file è il segreto principale del sistema e NON deve mai essere condiviso.
openssl genrsa -out ca.key 2048

# Comunica all'utente l'inizio del secondo processo logico: la creazione del certificato.
echo "2. Generazione del certificato pubblico root (ca.crt)..."

# Invoca OpenSSL per creare il certificato pubblico auto-firmato che rappresenta la nostra CA.
# 'req': richiama il modulo per la gestione delle richieste di certificati (PKCS#10).
# '-new': indica che stiamo creando una nuova richiesta.
# '-x509': forza l'output a essere un certificato auto-firmato, invece di una semplice richiesta.
# '-days 3650': imposta la validità temporale del certificato a 10 anni (365 giorni * 10).
# '-key ca.key': specifica la chiave privata (appena creata) da usare per firmare questo certificato.
# '-out ca.crt': definisce il nome del file pubblico risultante.
# '-subj "..."': inietta automaticamente i dati del "Distinguished Name" (DN) scavalcando il prompt interattivo.
openssl req -new -x509 -days 3650 -key ca.key -out ca.crt \
    -subj "/C=IT/ST=Piemonte/L=Vercelli/O=BitPub/OU=BitPub IoT Security/CN=BitPub Root CA"

# Stampa un messaggio di successo per confermare che l'intero flusso è terminato senza errori.
echo "--- Operazione completata con successo! ---"

# Fornisce un promemoria finale sulla natura e la posizione dei file generati..
echo "Troverai i file 'ca.key' (DA TENERE SEGRETO) e 'ca.crt' (PUBBLICO) nella cartella 'certs'."