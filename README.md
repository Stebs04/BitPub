# BitPub - Connected Games Platform

[![Build Status](https://img.shields.io/badge/build-passing-brightgreen.svg)]()
[![Java](https://img.shields.io/badge/Java-21-orange.svg)]()
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.4-brightgreen.svg)]()
[![Docker](https://img.shields.io/badge/Docker-Compose-blue.svg)]()

**Progetto di Laboratorio PISSIR**  
**Università del Piemonte Orientale (UPO)**

## 👥 Team di Sviluppo
Questo progetto è stato realizzato congiuntamente da:
- **Stefano Bellan** (Matricola: 20054330)
- **Timothy Giolito** (Matricola: 20054431)
- **Luca Franzon** (Matricola: 20054744)

---

## 📖 Descrizione del Progetto
**BitPub** è una piattaforma distribuita per la gestione di giochi connessi (Connected Games Platform), progettata con un'architettura **Edge-Cloud**. Il sistema permette di gestire utenti, cataloghi di giochi, partite (match), tornei e statistiche in tempo reale, utilizzando una comunicazione a bassa latenza basata sul protocollo MQTT per l'integrazione con i nodi Edge e i simulatori.

## 🏗️ Architettura e Moduli
L'architettura del sistema si divide in diversi moduli interdipendenti:

- **BitPub-Cloud**: Il cuore del sistema, basato su microservizi (Spring Boot e Spring Cloud). I servizi implementati sono:
  - `gateway-service` (API Gateway)
  - `auth-service` (Gestione Autenticazione)
  - `user-service`, `locale-service`, `game-catalog-service`
  - `match-service` e `tournament-service` (Logica di gioco)
  - `statistics-service` e `notification-service`
- **BitPub-Edge**: Nodo edge che comunica con il cloud (via MQTT) e gestisce l'elaborazione di prossimità per i dispositivi di gioco, minimizzando la latenza.
- **BitPub-Common**: Libreria condivisa che contiene i DTO, i modelli e le classi di utilità utilizzate da tutti i moduli.
- **BitPub-WebApp**: Interfaccia utente web per l'amministrazione e l'interazione con la piattaforma.
- **BitPub-JavaFX**: Client desktop sviluppato in JavaFX.
- **BitPub-Simulators**: Strumenti di simulazione per testare il carico e il comportamento della piattaforma con dispositivi di gioco virtuali.

## 🚀 Tecnologie Utilizzate
- **Linguaggi**: Java 21, HTML/JS/CSS (WebApp)
- **Framework Cloud & Backend**: Spring Boot 3.2.4, Spring Cloud
- **Broker di Messaggistica**: Eclipse Mosquitto (MQTT 2.0)
- **Database**: PostgreSQL 15
- **Tool di Build & Utility**: Maven, Lombok, MapStruct
- **Infrastruttura e Deployment**: Docker, Docker Compose

## 🛠️ Come Avviare il Progetto
Il progetto è interamente containerizzato e può essere avviato in locale utilizzando Docker Compose.

1. **Clonare il repository**:
   ```bash
   git clone <url-repository>
   cd BitPub
   ```

2. **Avviare l'infrastruttura**:
   Utilizzare lo script `rebuild_no_cache` per avviare automaticamente il database PostgreSQL, il broker Mosquitto, tutti i microservizi Cloud, l'Edge Node e la WebApp frontend.
   ```powershell
   .\scripts\rebuild_no_cache.ps1
   ```

3. **Accesso ai Servizi**:
   - Una volta avviato, per l'**Interfaccia Web** andare su: [http://localhost:3000](http://localhost:3000)
   - **API Gateway**: `http://localhost:8080`
   - **Broker MQTT**: Porte `1883` (TCP) e `9001` (WebSocket)
   - **Database PostgreSQL**: Porta `5432`

4. **Avviare il Simulator Panel**:
   Per aprire il pannello dei simulatori, navigare nella cartella `BitPub-Simulators/demo-control-panel` ed eseguire il seguente comando:
   ```bash
   cd BitPub-Simulators
   mvn spring-boot:run "-Dspring-boot.run.jvmArguments=-Dserver.port=8090"
   ```
   Una volta avviato, la pagina dei simulatori sarà accessibile all'indirizzo: [http://localhost:8090](http://localhost:8090)

## 📚 Documentazione
Per ulteriori dettagli tecnici sulle API, diagrammi architetturali e configurazioni specifiche per il broker MQTT, fare riferimento ai materiali presenti all'interno della cartella `/docs`.

---
*Realizzato con cura per il corso di Laboratorio PISSIR.*
