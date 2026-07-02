package it.uniupo.pissir.bitpub.common.security;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.security.Principal;

@Getter
@AllArgsConstructor
public class UserPrincipal implements Principal {
    private final String id;
    private final String username;
    private final String role;
    private final String localeId;

    @Override
    public String getName() {
        return username;
    }
}
