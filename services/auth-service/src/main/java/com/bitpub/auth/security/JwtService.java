package com.bitpub.auth.security;

import com.bitpub.auth.model.User;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final TokenProvider tokenProvider;

    public JwtService(TokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    public String generateTokenForUser(User user, String traceId) {
        return tokenProvider.generateToken(user, traceId);
    }
}
