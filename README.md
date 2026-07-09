<p align="center">
  <img src="docs/logo.png" alt="BitPub Logo" width="200" />
</p>

# BitPub — La Piattaforma per i Giochi Connessi

[![Java](https://img.shields.io/badge/Java-21-orange.svg)]()
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.4-brightgreen.svg)]()
[![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud-2023.0.1-brightgreen.svg)]()
[![React](https://img.shields.io/badge/React-19-blue.svg)]()
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue.svg)]()
[![MQTT](https://img.shields.io/badge/MQTT-Mosquitto%202.0-purple.svg)]()
[![Docker](https://img.shields.io/badge/Docker-Compose-blue.svg)]()

> **Progetto di Laboratorio PISSIR**  
> **Università del Piemonte Orientale (UPO)** — A.A. 2025/2026

**BitPub** è una piattaforma distribuita di ultima generazione dedicata ai **Giochi Connessi**. L'obiettivo è digitalizzare l'esperienza fisica all'interno di pub e sale giochi (calciobalilla, biliardo, freccette) attraverso l'uso di sensori IoT e la sincronizzazione Cloud in tempo reale. Grazie a un'architettura **Edge-Cloud a Microservizi**, BitPub unisce l'esperienza reale a dinamiche tipiche degli e-sports.

---

## 👥 Team e Responsabilità

Il progetto è stato sviluppato in modo collaborativo, distribuendo il carico di lavoro in maniera bilanciata tra i tre sviluppatori:

| Sviluppatore | Matricola | Responsabilità Principali |
| :--- | :--- | :--- |
| **Luca Franzon** | `20054744` | **Frontend & Security:** Sviluppo della `BitPub-WebApp` (React 19, TypeScript, TailwindCSS 4), del `gateway-service` (Spring Cloud Gateway) e dei sistemi di identità e autenticazione (`auth-service`, `user-service` con JWT). |
| **Timothy Giolito** | `20054431` | **Real-Time Engine & IoT:** Progettazione del motore di gioco (`match-service`), sviluppo del nodo di prossimità (`BitPub-Edge`) per il buffering offline, integrazione del broker `Mosquitto MQTT` e creazione dei simulatori hardware. |
| **Stefano Bellan** | `20054330` | **Core Business & Infra:** Sviluppo dei servizi `tournament-service` (tabelloni automatici), `statistics-service` (leaderboard), `locale-service` e `game-catalog-service`. Gestione dell'infrastruttura Docker e delle push notifications via WebSockets. |

---

## 📖 Indice
1. [Architettura & Design](#-architettura--design)
2. [Guida all'Avvio (Come farlo partire)](#-guida-allavvio-come-farlo-partire)
3. [Esecuzione dei Test](#-esecuzione-dei-test)
4. [Guida all'Uso del Sistema](#-guida-alluso-del-sistema)
   - [Come creare gli Utenti](#1-come-creare-gli-utenti)
   - [Come creare Locali e Macchine](#2-come-creare-locali-e-macchine)
   - [Come creare i Giochi](#3-come-creare-i-giochi-catalogo)
   - [Come creare i Tornei](#4-come-creare-i-tornei)
   - [Come giocare alle Partite](#5-come-giocare-alle-partite)
5. [Ecosistema dei Microservizi](#-ecosistema-dei-microservizi)

---

## 🏗 Architettura & Design

BitPub si basa su una moderna architettura **Edge-Cloud**.

- **Edge Layer:** I giochi fisici (tramite simulatori o sensori reali) inviano eventi a un broker MQTT locale. Il nodo `BitPub-Edge` li elabora, li valida e li bufferizza, gestendo le disconnessioni di rete temporanee verso il cloud.
- **Cloud Layer:** Una flotta di microservizi Spring Boot riceve gli eventi, calcola i punteggi, gestisce i tornei e salva i dati in un cluster PostgreSQL.
- **Client Layer:** Una WebApp in React si interfaccia con il sistema via REST tramite l'API Gateway e riceve aggiornamenti in tempo reale via WebSocket.

---

## 🏁 Guida all'Avvio (Come farlo partire)

### Prerequisiti
*   **Docker** e **Docker Compose** installati (per avviare l'intero stack).
*   **Node.js 20+** (se si vuole lanciare il frontend separatamente).
*   **JDK 21** e **Maven** (se si vuole compilare in locale o lanciare i test).

### Avvio dell'intero stack via Docker

Questo è il metodo raccomandato. Avvierà i database, il broker MQTT, il Gateway, l'Edge e tutti i microservizi.

```bash
# Da PowerShell (Windows)
.\scripts\rebuild_no_cache.ps1

# Oppure tramite terminale standard (Bash/CMD)
docker-compose up --build -d
```
> *Nota: attendi circa 30-60 secondi affinché tutti i container (in particolare Postgres e il Gateway) siano pronti e in ascolto sulle rispettive porte.*

### Accesso ai Servizi
Una volta avviato lo stack:
*   **WebApp (Frontend):** [http://localhost:3000](http://localhost:3000)
*   **API Gateway (Backend):** [http://localhost:8080](http://localhost:8080)
*   **Pannello di Controllo Simulatori:** Avviabile in locale sulla porta 8090 (vedi sotto).

### Avvio del Pannello Simulatori in locale
Per generare fisicamente gli "eventi" (come i gol del calciobalilla o i tiri a freccette), serve lanciare il pannello di simulazione:
```bash
cd BitPub-Simulators/demo-control-panel
mvn spring-boot:run "-Dspring-boot.run.jvmArguments=-Dserver.port=8090"
```
Poi naviga su: [http://localhost:8090](http://localhost:8090).

---

## 🧪 Esecuzione dei Test

Il progetto è dotato di suite di test unitari e di integrazione. Per eseguire i test su tutta la codebase backend, posizionati nella root del progetto e usa Maven:

```bash
# Esegui tutti i test nei microservizi e nei pacchetti comuni
mvn clean test
```

Se desideri eseguire i test solo per un servizio specifico (es. `match-service`):
```bash
mvn clean test -pl BitPub-Cloud/match-service
```

---

## 🎮 Guida all'Uso del Sistema

### 1. Come creare gli Utenti
All'interno dell'architettura è presente un'autenticazione RBAC (Role-Based Access Control) con JWT.
Per registrare un nuovo utente:
1. Apri la **WebApp** ([http://localhost:3000](http://localhost:3000)).
2. Vai su **Sign Up / Registrati**.
3. Inserisci Email, Username e Password.
4. Di default verrà assegnato il ruolo `PLAYER`. 
   > *Per scopi di test o amministrazione, l'utente `platform_admin` (password: `password123`) viene autogenerato dal DB con ruolo `PLATFORM_ADMIN` al primo avvio.*

### 2. Come creare Locali e Macchine
Solo un utente con ruolo `PLATFORM_ADMIN` (o `LOCALE_ADMIN` autorizzato) può definire la struttura fisica:
1. Accedi alla WebApp con un account Admin.
2. Vai nella dashboard, sezione **Gestione Locali**.
3. Clicca su **Aggiungi Locale**, indicando nome e indirizzo.
4. Dopo aver creato il locale, apri i suoi dettagli e clicca su **Registra Macchina** (es. "Tavolo Calciobalilla 1"). Il sistema assocerà la macchina al locale e genererà un ID univoco da utilizzare nel simulatore.

### 3. Come creare i Giochi (Catalogo)
Il `game-catalog-service` contiene i regolamenti:
1. Vai nella sezione **Catalogo Giochi**.
2. Clicca su **Aggiungi Gioco**.
3. Scegli il tipo di gioco:
   - *Calciobalilla* (regole: si arriva a 10 goal).
   - *Freccette* (regole: 501 a scalare, chiusura doppia).
   - *Biliardo* (regole: Palla 8, piene e mezze).
4. Assegna questo tipo di gioco alla Macchina fisica precedentemente creata.

### 4. Come creare i Tornei
1. Con un account avente privilegi adeguati (o tramite `PLATFORM_ADMIN`), vai nella sezione **Tornei**.
2. Clicca su **Crea Nuovo Torneo**.
3. Specifica:
   - Nome del torneo (es. "Coppa UPO 2026").
   - Tipo di Gioco associato (deve esistere nel catalogo).
   - Numero massimo di partecipanti (es. 8 o 16 per un tabellone a eliminazione diretta).
4. Una volta creato, i giocatori (account `PLAYER`) possono accedere alla sezione Tornei e cliccare su **Iscriviti**.
5. Quando si raggiunge il numero massimo di iscritti, il tabellone viene **generato automaticamente** e iniziano gli abbinamenti del primo turno.

### 5. Come giocare alle Partite
Ecco il flusso per disputare un incontro (es. Calciobalilla):

1. **Creazione della Lobby (Cloud):** Un utente dalla WebApp seleziona una macchina dal suo locale e clicca "Inizia Partita". Lo stato della macchina diventa `WAITING_FOR_PLAYERS`.
2. **Ingresso Giocatori:** Il secondo utente scansiona il QR Code o entra nella stessa lobby. La partita passa allo stato `IN_PROGRESS`.
3. **Generazione Eventi (Fisico/Simulatore):** 
   - Apri il **Pannello Simulatori** ([http://localhost:8090](http://localhost:8090)).
   - Inserisci l'ID della macchina in partita.
   - Clicca i bottoni per simulare gli eventi del sensore (es. "Goal Squadra A", "Goal Squadra B").
4. **Sincronizzazione Real-Time:** 
   - L'evento passa tramite l'Edge Node e arriva al Match Service.
   - Guardando la WebApp, noterai che il punteggio **si aggiorna istantaneamente** senza ricaricare la pagina grazie alle WebSocket!
5. **Fine Partita:** Quando vengono raggiunti i 10 goal (per il calciobalilla), la partita termina automaticamente (stato `FINISHED`). Le statistiche e le leaderboard vengono aggiornate istantaneamente per mostrare il vincitore!

---

## 📦 Ecosistema dei Microservizi

Ecco l'elenco dei servizi presenti, il responsabile tecnico e le porte associate:

| Servizio | Porta | Descrizione | Responsabile |
| :--- | :--- | :--- | :--- |
| `gateway-service` | 8080 | Gateway API, Routing e validazione token JWT | Luca F. |
| `auth-service` | 8081 | Registrazione, login ed emissione JWT | Luca F. |
| `user-service` | 8082 | Gestione profili e ruoli | Luca F. |
| `locale-service` | 8083 | Gestione sale, pub e macchine installate | Stefano B. |
| `game-catalog-service` | 8084 | Definizione giochi e configurazione sensori | Stefano B. |
| `match-service` | 8085 | Motore in tempo reale delle partite | Timothy G. |
| `tournament-service` | 8086 | Generazione tabelloni e iscrizioni | Stefano B. |
| `statistics-service` | 8087 | Leaderboard globali e aggregazioni dati | Stefano B. |
| `notification-service` | 8088 | Server WebSocket per le notifiche push | Stefano B. |
| `bitpub-edge` | 8089 | Nodo Edge di prossimità per i buffer MQTT | Timothy G. |
