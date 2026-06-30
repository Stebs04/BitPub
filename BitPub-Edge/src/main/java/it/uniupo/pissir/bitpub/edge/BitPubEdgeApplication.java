package it.uniupo.pissir.bitpub.edge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BitPubEdgeApplication {
    public static void main(String[] args) {
        SpringApplication.run(BitPubEdgeApplication.class, args);
    }
}
