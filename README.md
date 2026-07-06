# BitPub — Connected Games Platform

[![Java](https://img.shields.io/badge/Java-21-orange.svg)]()
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.4-brightgreen.svg)]()
[![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud-2023.0.1-brightgreen.svg)]()
[![React](https://img.shields.io/badge/React-19-blue.svg)]()
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue.svg)]()
[![MQTT](https://img.shields.io/badge/MQTT-Mosquitto%202.0-purple.svg)]()
[![Docker](https://img.shields.io/badge/Docker-Compose-blue.svg)]()

**Progetto di Laboratorio PISSIR**
**Università del Piemonte Orientale (UPO)** — A.A. 2025/2026

---

## Team di Sviluppo

| Studente | Matricola |
|----------|-----------|
| **Stefano Bellan** | 20054330 |
| **Timothy Giolito** | 20054431 |
| **Luca Franzon** | 20054744 |

---

## Indice

1. [Descrizione](#1-descrizione)
2. [Architettura](#2-architettura)
3. [Moduli e Servizi](#3-moduli-e-servizi)
4. [Stack Tecnologico](#4-stack-tecnologico)
5. [Flusso MQTT Edge-Cloud](#5-flusso-mqtt-edge-cloud)
6. [Ruoli Utente e Autorizzazione](#6-ruoli-utente-e-autorizzazione)
7. [Funzionalità Principali](#7-funzionalità-principali)
8. [Requisiti](#8-requisiti)
9. [Avvio del Progetto](#9-avvio-del-progetto)
10. [Porte e Endpoint](#10-porte-e-endpoint)
11. [API Gateway — Routing](#11-api-gateway--routing)
12. [Database](#12-database)
13. [Struttura del Repository](#13-struttura-del-repository)
14. [Documentazione](#14-documentazione)

---

## 1. Descrizione

**BitPub** è una piattaforma distribuita per la gestione di **giochi connessi**
(*Connected Games Platform*), progettata con architettura **Edge-Cloud** a
microservizi.

Il sistema digitalizza l'esperienza di gioco di un locale fisico (pub, sala
giochi): tavoli da **calciobalilla**, **biliardo** e bersagli da **freccette**
sono dotati di sensori che pubblicano eventi su un broker MQTT locale. Un **nodo
Edge** valida e inoltra gli eventi al **cloud**, dove i microservizi gestiscono
partite, tornei, classifiche e notifiche in tempo reale. I giocatori seguono e
interagiscono con le partite dalla **WebApp**.

Obiettivi didattici coperti:

- Architettura **a microservizi** con API Gateway e servizi indipendenti.
- Comunicazione **event-driven** a bassa latenza tramite **MQTT** (pub/sub).
- Pattern **Edge-Cloud**: elaborazione di prossimità + logica centralizzata.
- **Notifiche real-time** al frontend via WebSocket (MQTT over WS).
- Persistenza relazionale, autenticazione **JWT**, autorizzazione per **ruolo**.

---

## 2. Architettura

```
                          ┌────────────────────────────────────────┐
                          │              BitPub-Cloud               │
   ┌──────────┐  REST     │  ┌──────────┐   route   ┌────────────┐  │
   │ WebApp   │──────────▶│  │ Gateway  │──────────▶│ auth       │  │
   │ (React)  │◀ ─ ─ ─ ─ ─│  │  :8080   │           │ user       │  │
   └────┬─────┘  WS/MQTT  │  └──────────┘           │ locale     │  │
        │  :9001          │        │                │ catalog    │  │
        │                 │        ▼                │ match      │  │
        │                 │  ┌──────────┐           │ tournament │  │
        │                 │  │Mosquitto │◀─────────▶│ statistics │  │
        │                 │  │  MQTT    │           │ notification│ │
        │                 │  └────┬─────┘           └────────────┘  │
        │                 └───────┼────────────────────────────────┘
        │                         │ MQTT (cloud broker)
        │                 ┌───────┴────────┐
        │                 │  BitPub-Edge   │  valida + forwarda (REST → match)
        │                 │     :8089      │
        │                 └───────┬────────┘
        │                         │ MQTT (broker locale)
        │                 ┌───────┴────────────────────────┐
        │                 │        BitPub-Simulators        │
        └────────────────▶│ biliardo · calciobalilla ·      │
                          │ freccette · demo-control-panel  │
                          └─────────────────────────────────┘
```

La **WebApp** parla **solo** con l'API Gateway (`:8080`), mai direttamente con i
microservizi. Il broker **Mosquitto** è condiviso da cloud ed edge (in Docker
puntano allo stesso container).

---

## 3. Moduli e Servizi

### BitPub-Cloud — microservizi Spring Boot

| Servizio | Porta | Responsabilità |
|----------|-------|----------------|
| `gateway-service` | 8080 | API Gateway: unico ingresso, routing, inoltro claim JWT come header |
| `auth-service` | 8081 | Registrazione, login, emissione token JWT |
| `user-service` | 8082 | Anagrafica utenti |
| `locale-service` | 8083 | Locali fisici e macchine/istanze di gioco |
| `game-catalog-service` | 8084 | Catalogo giochi e definizioni sensori |
| `match-service` | 8085 | Partite, matchmaking (lobby), gameplay a turni, ingest eventi sensori |
| `tournament-service` | 8086 | Tornei, iscrizioni, tabellone a eliminazione diretta |
| `statistics-service` | 8087 | Statistiche e leaderboard |
| `notification-service` | 8088 | Notifiche utente via MQTT |

### Altri moduli

- **BitPub-Edge** (`:8089`) — nodo edge: consuma MQTT dei dispositivi, valida e
  inoltra al `match-service` via REST. Internamente gira su 8085, mappato a 8089
  in Docker per non collidere col match-service.
- **BitPub-Common** — libreria condivisa: DTO, eventi, costanti dei topic MQTT,
  eccezioni. Usata da tutti i servizi (contratto unico).
- **BitPub-Simulators** — simulatori dei giochi fisici (biliardo, calciobalilla,
  freccette) + **demo-control-panel** (`:8090`) per pilotarli da UI.
- **BitPub-WebApp** (`:3000`) — frontend React 19 + TypeScript, servito in
  produzione da Nginx.

---

## 4. Stack Tecnologico

### Backend

- **Java 21**, **Spring Boot 3.2.4**, **Spring Cloud 2023.0.1** (Gateway)
- **Spring Data JPA** (query derivate + `@Query` JPQL) su **PostgreSQL 15**
- **Eclipse Paho MQTT** / Spring Integration MQTT — pub/sub eventi e stato
- **Lombok** (boilerplate) · **MapStruct** (mapping DTO ↔ Entity)
- **JWT** per autenticazione; autorizzazione per ruolo via header inoltrati dal
  gateway (`X-User-Id`, `X-User-Role`, `X-User-Locale-Id`)

### Frontend

- **React 19** + **TypeScript** + **Vite**
- **TailwindCSS 4**, **Framer Motion** (animazioni), **Lucide React** (icone)
- **Axios** (client REST centralizzato in `services/api.ts`)
- **Zustand** (stato auth) · **React Router 7** (routing)
- **MQTT over WebSocket** per le notifiche real-time (`notificationService.ts`)

### Infrastruttura

- **Eclipse Mosquitto 2.0** (MQTT broker, TCP `1883` + WS `9001`)
- **Docker** + **Docker Compose** (stack completo)
- **Maven** multi-module (reactor)

---

## 5. Flusso MQTT Edge-Cloud

```
[Simulatore / Device fisico]
        │  pubblica evento su topic MQTT locale
        ▼
[BitPub-Edge]  ── valida, arricchisce, inoltra ──▶  (REST)
        ▼
[match-service]  ── aggiorna stato partita, pubblica su MQTT cloud
        ▼
[notification-service]  ── ascolta MQTT, genera notifiche utente
        ▼  WebSocket (porta 9001)
[BitPub-WebApp]  ── aggiornamento real-time in UI
```

**Topic principali:**

| Topic | Uso |
|-------|-----|
| `bitpub/match/{matchId}/state` | Aggiornamenti stato partita |
| `bitpub/match/{matchId}/event` | Eventi di gioco (goal, dardo, buca…) |
| `bitpub/notifications/{userId}` | Notifiche personali |

Gli eventi sensore sono **idempotenti** (deduplicati per `eventId`), così un
re-invio dall'edge non conta due volte lo stesso punto.

---

## 6. Ruoli Utente e Autorizzazione

| Ruolo | Permessi |
|-------|----------|
| `PLATFORM_ADMIN` | Gestione utenti, statistiche globali, backfill leaderboard |
| `GAME_ADMIN` | Gestione catalogo giochi e sensori |
| `LOCALE_ADMIN` | Gestione del **proprio** locale, macchine e tornei |
| `PLAYER` | Gioca partite, vede la leaderboard, si iscrive ai tornei |

Il token JWT è conservato in `sessionStorage` (chiave `bitpub_token`) per
isolare tab e sessioni in incognito. Il gateway valida il token e propaga i
claim ai microservizi come header; ogni servizio applica il controllo di ruolo.

---

## 7. Funzionalità Principali

- **Autenticazione JWT** con quattro ruoli e autorizzazione fine per servizio.
- **Gestione locali e macchine**: ogni `LOCALE_ADMIN` opera solo sul proprio
  locale; le partite sono visibili/gestibili solo nell'ambito del locale.
- **Matchmaking a lobby**: il primo giocatore crea una lobby
  `WAITING_FOR_PLAYERS`; l'ingresso del secondo porta la partita `IN_PROGRESS`
  in tempo reale (transizione propagata via MQTT).
- **Gameplay a turni** modellato per gioco:
  - *Calciobalilla* — primo a 10 goal;
  - *Biliardo* — 8-ball con spaccata e assegnazione Piene/Spezzate;
  - *Freccette* — regola ufficiale **501 double-out** con gestione del *bust*.
- **Tornei a eliminazione diretta**: iscrizione dei `PLAYER`; il **tabellone si
  genera automaticamente** al raggiungimento di `maxParticipants` (potenza di 2).
  Solo i giocatori **abbinati** dal tabellone possono connettersi alla relativa
  partita. Avanzamento automatico dei vincitori fino alla finale.
- **Statistiche e leaderboard** aggiornate a fine partita; **backfill**
  (`PLATFORM_ADMIN`) per ricostruire la classifica dallo storico dei match.
- **Notifiche real-time** in WebApp via WebSocket.

---

## 8. Requisiti

Per l'esecuzione containerizzata (consigliata):

- **Docker** + **Docker Compose**

Per lo sviluppo locale dei singoli moduli:

- **JDK 21** e **Maven 3.9+**
- **Node.js 20+** e **npm** (per la WebApp)

---

## 9. Avvio del Progetto

Il progetto è interamente containerizzato.

### Stack completo (Docker)

```powershell
# Rebuild completo (prima volta o dopo modifiche Java) — Windows/PowerShell
.\scripts\rebuild_no_cache.ps1

# In alternativa, direttamente con Docker Compose
docker-compose up --build -d
```

Questo avvia PostgreSQL, Mosquitto, tutti i microservizi Cloud, il nodo Edge e
la WebApp.

### Solo WebApp in sviluppo

```bash
cd BitPub-WebApp
npm install
npm run dev      # dev server Vite (:5173)
npm run lint     # linting con oxlint
npm run build    # build di produzione
```

### Demo Control Panel (simulatori)

```bash
cd BitPub-Simulators/demo-control-panel
mvn spring-boot:run "-Dspring-boot.run.jvmArguments=-Dserver.port=8090"
# → http://localhost:8090
```

### Build Maven

```bash
# Tutto il progetto, dalla root
mvn clean install -DskipTests

# Un singolo modulo (con dipendenze)
mvn clean install -pl BitPub-Cloud/match-service -am -DskipTests
```

### Log dei container

```bash
docker-compose logs -f match-service
docker-compose logs -f edge-app
```

---

## 10. Porte e Endpoint

| Servizio | Container | Porta Host |
|----------|-----------|------------|
| **WebApp (Nginx)** | `bitpub-webapp` | **3000** |
| **API Gateway** | `bitpub-gateway` | **8080** |
| auth-service | `bitpub-auth` | 8081 |
| user-service | `bitpub-user` | 8082 |
| locale-service | `bitpub-locale` | 8083 |
| game-catalog-service | `bitpub-game-catalog` | 8084 |
| match-service | `bitpub-match` | 8085 |
| tournament-service | `bitpub-tournament` | 8086 |
| statistics-service | `bitpub-statistics` | 8087 |
| notification-service | `bitpub-notification` | 8088 |
| Edge Node | `bitpub-edge` | 8089 (8085 interno) |
| Demo Control Panel | *(locale)* | 8090 |
| PostgreSQL | `bitpub-postgres` | 5432 |
| Mosquitto MQTT | `bitpub-mosquitto` | 1883 (TCP), 9001 (WS) |

Accessi rapidi dopo l'avvio:

- **WebApp** → [http://localhost:3000](http://localhost:3000)
- **API Gateway** → `http://localhost:8080`
- **Simulator Panel** → [http://localhost:8090](http://localhost:8090)

---

## 11. API Gateway — Routing

Tutte le chiamate della WebApp passano dal gateway (`:8080`):

```
/api/v1/auth/**         → auth-service
/api/v1/users/**        → user-service
/api/v1/locales/**      → locale-service
/api/v1/catalog/**      → game-catalog-service
/api/v1/tournaments/**  → tournament-service
/api/v1/statistics/**   → statistics-service
/api/matches/**         → match-service   (NB: path senza /v1)
```

> `match-service` usa il prefisso `/api/matches/**` (senza `/v1`), a differenza
> di tutti gli altri servizi.

---

## 12. Database

- **Singolo database PostgreSQL** condiviso: `bitpub_db`
- Credenziali: `bitpub` / `bitpub_password`
- Ogni microservizio gestisce il proprio schema tramite JPA
- Dati persistiti nel volume Docker `pgdata`

Connessione diretta:

```bash
docker exec -it bitpub-postgres psql -U bitpub -d bitpub_db
```

---

## 13. Struttura del Repository

```
BitPub/                          (root — Maven reactor parent)
├── pom.xml                      BOM e gestione versioni Spring
├── docker-compose.yml           stack completo (servizi + infra)
├── scripts/
│   ├── rebuild_no_cache.ps1     rebuild Docker senza cache
│   └── demo.ps1                 avvio rapido demo
├── docs/
│   ├── mqtt/mosquitto.conf      config broker MQTT
│   ├── openapi/                 spec OpenAPI dei servizi
│   ├── uml/                     diagrammi UML
│   └── relazione/               relazione tecnica di progetto
│
├── bitpub-common/               libreria condivisa (DTO, eventi, costanti)
│
├── BitPub-Cloud/                microservizi Spring Boot
│   ├── gateway-service/         auth-service/        user-service/
│   ├── locale-service/          game-catalog-service/
│   ├── match-service/           tournament-service/
│   └── statistics-service/      notification-service/
│
├── BitPub-Edge/                 nodo Edge (Spring Boot + MQTT)
│
├── BitPub-Simulators/           simulatori giochi fisici
│   ├── biliardo-simulator/      calciobalilla-simulator/
│   ├── freccette-simulator/     demo-control-panel/
│
└── BitPub-WebApp/               frontend React + TypeScript
    └── src/
        ├── components/          pages/            routes/
        ├── services/            (api.ts, notificationService.ts)
        └── store/               (authStore.ts)
```

---

## 14. Documentazione

Materiale tecnico nella cartella `/docs`:

- `docs/openapi/` — specifiche **OpenAPI** dei servizi REST
- `docs/uml/` — **diagrammi UML** (componenti, sequenza, entità)
- `docs/mqtt/mosquitto.conf` — configurazione del **broker MQTT**
- `docs/relazione/` — **relazione tecnica** completa del progetto

---

*Realizzato per il corso di Laboratorio PISSIR — Università del Piemonte
Orientale (UPO).*
