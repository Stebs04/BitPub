package com.bitpub.controllers;

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Group;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.shape.Circle;

/**
 * Controller avanzato per la simulazione del Calciobalilla.
 * Gestisce il motore fisico, le collisioni con le porte, il movimento oscillatorio
 * delle aste e il ciclo di vita della partita (Play/Pausa/Vittoria).
 *
 * @author Stefano Bellan 20054330
 */
public class CalciobalillaUIController {

    // --- COMPONENTI UI (Iniezione FXML) ---
    @FXML private Label punteggioRosso;
    @FXML private Label punteggioBlu;
    @FXML private Circle pallina;
    @FXML private Label messaggioCentrale;
    @FXML private Button btnAvviaPausa;

    /** Gruppo contenente i nodi grafici delle aste e dei giocatori Rossi */
    @FXML private Group squadraRossa;

    /** Gruppo contenente i nodi grafici delle aste e dei giocatori Blu */
    @FXML private Group squadraBlu;

    // --- COSTANTI E STATO DEL GIOCO ---
    private int scoreRosso = 0;
    private int scoreBlu = 0;
    private final int MAX_GOL = 10;

    // --- FISICA E ANIMAZIONE ---
    private double velocityX = 15.0;
    private double velocityY = 8.0;
    private boolean isPlaying = false;
    private AnimationTimer gameLoop;

    /** Variabile di accumulo per calcolare il moto armonico delle aste */
    private double angoloAste = 0;

    /**
     * Inizializza il controller. Configura il motore fisico ma non avvia l'animazione
     * finché l'utente non interagisce con la UI.
     */
    @FXML
    public void initialize() {
        creaMotoreFisico();
    }

    /**
     * Gestisce l'alternanza tra stato di riproduzione e pausa.
     * Aggiorna dinamicamente lo stile CSS del pulsante di controllo.
     */
    @FXML
    public void togglePartita() {
        // Se la partita è terminata, il tasto agisce come reset per un nuovo match
        if (scoreRosso >= MAX_GOL || scoreBlu >= MAX_GOL) {
            resetMatch();
        }

        isPlaying = !isPlaying;
        if (isPlaying) {
            btnAvviaPausa.setText("⏸ PAUSA");
            btnAvviaPausa.setStyle("-fx-background-color: #f39c12; -fx-text-fill: white; -fx-font-size: 20px; -fx-font-weight: bold; -fx-padding: 15 40; -fx-background-radius: 15;");
            messaggioCentrale.setVisible(false);
            gameLoop.start();
        } else {
            btnAvviaPausa.setText("▶ RIPRENDI");
            btnAvviaPausa.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-font-size: 20px; -fx-font-weight: bold; -fx-padding: 15 40; -fx-background-radius: 15;");
            gameLoop.stop();
        }
    }

    /**
     * Ripristina i parametri di gioco allo stato iniziale.
     * Ferma il loop fisico e resetta le posizioni grafiche dei componenti.
     */
    @FXML
    public void resetMatch() {
        if (gameLoop != null) gameLoop.stop();
        isPlaying = false;
        scoreRosso = 0;
        scoreBlu = 0;
        aggiornaTestoPunteggi();

        // Reset posizionale dei nodi grafici
        pallina.setTranslateX(0);
        pallina.setTranslateY(0);
        squadraRossa.setTranslateY(0);
        squadraBlu.setTranslateY(0);

        messaggioCentrale.setVisible(false);
        btnAvviaPausa.setText("▶ AVVIA PARTITA");
        btnAvviaPausa.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-font-size: 20px; -fx-font-weight: bold; -fx-padding: 15 40; -fx-background-radius: 15;");
    }

