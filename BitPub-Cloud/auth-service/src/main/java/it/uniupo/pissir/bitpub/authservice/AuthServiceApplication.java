package it.uniupo.pissir.bitpub.authservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import it.uniupo.pissir.bitpub.common.security.JwtUtils;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import(JwtUtils.class)
public class AuthServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}
