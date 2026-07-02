package it.uniupo.pissir.bitpub.localeservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"it.uniupo.pissir.bitpub.localeservice", "it.uniupo.pissir.bitpub.common"})
public class LocaleServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(LocaleServiceApplication.class, args);
    }
}
