package com.bitpub.security;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import javax.net.ssl.*;
import java.security.cert.X509Certificate;

/**
 * Utility per la configurazione della sicurezza TLS nel progetto BitPub.
 */
public class TlsUtility {

    /**
     * Costruisce un SSLContext permissivo che accetta qualsiasi certificato server.
     * Necessario per connettersi a broker con certificati self-signed o CA privata.
     */
    public static SSLContext buildPermissiveSslContext() throws Exception {
        TrustManager[] trustAllCerts = new TrustManager[]{
            new X509ExtendedTrustManager() {
                @Override public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                @Override public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                @Override public void checkServerTrusted(X509Certificate[] certs, String authType) {}
                @Override public void checkClientTrusted(X509Certificate[] certs, String authType, java.net.Socket socket) {}
                @Override public void checkServerTrusted(X509Certificate[] certs, String authType, java.net.Socket socket) {}
                @Override public void checkClientTrusted(X509Certificate[] certs, String authType, SSLEngine engine) {}
                @Override public void checkServerTrusted(X509Certificate[] certs, String authType, SSLEngine engine) {}
            }
        };

        SSLContext context = SSLContext.getInstance("TLSv1.2");
        context.init(null, trustAllCerts, new java.security.SecureRandom());
        return context;
    }

    /**
     * Applica la configurazione TLS permissiva alle MqttConnectOptions.
     *
     * IMPORTANTE: Questo metodo deve essere chiamato PRIMA della costruzione
     * di MqttClient, perché Paho legge il SSLContext di default durante
     * l'istanziazione del client, non solo al momento della connect().
     *
     * @param options       Le opzioni di connessione MQTT da configurare.
     * @param baseCertsPath Percorso base dei certificati (attualmente non usato,
     *                      riservato per futura implementazione mTLS completo).
     */
    public static void applyTlsToOptions(MqttConnectOptions options, String baseCertsPath) throws Exception {
        // Costruisce il contesto SSL permissivo
        SSLContext context = buildPermissiveSslContext();

        // FIX BUG PAHO: Forza il contesto come default JVM PRIMA che MqttClient
        // venga istanziato. Senza questa riga, Paho ignora la SocketFactory impostata
        // su MqttConnectOptions e usa il TrustManager di default della JVM,
        // causando il PKIX path building failed.
        SSLContext.setDefault(context);

        // Imposta esplicitamente anche sulla options per doppia sicurezza
        options.setSocketFactory(context.getSocketFactory());
        options.setHttpsHostnameVerificationEnabled(false);
    }
}