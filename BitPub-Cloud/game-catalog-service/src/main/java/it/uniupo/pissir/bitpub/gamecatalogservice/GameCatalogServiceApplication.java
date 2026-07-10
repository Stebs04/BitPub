/**
 * Autore: Stefano Bellan Matricola 20054330
 */
package it.uniupo.pissir.bitpub.gamecatalogservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"it.uniupo.pissir.bitpub.gamecatalogservice", "it.uniupo.pissir.bitpub.common"})
public class GameCatalogServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(GameCatalogServiceApplication.class, args);
    }
}
