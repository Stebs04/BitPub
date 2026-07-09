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
 * Semina un locale demo assegnato all'utente seed "locale_admin" (user-service), con le
 * game instance dei tipi di gioco seminati da GameCatalogDataSeeder, cosi' il ruolo
 * LOCALE_ADMIN e' testabile end-to-end subito dopo il primo avvio.
 * Idempotente: salta se "locale_admin" ha gia' un locale assegnato.
 * Attende (con retry) che user-service e game-catalog-service siano raggiungibili, dato che
 * l'ordine di startup dei container non e' garantito.
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
            RestClient.create(userServiceUrl)
                    .patch()
                    .uri("/api/v1/users/{id}/locale?localeId={localeId}", adminId, localeId)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.error("LocaleDataSeeder: sync localeId fallita per adminId {}", adminId, e);
        }
    }
}
