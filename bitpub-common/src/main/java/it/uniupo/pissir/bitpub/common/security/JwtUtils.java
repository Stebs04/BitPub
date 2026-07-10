/**
 * Autore: Luca Franzon 20054744
 *
 * Classe di utilità per la gestione dei JSON Web Token (JWT).
 * Offre metodi per la generazione, la validazione e l'estrazione dei dati dai token.
 */
package it.uniupo.pissir.bitpub.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.function.Function;

@Component
public class JwtUtils {

    // Segreto utilizzato per la firma dei token, configurabile tramite properties
    @Value("${bitpub.jwt.secret:defaultSecretKeyWhichShouldBeVeryLongAndSecureEnoughForHS256AlgorithmToWorkProperly}")
    private String jwtSecret;

    // Tempo di validità del token, preimpostato a un giorno in assenza di configurazione
    @Value("${bitpub.jwt.expirationMs:86400000}")
    private int jwtExpirationMs;

    // Generazione della chiave segreta a partire dalla stringa configurata
    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // Generazione di un token standard senza l'identificativo del locale
    public String generateToken(String username, String role, String userId) {
        return generateToken(username, role, userId, null);
    }

    // Generazione completa di un token inserendo tutte le informazioni necessarie nel payload
    public String generateToken(String username, String role, String userId, String localeId) {
        return Jwts.builder()
                .setSubject(username)
                .claim("role", role)
                .claim("userId", userId)
                .claim("localeId", localeId)
                .setIssuedAt(new Date())
                .setExpiration(new Date((new Date()).getTime() + jwtExpirationMs))
                .signWith(getSigningKey())
                .compact();
    }

    // Metodi per l'estrazione specifica dei singoli campi contenuti nel token
    
    public String getUsernameFromToken(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public String getRoleFromToken(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class));
    }

    public String getUserIdFromToken(String token) {
        return extractClaim(token, claims -> claims.get("userId", String.class));
    }

    public String getLocaleIdFromToken(String token) {
        return extractClaim(token, claims -> claims.get("localeId", String.class));
    }

    public Date getExpirationDateFromToken(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    // Metodo generico per applicare una funzione di estrazione sui claim del token
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    // Parsing e validazione della firma per recuperare tutto il corpo del token
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // Verifica temporale per capire se il token ha superato la sua data di scadenza
    private Boolean isTokenExpired(String token) {
        final Date expiration = getExpirationDateFromToken(token);
        return expiration.before(new Date());
    }

    // Controllo generale sulla validità del token assicurandosi che non sia scaduto o malformato
    public Boolean validateToken(String token) {
        try {
            return !isTokenExpired(token);
        } catch (Exception e) {
            // Qualsiasi anomalia durante il parsing rende il token non valido
            return false;
        }
    }
}
