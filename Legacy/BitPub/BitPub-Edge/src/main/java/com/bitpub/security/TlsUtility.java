package com.bitpub.security;

import javax.net.ssl.*;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controller architetturale crittografico preposto al consolidamento dell'identità
 * dell'Edge Node. La classe progetta e finalizza il container TLS (Transport Layer Security)
 * implementando i requisiti di Mutual Authentication (mTLS), obbligatori per sventare
 * attacchi di Spoofing o Man In The Middle (MITM) sulla rete IoT.
 * Rifiuta l'hardcoding delle credenziali privilegiando l'estrazione delle stesse
 * direttamente dal layer delle variabili d'ambiente (Twelve-Factor App methodology).
 */
public class TlsUtility {

    // Logback integration per l'auditing dei processi di bootstrapping crittografico
    private static final Logger logger = LoggerFactory.getLogger(TlsUtility.class);

    // Mappatura immutabile delle chiavi d'ambiente che definiscono i vettori d'identità
    private static final String ENV_CA_CERT      = "BITPUB_CA_CERT_PATH";
    private static final String ENV_CLIENT_CERT  = "BITPUB_CLIENT_CERT_PATH";
    private static final String ENV_CLIENT_KEY   = "BITPUB_CLIENT_KEY_PATH";
    private static final String ENV_KEY_PASSWORD = "BITPUB_KEY_PASSWORD";

    /**
     * Motore di derivazione per l'impalcatura sicura dei socket di rete.
     * Consolida il perimetro di trust incorporando il certificato dell'autorità radice (CA)
     * all'interno del TrustManager e confezionando contestualmente il passaporto client
     * (chiave privata e certificato pubblico) all'interno del KeyManager.
     *
     * @return Una factory SSLContext ancorata univocamente al protocollo TLSv1.2
     * @throws Exception Blocca il bootstrapping se l'ambiente di deployment è mal configurato o i certificati risultano corrotti
     */
    public static SSLContext createSSLContext() throws Exception {
        logger.info("[SECURITY] Inizializzazione SSLContext blindato (mTLS)...");

        // Lettura stringente dello stack d'ambiente. La mancanza di un solo parametro
        // inficia la validità del demone che non verrebbe avviato (Fail-fast principle).
        String caPath = getEnvOrThrow(ENV_CA_CERT);
        String clientCertPath = getEnvOrThrow(ENV_CLIENT_CERT);
        String clientKeyPath = getEnvOrThrow(ENV_CLIENT_KEY);

        // Passphrase di protezione della chiave asimmetrica (default stringa vuota se assente)
        char[] password = System.getenv(ENV_KEY_PASSWORD) != null ?
                System.getenv(ENV_KEY_PASSWORD).toCharArray() : new char[0];

        // Allocazione dell'interprete per i certificati X.509
        CertificateFactory cf = CertificateFactory.getInstance("X.509");

        // FASE 1: TRUST MANAGER (Valutazione dell'identità del server Cloud)
        X509Certificate caCert;
        try (InputStream caIs = Files.newInputStream(Paths.get(caPath))) {
            caCert = (X509Certificate) cf.generateCertificate(caIs);
        }

        // Costruzione del TrustStore in memoria volatile (RAM) senza dipendere da file JKS precompilati
        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        trustStore.load(null, null);
        trustStore.setCertificateEntry("ca-root", caCert);

        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(trustStore);

        // FASE 2: KEY MANAGER (Dimostrazione della propria identità al server Cloud)
        X509Certificate clientCert;
        try (InputStream cliIs = Files.newInputStream(Paths.get(clientCertPath))) {
            clientCert = (X509Certificate) cf.generateCertificate(cliIs);
        }

        PrivateKey privateKey = loadPrivateKey(clientKeyPath);

        // Assemblaggio della catena client all'interno del KeyStore in memoria
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null, null);
        keyStore.setKeyEntry("edge-client", privateKey, password, new java.security.cert.Certificate[]{clientCert});

        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, password);

        // FASE 3: CONSOLIDAMENTO E FORZATURA DEL PROTOCOLLO
        // Inibizione preventiva di stack obsoleti (SSLv3, TLS 1.0, TLS 1.1) fissando il contesto su TLS 1.2
        SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
        sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), new SecureRandom());

        logger.info("[SECURITY] SSLContext creato con successo (TLSv1.2 attivo).");
        return sslContext;
    }

    /**
     * Algoritmo di parsing e demuxing per file PEM formattati secondo gli standard PKCS#8.
     * Ripulisce il tracciato dai banner ASCII e reidrata la struttura binaria DER.
     *
     * @param path Percorso fisico del file contenente la chiave privata
     * @return L'istanza PrivateKey caricata e pronta per l'handshaking
     * @throws Exception Fallimento del parsing RSA o curva ellittica (EC)
     */
    private static PrivateKey loadPrivateKey(String path) throws Exception {
        String pem = Files.readString(Paths.get(path));

        // Sanitizzazione massiva della codifica Base64 eliminando intestazioni e spaziature di riga
        String encodedKey = pem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s+", "");

        byte[] der = Base64.getDecoder().decode(encodedKey);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(der);

        try {
            // Tentativo primario di risoluzione tramite modulo RSA
            return KeyFactory.getInstance("RSA").generatePrivate(spec);
        } catch (Exception e) {
            // Fallback implicito per il supporto alla crittografia a Curve Ellittiche (EC)
            return KeyFactory.getInstance("EC").generatePrivate(spec);
        }
    }

    /**
     * Procedura di estrazione ed enforcement della presenza per i parametri d'ambiente.
     *
     * @param name L'etichetta associata alla variabile OS
     * @return La stringa di sistema
     * @throws RuntimeException Eccezione fatale che intercetta la carenza di configurazione
     */
    private static String getEnvOrThrow(String name) {
        String value = System.getenv(name);
        if (value == null || value.isEmpty()) {
            throw new RuntimeException("Variabile d'ambiente di sicurezza mancante: " + name);
        }
        return value;
    }
}