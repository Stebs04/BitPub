package it.uniupo.pissir.bitpub.gamecatalogservice.config;

import it.uniupo.pissir.bitpub.gamecatalogservice.domain.GameType;
import it.uniupo.pissir.bitpub.gamecatalogservice.domain.SensorDefinition;
import it.uniupo.pissir.bitpub.gamecatalogservice.repository.GameTypeRepository;
import it.uniupo.pissir.bitpub.gamecatalogservice.repository.SensorDefinitionRepository;
import it.uniupo.pissir.bitpub.gamecatalogservice.service.GameCatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Semina i tipi di gioco base (Calciobalilla, Biliardo, Freccette) cosi' che un LOCALE_ADMIN
 * abbia machine da poter aggiungere al proprio locale senza dover prima passare da GAME_ADMIN.
 * Idempotente: salta i tipi gia' presenti (findByName), non tocca eventuali dati creati a mano.
 */
// @Component  // ponytail: seed demo disabilitato - all'avvio si creano solo i 4 utenti base
@RequiredArgsConstructor
public class GameCatalogDataSeeder implements CommandLineRunner {

    private final GameTypeRepository gameTypeRepository;
    private final SensorDefinitionRepository sensorDefinitionRepository;
    private final GameCatalogService gameCatalogService;

    // Sensori di controllo: non segnano punti, si attivano sempre.
    private static final SensorSeed START = new SensorSeed("MATCH_START", "Inizio partita", false, 0, 1.0);
    private static final SensorSeed END = new SensorSeed("MATCH_END", "Fine partita", false, 0, 1.0);

    @Override
    public void run(String... args) {
        seedGameType("Calciobalilla", "Calciobalilla simulato con sensori di goal per squadra", 10,
                List.of(
                        START,
                        new SensorSeed("GOAL", "Goal segnato", false, 1, 0.5),
                        END
                ));

        seedGameType("Biliardo", "Biliardo simulato con sensori di imbucata e fallo", 7,
                List.of(
                        START,
                        new SensorSeed("BALL_POCKETED", "Palla imbucata", false, 1, 0.5),
                        new SensorSeed("FOUL", "Fallo commesso", false, 0, 1.0),
                        END
                ));

        seedGameType("Freccette", "Freccette simulate con sensore di punteggio per lancio", 100,
                List.of(
                        START,
                        new SensorSeed("DART_HIT", "Freccia lanciata", false, 20, 0.7),
                        END
                ));
    }

    private void seedGameType(String name, String description, int winScoreTarget, List<SensorSeed> sensors) {
        if (gameTypeRepository.findByName(name).isPresent()) {
            return;
        }

        GameType gameType = GameType.builder()
                .name(name)
                .description(description)
                .rulesEngineId(name.trim().toLowerCase().replaceAll("\\s+", "_"))
                .winScoreTarget(winScoreTarget)
                .sensors(new java.util.ArrayList<>())
                .build();
        GameType saved = gameTypeRepository.save(gameType);

        for (SensorSeed s : sensors) {
            sensorDefinitionRepository.save(SensorDefinition.builder()
                    .type(s.type())
                    .description(s.description())
                    .isActuator(s.isActuator())
                    .scoreIncrement(s.scoreIncrement())
                    .successProbability(s.successProbability())
                    .gameType(saved)
                    .build());
        }

        // Pubblica lo snapshot retained cosi' i simulatori hanno le regole anche per i giochi seminati.
        gameCatalogService.publishConfig(saved.getId());
    }

    private record SensorSeed(String type, String description, boolean isActuator, int scoreIncrement,
                              double successProbability) {}
}
