# System Agents DAG Configuration

Questo documento definisce l'architettura di orchestrazione multi-agente per il repository **BitPub** (piattaforma distribuita Cloud-Edge per Giochi Connessi con microservizi Spring Boot 3, broker MQTT Mosquitto, interfacce React 19/TypeScript e infrastruttura DevOps Docker Compose).

Per massimizzare l'acquisizione formale della conoscenza, prevenire allucinazioni del modello e azzerare le esecuzioni ridondanti o i conflitti di concorrenza, l'interazione tra gli agenti è strutturata rigorosamente come un **Grafo Aciclico Diretto (DAG - Directed Acyclic Graph)**. La conoscenza fluisce in modo unidirezionale e *strictly downstream*: ogni agente (Nodo) si attiva esclusivamente al soddisfacimento dei contratti di input emessi dai nodi upstream e produce output tipizzati per i nodi a valle.

---

## 1. Execution Graph (Mermaid/ASCII)

Il flusso di esecuzione del sistema è articolato su **quattro livelli di dipendenza (Level 0 - Level 3)**. Di seguito è riportata la rappresentazione formale della topologia del DAG, sia in formato grafico Mermaid che testuale ASCII.

### Mermaid DAG Topology

```mermaid
graph TD
    %% Level 0 - Root
    subgraph L0["Level 0 — Root Orchestration"]
        N0["NODE_0: NODE_ORCHESTRATOR<br/>(System Orchestrator & Router)"]
    end

    %% Level 1 - Architectural & Domain Context
    subgraph L1["Level 1 — Architectural & Domain Authority"]
        N1["NODE_1: NODE_ARCHITECT<br/>(API, DTO & MQTT Contracts Authority)"]
    end

    %% Level 2 - Parallel Execution Nodes
    subgraph L2["Level 2 — Parallel Vertical Execution Nodes (Zero Mutual Dependencies)"]
        N2_1["NODE_2.1: NODE_AUTH_USER<br/>(Identity, Security & Auth)"]
        N2_2["NODE_2.2: NODE_CATALOG_LOCALE<br/>(Locale & Game Catalog)"]
        N2_3["NODE_2.3: NODE_MATCH_REALTIME<br/>(Real-Time Match Engine)"]
        N2_4["NODE_2.4: NODE_TOURNAMENT_STATS<br/>(Tournaments & Statistics)"]
        N2_5["NODE_2.5: NODE_GATEWAY_NOTIF<br/>(API Gateway & WS Notif)"]
        N2_6["NODE_2.6: NODE_EDGE_MQTT<br/>(Edge Node & IoT Simulators)"]
        N2_7["NODE_2.7: NODE_FRONTEND<br/>(WebApp React/TS/Tailwind)"]
    end

    %% Level 3 - Verification & DevOps
    subgraph L3["Level 3 — Verification, Integration & Release"]
        N3_1["NODE_3.1: NODE_TEST_E2E<br/>(E2E & Integration Engineer)"]
        N3_2["NODE_3.2: NODE_DOCKER_DEVOPS<br/>(Containerization & DevOps)"]
    end

    %% Edges: Level 0 -> Level 1
    N0 ==>|"Task Dispatch Plan & Context Scope"| N1

    %% Edges: Level 1 -> Level 2 (Validated Contracts Broadcast)
    N1 -->|"Auth/User DTOs, JWT Specs"| N2_1
    N1 -->|"Locale/Machine DTOs, OpenAPI"| N2_2
    N1 -->|"MqttTopics, SensorEvent Schema"| N2_3
    N1 -->|"Bracket/Stats DTOs, Leaderboard API"| N2_4
    N1 -->|"JWT Headers Contract, Routes, WS Spec"| N2_5
    N1 -->|"MqttTopics, Sync Schema, Simulators Spec"| N2_6
    N1 -->|"OpenAPI JSON/YAML, TypeScript Interfaces"| N2_7

    %% Edges: Level 2 -> Level 3.1 (E2E Integration Gate)
    N2_1 -->|"auth-service.jar, user-service.jar"| N3_1
    N2_2 -->|"locale-service.jar, catalog-service.jar"| N3_1
    N2_3 -->|"match-service.jar"| N3_1
    N2_4 -->|"tournament-service.jar, statistics-service.jar"| N3_1
    N2_5 -->|"gateway-service.jar, notification-service.jar"| N3_1
    N2_6 -->|"bitpub-edge.jar, demo-control-panel.jar"| N3_1
    N2_7 -->|"React WebApp Build (dist/)"| N3_1

    %% Edges: Level 3.1 -> Level 3.2
    N3_1 ==>|"Certified Test Matrix & E2E Pass Logs"| N3_2

    %% Error Feedback Loop (Strict Downstream Immutability / Re-routing)
    N3_1 -.->|"Defect Contract / Contract Violation<br/>(New DAG Cycle Re-route)"| N0
    N3_2 -.->|"Infrastructure/Docker Build Failure<br/>(New DAG Cycle Re-route)"| N0

    classDef l0 style=#1e293b,stroke=#38bdf8,stroke-width:2px,color:#fff;
    classDef l1 style=#312e81,stroke=#818cf8,stroke-width:2px,color:#fff;
    classDef l2 style=#065f46,stroke=#34d399,stroke-width:2px,color:#fff;
    classDef l3 style=#701a75,stroke=#f472b6,stroke-width:2px,color:#fff;
    class N0 l0;
    class N1 l1;
    class N2_1,N2_2,N2_3,N2_4,N2_5,N2_6,N2_7 l2;
    class N3_1,N3_2 l3;
```

