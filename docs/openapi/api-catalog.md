# Catalogo API e Istruzioni OpenAPI - BitPub

Questo documento fornisce una panoramica degli endpoint esposti dal Gateway API di BitPub e le istruzioni per accedere alle specifiche OpenAPI generate dinamicamente dai singoli microservizi Spring Boot.

## API Gateway

Tutte le richieste esterne (dalla Web App, dall'App JavaFX e dalle chiamate HTTP dell'Edge App) passano attraverso lo **Spring Cloud Gateway**.

- **URL Base Produzione**: `https://api.bitpub.com`
- **URL Base Sviluppo (Docker Compose)**: `http://localhost:8080` (porta standard gateway)

Il routing è gestito principalmente per prefisso.

| Microservizio | Path Prefix | Descrizione |
|---|---|---|
| Auth Service | `/api/v1/auth/**` | Login, Registrazione, Refresh Token |
| User Service | `/api/v1/users/**` | Profilo utente, Permessi |
| Locale Service | `/api/v1/locales/**` | Gestione locali fisici e macchine installate |
| Catalog Service | `/api/v1/catalog/**` | Tipi di giochi supportati, definizioni e regole |
| Match Service | `/api/v1/matches/**` | Stato partite, inoltro eventi HTTP dall'Edge |
| Tournament Service | `/api/v1/tournaments/**` | Iscrizioni, tabelle tornei |
| Statistics Service | `/api/v1/statistics/**` | Leaderboard globale, statistiche per giocatore/locale |
| Notification Service | `/ws/notifications` | Endpoint WebSocket (o SSE via REST) per UI real-time |

## Autenticazione e Sicurezza

Le API (eccetto `/auth/**` e alcuni endpoint pubblici del catalogo) sono protette tramite **JWT (JSON Web Token)**.
Il token deve essere passato nell'header HTTP:
```http
Authorization: Bearer <tuo_jwt_token>
```

Il Gateway valida la firma del token e, se valido, estrae i claims (es. `user_id`, `roles`) passandoli come header aggiuntivi (es. `X-User-Id`) ai microservizi a valle.

## Accesso a OpenAPI (Swagger UI)

I microservizi Spring Boot utilizzano `springdoc-openapi-starter-webmvc-ui` o `webflux-ui`. Le specifiche sono generate a runtime partendo dalle annotazioni nel codice.

Quando l'ambiente è in esecuzione tramite `docker-compose`, è possibile accedere alla documentazione Swagger dei singoli servizi per ispezionare gli endpoint nel dettaglio ed eseguire test interattivi.

- **Gateway Service API Docs (aggregatore opzionale se configurato)**: `http://localhost:8080/swagger-ui.html`
- **Auth Service**: `http://localhost:8081/swagger-ui.html`
- **User Service**: `http://localhost:8082/swagger-ui.html`
- **Locale Service**: `http://localhost:8083/swagger-ui.html`
- **Game Catalog Service**: `http://localhost:8084/swagger-ui.html`
- **Match Service**: `http://localhost:8085/swagger-ui.html`
- **Tournament Service**: `http://localhost:8086/swagger-ui.html`
- **Statistics Service**: `http://localhost:8087/swagger-ui.html`

### Formato JSON/YAML
È possibile ottenere il formato grezzo della documentazione (utile per generare client Typescript per la React App) accedendo a:
- JSON: `http://localhost:<porta_servizio>/v3/api-docs`
- YAML: `http://localhost:<porta_servizio>/v3/api-docs.yaml`

*(Nota: le porte specifiche dipendono dal mapping finale definito nel file `docker-compose.yml`)*
