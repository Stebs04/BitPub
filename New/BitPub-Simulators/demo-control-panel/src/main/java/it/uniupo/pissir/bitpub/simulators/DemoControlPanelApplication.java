package it.uniupo.pissir.bitpub.simulators;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DemoControlPanelApplication {
    public static void main(String[] args) {
        SpringApplication.run(DemoControlPanelApplication.class, args);
    }
}
