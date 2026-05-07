-- Script di popolamento iniziale Admin
-- Garantisce l'idempotenza: inserisce l'utente solo se non esiste già un ruolo ADMIN
INSERT INTO utenti (username, role, nome, cognome, email, password)
SELECT 'admin', 'ADMIN', 'Amministratore', 'Di Sistema', 'admin@bitpub.com', 'BitPub@Admin2024!'
    WHERE NOT EXISTS (
    SELECT 1 FROM utenti WHERE role = 'ADMIN'
);