### ASCII Execution Graph

```
===================================================================================================================
                                      [Level 0 - ROOT ORCHESTRATION]
                                       +--------------------------+
                                       | NODE_0: NODE_ORCHESTRATOR|
                                       +--------------------------+
                                                     |
                                                     v  Task Dispatch Plan & Target Subgraph
                                       +--------------------------+
                                       |  NODE_1: NODE_ARCHITECT  |
                                       +--------------------------+
                                      [Level 1 - CONTRACTS AUTHORITY]
                                                     |
            +--------------------+-------------------+-------------------+--------------------+
            |                    |                   |                   |                    |
            v                    v                   v                   v                    v
  +------------------+  +------------------+  +------------------+  +------------------+  +------------------+
  |    NODE_2.1      |  |    NODE_2.2      |  |    NODE_2.3      |  |    NODE_2.4      |  |  NODE_2.5 - 2.7  |
  |  NODE_AUTH_USER  |  |NODE_CATALOG_LOCAL|  |NODE_MATCH_REALTIM|  |NODE_TOURNAMENT_ST|  |GATEWAY/EDGE/FRONT|
  +------------------+  +------------------+  +------------------+  +------------------+  +------------------+
                            [Level 2 - PARALLEL VERTICAL EXECUTION NODES]
            |                    |                   |                   |                    |
            +--------------------+-------------------+-------------------+--------------------+
                                                     |
                                                     v  Compiled Artifacts & Unit Test Pass Reports
                                       +--------------------------+
                                       |  NODE_3.1: NODE_TEST_E2E |  <-- [Level 3 - VERIFICATION]
                                       +--------------------------+
                                                     |
                                                     v  Certified Test Matrix & E2E Pass Logs
                                       +--------------------------+
                                       | NODE_3.2: DOCKER_DEVOPS  |  <-- [Level 3 - DEVOPS RELEASE]
                                       +--------------------------+
===================================================================================================================
```

---

## 2. Global Execution Guardrails

Per preservare l'integrità architettonica di BitPub e garantire una collaborazione multi-agente esente da conflitti, tutti gli agenti operanti nel DAG devono rispettare le seguenti regole di esecuzione globali:

