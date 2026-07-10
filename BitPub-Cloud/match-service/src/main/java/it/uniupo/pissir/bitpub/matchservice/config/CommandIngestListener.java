// Autore: Timothy Giolito 20054431
package it.uniupo.pissir.bitpub.matchservice.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.uniupo.pissir.bitpub.common.exception.BitpubException;
import it.uniupo.pissir.bitpub.common.mqtt.MqttCommandWrapper;
import it.uniupo.pissir.bitpub.matchservice.dto.GameActionRequestDto;
import it.uniupo.pissir.bitpub.matchservice.service.impl.MatchServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Componente per il consumo delle azioni interattive inoltrate dal livello Edge sul topic MQTT.
 * Le azioni vengono inoltrate a {@link MatchServiceImpl#processGameAction}, che verifica i turni e
 * ripubblica il nuovo stato della partita verso la WebApp.
 * L'identità del chiamante viene trasmessa nel wrapper poiché MQTT non dispone di header HTTP nativi.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CommandIngestListener {

    private final MatchServiceImpl matchService;
    private final ObjectMapper objectMapper;

    // Metodo attivato all'arrivo di un messaggio sul canale MQTT in ingresso dedicato ai comandi
    @ServiceActivator(inputChannel = "mqttCommandInboundChannel")
    public void onGameAction(Message<String> message) {
        try {
            // Deserializzazione del wrapper e del comando inviato
            MqttCommandWrapper wrapper = objectMapper.readValue(message.getPayload(), MqttCommandWrapper.class);
            GameActionRequestDto action = objectMapper.readValue(wrapper.payload(), GameActionRequestDto.class);
            
            // Elaborazione dell'azione all'interno del servizio di partita
            matchService.processGameAction(wrapper.targetId(), wrapper.actorUserId(), action);
            log.info("Azione di gioco elaborata tramite MQTT: partita={}, attore={}", wrapper.targetId(), wrapper.actorUserId());
            
        } catch (BitpubException e) {
            // Rifiuto dell'azione (ad esempio, per turno non valido). L'eccezione viene loggata 
            // ma non propaga, per evitare crash del listener o continui retry dello stesso errore logico.
            log.info("Azione di gioco rifiutata tramite MQTT ({}): {}", e.getStatus(), e.getMessage());
        } catch (Exception e) {
            // Eventuali messaggi corrotti vengono scartati per non bloccare la coda di elaborazione
            log.error("Impossibile elaborare il comando in ingresso: {}", message.getPayload(), e);
        }
    }

    /**
     * Consuma i risultati finali della partita inoltrati dal livello Edge e li persiste tramite
     * {@link MatchServiceImpl#applyFinalResult}. Sostituisce la vecchia chiamata REST. 
     * L'operazione è idempotente basandosi sull'identificativo della partita, quindi eventuali 
     * riconsegne dovute a QoS 1 sono sicure.
     */
    @ServiceActivator(inputChannel = "mqttMatchResultInboundChannel")
    public void onMatchResult(Message<String> message) {
        try {
            // Estrazione del payload e dei punteggi dal risultato finale
            JsonNode node = objectMapper.readTree(message.getPayload());
            String matchId = node.path("matchId").asText(null);
            
            Map<String, Integer> scores = new LinkedHashMap<>();
            node.path("scoreByTeam").fields()
                    .forEachRemaining(e -> scores.put(e.getKey(), e.getValue().asInt()));
                    
            // Applica il risultato finale sullo stato della partita
            matchService.applyFinalResult(matchId, scores);
            
            log.info("Risultato finale applicato via MQTT: partita={}, punteggi={}, vincitore={}",
                    matchId, scores, node.path("winnerName").asText(null));
                    
        } catch (Exception e) {
            // Messaggio corrotto o malformato scartato per prevenire il blocco del listener
            log.error("Impossibile elaborare il risultato della partita: {}", message.getPayload(), e);
        }
    }
}
