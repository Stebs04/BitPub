package com.bitpub.cloud.config;

import com.bitpub.cloud.security.AuditInterceptor;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.GsonHttpMessageConverter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Configurazione Web MVC centralizzata.
 * Gestisce la registrazione degli intercettori di sicurezza e forza l'utilizzo
 * di GSON come convertitore JSON globale per le API REST.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private AuditInterceptor auditInterceptor;

    /**
     * Registra l'AuditInterceptor per monitorare il traffico API v1.
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(auditInterceptor)
                .addPathPatterns("/api/v1/**");
    }

    /**
     * Configura GSON come convertitore di messaggi HTTP predefinito.
     * Questo assicura che il formato JSON delle API sia identico a quello dei messaggi MQTT.
     */
    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        GsonHttpMessageConverter gsonConverter = new GsonHttpMessageConverter();
        gsonConverter.setGson(gson());
        // Aggiungiamo il convertitore in prima posizione per dargli la precedenza su Jackson
        converters.add(0, gsonConverter);
    }

    /**
     * Bean GSON centralizzato con configurazione personalizzata per le date.
     */
    @Bean
    public Gson gson() {
        return new GsonBuilder()
                .setDateFormat("yyyy-MM-dd HH:mm:ss") // Standard BitPub per la coerenza temporale
                .serializeNulls()                     // Utile per HATEOAS se mancano alcuni link
                .create();
    }
}