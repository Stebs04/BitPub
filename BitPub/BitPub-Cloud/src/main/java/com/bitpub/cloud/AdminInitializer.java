package com.bitpub.cloud;

import com.bitpub.models.Utente;
import com.bitpub.repository.UtenteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Inizializzatore eseguito all'avvio dell'applicazione.
 * Si occupa di creare l'utente ADMIN di default se non esiste nel database.
 *
 * @author BitPub Team
 * @version 1.0
 */
@Component
public class AdminInitializer implements ApplicationRunner {

    @Autowired
    private UtenteRepository utenteRepository;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        // Verifica se esiste già un utente con ruolo ADMIN
        if (utenteRepository.findByRuolo("ADMIN").isEmpty()) {
            System.out.println("Nessun ADMIN trovato. Creazione dell'ADMIN di default...");
            
            // Password comunicata: BitPub@Admin2024!
            Utente admin = new Utente(
                    "admin", 
                    "ADMIN", 
                    "Amministratore", 
                    "Di Sistema", 
                    "admin@bitpub.com", 
                    "BitPub@Admin2024!"
            );
            
            utenteRepository.save(admin);
            System.out.println("Utente ADMIN di default creato con successo: admin");
        } else {
            System.out.println("Utente ADMIN già presente nel database.");
        }
    }
}
