package it.uniupo.pissir.bitpub.localeservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.uniupo.pissir.bitpub.common.exception.BitpubException;
import it.uniupo.pissir.bitpub.common.exception.ResourceNotFoundException;
import it.uniupo.pissir.bitpub.localeservice.config.MqttConfig.GamePublisher;
import it.uniupo.pissir.bitpub.localeservice.domain.GameInstance;
import it.uniupo.pissir.bitpub.localeservice.domain.Locale;
import it.uniupo.pissir.bitpub.localeservice.dto.AddGameInstanceRequest;
import it.uniupo.pissir.bitpub.localeservice.dto.CreateLocaleRequest;
import it.uniupo.pissir.bitpub.localeservice.dto.GameInstanceDto;
import it.uniupo.pissir.bitpub.localeservice.dto.LocaleDto;
import it.uniupo.pissir.bitpub.localeservice.repository.GameInstanceRepository;
import it.uniupo.pissir.bitpub.localeservice.repository.LocaleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Autore: Stefano Bellan Matricola 20054330
 * 
 * Classe di test unitario per il LocaleService. Verifica la corretta esecuzione della logica di business, 
 * comprese le autorizzazioni di accesso, la persistenza e la pubblicazione di eventi MQTT.
 */
@ExtendWith(MockitoExtension.class)
class LocaleServiceTest {

    @Mock private LocaleRepository localeRepository;
    @Mock private GameInstanceRepository gameInstanceRepository;
    @Mock private GamePublisher gamePublisher;
    // ObjectMapper reale: publishGameEvent lo usa solo per serializzare una Map, nessun mock utile.
    private final ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks private LocaleService service;

    @BeforeEach
    void wireObjectMapper() {
        // @InjectMocks non inietta il campo final objectMapper; lo passo via costruttore reale.
        service = new LocaleService(localeRepository, gameInstanceRepository, gamePublisher, objectMapper);
    }

    private Locale locale(String id, String adminId) {
        return Locale.builder().id(id).name("Bar").address("Via 1").adminId(adminId).gameInstances(new java.util.ArrayList<>()).build();
    }

    private GameInstance instance(String id, Locale l, boolean active) {
        return GameInstance.builder().id(id).localInstanceId("m1").gameTypeId("pool").locale(l).active(active).build();
    }

    // --- createLocale ---
    @Test
    void createLocale_savesAndMapsDto() {
        CreateLocaleRequest req = new CreateLocaleRequest();
        req.setName("Bar"); req.setAddress("Via 1"); req.setAdminId(null); // null => nessuna sync HTTP esterna
        when(localeRepository.save(any(Locale.class))).thenAnswer(inv -> {
            Locale l = inv.getArgument(0);
            return Locale.builder().id("loc1").name(l.getName()).address(l.getAddress())
                    .adminId(l.getAdminId()).gameInstances(l.getGameInstances()).build();
        });

        LocaleDto dto = service.createLocale(req);

        assertThat(dto.getId()).isEqualTo("loc1");
        assertThat(dto.getName()).isEqualTo("Bar");
    }

    @Test
    void getLocaleById_missing_throwsNotFound() {
        when(localeRepository.findById("x")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getLocaleById("x")).isInstanceOf(ResourceNotFoundException.class);
    }

    // --- assertLocaleManageable (via addGameInstance) ---
    @Test
    void addGameInstance_platformAdmin_ok() {
        Locale l = locale("loc1", "ownerX");
        when(localeRepository.findById("loc1")).thenReturn(Optional.of(l));
        when(gameInstanceRepository.findByLocaleIdAndLocalInstanceId("loc1", "m1")).thenReturn(Optional.empty());
        when(gameInstanceRepository.save(any())).thenAnswer(inv -> { GameInstance g = inv.getArgument(0); g.setId("gi1"); return g; });

        AddGameInstanceRequest req = new AddGameInstanceRequest();
        req.setLocalInstanceId("m1"); req.setGameTypeId("pool");

        GameInstanceDto dto = service.addGameInstance("loc1", req, "someoneElse", "PLATFORM_ADMIN");

        assertThat(dto.getId()).isEqualTo("gi1");
        verify(gamePublisher).publish(anyString(), anyString());
    }

