package com.bitpub.stats;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class StatsApplication {
    public static void main(String[] args) {
        SpringApplication.run(StatsApplication.class, args);
    }
}
