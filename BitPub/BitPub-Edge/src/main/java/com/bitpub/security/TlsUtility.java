package com.bitpub.security;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;

import javax.net.ssl.*;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

public class TlsUtility {

    private static class MtlsSSLSocketFactory extends SSLSocketFactory {

        private final SSLSocketFactory delegate;

        public MtlsSSLSocketFactory(String certsPath) throws Exception {

            CertificateFactory cf = CertificateFactory.getInstance("X.509");

            X509Certificate caCert;
            try (InputStream caIs = Files.newInputStream(Paths.get(certsPath, "ca.crt"))) {
                caCert = (X509Certificate) cf.generateCertificate(caIs);
            }

            KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            trustStore.load(null, null);
            trustStore.setCertificateEntry("ca", caCert);

            TrustManagerFactory tmf = TrustManagerFactory.getInstance(
                    TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(trustStore);

            X509Certificate clientCert;
            try (InputStream cliIs = Files.newInputStream(Paths.get(certsPath, "client.crt"))) {
                clientCert = (X509Certificate) cf.generateCertificate(cliIs);
            }

            PrivateKey privateKey = loadPkcs8Key(Paths.get(certsPath, "client_pkcs8.key"));

            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null, null);
            keyStore.setKeyEntry("client", privateKey, new char[0],
                    new Certificate[]{clientCert});

            KeyManagerFactory kmf = KeyManagerFactory.getInstance(
                    KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(keyStore, new char[0]);

            SSLContext ctx = SSLContext.getInstance("TLSv1.2");
            ctx.init(kmf.getKeyManagers(), tmf.getTrustManagers(), new SecureRandom());
            this.delegate = ctx.getSocketFactory();
        }

        private static PrivateKey loadPkcs8Key(Path keyPath) throws Exception {
            String pem = Files.readString(keyPath);
            String b64 = pem
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s+", "");
            byte[] der = Base64.getDecoder().decode(b64);
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(der);
            try {
                return KeyFactory.getInstance("RSA").generatePrivate(spec);
            } catch (InvalidKeySpecException e) {
                return KeyFactory.getInstance("EC").generatePrivate(spec);
            }
        }

        @Override public String[] getDefaultCipherSuites()  { return delegate.getDefaultCipherSuites(); }
        @Override public String[] getSupportedCipherSuites() { return delegate.getSupportedCipherSuites(); }

        @Override
        public Socket createSocket() throws IOException { return delegate.createSocket(); }
        @Override
        public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException {
            return delegate.createSocket(s, host, port, autoClose);
        }
        @Override
        public Socket createSocket(String host, int port) throws IOException {
            return delegate.createSocket(host, port);
        }
        @Override
        public Socket createSocket(String host, int port, InetAddress local, int localPort) throws IOException {
            return delegate.createSocket(host, port, local, localPort);
        }
        @Override
        public Socket createSocket(InetAddress host, int port) throws IOException {
            return delegate.createSocket(host, port);
        }
        @Override
        public Socket createSocket(InetAddress addr, int port, InetAddress local, int localPort) throws IOException {
            return delegate.createSocket(addr, port, local, localPort);
        }
    }

    public static void applyTlsToOptions(MqttConnectOptions options,
                                          String baseCertsPath) throws Exception {
        options.setSocketFactory(new MtlsSSLSocketFactory(baseCertsPath));
        // Se il broker è raggiunto via IP (non hostname), decommentare:
        // options.setHttpsHostnameVerificationEnabled(false);
    }
}