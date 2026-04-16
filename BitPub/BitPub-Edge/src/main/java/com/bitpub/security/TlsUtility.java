package com.bitpub.security;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

/**
 * Utility di basso livello per il provisioning del layer di sicurezza M2M (Machine-to-Machine).
 * Questa classe astrae la configurazione del contesto crittografico TLS, permettendo
 * l'autenticazione del broker Cloud tramite una Certificate Authority (CA) privata.
 *
 * @author Stefano Bellan 20054330
 */
public class TlsUtility {

    /**
     * Genera una {@link SSLSocketFactory} custom basata su un certificato CA specifico.
     * Implementa il pattern "Trust-on-First-Use" limitato alla CA fornita, isolando
     * l'applicazione dai certificati di sistema per una maggiore sicurezza (Cert Pinning/Hardening).
     *
     * @param caCrtFilePath Percorso del file "ca.crt" (formato X.509 PEM).
     * @return Una factory configurata per connessioni TLSv1.2.
     * @throws Exception Se il certificato è malformato, il file non è accessibile o
     *                   l'algoritmo crittografico non è supportato dal JRE.
     */
    public static SSLSocketFactory getSocketFactory(String caCrtFilePath) throws Exception {

        // 1. Istanza del parser per certificati standard X.509
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        X509Certificate caCert;

        // 2. Caricamento del certificato tramite BufferedInputStream per ottimizzare l'I/O
        // L'uso del try-with-resources garantisce la chiusura dello stream anche in caso di errore.
        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(caCrtFilePath))) {
            caCert = (X509Certificate) cf.generateCertificate(bis);
        }

        /*
         * 3. Creazione di un TrustStore "In-Memory".
         * Non utilizziamo un file JKS fisico su disco per evitare la gestione di password
         * e permessi file aggiuntivi, aumentando la sicurezza runtime.
         */
        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        trustStore.load(null, null);

        // 4. Mapping del certificato CA nel TrustStore con alias univoco
        trustStore.setCertificateEntry("bitpub-ca", caCert);

        /*
         * 5. Inizializzazione del TrustManagerFactory.
         * Questo componente è responsabile di decidere se un certificato presentato
         * dal server (Broker) sia attendibile o meno.
         */
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(trustStore);

        /*
         * 6. Setup del contesto SSL forzando TLSv1.2.
         * Nota: Si consiglia TLSv1.2 come requisito minimo per conformità agli standard di sicurezza moderni.
         */
        SSLContext sslContext = SSLContext.getInstance("TLSv1.2");

        // Inizializzazione contesto: (KeyManager: null, TrustManager: tmf, SecureRandom: null)
        // Non usiamo certificati client (mTLS) in questa fase, solo validazione server.
        sslContext.init(null, tmf.getTrustManagers(), null);

        return sslContext.getSocketFactory();
    }
}
