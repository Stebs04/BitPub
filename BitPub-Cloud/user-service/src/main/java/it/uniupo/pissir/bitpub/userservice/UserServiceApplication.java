package it.uniupo.pissir.bitpub.userservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import it.uniupo.pissir.bitpub.common.security.PasswordUtils;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import(PasswordUtils.class)
public class UserServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