### 2.1. Regole di Immutabilità e Confini di Directory (Domain Ownership)
- **Zero Cross-Domain Mutation:** Ogni nodo del DAG è proprietario esclusivo di un perimetro di directory rigorosamente definito (`Scope`). È assolutamente vietato a un agente creare, eliminare o modificare file al di fuori del proprio `Scope`.
- **Modifiche Trans-Dominio (Cross-Cutting Changes):** Qualora uno sviluppo verticale in un nodo di *Level 2* richieda una modifica strutturale ai contratti condivisi (ad esempio un nuovo DTO in `bitpub-common`, un nuovo topic MQTT in `MqttTopics.java` o una modifica all'OpenAPI in `docs/openapi/`), l'agente non può procedere in autonomia. Deve emettere una *Contract Amendment Request* e restituire il controllo al *Level 0 / Level 1* per la ripubblicazione del contratto.
- **State Immutability (Flusso strictly downstream):** Gli agenti del DAG non possono eseguire scritture o mutazioni a ritroso su rami *upstream*. Se i nodi di verifica (*Level 3*) rilevano un'incongruenza di integrazione o un test E2E fallito, non modificano direttamente il codice di livello 2, ma instradano un payload strutturato di errore a `NODE_ORCHESTRATOR` (`NODE_0`) per l'avvio di una nuova iterazione pulita del DAG.
- **Integrità della Documentazione:** Tutti i commenti tecnici, le javadoc e le annotazioni OpenAPI esistenti non devono essere rimossi o indeboliti durante il refactoring o la scrittura di nuovo codice.

### 2.2. Context Filtering (Prevenzione del Clutter e delle Allucinazioni)
- **Contesto Segmentato e Tipizzato:** Gli agenti di *Level 2* hanno il divieto di indicizzare o leggere l'intero repository. Devono ingerire esclusivamente:
  1. Il *Contract Bundle* emesso da `NODE_ARCHITECT` (DTO del pacchetto `bitpub-common`, interfacce OpenAPI pertinenti e costanti dei topic MQTT `MqttTopics.java`).
  2. I file di codice all'interno delle proprie directory (`Scope`).
  3. I capitoli pertinenti della documentazione d'architettura (`README.md`, `docs/openapi/api-catalog.md`, `docs/mqtt/topic-catalog.md`).
- **Nessuna Presunzione sulle Dipendenze Inter-Servizio:** I microservizi comunicano esclusivamente tramite interfacce REST definite nel Gateway (`/api/v1/**`) o tramite topic MQTT validati via Edge Node. Nessuna chiamata di rete hardcoded con localhost è permessa in produzione: vanno utilizzate le variabili d'ambiente e la risoluzione del DNS di Docker Compose (es. `http://user-service:8082`).

### 2.3. Gestione della Concorrenza e del Contesto Pulito
- **Parallelismo Massivo su Level 2:** Tutti i nodi di *Level 2* (`NODE_2.1` – `NODE_2.7`) sono indipendenti tra loro e **devono essere eseguiti in parallelo** dal motore di orchestrazione non appena il nodo `NODE_ARCHITECT` completa la compilazione e validazione dei contratti `BitPub-Common`.
- **Isolamento del Build e del Testing:** Ogni sub-agente esegue i test unitari o di slice Maven/Npm nel proprio contesto isolato (`mvn clean test -pl BitPub-Cloud/<service>` o `npm test` nella WebApp). Non è consentito eseguire modifiche sul parent POM o su moduli condivisi in concorrenza.
- **Clean Context Recovery:** Prima di avviare il *Level 3*, l'orchestratore verifica che tutti i rami paralleli abbiano uno status `DONE` con build di successo e zero modifiche non committate fuori perimetro.

---

## 3. Nodes Definition

### Node 0: System Orchestrator & Router
- **ID**: `NODE_ORCHESTRATOR`
- **Dependencies**: `None (Root - Entry Point)`
- **Scope**: `/` (Lettura in sola analisi della root directory, coordinamento del workspace, log di orchestrazione del DAG).
- **Input Contract**: Richiesta utente esplicita (`USER_REQUEST`), stato del git working tree, `README.md`, `pom.xml` principale, metadati di configurazione dell'ambiente Docker/Windows.
- **Output Contract**: `TaskDispatchPlan` (piano di esecuzione JSON/YAML con l'elenco dei nodi del DAG da attivare), `TargetDomainScope` (mapping di directory coinvolte), assegnazione iniziale dei contratti al *Level 1*.
- **Behavior & Allowed Tools**:
  - *Comportamento*: Analizza la richiesta dell'utente per identificare quali domini d'architettura sono impattati (Cloud, Edge, IoT, WebApp, DevOps). Smista il lavoro e istanzia le chiamate ai sub-agenti secondo l'ordine del DAG. Ha il divieto assoluto di scrivere o modificare codice sorgente applicativo.
  - *Tool Abilitati*: Strumenti in sola lettura (`list_dir`, `view_file`, `grep_search`). Non autorizzato all'uso di comandi di scrittura o modifica file applicativi.

---

### Node 1: Architectural, API & MQTT Contracts Authority
- **ID**: `NODE_ARCHITECT`
- **Dependencies**: `NODE_ORCHESTRATOR`
- **Scope**: `bitpub-common/**`, `docs/openapi/**`, `docs/mqtt/**`, `docs/*.md`, `pom.xml` (parent POM per la gestione delle versioni e delle dipendenze Maven).
- **Input Contract**: `TaskDispatchPlan` fornito dal Root Orchestrator, schemi DTO ed eventi esistenti nel pacchetto `it.uniupo.pissir.bitpub.common/**`, costanti topic MQTT in `MqttTopics.java`, specifiche in `docs/openapi/api-catalog.md` e `docs/mqtt/topic-catalog.md`.
- **Output Contract**: 
  - Modulo `BitPub-Common` compilato, versionato e installato nella cache locale (`bitpub-common.jar`).
  - `ContractBundle`: specifiche OpenAPI 3.0 aggiornate per gli endpoint `/api/v1/**`, schemi dei payload MQTT (`SensorEvent`, `MqttCommandWrapper`, `TournamentResultCommand`), classi DTO invarianti (`UserDto`, `LocaleDto`, `MachineDto`, `GameDto`, `Role`, `ErrorResponse`).
- **Behavior & Allowed Tools**:
  - *Comportamento*: È l'unica autorità autorizzata a definire o alterare le interfacce tra i microservizi. Garantisce la retrocompatibilità dei contratti. Verifica la correttezza formale delle annotazioni Jackson e delle specifiche di topic MQTT (`bitpub/sensors/%s/%s/event`, `bitpub/cloud/sensors/%s/event`, ecc.).
  - *Tool Abilitati*: `view_file`, `grep_search`, `replace_file_content`, `multi_replace_file_content`, `write_to_file`, `run_command` (esclusivamente per eseguire `mvn clean install -pl bitpub-common` o verifiche d'astrazione).

---

### Node 2.1: Identity, Security & Auth Services Dev
- **ID**: `NODE_AUTH_USER`
- **Dependencies**: `NODE_ARCHITECT`
- **Scope**: `BitPub-Cloud/auth-service/**`, `BitPub-Cloud/user-service/**`
- **Input Contract**: DTO del modulo `bitpub-common` (`UserDto`, `Role`), costanti di sicurezza, specifica token JWT (header `Authorization: Bearer <jwt>`, claims `user_id`, `roles`), contratto OpenAPI per `/api/v1/auth/**` e `/api/v1/users/**`.
- **Output Contract**:
  - Servizi Spring Boot verificati: `auth-service` (porta 8081) e `user-service` (porta 8082).
  - Schema entità JPA e migrazioni per utenti, credenziali hash (BCrypt) e ruoli RBAC (`PLAYER`, `LOCALE_ADMIN`, `GAME_ADMIN`, `PLATFORM_ADMIN`).
  - Seeding idempotente degli account di default (`UserDataSeeder`: `player`, `locale_admin`, `game_admin`, `platform_admin` con password comune `password123`).
- **Behavior & Allowed Tools**:
  - *Comportamento*: Implementa autenticazione, registrazione e verifica JWT tramite Spring Security 6 / Nimbus. Progetta gli endpoint REST del profilo utente e del controllo permessi. Assicura che le comunicazioni inter-servizio verso il database PostgreSQL (`bitpub_db`) utilizzino connection pooling efficienti.
  - *Tool Abilitati*: Strumenti di lettura/scrittura file nello scope, `run_command` per l'esecuzione di test isolati (`mvn clean test -pl BitPub-Cloud/auth-service`, `mvn clean test -pl BitPub-Cloud/user-service`). Utilizzo di H2 in memoria per i test di slice `@DataJpaTest`.

---

### Node 2.2: Locale & Game Catalog Services Dev
- **ID**: `NODE_CATALOG_LOCALE`
- **Dependencies**: `NODE_ARCHITECT`
- **Scope**: `BitPub-Cloud/locale-service/**`, `BitPub-Cloud/game-catalog-service/**`
- **Input Contract**: DTO di dominio (`LocaleDto`, `MachineDto`, `GameDto`), specifiche OpenAPI per `/api/v1/locales/**` e `/api/v1/catalog/**`, formato degli heartbeat MQTT in `MqttTopics.EDGE_HEARTBEAT_FORMAT`, specifiche RBAC dal contratto di sicurezza.
- **Output Contract**:
  - Servizi Spring Boot verificati: `locale-service` (porta 8083) e `game-catalog-service` (porta 8084).
  - API REST per la creazione di locali fisici, la registrazione di macchine da gioco ("Tavolo Calciobalilla 1", "Bersaglio Freccette", "Tavolo Biliardo") e l'assegnazione di giochi del catalogo.
  - Motore del catalogo con regole cablate (Calciobalilla: vince chi raggiunge 10 gol; Freccette: chiusura 501 a scalare con doppia; Biliardo: Palla 8 piene/mezze).
- **Behavior & Allowed Tools**:
  - *Comportamento*: Sviluppa la logica gestionale delle sale da gioco e dei macchinari IoT. Implementa la verifica delle autorizzazioni consultando i claims in header iniettati dal Gateway (`X-User-Id`, `X-User-Roles`). Consuma o pubblica aggiornamenti di stato macchina sul broker MQTT.
  - *Tool Abilitati*: Strumenti di lettura/scrittura file nello scope, `run_command` per `mvn clean test -pl BitPub-Cloud/locale-service` e `mvn clean test -pl BitPub-Cloud/game-catalog-service`.

---

### Node 2.3: Real-Time Game Engine & Match Service Dev
- **ID**: `NODE_MATCH_REALTIME`
- **Dependencies**: `NODE_ARCHITECT`
- **Scope**: `BitPub-Cloud/match-service/**`
- **Input Contract**: Costanti dei topic da `MqttTopics.java` (`CLOUD_SENSOR_INGEST_FORMAT`, `CLOUD_MATCH_ACTION_FORMAT`, `EDGE_MATCH_SYNC_FORMAT`, `CLOUD_MATCH_RESULT_FORMAT`), DTO degli eventi di gioco (`SensorEvent`, `MqttCommandWrapper`), specifiche OpenAPI per `/api/v1/matches/**`.
- **Output Contract**:
  - Motore real-time della partita verificato: `match-service` (porta 8085).
  - Listener MQTT di ingestione (`CommandIngestListener`) e macchina a stati del ciclo di vita della partita (`WAITING_FOR_PLAYERS` $\rightarrow$ `IN_PROGRESS` $\rightarrow$ `FINISHED`).
  - Pubblicazione dei punteggi validati in tempo reale verso i topic MQTT di visualizzazione Kiosk e verso i client REST/WebSocket.
- **Behavior & Allowed Tools**:
  - *Comportamento*: Implementa la logica real-time dei giochi connessi. Riceve eventi dai sensori IoT via Edge Node (es. gol nel calciobalilla), valida le transizioni di punteggio e decreta la fine automatica al raggiungimento del limite regolamentare. Gestisce la concorrenza e l'idempotenza dei messaggi MQTT con QoS 1.
  - *Tool Abilitati*: Strumenti di lettura/scrittura file nello scope, `run_command` per `mvn clean test -pl BitPub-Cloud/match-service`. Uso di `@WithMockUser` e Spring Integration per i test dei listener MQTT.

---

### Node 2.4: Tournaments, Statistics & Leaderboard Dev
- **ID**: `NODE_TOURNAMENT_STATS`
- **Dependencies**: `NODE_ARCHITECT`
- **Scope**: `BitPub-Cloud/tournament-service/**`, `BitPub-Cloud/statistics-service/**`
- **Input Contract**: DTO dei tornei e delle statistiche, OpenAPI per `/api/v1/tournaments/**` e `/api/v1/statistics/**`, eventi di fine partita e risultati dai topic `CLOUD_TOURNAMENT_RESULT_FORMAT`, `TOURNAMENT_STATE_FORMAT` e `TOURNAMENT_ENDED_FORMAT`.
- **Output Contract**:
  - Servizi Spring Boot verificati: `tournament-service` (porta 8086) e `statistics-service` (porta 8087).
  - Algoritmo di generazione automatica dei tabelloni a eliminazione diretta (8 o 16 giocatori) al completamento delle iscrizioni.
  - Motore di aggregazione statistica e leaderboard globale/per locale in tempo reale.
- **Behavior & Allowed Tools**:
  - *Comportamento*: Sviluppa il sistema competitivo di BitPub. Il servizio tornei orchestra gli abbinamenti del primo turno e i turni successivi in base ai risultati accertati. Il servizio statistiche aggiorna le classifiche di rendimento (vittorie/sconfitte, gol fatti/subiti, punteggi freccette) in base ai risultati notificati dal `match-service`.
  - *Tool Abilitati*: Strumenti di lettura/scrittura file nello scope, `run_command` per `mvn clean test -pl BitPub-Cloud/tournament-service` e `mvn clean test -pl BitPub-Cloud/statistics-service`.

---

### Node 2.5: API Gateway & WebSocket Notification Service Dev
- **ID**: `NODE_GATEWAY_NOTIF`
- **Dependencies**: `NODE_ARCHITECT`
- **Scope**: `BitPub-Cloud/gateway-service/**`, `BitPub-Cloud/notification-service/**`
- **Input Contract**: Mapping di routing per tutti i microservizi (`/api/v1/**`), contratto di validazione JWT da `NODE_AUTH_USER` (claims `X-User-Id`, `X-User-Roles`), topic MQTT di notifica (`MqttTopics.NOTIFICATIONS_FORMAT`), specifiche WebSocket per `/ws/notifications`.
- **Output Contract**:
  - API Gateway reattivo configurato: `gateway-service` (porta 8080) con filtro di sicurezza JWT e policy CORS di produzione/dev.
  - Server WebSocket di notifica real-time: `notification-service` (porta 8088) per lo streaming di eventi verso i browser connessi alla WebApp.
- **Behavior & Allowed Tools**:
  - *Comportamento*: Progetta il punto d'ingresso unificato per tutti i client HTTP/REST. Garantisce la sicurezza filtrando le richieste prive di token JWT valido (eccetto endpoint di login/registrazione pubblici) e iniettando le intestazioni di identità ai servizi di backend. Implementa il ponte broker MQTT $\rightarrow$ WebSocket per push istantanei del punteggio.
  - *Tool Abilitati*: Strumenti di lettura/scrittura file nello scope, `run_command` per `mvn clean test -pl BitPub-Cloud/gateway-service` e `mvn clean test -pl BitPub-Cloud/notification-service`.

---

### Node 2.6: Edge Node, MQTT Buffering & Simulators Dev
- **ID**: `NODE_EDGE_MQTT`
- **Dependencies**: `NODE_ARCHITECT`
- **Scope**: `BitPub-Edge/**`, `BitPub-Simulators/**`
- **Input Contract**: Specifica dei topic MQTT in `MqttTopics.java` (`SENSOR_EVENT_FORMAT`, `ACTUATOR_COMMAND_FORMAT`, `EDGE_HEARTBEAT_FORMAT`, `CLOUD_SENSOR_INGEST_FORMAT`, `EDGE_MATCH_SYNC_FORMAT`), requisiti di tolleranza di partizione (offlining e buffering locale), contratti del pannello di controllo dei simulatori (`demo-control-panel` su porta 8090).
- **Output Contract**:
  - Applicazione Edge di prossimità verificata: `bitpub-edge` (porta container 8085 / porta host 8089) con store offline (SQLite/memoria) per la sincronizzazione post-riconnessione.
  - Simulatori hardware funzionali che emettono payload JSON conformi a `SensorEvent` per simulare sensori ottici di gol, sensori bersaglio freccette e interruttori gettoniera.
- **Behavior & Allowed Tools**:
  - *Comportamento*: Costruisce l'anello di congiunzione tra l'hardware fisico dei locali e il Cloud. L'Edge Node gestisce le disconnessioni di rete: riceve eventi dal broker locale, li valida contro lo stato locale autorevole (`LocalMatchState`) e li inoltra in sicurezza ai topic Cloud (`bitpub/cloud/sensors/#`) non appena la connettività è ripristinata.
  - *Tool Abilitati*: Strumenti di lettura/scrittura file nello scope, `run_command` per `mvn clean test -pl BitPub-Edge` e `mvn clean test -pl BitPub-Simulators/demo-control-panel`.

---

### Node 2.7: WebApp React/TypeScript Frontend Dev
- **ID**: `NODE_FRONTEND`
- **Dependencies**: `NODE_ARCHITECT`
- **Scope**: `BitPub-WebApp/**`
- **Input Contract**: Schemi OpenAPI JSON/YAML di tutti i servizi backend, interfacce TypeScript corrispondenti ai DTO, URL base del Gateway API (`http://localhost:8080`), endpoint WebSocket `/ws/notifications`, design token TailwindCSS 4 e specifiche di UX di pregio d'interfaccia.
- **Output Contract**:
  - Applicazione Web React 19 / TypeScript compilata e verificata (bundle di produzione `dist/` e server di sviluppo su porta 3000).
  - Interfacce utente dinamiche per la registrazione, dashboard locali, catalogo giochi, tornei ed esperienza di gioco in tempo reale con aggiornamento istantaneo del punteggio.
- **Behavior & Allowed Tools**:
  - *Comportamento*: Realizza un'interfaccia ad alto impatto visivo (glassmorphism, animazioni fluide, dark mode curata, palette HSL armoniose). Consuma le API REST passando il Bearer Token JWT e gestisce le sottoscrizioni WebSocket per riflettere all'istante ogni gol o colpo a segno senza ricaricare la pagina. Ha il divieto assoluto di usare tipi `any` in TypeScript.
  - *Tool Abilitati*: Strumenti di lettura/scrittura file nella cartella `BitPub-WebApp`, `run_command` all'interno della cartella per `npm test`, `npx tsc --noEmit` e `npm run build`.

---

### Node 3.1: E2E & Integration Verification Engineer
- **ID**: `NODE_TEST_E2E`
- **Dependencies**: `NODE_AUTH_USER`, `NODE_CATALOG_LOCALE`, `NODE_MATCH_REALTIME`, `NODE_TOURNAMENT_STATS`, `NODE_GATEWAY_NOTIF`, `NODE_EDGE_MQTT`, `NODE_FRONTEND`
- **Scope**: `docs/TESTING.md`, `files_to_test*.txt`, suite di test di integrazione trasversali, configurazioni Testcontainers.
- **Input Contract**: Tutti i JAR e i bundle compilati emessi dai nodi di *Level 2*, specifiche di configurazione Testcontainers per PostgreSQL 15 ed Eclipse Mosquitto 2.0 (incluso supporto per named pipe / TCP su Windows/Docker Desktop come da `docs/TESTING.md`).
- **Output Contract**:
  - `CertifiedTestMatrix`: report formale di successo di tutti i test unitari, di slice e d'integrazione end-to-end (`mvn verify`).
  - `IntegrationDefectTicket`: (in caso di fallimento) rapporto strutturato sul difetto riscontrato, con log di eccezione e indicazione del contratto violato, re-instradato a `NODE_ORCHESTRATOR`.
- **Behavior & Allowed Tools**:
  - *Comportamento*: Agente di verifica della qualità globale. Non scrive funzionalità di business, ma esegue test di integrazione end-to-end che simulano l'intero ciclo di vita utente: dalla registrazione su `auth-service`, alla creazione del tavolo su `locale-service`, dall'avvio della partita su `match-service` fino alla simulazione di un evento di gol via MQTT su `bitpub-edge` e verifica della notifica WebSocket.
  - *Tool Abilitati*: `view_file`, `grep_search`, `run_command` per `mvn clean test` e `mvn verify` sull'intero progetto o su suite integrate. Divieto di modifica ai file di codice sorgente di produzione.

---

### Node 3.2: Containerization, Docker DevOps & Infrastructure Release
- **ID**: `NODE_DOCKER_DEVOPS`
- **Dependencies**: `NODE_TEST_E2E`
- **Scope**: `docker-compose.yml`, `scripts/**`, tutti i file `Dockerfile` dei singoli microservizi e della WebApp, `.devcontainer/**`.
- **Input Contract**: `CertifiedTestMatrix` approvato da `NODE_TEST_E2E`, specifiche delle porte e delle variabili d'ambiente dei microservizi, mapping dei volumi PostgreSQL (`pgdata`) e configurazioni del broker Mosquitto (`docs/mqtt/mosquitto.conf`).
- **Output Contract**:
  - Infrastruttura Docker Compose pronta per la produzione e per l'avvio in ambiente di sviluppo (`docker-compose up --build -d`).
  - Script di build automatizzati verificati (`scripts/rebuild_no_cache.ps1` e script Shell).
  - Certificato di Healthcheck dell'intero stack di 10 container (Postgres, Mosquitto, Gateway, Auth, User, Locale, Catalog, Match, Tournament, Statistics, Notification, Edge, WebApp).
- **Behavior & Allowed Tools**:
  - *Comportamento*: Gestisce il deployment e l'orchestrazione dei container. Garantisce l'avvio ordinato dei servizi sfruttando le direttive `depends_on`, `healthcheck` e variabili d'ambiente uniformi. Verifica il bridge di rete `bitpub-network` e l'accessibilità esterna sulle porte di front-end (3000) e gateway (8080).
  - *Tool Abilitati*: Strumenti di lettura/scrittura sui file di configurazione infrastrutturale (`docker-compose.yml`, `Dockerfile`, script `.ps1`/`.sh`), `run_command` per la verifica di validità sintattica e di pre-build Docker (`docker-compose config`).

---

## 4. Edge Routing & Fallback Strategies

La propagazione delle transizioni tra i nodi del DAG è regolata da **Gate di Transizione Rigidi** e **Strategie di Fallback Invarianti**. Il sistema impedisce la corruzione dello stato bloccando l'esecuzione non appena un contratto non è rispettato.

```
       [NODE_ORCHESTRATOR]
                |
                v
        [NODE_ARCHITECT]  <---------------------------------------------+
                |                                                       |
                | (Contract Bundle Verified)                            |
                v                                                       |
   +-------------------------+                                          |
   | Level 2 Parallel Nodes  |                                          |
   +-------------------------+                                          |
                |                                                       |
                | (Unit Tests Pass & Artifacts Built)                   |
                v                                                       |
        [NODE_TEST_E2E]                                                 |
         /           \                                                  |
(E2E Pass)           (E2E / Integration Defect Detected)                |
       /               \                                                |
      v                 +---> [Emit Defect Contract]                    |
[NODE_DOCKER_DEVOPS]                   |                                |
      |                                +---> [Re-route to NODE_0/1] ----+
      v                                      (Zero Upstream Mutation)
  [DEPLOYMENT READY]
```

### 4.1. Condizioni di Avanzamento Downstream (Transition Gates)
1. **Gate 0 $\rightarrow$ 1 (Orchestration to Architect):**
   - *Condizione:* Il `TaskDispatchPlan` deve definire in modo chiaro i domini impattati e non presentare ambiguità.
2. **Gate 1 $\rightarrow$ 2 (Architect to Parallel Dev Nodes):**
   - *Condizione:* Il comando `mvn clean install -pl BitPub-Common` deve terminare con `SUCCESS`. Tutte le specifiche OpenAPI e le definizioni in `MqttTopics.java` devono essere prive di errori di sintassi.
3. **Gate 2 $\rightarrow$ 3.1 (Parallel Dev Nodes to E2E Test Engineer):**
   - *Condizione:* Ogni nodo del Level 2 attivato deve presentare il 100% dei test unitari/slice in stato pass (`mvn test` o `npm test` = SUCCESS) e nessun file non committato al di fuori del proprio `Scope`.
4. **Gate 3.1 $\rightarrow$ 3.2 (E2E Test to Docker DevOps):**
   - *Condizione:* La suite di test di integrazione Testcontainers (o `mvn verify`) deve generare una `CertifiedTestMatrix` positiva, dimostrando la corretta interazione end-to-end tra REST API, database PostgreSQL, broker Mosquitto MQTT e WebApp React.

### 4.2. Strategie di Fallback e Gestione degli Errori di Integrazione
Per rispettare l'immutabilità dello stato (*State Immutability*) e impedire che un agente a valle compia modifiche ad hoc e disordinate sul codice a monte, si applicano le seguenti politiche di routing delle eccezioni:

- **Schema Drift / Incompatibilità di Contratto REST/MQTT (rilevato in Level 2 o Level 3):**
  - *Sintomo:* Un servizio si attende un campo DTO o un topic MQTT non più allineato con le modifiche in corso, oppure il client TypeScript di `NODE_FRONTEND` fallisce la compilazione contro i tipi OpenAPI.
  - *Strategia di Fallback:* Il nodo che rileva il drift interrompe immediatamente la propria esecuzione ed emette un `ContractViolationReport`. L'orchestratore blocca i nodi paralleli interessati ed esegue il re-routing esclusivo verso `NODE_ARCHITECT` (`NODE_1`), assegnandogli il task di correggere il contratto in `bitpub-common` o negli schemi OpenAPI. Solo dopo la nuova pubblicazione del contratto il Level 2 viene riattivato.
- **Fallimento del Test di Integrazione E2E / Testcontainers (rilevato da NODE_TEST_E2E):**
  - *Sintomo:* Un test di scenario integrato fallisce (es. il punteggio calcolato da `match-service` non coincide con le classifiche su `statistics-service` o l'Edge non sincronizza lo stato in offlining).
  - *Strategia di Fallback:* `NODE_TEST_E2E` ha il divieto di apportare correzioni al codice sorgente dei microservizi. Emette un `IntegrationDefectTicket` con il log di errore e re-instrada il flusso a `NODE_ORCHESTRATOR` (`NODE_0`), che avvia un nuovo ciclo di DAG attivando solo i nodi di Level 2 proprietari dei servizi difettosi (`NODE_MATCH_REALTIME`, `NODE_TOURNAMENT_STATS`, ecc.).
- **Fallimento della Build Docker / Errore di Rete nei Container (rilevato da NODE_DOCKER_DEVOPS):**
  - *Sintomo:* Un microservizio non riesce a connettersi al database PostgreSQL, al broker Mosquitto sulla porta 1883 o la build Docker di un modulo fallisce in `docker-compose up --build`.
  - *Strategia di Fallback:* `NODE_DOCKER_DEVOPS` analizza i log infrastrutturali. Se il problema è nel Dockerfile o in una variabile di environment di `docker-compose.yml`, lo risolve all'interno del proprio `Scope`. Se il problema deriva da un'errata configurazione applicativa (es. un property d'ambiente mancante in `application.yml` di un microservizio), il DAG re-instrada la richiesta al nodo proprietario in Level 2 per l'allineamento della configurazione.
- **Prevenzione di Deadlock e Loop Infiniti:**
  - Ogni *Contract Amendment Request* o *IntegrationDefectTicket* incrementa un contatore di iterazione del DAG. Se un task supera un numero massimo di **3 tentativi di ricalcolo del DAG**, il Root Orchestrator sospende l'esecuzione automatica ed emette un avviso esplicito di *Architectural Escalation*, richiedendo il parere umano o una verifica di coerenza sui requisiti.
