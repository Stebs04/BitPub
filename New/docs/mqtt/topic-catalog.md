# Catalogo Topic MQTT - BitPub

Questo documento descrive i topic MQTT utilizzati all'interno dell'ecosistema BitPub, specialmente tra i Simulatori (Hardware) e l'applicazione Edge (Locale).

## Broker MQTT
- **Implementazione**: Eclipse Mosquitto
- **Sicurezza**: mTLS (Mutual TLS) abilitato in produzione, auth username/password in dev.
- **QoS Consigliato**: QoS 1 (At least once) per eventi critici (es. gol, colpi freccette), QoS 0 per eventi non critici (es. heartbeat sensori).

## Struttura Base del Topic
`bitpub/locales/{localeId}/machines/{machineId}/{category}/{action}`

## Topic in Ingresso (Da Simulatori a Edge)

### 1. Inizio / Fine Partita (Calciobalilla / Biliardo)
- **Topic**: `bitpub/locales/+/machines/+/match/status`
- **Descrizione**: Segnala l'inizio o la fine di una partita quando viene inserito un gettone o premuto start sul simulatore.
- **Payload Example**:
  ```json
  {
    "eventId": "123e4567-e89b-12d3-a456-426614174000",
    "timestamp": "2023-10-27T10:00:00Z",
    "status": "STARTED" // o "FINISHED"
  }
  ```

### 2. Evento Goal (Calciobalilla)
- **Topic**: `bitpub/locales/+/machines/+/events/goal`
- **Descrizione**: Emesso dal sensore ottico quando la pallina entra in porta.
- **Payload Example**:
  ```json
  {
    "eventId": "123e4567-e89b-12d3-a456-426614174001",
    "timestamp": "2023-10-27T10:05:22Z",
    "team": "RED",
    "sensorId": "sensor_red_goal"
  }
  ```

### 3. Evento Hit (Freccette)
- **Topic**: `bitpub/locales/+/machines/+/events/dart_hit`
- **Descrizione**: Emesso dal bersaglio elettronico.
- **Payload Example**:
  ```json
  {
    "eventId": "123e4567-e89b-12d3-a456-426614174002",
    "timestamp": "2023-10-27T10:15:00Z",
    "multiplier": 3,
    "segmentValue": 20
  }
  ```

### 4. Heartbeat (Salute Macchina)
- **Topic**: `bitpub/locales/+/machines/+/health/heartbeat`
- **Descrizione**: Inviato regolarmente (es. ogni 30s) per indicare che la macchina è online.
- **Payload Example**:
  ```json
  {
    "status": "ONLINE",
    "uptimeSeconds": 3600
  }
  ```

## Topic in Uscita (Da Edge/Cloud a Kiosk/Simulatore)

### 1. Aggiornamento Punteggio Real-time (Sottoscritto da Kiosk)
- **Topic**: `bitpub/locales/+/machines/+/match/score`
- **Descrizione**: Inviato dal Cloud (o Edge) per aggiornare lo schermo del Kiosk con il punteggio validato.
- **Payload Example**:
  ```json
  {
    "matchId": "...",
    "scoreTeamRed": 5,
    "scoreTeamBlue": 3
  }
  ```

### 2. Comandi Remoti (Da Admin Piattaforma a Macchina)
- **Topic**: `bitpub/locales/+/machines/+/commands`
- **Descrizione**: Invia comandi di reboot, update firmware, o blocco macchina.
- **Payload Example**:
  ```json
  {
    "command": "RESTART_SYSTEM"
  }
  ```
