/**
 * Autore: Timothy Giolito 20054431
 */
package it.uniupo.pissir.bitpub.edge;

import it.uniupo.pissir.bitpub.common.security.JwtUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;

// Importiamo solo JwtUtils da bitpub-common per validare i comandi della WebApp al momento dell'invio.
// Preferisco non allargare il component-scan per evitare di tirare dentro anche il filtro auth servlet del modulo common.
@SpringBootApplication
@EnableScheduling
@Import(JwtUtils.class)
public class BitPubEdgeApplication {
    public static void main(String[] args) {
        SpringApplication.run(BitPubEdgeApplication.class, args);
    }
}
