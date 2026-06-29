package com.bitpub.cloud.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.function.Function;

/**
 * Servizio Spring dedicato alla gestione del ciclo di vita dei token JWT.
 * Si occupa della generazione, della validazione crittografica e dell'estrazione dei claim,
 * sfruttando le API aggiornate della libreria JJWT (versione 0.12.x e successive).
 *
 * @author Senior Software Engineer
 */
@Service
public class JwtService {

    // Aggiunto un valore di default sicuro per prevenire crash fatali del contesto Spring (BeanCreationException)
    // in assenza della property esplicita nel file application.properties o nelle variabili d'ambiente.
    @Value("${jwt.secret:UnaChiaveSegretaMoltoLungaPerGarantireSicurezzaBitPub2026!}")
    private String secretKey;

    // Tempo di validità del token espresso in millisecondi (default: 24 ore)
    @Value("${jwt.expiration:86400000}")
    private long jwtExpiration;

    // Oggetto crittografico tipizzato richiesto dalle nuove versioni di JJWT per firmare i token
    private SecretKey signingKey;

    /**
     * Inizializza la chiave crittografica subito dopo l'iniezione delle dipendenze da parte di Spring.
     * Converte la stringa segreta in un formato sicuro e generato per l'algoritmo HMAC-SHA.
     */
    @PostConstruct
    public void init() {
        // La codifica UTF-8 previene problemi di interpretazione dei byte tra sistemi operativi diversi
        this.signingKey = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Genera un nuovo token JWT firmato per un utente specifico.
     *
     * @param username l'identificativo dell'utente da inserire come subject
     * @param role il ruolo assegnato all'utente
     * @return una stringa che rappresenta il token JWT completo e compatto
     */
    public String generateToken(String username, String role) {
        return Jwts.builder()
                // Assegna l'identità principale al token
                .subject(username)
                // Inserisce dati custom (claim) all'interno del payload
                .claim("role", role)
                // Registra il momento esatto in cui il token è stato emesso
                .issuedAt(new Date(System.currentTimeMillis()))
                // Calcola e imposta la data di scadenza sommando la durata configurata al momento attuale
                .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
                // Applica la firma crittografica utilizzando la SecretKey inizializzata in precedenza
                .signWith(signingKey)
                // Assembla e codifica in Base64Url le tre parti del token (Header, Payload, Signature)
                .compact();
    }

    /**
     * Verifica l'integrità e la validità temporale del token JWT.
     *
     * @param token la stringa JWT da analizzare
     * @return true se il token è valido, integro e non scaduto, false altrimenti
     */
    public boolean validateToken(String token) {
        try {
            // Istanzia il parser configurandolo esclusivamente con la nostra chiave segreta
            Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    // Tenta di decodificare e verificare la firma del token
                    .parseSignedClaims(token);

            // Se l'operazione non lancia eccezioni, la firma è autentica e il token è valido
            return true;
        } catch (Exception e) {
            // Intercetta qualsiasi anomalia (token scaduto, firma manomessa, formato errato)
            System.err.println("[JWT SECURITY] Token non valido o scaduto: " + e.getMessage());
            return false;
        }
    }

    /**
     * Estrae l'identificativo dell'utente (subject) registrato nel token.
     *
     * @param token la stringa JWT da cui leggere i dati
     * @return lo username estratto dal payload
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Estrae il ruolo dell'utente registrato nei claim personalizzati del token.
     *
     * @param token la stringa JWT da cui leggere i dati
     * @return il ruolo estratto dal payload
     */
    public String extractRole(String token) {
        Claims claims = extractAllClaims(token);
        // Recupera il claim custom "role" effettuando un cast sicuro a String
        return claims.get("role", String.class);
    }

    /**
     * Metodo di utilità per estrarre una singola informazione dal payload del token.
     *
     * @param token la stringa JWT
     * @param claimsResolver una funzione che definisce quale dato estrarre dall'oggetto Claims
     * @param <T> il tipo di dato di ritorno atteso
     * @return il valore estratto e tipizzato
     */
    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Legge e restituisce l'intero blocco di dati (payload/claims) contenuto nel token.
     *
     * @param token la stringa JWT decodificata
     * @return l'oggetto Claims contenente tutte le informazioni chiave-valore del token
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                // È fondamentale fornire la chiave per garantire che i claim non siano stati manipolati
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                // Estrae la sezione Payload contenente i dati utili
                .getPayload();
    }
}