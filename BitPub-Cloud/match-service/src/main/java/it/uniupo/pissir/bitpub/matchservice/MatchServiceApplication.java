package it.uniupo.pissir.bitpub.matchservice;

import it.uniupo.pissir.bitpub.common.exception.GlobalExceptionHandler;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

// Register the shared @ControllerAdvice so BitpubException maps to its real HTTP status
// (e.g. "Non e' il tuo turno" -> 403) instead of a default 500. Without this the Edge sees
// a 5xx and wrongly buffers/retries a logical rejection.
@SpringBootApplication
@Import(GlobalExceptionHandler.class)
public class MatchServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(MatchServiceApplication.class, args);
    }
}
