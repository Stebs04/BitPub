-- Script di popolamento iniziale Admin
-- Garantisce l'idempotenza: inserisce l'utente solo se non esiste già un ruolo ADMIN
INSERT INTO utenti (username, role, nome, cognome, email, password, attivo, credito, anni)
SELECT 'admin', 'ADMIN', 'Amministratore', 'Di Sistema', 'admin@bitpub.com', '$2a$10$oKqW09Vf6CijtiOPbMxshuJ/fPN1rOCskhXwqzamrzV.eHJ8PMDqK', true, 0.0, 99
    WHERE NOT EXISTS (
    SELECT 1 FROM utenti WHERE role = 'ADMIN'
);