    /**
     * Definisce il ciclo di calcolo della fisica ad alta frequenza.
     * Include il calcolo del moto armonico per le aste e la rilevazione delle collisioni.
     */
    private void creaMotoreFisico() {
        gameLoop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                // 1. ANIMAZIONE ASTE: Moto sinusoidale per simulare il movimento dei giocatori
                angoloAste += 0.05;
                squadraRossa.setTranslateY(Math.sin(angoloAste) * 30);
                squadraBlu.setTranslateY(Math.cos(angoloAste * 0.8) * 30);

                // 2. CALCOLO TRAIETTORIA PALLINA
                double nextX = pallina.getTranslateX() + velocityX;
                double nextY = pallina.getTranslateY() + velocityY;

                // Definizione confini campo (1400x700)
                double limiteX = 670;
                double limiteY = 330;

                // 3. RILEVAMENTO GOL E COLLISIONI VERTICALI (Bordi corti)
                if (nextX > limiteX) {
                    // Controllo se la coordinata Y è all'interno del raggio della porta
                    if (nextY > -100 && nextY < 100) {
                        gestisciGol("ROSSO");
                        return;
                    } else {
                        // Rimbalzo su muro di fondo
                        velocityX *= -1;
                        velocityY += (Math.random() - 0.5) * 5;
                    }
                } else if (nextX < -limiteX) {
                    if (nextY > -100 && nextY < 100) {
                        gestisciGol("BLU");
                        return;
                    } else {
                        velocityX *= -1;
                        velocityY += (Math.random() - 0.5) * 5;
                    }
                }

                // 4. RIMBALZO SPONDE ORIZZONTALI (Bordi lunghi)
                if (nextY > limiteY || nextY < -limiteY) {
                    velocityY *= -1;
                    velocityX += (Math.random() - 0.5) * 5;
                }

                // 5. CLAMPING VELOCITÀ E AGGIORNAMENTO GRAFICO
                velocityX = Math.max(-25, Math.min(25, velocityX));
                velocityY = Math.max(-25, Math.min(25, velocityY));

                pallina.setTranslateX(nextX);
                pallina.setTranslateY(nextY);
            }
        };
    }

    /**
     * Gestisce l'evento di segnatura, aggiorna il punteggio e verifica le condizioni di vittoria.
     *
     * @param squadra Il nome della squadra che ha segnato ("ROSSO" o "BLU").
     */
    private void gestisciGol(String squadra) {
        gameLoop.stop(); // Interruzione momentanea per enfasi visiva sul gol

        if (squadra.equals("ROSSO")) {
            scoreRosso++;
            messaggioCentrale.setText("⚽ GOL ROSSO! ⚽");
            messaggioCentrale.setStyle("-fx-text-fill: #e74c3c; -fx-background-color: rgba(0,0,0,0.8); -fx-padding: 20 50; -fx-background-radius: 20; -fx-font-size: 80px; -fx-font-weight: bold;");
        } else {
            scoreBlu++;
            messaggioCentrale.setText("⚽ GOL BLU! ⚽");
            messaggioCentrale.setStyle("-fx-text-fill: #3498db; -fx-background-color: rgba(0,0,0,0.8); -fx-padding: 20 50; -fx-background-radius: 20; -fx-font-size: 80px; -fx-font-weight: bold;");
        }

        aggiornaTestoPunteggi();
        messaggioCentrale.setVisible(true);

        // CONTROLLO END-GAME
        if (scoreRosso >= MAX_GOL || scoreBlu >= MAX_GOL) {
            finalizzarePartita();
            return;
        }

        // LOGICA DI RIPRESA: Timer asincrono per il cooldown dopo il gol
        new Thread(() -> {
            try { Thread.sleep(1500); } catch (InterruptedException e) { e.printStackTrace(); }

            Platform.runLater(() -> {
                messaggioCentrale.setVisible(false);
                pallina.setTranslateX(0);
                pallina.setTranslateY(0);
                // Direziona la palla verso chi ha subito il gol
                velocityX = squadra.equals("ROSSO") ? -12.0 : 12.0;
                velocityY = (Math.random() * 10) - 5;
                if (isPlaying) gameLoop.start();
            });
        }).start();
    }

    /**
     * Imposta lo stato della UI per la fine della partita e proclama il vincitore.
     */
    private void finalizzarePartita() {
        String vincitore = (scoreRosso >= MAX_GOL) ? "ROSSA" : "BLU";
        String coloreHex = (scoreRosso >= MAX_GOL) ? "#e74c3c" : "#3498db";

        messaggioCentrale.setText("🏆 VITTORIA SQUADRA " + vincitore + " 🏆");
        messaggioCentrale.setStyle("-fx-text-fill: " + coloreHex + "; -fx-background-color: rgba(0,0,0,0.9); -fx-padding: 40 80; -fx-background-radius: 20; -fx-font-size: 70px; -fx-font-weight: bold;");

        isPlaying = false;
        btnAvviaPausa.setText("🔄 NUOVA PARTITA");
        btnAvviaPausa.setStyle("-fx-background-color: #9b59b6; -fx-text-fill: white; -fx-font-size: 20px; -fx-font-weight: bold; -fx-padding: 15 40; -fx-background-radius: 15;");
    }

    /**
     * Sincronizza i testi delle Label con i valori numerici degli score.
     */
    private void aggiornaTestoPunteggi() {
        punteggioRosso.setText(String.valueOf(scoreRosso));
        punteggioBlu.setText(String.valueOf(scoreBlu));
    }
}
