package com.bitpub.security;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
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
 * Utility per la configurazione della sicurezza TLS nel progetto BitPub.
 * Questa classe si occupa di caricare i certificati per stabilire una
 * connessione mTLS (Mutual TLS) sicura con il broker Mosquitto.
 */
public class TlsUtility {

    /**
     * Crea una SSLSocketFactory configurata con i certificati di BitPub.
     * * @param caCrtPath Percorso del certificato della Root CA (ca.crt)
     * @param clientCrtPath Percorso del certificato del client (client.crt)
     * @param clientKeyPath Percorso della chiave privata del client (client.key)
     * @return SSLSocketFactory pronta per l'uso con MqttConnectOptions
     */
    public static SSLSocketFactory getSocketFactory(String caCrtPath, String clientCrtPath, String clientKeyPath) throws Exception {

        // 1. Carichiamo il certificato della Root CA per fidarci del Broker
        CertificateFactory cf = CertificateFactory.getInstance("X.509");

        X509Certificate caCert;
        try (InputStream is = new BufferedInputStream(new FileInputStream(caCrtPath))) {
            caCert = (X509Certificate) cf.generateCertificate(is);
        }

        // Creiamo il TrustStore che contiene la nostra CA
        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        trustStore.load(null, null);
        trustStore.setCertificateEntry("ca-root", caCert);

        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(trustStore);

        // 2. Carichiamo il certificato pubblico del Client
        X509Certificate clientCert;
        try (InputStream is = new BufferedInputStream(new FileInputStream(clientCrtPath))) {
            clientCert = (X509Certificate) cf.generateCertificate(is);
        }

        // 3. Carichiamo la chiave privata del Client
        // Nota: Java richiede che la chiave sia in formato PKCS#8 per essere letta facilmente.
        // Se la chiave è stata generata come RSA (PKCS#1), rimuoviamo gli header e la decodifichiamo.
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

        // Creiamo il KeyStore per l'identità del Client (Certificato + Chiave)
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null, null);
        keyStore.setCertificateEntry("client-cert", clientCert);
        // La password "password" è temporanea e interna al KeyStore in memoria
        keyStore.setKeyEntry("client-key", privateKey, "password".toCharArray(), new java.security.cert.Certificate[]{clientCert});

        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, "password".toCharArray());

        // 4. Inizializziamo il contesto SSL con TrustManager (per il server) e KeyManager (per noi)
        SSLContext context = SSLContext.getInstance("TLSv1.2");
        context.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

        return context.getSocketFactory();
    }

    /**
     * Applica la configurazione TLS alle opzioni di connessione MQTT.
     *
     * <p><b>Fix Bug #3:</b> Il metodo ora rilancia l'eccezione invece di catturarla
     * silenziosamente. Prima il codice stampava l'errore ma continuava, causando
     * un tentativo di connect() senza TLS che falliva in modo opaco.</p>
     *
     * @throws Exception se un certificato è mancante, corrotto o il formato è errato
     */
    public static void applyTlsToOptions(MqttConnectOptions options, String baseCertsPath) throws Exception {
        String caPath  = baseCertsPath + "/ca.crt";
        String crtPath = baseCertsPath + "/client.crt";
        String keyPath = baseCertsPath + "/client.key";

        options.setSocketFactory(getSocketFactory(caPath, crtPath, keyPath));
        // Disabilitiamo l'autenticazione testuale perché usiamo i certificati
        options.setUserName(null);
        options.setPassword(null);
    }
}