package com.bitpub.cloud.security;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

/**
 * Utility TLS lato Cloud per la configurazione del Mutual TLS (mTLS).
 * Rifattorizzata come @Component per l'iniezione sicura dei segreti
 * tramite variabili d'ambiente o application.yml.
 *
 * @author Stefano Bellan
 */
@Component // Diventa un Bean di Spring per poter usare @Value
public class CloudTlsUtility {

    @Value("${mqtt.tls.ca-path}")
    private String caCrtPath;

    @Value("${mqtt.tls.client-crt-path}")
    private String clientCrtPath;

    @Value("${mqtt.tls.client-key-path}")
    private String clientKeyPath;

    @Value("${mqtt.tls.keystore-password}")
    private String keystorePassword;

    /**
     * Costruisce una SSLSocketFactory configurata con i certificati BitPub.
     * Ora i percorsi e le password sono dinamici.
     */
    public SSLSocketFactory getSocketFactory() throws Exception {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");

        // 1. Root CA
        X509Certificate caCert;
        try (InputStream is = new BufferedInputStream(new FileInputStream(caCrtPath))) {
            caCert = (X509Certificate) cf.generateCertificate(is);
        }

        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        trustStore.load(null, null);
        trustStore.setCertificateEntry("ca-root", caCert);

        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(trustStore);

        // 2. Client Cert
        X509Certificate clientCert;
        try (InputStream is = new BufferedInputStream(new FileInputStream(clientCrtPath))) {
            clientCert = (X509Certificate) cf.generateCertificate(is);
        }

        // 3. Client Key
        byte[] keyBytes = Files.readAllBytes(Paths.get(clientKeyPath));
        String keyString = new String(keyBytes)
                .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                .replace("-----END RSA PRIVATE KEY-----", "")
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");

        byte[] decodedKey = Base64.getDecoder().decode(keyString);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decodedKey);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        PrivateKey privateKey = kf.generatePrivate(spec);

        // 4. KeyStore con identità del Cloud e password iniettata
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null, null);
        keyStore.setCertificateEntry("client-cert", clientCert);

        // Uso della variabile sicura invece di "password".toCharArray()
        keyStore.setKeyEntry("client-key", privateKey, keystorePassword.toCharArray(),
                new java.security.cert.Certificate[]{clientCert});

        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, keystorePassword.toCharArray());

        // 5. Contesto SSL
        SSLContext context = SSLContext.getInstance("TLSv1.2");
        context.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

        return context.getSocketFactory();
    }

    /**
     * Applica la configurazione mTLS alle opzioni di connessione MQTT.
     */
    public void applyTlsToOptions(MqttConnectOptions options) {
        try {
            options.setSocketFactory(getSocketFactory());
            options.setHttpsHostnameVerificationEnabled(false);
            System.out.println("[CLOUD-TLS] SSLSocketFactory configurata con successo tramite variabili d'ambiente.");
        } catch (Exception e) {
            throw new RuntimeException("[CLOUD-FATAL] Impossibile configurare il layer TLS. " +
                    "Verificare i certificati e la password del keystore.", e);
        }
    }
}