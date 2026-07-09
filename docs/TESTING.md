# Testing setup

Unit/slice tests (`@WebMvcTest`, `@DataJpaTest` on H2, Mockito) run with no
extra setup: `mvn test`.

## Testcontainers (E2E + Fase 5 scenari)

Test che estendono Testcontainers (Postgres/MQTT) richiedono un Docker daemon
raggiungibile dalla libreria `docker-java`. Su **Windows + Docker Desktop**
recenti la named-pipe fallisce e il client di default negozia una API version
sotto il minimo del daemon (400 Bad Request). Due passi, una tantum:

1. Docker Desktop → Settings → General → abilita
   **"Expose daemon on tcp://localhost:2375 without TLS"** → Apply & Restart.

2. Crea `~/.docker-java.properties`:
   ```
   DOCKER_HOST=tcp://localhost:2375
   api.version=1.44
   ```
   (`api.version` forza una versione ≥ del minimo del daemon, min attuale 1.40;
   il default di docker-java è 1.32 e viene rifiutato.)

Su Linux/macOS o CI con Docker nativo di solito nulla di tutto ciò serve: la
socket unix `/var/run/docker.sock` funziona con i default.
