package it.uniupo.pissir.bitpub.gamecatalogservice.config;

import it.uniupo.pissir.bitpub.gamecatalogservice.domain.GameType;
import it.uniupo.pissir.bitpub.gamecatalogservice.domain.SensorDefinition;
import it.uniupo.pissir.bitpub.gamecatalogservice.repository.GameTypeRepository;
import it.uniupo.pissir.bitpub.gamecatalogservice.repository.SensorDefinitionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Semina i tipi di gioco base (Calciobalilla, Biliardo, Freccette) cosi' che un LOCALE_ADMIN
 * abbia machine da poter aggiungere al proprio locale senza dover prima passare da GAME_ADMIN.
 * Idempotente: salta i tipi gia' presenti (findByName), non tocca eventuali dati creati a mano.
 */
@Component
@RequiredArgsConstructor
public class GameCatalogDataSeeder implements CommandLineRunner {

    private final GameTypeRepository gameTypeRepository;
    private final SensorDefinitionRepository sensorDefinitionRepository;

    @Override
    public void run(String... args) {
        seedGameType("Calciobalilla", "Calciobalilla simulato con sensori di goal per squadra",
                List.of(
                        new SensorSeed("MATCH_START", "Inizio partita", false),
                        new SensorSeed("GOAL", "Goal segnato", false),
                        new SensorSeed("MATCH_END", "Fine partita", false)
                ));

        seedGameType("Biliardo", "Biliardo simulato con sensori di imbucata e fallo",
                List.of(
                        new SensorSeed("MATCH_START", "Inizio partita", false),
                        new SensorSeed("BALL_POCKETED", "Palla imbucata", false),
                        new SensorSeed("FOUL", "Fallo commesso", false),
                        new SensorSeed("MATCH_END", "Fine partita", false)
                ));

        seedGameType("Freccette", "Freccette simulate con sensore di punteggio per lancio",
                List.of(
                        new SensorSeed("MATCH_START", "Inizio partita", false),
                        new SensorSeed("DART_HIT", "Freccia lanciata", false),
                        new SensorSeed("MATCH_END", "Fine partita", false)
                ));
    }

    private void seedGameType(String name, String description, List<SensorSeed> sensors) {
        if (gameTypeRepository.findByName(name).isPresent()) {
            return;
        }

        GameType gameType = GameType.builder()
                .name(name)
                .description(description)
                .rulesEngineId(name.trim().toLowerCase().replaceAll("\\s+", "_"))
                .sensors(new java.util.ArrayList<>())
                .build();
        GameType saved = gameTypeRepository.save(gameType);

        for (SensorSeed s : sensors) {
            sensorDefinitionRepository.save(SensorDefinition.builder()
                    .type(s.type())
                    .description(s.description())
                    .isActuator(s.isActuator())
                    .gameType(saved)
                    .build());
        }
    }

    private record SensorSeed(String type, String description, boolean isActuator) {}
}
