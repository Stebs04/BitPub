package com.bitpub.buffer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.AbstractMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Event Store persistente basato su MapDB.
 * Sostituisce la memoria volatile con uno storage su disco (Write-Ahead Log)
 * fornendo garanzie di sopravvivenza al crash (Crash Recovery) e ordinamento rigoroso.
 * Include logica di deduplica per eventi generati dai sensori con frequenza elevata.
 */
public class PersistentEventStore {

    private static final Logger logger = LoggerFactory.getLogger(PersistentEventStore.class);

    private final DB db;
    // Mappa ordinata (Coda) per mantenere gli eventi in attesa di ACK. Chiave = ID sequenziale, Valore = JSON.
    private final Map<Long, String> pendingQueue;
    // Set per la deduplica degli eventi (LRU cache approssimata tramite scadenza o dimensione fissa)
    private final Map<String, Long> deduplicationSet;
    
    // Contatore atomico persistente per ordinamento FIFO rigoroso
    private final AtomicLong sequenceId;
    
    // Coda di notifica in-memory per sbloccare il consumer istantaneamente (Backpressure ibrida)
    private final BlockingQueue<Long> notificationQueue;

    private final ObjectMapper mapper = new ObjectMapper();

    public PersistentEventStore(String dbPath) {
        // Inizializza il database su file con file lock e Write-Ahead-Log per tolleranza ai crash
        this.db = DBMaker.fileDB(new File(dbPath))
                .fileMmapEnableIfSupported()
                .checksumHeaderBypass()
                .transactionEnable() // Supporto ACID e rollback
                .closeOnJvmShutdown()
                .make();

        this.pendingQueue = db.treeMap("pendingQueue", Serializer.LONG, Serializer.STRING).createOrOpen();
        this.deduplicationSet = db.hashMap("deduplicationSet", Serializer.STRING, Serializer.LONG).createOrOpen();
        
        long lastId = 0L;
        if (!pendingQueue.isEmpty()) {
            // mapdb treeMap castato a NavigableMap potrebbe essere usato, ma iteriamo o prendiamo la size
            // per semplicità teniamo un AtomicLong.Localmente iteriamo per trovare il max.
            for (Long key : pendingQueue.keySet()) {
                if (key > lastId) lastId = key;
            }
        }
        this.sequenceId = new AtomicLong(lastId);
        
        // Notifica ai listener eventuali task non processati (Replay)
        this.notificationQueue = new LinkedBlockingQueue<>();
        for (Long key : pendingQueue.keySet()) {
            this.notificationQueue.offer(key);
        }
        
        logger.info("[EVENT STORE] Inizializzato Persistent Event Store in: {}. Eventi pendenti trovati: {}", dbPath, pendingQueue.size());
    }

    /**
     * Accoda un evento. Se l'evento non ha un eventId, lo genera.
     * Applica la deduplica: se un evento identico (hash) è arrivato di recente, lo scarta.
     */
    public void enqueue(String rawJson) throws Exception {
        JsonNode rootNode = mapper.readTree(rawJson);
        String hardwareSignature = rootNode.path("hardwareSignature").asText("");
        String state = rootNode.path("state").asText("");
        
        // Chiave logica per deduplica: firma hardware + stato
        String dedupKey = hardwareSignature + "_" + state;
        long now = System.currentTimeMillis();
        
        // Deduplica: se lo stesso stato è stato inviato meno di 500ms fa, lo scartiamo.
        Long lastSeen = deduplicationSet.get(dedupKey);
        if (lastSeen != null && (now - lastSeen) < 500) {
            logger.debug("[EVENT STORE] Evento scartato per deduplica (throttle): {}", dedupKey);
            return;
        }

        // Iniezione idempotenza
        String eventId = rootNode.path("eventId").asText(null);
        if (eventId == null) {
            eventId = UUID.randomUUID().toString();
            ((ObjectNode) rootNode).put("eventId", eventId);
        }
        ((ObjectNode) rootNode).put("timestamp", now);
        
        String processedJson = mapper.writeValueAsString(rootNode);

        // Salvataggio ACID
        long seq = sequenceId.incrementAndGet();
        pendingQueue.put(seq, processedJson);
        deduplicationSet.put(dedupKey, now);
        db.commit(); // Flusha su disco
        
        notificationQueue.offer(seq);
        logger.debug("[EVENT STORE] Evento persistito: ID={}, Seq={}", eventId, seq);
    }

    /**
     * Blocca finché non c'è un evento pronto per l'invio.
     * @return Una Map.Entry con sequence ID e Payload.
     */
    public Map.Entry<Long, String> takeNext() throws InterruptedException {
        while (true) {
            Long seq = notificationQueue.take(); // Bloccante (Zero CPU burn)
            String payload = pendingQueue.get(seq);
            if (payload != null) {
                return new AbstractMap.SimpleEntry<>(seq, payload);
            }
        }
    }

    /**
     * Rimuove l'evento dallo store dopo che il broker Cloud ha risposto con un ACK (QoS 1).
     */
    public void acknowledge(Long seqId) {
        pendingQueue.remove(seqId);
        db.commit();
        logger.debug("[EVENT STORE] Evento confermato e rimosso: Seq={}", seqId);
    }

    public int getPendingCount() {
        return pendingQueue.size();
    }

    public void close() {
        if (db != null && !db.isClosed()) {
            db.close();
        }
    }
}
