package it.uniupo.pissir.bitpub.edge;

import it.uniupo.pissir.bitpub.common.security.JwtUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;

// Import only JwtUtils from bitpub-common (to validate WebApp commands at submit time).
// Not broadening component-scan, which would also pull common's servlet auth filter.
@SpringBootApplication
@EnableScheduling
@Import(JwtUtils.class)
public class BitPubEdgeApplication {
    public static void main(String[] args) {
        SpringApplication.run(BitPubEdgeApplication.class, args);
    }
}
