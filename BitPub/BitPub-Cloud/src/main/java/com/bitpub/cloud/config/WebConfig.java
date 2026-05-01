package com.bitpub.cloud.config;

import com.bitpub.cloud.security.AuditInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configurazione personalizzata del framework Web MVC per l'ecosistema BitPub.
 * Questa classe registra i componenti necessari per il monitoraggio del traffico
 * e la gestione del ciclo di vita delle richieste HTTP.
 *
 * @author Stefano Bellan 20054330
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /** Intercettore dedicato alla registrazione automatica degli eventi di Audit. */
    @Autowired
    private AuditInterceptor auditInterceptor;

    /**
     * Registra gli intercettori nel registro globale di Spring MVC.
     * Configura i path pattern specifici per i quali l'audit trail deve essere attivo.
     *
     * @param registry Il registro degli intercettori fornito dal framework.
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Applichiamo la logica di tracciamento (Audit Trail) esclusivamente alle rotte API v1
        // Questo garantisce che ogni chiamata verso gli endpoint REST venga loggata nel DB PostgreSQL
        registry.addInterceptor(auditInterceptor)
                .addPathPatterns("/api/v1/**");
    }
}
