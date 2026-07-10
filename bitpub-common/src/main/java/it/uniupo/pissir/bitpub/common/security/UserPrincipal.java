/**
 * Autore: Luca Franzon 20054744
 *
 * Rappresentazione dell'utente autenticato all'interno del sistema.
 * Contiene le informazioni chiave necessarie per le autorizzazioni.
 */
package it.uniupo.pissir.bitpub.common.security;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.security.Principal;

// Sfruttiamo Lombok per evitare la stesura manuale di costruttori e metodi getter
@Getter
@AllArgsConstructor
public class UserPrincipal implements Principal {
    private final String id;
    private final String username;
    private final String role;
    private final String localeId;

    // Implementazione del metodo dell'interfaccia Principal per restituire l'identificativo principale
    @Override
    public String getName() {
        return username;
    }
}
