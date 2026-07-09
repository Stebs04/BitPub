package it.uniupo.pissir.bitpub.authservice.service;

import it.uniupo.pissir.bitpub.authservice.dto.JwtResponse;
import it.uniupo.pissir.bitpub.authservice.dto.LoginRequest;
import it.uniupo.pissir.bitpub.common.exception.BitpubException;
import it.uniupo.pissir.bitpub.common.security.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String USER_URL = "http://user-service:8082";

    @Mock
    private JwtUtils jwtUtils;

    private AuthService authService;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();
        authService = new AuthService(jwtUtils, restClient);
        ReflectionTestUtils.setField(authService, "userServiceUrl", USER_URL);
    }

    private LoginRequest login(String u, String p) {
        LoginRequest r = new LoginRequest();
        r.setUsername(u);
        r.setPassword(p);
        return r;
    }

    @Test
    void login_valid_returnsTokenAndUserData() {
        String userJson = """
                {"id":"u1","username":"alice","email":"alice@bitpub.it","role":"PLAYER","localeId":"loc1"}
                """;
        server.expect(requestTo(USER_URL + "/api/v1/users/verify"))
              .andExpect(method(org.springframework.http.HttpMethod.POST))
              .andRespond(withSuccess(userJson, MediaType.APPLICATION_JSON));

        when(jwtUtils.generateToken("alice", "PLAYER", "u1", "loc1")).thenReturn("jwt-token");

        JwtResponse res = authService.login(login("alice", "secret"));

        assertThat(res.getToken()).isEqualTo("jwt-token");
        assertThat(res.getType()).isEqualTo("Bearer");
        assertThat(res.getId()).isEqualTo("u1");
        assertThat(res.getUsername()).isEqualTo("alice");
        assertThat(res.getEmail()).isEqualTo("alice@bitpub.it");
        assertThat(res.getRole()).isEqualTo("PLAYER");
        assertThat(res.getLocaleId()).isEqualTo("loc1");
        server.verify();
    }

    @Test
    void login_userServiceRejects_throwsUnauthorized() {
        server.expect(requestTo(USER_URL + "/api/v1/users/verify"))
              .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> authService.login(login("alice", "wrong")))
                .isInstanceOf(BitpubException.class)
                .satisfies(e -> assertThat(((BitpubException) e).getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED));
        server.verify();
    }
}
