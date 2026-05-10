package com.bitpub.edge;

import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Gestore centralizzato per il ciclo di vita e le macchine a stati finiti dei tavoli.
 * L'architettura è stata ingegnerizzata per assorbire un traffico di rete parallelo asincrono,
 * sfruttando esclusivamente costrutti atomici nativi (ConcurrentHashMap) in luogo 
 * della sincronizzazione manuale esplicita (blocco synchronized). 
 * Questo approccio previene deadlock, incrementa notevolmente le performance in ambienti IoT
 * a elevata intensità di IO e garantisce consistenza totale (thread-safety) nella transizione degli stati.
 */
public class GameTableStateManager {

    // Registratore diagnostico SLF4J
    private static final Logger logger = LoggerFactory.getLogger(GameTableStateManager.class);

    // Mappatura immutabile delle etichette di stato per proteggere la logica da errori di battitura e refactoring
    private static final String STATE_FREE = "FREE";
    private static final String STATE_OCCUPIED = "OCCUPIED";

    /**
     * Struttura di memorizzazione partizionata intrinsecamente atomica.
     * Isola il lock sul singolo segmento di bucket anziché sull'intera collezione,
     * consentendo letture e scritture ad altissimo parallelismo.
     */
    private final ConcurrentHashMap<Integer, String> tableStates = new ConcurrentHashMap<>();

    /**
     * Eleva forzatamente lo stato del tavolo a OCCUPATO.
     * Data l'autorità dell'evento d'inizio partita, la sostituzione è incondizionata
     * e si affida al meccanismo put atomico della struttura sottostante.
     *
     * @param tableId Identificativo logico intero che mappa fisicamente la macchina
     */
    public void setOccupied(int tableId) {
        // La sovrascrittura diretta esclude la comparsa di stati intermedi volatili
        tableStates.put(tableId, STATE_OCCUPIED);
        logger.info("[STATE MANAGER] Transizione stato atomica: Tavolo {} -> {}", tableId, STATE_OCCUPIED);
    }

    /**
     * Sblocca operativamente un tavolo applicando un'euristica difensiva.
     *
     * @param tableId L'indirizzo logico del tavolo da ripristinare
     */
    public void setFree(int tableId) {
        // Applicazione del pattern Compare-And-Swap (CAS): la mappa muta il valore 
        // solo e soltanto se l'istanza è effettivamente bloccata, proteggendo il dato
        // da eventi duplicati o di latenza asincrona non previsti.
        boolean successo = tableStates.replace(tableId, STATE_OCCUPIED, STATE_FREE);
        
        if (!successo) {
            // Logica compensativa: qualora il macchinario non fosse ancora entrato
            // nel radar del gestore, lo si istanzia proattivamente al livello base
            tableStates.putIfAbsent(tableId, STATE_FREE);
        }
        
        logger.info("[STATE MANAGER] Transizione stato atomica: Tavolo {} -> {}", tableId, STATE_FREE);
    }

    /**
     * Esegue un probing logico per verificare il blocco attivo di una postazione.
     *
     * @param tableId Riferimento del tavolo
     * @return true se bloccato logicamente, false in caso contrario
     */
    public boolean isOccupied(int tableId) {
        return STATE_OCCUPIED.equals(getStatus(tableId));
    }

    /**
     * Legge in sicurezza lo stato di occupazione. Implementa un meccanismo nativo 
     * di lazily-initialization: le postazioni inesistenti vengono incorporate e 
     * flaggate come libere istantaneamente per mantenere coesa l'infrastruttura in memoria.
     *
     * @param tableId Il puntatore della macchina
     * @return Stringa descrittiva garantita non-nulla rappresentante lo stato corrente
     */
    public String getStatus(int tableId) {
        // Inserimento atomico condizionato: protegge l'operazione in caso 
        // due letture parallele richiedano l'inserimento inaugurale simultaneamente.
        tableStates.putIfAbsent(tableId, STATE_FREE);
        return tableStates.get(tableId);
    }

    /**
     * Procedura di ispezione amministrativa per riversare l'istantanea dello stato 
     * all'interno dei log, utile in fase di debugging e health-checking.
     */
    public void monitoraStatoTavoli() {
        logger.info("=== MONITORAGGIO STATO ATOMICO TAVOLI ===");
        if (tableStates.isEmpty()) {
            logger.info("Nessun tavolo attualmente registrato nel sistema.");
        } else {
            // L'iteratore di ConcurrentHashMap supporta in modo nativo letture debolmente consistenti (weakly consistent),
            // garantendo l'esecuzione senza bloccare il traffico scrivente in tempo reale
            tableStates.forEach((id, status) -> 
                logger.info("Tavolo ID: {} | Stato: {}", id, status)
            );
        }
        logger.info("=========================================");
    }
}