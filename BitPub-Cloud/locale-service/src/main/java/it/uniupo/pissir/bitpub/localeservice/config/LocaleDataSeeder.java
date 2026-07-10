package it.uniupo.pissir.bitpub.localeservice.config;

import it.uniupo.pissir.bitpub.localeservice.domain.GameInstance;
import it.uniupo.pissir.bitpub.localeservice.domain.Locale;
import it.uniupo.pissir.bitpub.localeservice.repository.GameInstanceRepository;
import it.uniupo.pissir.bitpub.localeservice.repository.LocaleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Autore: Stefano Bellan Matricola 20054330
 * 
 * Inizializza un locale dimostrativo per l'amministratore predefinito "locale_admin".
 * Si occupa di assegnare le macchine da gioco basandosi sui dati forniti dal catalogo.
 * L'operazione e' sicura e non duplica i dati se l'utente ha gia' un locale assegnato.
 * Il processo prevede dei tentativi successivi nel caso i servizi dipendenti non siano immediatamente pronti all'avvio.
 */
// @Component  // ponytail: seed demo disabilitato - all'avvio si creano solo i 4 utenti base
@RequiredArgsConstructor
@Slf4j
public class LocaleDataSeeder implements CommandLineRunner {

    private static final String DEMO_ADMIN_USERNAME = "locale_admin";
    private static final int MAX_RETRIES = 20;
    private static final long RETRY_DELAY_MS = 3000;

    private final LocaleRepository localeRepository;
    private final GameInstanceRepository gameInstanceRepository;

    @Value("${user.service.url:http://localhost:8082}")
    private String userServiceUrl;

    @Value("${catalog.service.url:http://localhost:8084}")
    private String catalogServiceUrl;

    @Override
    public void run(String... args) {
        String adminId = waitForAdminId();
        if (adminId == null) {
            log.warn("LocaleDataSeeder: user-service non raggiungibile o '{}' non trovato, seed demo saltato", DEMO_ADMIN_USERNAME);
            return;
        }
        if (!localeRepository.findByAdminId(adminId).isEmpty()) {
            return;
        }

        Locale locale = Locale.builder()
                .name("BitPub Demo Pub")
                .address("Via Roma 1, Torino")
                .adminId(adminId)
                .createdAt(Instant.now())
                .gameInstances(new ArrayList<>())
                .build();
        Locale saved = localeRepository.save(locale);
        syncAdminLocaleId(adminId, saved.getId());

        List<Map> gameTypes = fetchGameTypes();
        List<GameInstance> instances = new ArrayList<>();
        int counter = 1;
        for (Map type : gameTypes) {
            String typeName = String.valueOf(type.get("name"));
            String typeId = String.valueOf(type.get("id")); // UUID catalogo: e' la chiave con cui il simulatore trova le regole
            instances.add(GameInstance.builder()
                    .localInstanceId(typeName.toLowerCase() + "-" + counter++)
                    .gameTypeId(typeId)
                    .locale(saved)
                    .installedAt(Instant.now())
                    .active(true)
                    .build());
        }
        if (!instances.isEmpty()) {
            gameInstanceRepository.saveAll(instances);
        }
        log.info("LocaleDataSeeder: locale demo '{}' assegnato a '{}' con {} macchina/e", saved.getName(), DEMO_ADMIN_USERNAME, instances.size());
    }

    private String waitForAdminId() {
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                Map response = RestClient.create(userServiceUrl)
                        .get()
                        .uri("/api/v1/users/by-username/{username}", DEMO_ADMIN_USERNAME)
                        .retrieve()
                        .body(Map.class);
                if (response != null && response.get("id") != null) {
                    return response.get("id").toString();
                }
            } catch (Exception e) {
                // user-service non ancora pronto, ritenta
            }
            try {
                Thread.sleep(RETRY_DELAY_MS);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                return null;
            }
        }
        return null;
    }

    private List<Map> fetchGameTypes() {
        try {
            List response = RestClient.create(catalogServiceUrl)
                    .get()
                    .uri("/api/v1/catalog/games")
                    .retrieve()
                    .body(List.class);
            List<Map> types = new ArrayList<>();
            if (response != null) {
                for (Object o : response) {
                    if (o instanceof Map && ((Map) o).get("id") != null && ((Map) o).get("name") != null) {
                        types.add((Map) o);
                    }
                }
            }
            return types;
        } catch (Exception e) {
            log.warn("LocaleDataSeeder: game-catalog-service non raggiungibile, locale demo seminato senza macchine");
            return new ArrayList<>();
        }
    }

    private void syncAdminLocaleId(String adminId, String localeId) {
        try {
            // Aggiorno il profilo utente con l'id del nuovo locale assegnato
            RestClient.create(userServiceUrl)
                    .patch()
                    .uri("/api/v1/users/{id}/locale?localeId={localeId}", adminId, localeId)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.error("LocaleDataSeeder: sincronizzazione fallita per l'admin con ID {}", adminId, e);
        }
    }
}
