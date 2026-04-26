package com.bitpub.controllers;

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Group;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.shape.Circle;

/**
 * Controller avanzato per l'interfaccia grafica del Calciobalilla.
 * Gestisce l'aggiornamento visivo, il motore fisico di gioco, le collisioni
 * e l'interazione diretta dell'utente.
 *
 * @author Stefano Bellan 20054330
 */
public class CalciobalillaUIController {

    @FXML private Label punteggioRosso;
    @FXML private Label punteggioBlu;
    @FXML private Circle pallina;
    @FXML private Label messaggioCentrale;
    @FXML private Button btnAvviaPausa;
    @FXML private Group squadraRossa;
    @FXML private Group squadraBlu;

    private int scoreRosso = 0;
    private int scoreBlu = 0;
    private final int MAX_GOL = 10;

    private double velocityX = 15.0;
    private double velocityY = 8.0;
    private boolean isPlaying = false;
    private AnimationTimer gameLoop;

    private double angoloAste = 0;

    /**
     * Metodo di inizializzazione automatico di JavaFX.
     * Prepara il motore fisico in background, in attesa del comando di avvio dell'utente.
     */
    @FXML
    public void initialize() {
        creaMotoreFisico();
    }

    /**
     * Avvia o mette in pausa l'animazione di gioco, modificando visivamente il bottone
     * per restituire un feedback immediato all'utente.
     */
    @FXML
    public void togglePartita() {
        // Se si preme "Avvia" al termine di un match, riavvia la partita da zero
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
     * Resetta completamente i punteggi e la posizione fisica degli elementi visivi
     * per preparare il campo a una nuova sfida.
     */
    @FXML
    public void resetMatch() {
        if (gameLoop != null) gameLoop.stop();
        isPlaying = false;
        scoreRosso = 0;
        scoreBlu = 0;
        aggiornaTestoPunteggi();

        // Riposizionamento nodi al centro del campo
        pallina.setTranslateX(0);
        pallina.setTranslateY(0);
        squadraRossa.setTranslateY(0);
        squadraBlu.setTranslateY(0);

        messaggioCentrale.setVisible(false);
        btnAvviaPausa.setText("▶ AVVIA PARTITA");
        btnAvviaPausa.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-font-size: 20px; -fx-font-weight: bold; -fx-padding: 15 40; -fx-background-radius: 15;");
    }

    /**
     * Costruisce il ciclo di aggiornamento grafico ad alta frequenza (60 FPS)
     * che si occupa del movimento della pallina e delle aste.
     */
    private void creaMotoreFisico() {
        gameLoop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                // Calcolo dello scorrimento dinamico delle aste (effetto simulazione umana)
                angoloAste += 0.05;
                squadraRossa.setTranslateY(Math.sin(angoloAste) * 30);
                squadraBlu.setTranslateY(Math.cos(angoloAste * 0.8) * 30);

                double nextX = pallina.getTranslateX() + velocityX;
                double nextY = pallina.getTranslateY() + velocityY;

                double limiteX = 670;
                double limiteY = 330;

                // Gestione dei rimbalzi sui bordi verticali o eventuale ingresso in porta
                if (nextX > limiteX) {
                    if (nextY > -100 && nextY < 100) {
                        gestisciGol("ROSSO");
                        return;
                    } else {
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

                // Gestione rimbalzo sponde orizzontali lunghe
                if (nextY > limiteY || nextY < -limiteY) {
                    velocityY *= -1;
                    velocityX += (Math.random() - 0.5) * 5;
                }

                // Normalizzazione della velocità per non sforare l'area
                velocityX = Math.max(-25, Math.min(25, velocityX));
                velocityY = Math.max(-25, Math.min(25, velocityY));

                pallina.setTranslateX(nextX);
                pallina.setTranslateY(nextY);
            }
        };
    }

    /**
     * Interrompe l'azione di gioco e aggiorna il tabellone quando avviene una marcatura.
     * Gestisce anche l'avvio della logica di fine partita se si raggiunge il tetto massimo.
     *
     * @param squadra Identificativo della squadra che ha effettuato la marcatura ("ROSSO" o "BLU").
     */
    private void gestisciGol(String squadra) {
        gameLoop.stop();

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

        if (scoreRosso >= MAX_GOL || scoreBlu >= MAX_GOL) {
            finalizzarePartita();
            return;
        }

        // Timer di cooldown, il ripristino UI viene demandato in modo sicuro al Thread di FX
        new Thread(() -> {
            try { Thread.sleep(1500); } catch (InterruptedException e) { e.printStackTrace(); }

            Platform.runLater(() -> {
                messaggioCentrale.setVisible(false);
                pallina.setTranslateX(0);
                pallina.setTranslateY(0);

                // Servizio assegnato a chi ha subito il gol
                velocityX = squadra.equals("ROSSO") ? -12.0 : 12.0;
                velocityY = (Math.random() * 10) - 5;
                if (isPlaying) gameLoop.start();
            });
        }).start();
    }

    /**
     * Calcola e visualizza i complimenti finali fermando in modo definitivo i calcoli del match.
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
     * Sincronizza i testi dei punteggi a schermo.
     */
    private void aggiornaTestoPunteggi() {
        punteggioRosso.setText(String.valueOf(scoreRosso));
        punteggioBlu.setText(String.valueOf(scoreBlu));
    }
}