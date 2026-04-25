package com.bitpub.cloud.security;

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
 * Utility TLS lato Cloud per la configurazione del Mutual TLS (mTLS)
 * verso il broker Mosquitto sulla porta 8883.
 *
 * <p>Classe speculare a TlsUtility (modulo Edge). Duplicata per rispettare
 * i confini architetturali: il Cloud non deve dipendere dal modulo Edge.</p>
 *
 * @author Stefano Bellan 20054330
 */
public class CloudTlsUtility {

    /**
     * Costruisce una SSLSocketFactory configurata con i certificati BitPub.
     *
     * @param caCrtPath     Percorso del certificato della Root CA (ca.crt)
     * @param clientCrtPath Percorso del certificato del client/cloud (client.crt)
     * @param clientKeyPath Percorso della chiave privata PKCS#8 (client.key)
     * @return SSLSocketFactory pronta per l'uso con MqttConnectOptions
     * @throws Exception Se un certificato è mancante, corrotto o il formato è errato
     */
    public static SSLSocketFactory getSocketFactory(String caCrtPath, String clientCrtPath, String clientKeyPath) throws Exception {

        // 1. Carichiamo il certificato della Root CA per fidarci del Broker
        CertificateFactory cf = CertificateFactory.getInstance("X.509");

        X509Certificate caCert;
        try (InputStream is = new BufferedInputStream(new FileInputStream(caCrtPath))) {
            caCert = (X509Certificate) cf.generateCertificate(is);
        }

        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        trustStore.load(null, null);
        trustStore.setCertificateEntry("ca-root", caCert);

        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(trustStore);

        // 2. Carichiamo il certificato pubblico del Client Cloud
        X509Certificate clientCert;
        try (InputStream is = new BufferedInputStream(new FileInputStream(clientCrtPath))) {
            clientCert = (X509Certificate) cf.generateCertificate(is);
        }

        // 3. Carichiamo la chiave privata del Client (formato PKCS#8)
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

        // 4. KeyStore con identità del Cloud (certificato + chiave)
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null, null);
        keyStore.setCertificateEntry("client-cert", clientCert);
        keyStore.setKeyEntry("client-key", privateKey, "password".toCharArray(),
                new java.security.cert.Certificate[]{clientCert});

        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, "password".toCharArray());

        // 5. Contesto SSL TLS 1.2
        SSLContext context = SSLContext.getInstance("TLSv1.2");
        context.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

        return context.getSocketFactory();
    }

    /**
     * Applica la configurazione mTLS alle opzioni di connessione MQTT.
     * Lancia un'eccezione a runtime se i certificati non sono disponibili,
     * per evitare di procedere con una connessione non sicura.
     *
     * @param options      Le opzioni da configurare
     * @param baseCertsPath Percorso base della directory dei certificati
     * @throws RuntimeException se la configurazione TLS fallisce
     */
    public static void applyTlsToOptions(MqttConnectOptions options, String baseCertsPath) {
        try {
            String caPath  = baseCertsPath + "/ca.crt";
            String crtPath = baseCertsPath + "/client.crt";
            String keyPath = baseCertsPath + "/client.key";

            options.setSocketFactory(getSocketFactory(caPath, crtPath, keyPath));
            // Autenticazione via certificato: no username/password testuali
            options.setUserName(null);
            options.setPassword(null);

            System.out.println("[CLOUD-TLS] SSLSocketFactory configurata con successo.");
        } catch (Exception e) {
            // Fix #3: Rilanciamo l'eccezione invece di inghiottirla silenziosamente.
            // Senza questo, il Gateway procederebbe senza TLS e fallirebbe sul connect().
            throw new RuntimeException("[CLOUD-FATAL] Impossibile configurare il layer TLS. " +
                    "Verificare i certificati in: " + baseCertsPath, e);
        }
    }
}
