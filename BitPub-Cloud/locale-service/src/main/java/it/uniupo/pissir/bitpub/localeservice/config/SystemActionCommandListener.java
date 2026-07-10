package it.uniupo.pissir.bitpub.localeservice.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.uniupo.pissir.bitpub.common.exception.BitpubException;
import it.uniupo.pissir.bitpub.common.mqtt.MqttCommandWrapper;
import it.uniupo.pissir.bitpub.localeservice.dto.AddGameInstanceRequest;
import it.uniupo.pissir.bitpub.localeservice.dto.CreateLocaleRequest;
import it.uniupo.pissir.bitpub.localeservice.service.LocaleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

/**
 * Autore: Stefano Bellan Matricola 20054330
 * 
 * Ascoltatore dei messaggi MQTT per la gestione dei locali. 
 * Consuma i comandi inoltrati dall'Edge e orchestra le relative chiamate al {@link LocaleService}.
 * Il payload JSON contiene un parametro discriminante per identificare l'azione richiesta, insieme ai dati necessari.
 * 
 * L'Edge convalida il JWT ma non il ruolo; pertanto le operazioni critiche (come la creazione o rimozione di un locale) 
 * vengono controllate nuovamente qui. Le operazioni sulle macchine sono invece validate direttamente nel servizio.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SystemActionCommandListener {

    private static final String PLATFORM_ADMIN = "PLATFORM_ADMIN";

    private final LocaleService localeService;
    private final ObjectMapper objectMapper;

    @ServiceActivator(inputChannel = "systemActionInboundChannel")
    public void onSystemAction(Message<String> message) {
        try {
            MqttCommandWrapper wrapper = objectMapper.readValue(message.getPayload(), MqttCommandWrapper.class);
            JsonNode p = objectMapper.readTree(wrapper.payload());
            String action = p.path("action").asText();
            String actorId = wrapper.actorUserId();
            String actorRole = wrapper.actorRole();
            switch (action) {
                case "CREATE_LOCALE" -> {
                    requirePlatformAdmin(actorRole);
                    localeService.createLocale(toCreateLocale(p));
                }
                case "DELETE_LOCALE" -> {
                    requirePlatformAdmin(actorRole);
                    localeService.deleteLocale(p.path("id").asText());
                }
                case "ADD_GAME_INSTANCE" ->
                        localeService.addGameInstance(p.path("localeId").asText(), toAddGame(p), actorId, actorRole);
                case "TOGGLE_GAME_INSTANCE" ->
                        localeService.setGameInstanceActive(p.path("localeId").asText(),
                                p.path("gameInstanceId").asText(), p.path("active").asBoolean(), actorId, actorRole);
                case "DELETE_GAME_INSTANCE" ->
                        localeService.removeGameInstance(p.path("localeId").asText(),
                                p.path("gameInstanceId").asText(), actorId, actorRole);
                default -> log.warn("Unknown locale action: {}", action);
            }
            log.info("Processed locale action {} via MQTT (actor {})", action, actorId);
        } catch (BitpubException e) {
            // Rifiuto legittimo (non autorizzato, non trovato, duplicato). Si registra l'evento senza causare il blocco della coda.
            log.info("Azione sul locale rifiutata via MQTT ({}): {}", e.getStatus(), e.getMessage());
        } catch (Exception e) {
            // Errore generico o messaggio malformato. Catturiamo l'eccezione per non bloccare l'elaborazione dei messaggi successivi.
            log.error("Elaborazione del comando MQTT per il locale fallita: {}", message.getPayload(), e);
        }
    }

    private void requirePlatformAdmin(String actorRole) {
        if (!PLATFORM_ADMIN.equals(actorRole)) {
            throw new BitpubException("Only PLATFORM_ADMIN can create or delete a locale", HttpStatus.FORBIDDEN);
        }
    }

    private CreateLocaleRequest toCreateLocale(JsonNode p) {
        CreateLocaleRequest r = new CreateLocaleRequest();
        r.setName(p.path("name").asText());
        r.setAddress(p.path("address").asText());
        r.setAdminId(p.path("adminId").asText());
        return r;
    }

    private AddGameInstanceRequest toAddGame(JsonNode p) {
        AddGameInstanceRequest r = new AddGameInstanceRequest();
        r.setLocalInstanceId(p.path("localInstanceId").asText());
        r.setGameTypeId(p.path("gameTypeId").asText());
        return r;
    }
}
