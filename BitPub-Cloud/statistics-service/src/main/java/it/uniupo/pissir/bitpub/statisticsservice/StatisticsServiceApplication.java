/**
 * Autore: Stefano Bellan Matricola 20054330
 */
package it.uniupo.pissir.bitpub.statisticsservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * Entry point principale per il microservizio relativo alle statistiche.
 * Include la configurazione per la scansione dei componenti comuni di BitPub.
 */
@SpringBootApplication
@ComponentScan(basePackages = {"it.uniupo.pissir.bitpub.statisticsservice", "it.uniupo.pissir.bitpub.common"})
public class StatisticsServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(StatisticsServiceApplication.class, args);
    }
}
