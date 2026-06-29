package com.bitpub.edge;

import javax.net.ssl.*;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

/**
 * Factory statica per la costruzione del contesto crittografico mTLS.
 * Carica i certificati X.509 e la chiave privata PKCS#8 dal filesystem locale
 * e li assembla in un SSLContext pronto per essere iniettato nel socket Paho.
 */
public class SslContextFactory {

    /**
     * Costruisce un SSLContext con autenticazione mutua (mTLS).
     *
     * @param caCrtPath     Percorso assoluto del certificato della CA (ca.crt)
     * @param clientCrtPath Percorso assoluto del certificato client (client.crt)
     * @param clientKeyPath Percorso assoluto della chiave privata PKCS#8 (client_pkcs8.key)
     * @return SSLContext configurato con TrustManager e KeyManager
     * @throws Exception Se un file è assente, corrotto o il formato non è valido
     */
    public static SSLContext build(String caCrtPath, String clientCrtPath, String clientKeyPath) throws Exception {

        // --- TrustManager: verifica il certificato del broker tramite la CA ---
        CertificateFactory cf = CertificateFactory.getInstance("X.509");

        X509Certificate caCert;
        try (FileInputStream caIn = new FileInputStream(caCrtPath)) {
            caCert = (X509Certificate) cf.generateCertificate(caIn);
        }

        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        trustStore.load(null, null);
        trustStore.setCertificateEntry("ca", caCert);

        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(trustStore);

        // --- KeyManager: presenta il certificato client al broker ---
        X509Certificate clientCert;
        try (FileInputStream clientCertIn = new FileInputStream(clientCrtPath)) {
            clientCert = (X509Certificate) cf.generateCertificate(clientCertIn);
        }

        // Lettura della chiave privata PKCS#8 in formato DER (binario)
        byte[] keyBytes;
        try (FileInputStream keyIn = new FileInputStream(clientKeyPath)) {
            String keyPem = new String(keyIn.readAllBytes())
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");
            keyBytes = java.util.Base64.getDecoder().decode(keyPem);
        }

        java.security.spec.PKCS8EncodedKeySpec keySpec = new java.security.spec.PKCS8EncodedKeySpec(keyBytes);
        java.security.PrivateKey privateKey = java.security.KeyFactory.getInstance("RSA").generatePrivate(keySpec);

        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null, null);
        keyStore.setCertificateEntry("client-cert", clientCert);
        keyStore.setKeyEntry("client-key", privateKey, new char[0], new java.security.cert.Certificate[]{clientCert});

        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, new char[0]);

        // --- Assemblaggio finale del contesto TLS 1.2/1.3 ---
        SSLContext sslContext = SSLContext.getInstance("TLSv1.3");
        sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

        return sslContext;
    }
}