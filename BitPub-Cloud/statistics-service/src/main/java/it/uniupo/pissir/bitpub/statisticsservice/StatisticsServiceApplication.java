package it.uniupo.pissir.bitpub.statisticsservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"it.uniupo.pissir.bitpub.statisticsservice", "it.uniupo.pissir.bitpub.common"})
public class StatisticsServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(StatisticsServiceApplication.class, args);
    }
}