    @Test
    void addGameInstance_localAdminOwner_ok() {
        Locale l = locale("loc1", "admin1");
        when(localeRepository.findById("loc1")).thenReturn(Optional.of(l));
        when(gameInstanceRepository.findByLocaleIdAndLocalInstanceId("loc1", "m1")).thenReturn(Optional.empty());
        when(gameInstanceRepository.save(any())).thenAnswer(inv -> { GameInstance g = inv.getArgument(0); g.setId("gi1"); return g; });

        AddGameInstanceRequest req = new AddGameInstanceRequest();
        req.setLocalInstanceId("m1"); req.setGameTypeId("pool");

        assertThat(service.addGameInstance("loc1", req, "admin1", "LOCALE_ADMIN").getId()).isEqualTo("gi1");
    }

    @Test
    void addGameInstance_localAdminNotOwner_forbidden() {
        Locale l = locale("loc1", "admin1");
        when(localeRepository.findById("loc1")).thenReturn(Optional.of(l));

        AddGameInstanceRequest req = new AddGameInstanceRequest();
        req.setLocalInstanceId("m1"); req.setGameTypeId("pool");

        assertThatThrownBy(() -> service.addGameInstance("loc1", req, "otherAdmin", "LOCALE_ADMIN"))
                .isInstanceOf(BitpubException.class)
                .satisfies(e -> assertThat(((BitpubException) e).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
        verify(gameInstanceRepository, never()).save(any());
    }

    @Test
    void addGameInstance_playerRole_forbidden() {
        Locale l = locale("loc1", "admin1");
        when(localeRepository.findById("loc1")).thenReturn(Optional.of(l));

        AddGameInstanceRequest req = new AddGameInstanceRequest();
        req.setLocalInstanceId("m1"); req.setGameTypeId("pool");

        assertThatThrownBy(() -> service.addGameInstance("loc1", req, "p1", "PLAYER"))
                .isInstanceOf(BitpubException.class)
                .satisfies(e -> assertThat(((BitpubException) e).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void addGameInstance_duplicateLocalInstanceId_conflict() {
        Locale l = locale("loc1", "admin1");
        when(localeRepository.findById("loc1")).thenReturn(Optional.of(l));
        when(gameInstanceRepository.findByLocaleIdAndLocalInstanceId("loc1", "m1"))
                .thenReturn(Optional.of(instance("existing", l, true)));

        AddGameInstanceRequest req = new AddGameInstanceRequest();
        req.setLocalInstanceId("m1"); req.setGameTypeId("pool");

        assertThatThrownBy(() -> service.addGameInstance("loc1", req, "admin1", "LOCALE_ADMIN"))
                .isInstanceOf(BitpubException.class)
                .satisfies(e -> assertThat(((BitpubException) e).getStatus()).isEqualTo(HttpStatus.CONFLICT));
    }

    // --- removeGameInstance ---
    @Test
    void removeGameInstance_wrongLocale_notFound() {
        Locale l = locale("loc1", "admin1");
        when(gameInstanceRepository.findById("gi1")).thenReturn(Optional.of(instance("gi1", l, true)));

        assertThatThrownBy(() -> service.removeGameInstance("OTHER_LOCALE", "gi1", "admin1", "LOCALE_ADMIN"))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(gameInstanceRepository, never()).delete(any());
    }

    // --- getOnlineLocales ---
    @Test
    void getOnlineLocales_returnsOnlyLocalesWithActiveInstance() {
        Locale online = locale("on", "a");
        online.getGameInstances().add(instance("g1", online, true));
        Locale offline = locale("off", "a");
        offline.getGameInstances().add(instance("g2", offline, false));
        when(localeRepository.findAll()).thenReturn(List.of(online, offline));

        List<LocaleDto> result = service.getOnlineLocales();

        assertThat(result).extracting(LocaleDto::getId).containsExactly("on");
    }
}
