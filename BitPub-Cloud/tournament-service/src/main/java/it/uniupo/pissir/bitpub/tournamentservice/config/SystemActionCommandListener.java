/**
 * Autore: Stefano Bellan Matricola 20054330
 */
package it.uniupo.pissir.bitpub.tournamentservice.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.uniupo.pissir.bitpub.common.exception.BitpubException;
import it.uniupo.pissir.bitpub.common.mqtt.MqttCommandWrapper;
import it.uniupo.pissir.bitpub.tournamentservice.dto.TournamentDto;
import it.uniupo.pissir.bitpub.tournamentservice.service.impl.TournamentServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Ascoltatore che consuma i comandi di gestione (creazione, modifica, eliminazione) dei tornei
 * inoltrati dall'Edge tramite il topic MQTT di sistema, orchestrando le chiamate al servizio {@link TournamentServiceImpl}.
 * Il payload JSON contiene un campo 'action' che determina il tipo di operazione da eseguire.
 * Questo meccanismo va a sostituire le precedenti chiamate REST dirette fatte dalla WebApp.
 *
 * I tornei possono essere gestiti solo dagli amministratori di locale (LOCALE_ADMIN). Poiché il wrapper MQTT
 * non trasporta gli header HTTP (come ad esempio l'ID del locale), verifichiamo nuovamente il ruolo dell'utente
 * e recuperiamo il suo locale di competenza interrogando il locale-service. Questo passaggio extra è necessario 
 * per garantire i corretti permessi senza dover appesantire la struttura del messaggio MQTT.
 */
@Component
@Slf4j
public class SystemActionCommandListener {

    private static final String LOCALE_ADMIN = "LOCALE_ADMIN";

    private final TournamentServiceImpl tournamentService;
    private final ObjectMapper objectMapper;
    private final String localeServiceUrl;

    public SystemActionCommandListener(TournamentServiceImpl tournamentService,
                                       ObjectMapper objectMapper,
                                       @Value("${locale.service.url:http://localhost:8083}") String localeServiceUrl) {
        this.tournamentService = tournamentService;
        this.objectMapper = objectMapper;
        this.localeServiceUrl = localeServiceUrl;
    }

    @ServiceActivator(inputChannel = "systemActionInboundChannel")
    public void onSystemAction(Message<String> message) {
        try {
            MqttCommandWrapper wrapper = objectMapper.readValue(message.getPayload(), MqttCommandWrapper.class);
            if (!LOCALE_ADMIN.equals(wrapper.actorRole())) {
                log.warn("Rejected tournament action from non-admin actor {} (role {})", wrapper.actorUserId(), wrapper.actorRole());
                return;
            }
            String localeId = resolveAdminLocaleId(wrapper.actorUserId());
            if (localeId == null) {
                log.warn("Rejected tournament action: locale of actor {} not determinable", wrapper.actorUserId());
                return;
            }

            JsonNode p = objectMapper.readTree(wrapper.payload());
            String action = p.path("action").asText();
            switch (action) {
                case "CREATE_TOURNAMENT" -> {
                    TournamentDto dto = toTournament(p);
                    dto.setLocaleIds(List.of(localeId)); // il locale del torneo e' sempre quello dell'admin
                    tournamentService.createTournament(dto);
                }
                case "UPDATE_TOURNAMENT" -> {
                    String id = p.path("id").asText();
                    assertOwns(id, localeId);
                    TournamentDto dto = toTournament(p);
                    dto.setLocaleIds(List.of(localeId)); // il locale resta il proprio
                    tournamentService.updateTournament(id, dto);
                }
                case "DELETE_TOURNAMENT" -> {
                    String id = p.path("id").asText();
                    assertOwns(id, localeId);
                    tournamentService.deleteTournament(id);
                }
                default -> log.warn("Unknown tournament action: {}", action);
            }
            log.info("Processed tournament action {} via MQTT (actor {})", action, wrapper.actorUserId());
        } catch (BitpubException e) {
            // Eccezioni di business (es. utente non autorizzato o torneo inesistente).
            // Logghiamo l'evento senza bloccare il consumatore o respingere il messaggio QoS1, altrimenti andrebbe in loop.
            log.info("Azione sul torneo respinta via MQTT ({}): {}", e.getStatus(), e.getMessage());
        } catch (Exception e) {
            // Messaggio malformato o imprevisto: intercettiamo l'errore per evitare che blocchi la coda durevole.
            log.error("Impossibile elaborare il comando in ingresso per il torneo: {}", message.getPayload(), e);
        }
    }

    /** Il torneo deve appartenere al locale del chiamante (replica del controllo del controller REST). */
    private void assertOwns(String id, String localeId) {
        List<String> localeIds = tournamentService.getTournament(id).getLocaleIds();
        if (localeIds == null || !localeIds.contains(localeId)) {
            throw new BitpubException("Il torneo non appartiene al tuo locale", HttpStatus.FORBIDDEN);
        }
    }

    /** Recupera l'ID del locale gestito dall'amministratore specificato, oppure null se non ne gestisce nessuno. */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private String resolveAdminLocaleId(String adminId) {
        if (adminId == null) return null;
        try {
            List response = RestClient.create(localeServiceUrl)
                    .get()
                    .uri("/api/v1/locales/by-admin/{adminId}", adminId)
                    .retrieve()
                    .body(List.class);
            if (response != null && !response.isEmpty() && response.get(0) instanceof Map) {
                Object localeId = ((Map) response.get(0)).get("id");
                return localeId != null ? localeId.toString() : null;
            }
        } catch (Exception e) {
            log.error("Failed to resolve locale for adminId {}", adminId, e);
        }
        return null;
    }

    private TournamentDto toTournament(JsonNode p) {
        return TournamentDto.builder()
                .name(p.path("name").asText())
                .gameTypeId(p.path("gameTypeId").asText())
                .teamBased(p.path("teamBased").asBoolean())
                .maxParticipants(p.hasNonNull("maxParticipants") ? p.path("maxParticipants").asInt() : null)
                .build();
    }
}
