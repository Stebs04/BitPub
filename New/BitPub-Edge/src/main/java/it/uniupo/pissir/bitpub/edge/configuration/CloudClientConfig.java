package it.uniupo.pissir.bitpub.edge.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class CloudClientConfig {

    @Value("${bitpub.cloud.match-service-url}")
    private String matchServiceUrl;

    @Bean
    public RestClient restClient() {
        return RestClient.builder()
                .baseUrl(matchServiceUrl)
                .build();
    }
}
