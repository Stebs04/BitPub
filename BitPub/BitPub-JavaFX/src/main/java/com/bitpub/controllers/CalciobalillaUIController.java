package com.bitpub.controllers;

import javafx.animation.AnimationTimer;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Group;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

/**
 * Controller per l'interfaccia grafica e la fisica di gioco del Calciobalilla.
 * Gestisce l'aggiornamento visivo a 60 FPS, il calcolo delle collisioni 
 * e le interazioni dell'utente (avvio, pausa, reset).
 *
 * @author Stefano Bellan
 * @version 1.0
 */
public class CalciobalillaUIController {

    // --- Costanti Fisiche e di Gioco ---
    // Estrarre questi valori in costanti facilita il tuning del gameplay senza cercare nel codice.
    private static final int MAX_GOL = 10;
    private static final double MAX_VELOCITY = 25.0;
    private static final double CAMPO_LIMITE_X = 670.0;
    private static final double CAMPO_LIMITE_Y = 330.0;
    private static final double GOL_LIMITE_Y = 100.0;
    private static final double INCREMENTO_ANGOLO_ASTE = 0.05;

    // --- Elementi dell'Interfaccia Grafica ---
    @FXML private Label punteggioRosso;
    @FXML private Label punteggioBlu;
    @FXML private Circle pallina;
    @FXML private Label messaggioCentrale;
    @FXML private Button btnAvviaPausa;
    @FXML private Group squadraRossa;
    @FXML private Group squadraBlu;

    // --- Variabili di Stato ---
    private int scoreRosso = 0;
    private int scoreBlu = 0;
    private double velocityX = 15.0;
    private double velocityY = 8.0;
    private boolean isPlaying = false;
    private double angoloAste = 0;
    private AnimationTimer gameLoop;

    /**
     * Metodo di inizializzazione richiamato automaticamente da JavaFX dopo il caricamento dell'FXML.
     * Prepara il motore fisico in background, evitando di avviare calcoli inutili prima del click dell'utente.
     */
    @FXML
    public void initialize() {
        creaMotoreFisico();
    }

    /**
     * Gestisce la transizione di stato tra Avvio e Pausa della partita.
     * Se la partita era conclusa, resetta automaticamente i punteggi prima di iniziare.
     */
    @FXML
    public void togglePartita() {
        // Controllo stato finale: se la partita era finita, puliamo il tavolo per un nuovo match
        if (scoreRosso >= MAX_GOL || scoreBlu >= MAX_GOL) {
            resetMatch();
        }

        isPlaying = !isPlaying;
        
        // Modifica diretta degli stili (In scenari più ampi, preferire classi CSS esterne per separare la logica dalla vista)
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
     * Resetta lo stato logico e visivo del match alle condizioni iniziali.
     * Riposiziona gli attori al centro e azzera i contatori.
     */
    @FXML
    public void resetMatch() {
        if (gameLoop != null) gameLoop.stop();
        isPlaying = false;
        scoreRosso = 0;
        scoreBlu = 0;
        aggiornaTestoPunteggi();

        // Riposizionamento nodi al centro dello stage locale
        pallina.setTranslateX(0);
        pallina.setTranslateY(0);
        squadraRossa.setTranslateY(0);
        squadraBlu.setTranslateY(0);

        messaggioCentrale.setVisible(false);
        btnAvviaPausa.setText("▶ AVVIA PARTITA");
        btnAvviaPausa.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-font-size: 20px; -fx-font-weight: bold; -fx-padding: 15 40; -fx-background-radius: 15;");
    }

    /**
     * Inizializza il Game Loop utilizzando AnimationTimer, nativo in JavaFX.
     * Garantisce che l'aggiornamento avvenga in sincrono con il refresh del monitor (circa 60fps).
     */
    private void creaMotoreFisico() {
        gameLoop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                // Generazione di un movimento armonico sfalsato per simulare interazione umana
                angoloAste += INCREMENTO_ANGOLO_ASTE;
                squadraRossa.setTranslateY(Math.sin(angoloAste) * 30);
                squadraBlu.setTranslateY(Math.cos(angoloAste * 0.8) * 30);

                // Calcolo preventiva della posizione per intercettare le collisioni prima che avvengano
                double nextX = pallina.getTranslateX() + velocityX;
                double nextY = pallina.getTranslateY() + velocityY;

                // Controllo collisione: Sponde verticali (sinistra/destra) e riconoscimento Gol
                if (nextX > CAMPO_LIMITE_X) {
                    if (Math.abs(nextY) < GOL_LIMITE_Y) {
                        gestisciGol("ROSSO");
                        return; // Interrompe il loop frame corrente
                    } else {
                        velocityX *= -1; // Rimbalzo
                        velocityY += (Math.random() - 0.5) * 5; // Aggiunta di variazione per realismo
                    }
                } else if (nextX < -CAMPO_LIMITE_X) {
                    if (Math.abs(nextY) < GOL_LIMITE_Y) {
                        gestisciGol("BLU");
                        return; // Interrompe il loop frame corrente
                    } else {
                        velocityX *= -1;
                        velocityY += (Math.random() - 0.5) * 5;
                    }
                }

                // Controllo collisione: Sponde orizzontali (alto/basso)
                if (nextY > CAMPO_LIMITE_Y || nextY < -CAMPO_LIMITE_Y) {
                    velocityY *= -1;
                    velocityX += (Math.random() - 0.5) * 5;
                }

                // Normalizzazione della velocità tramite clamping per evitare che la pallina compenetri i bordi
                velocityX = Math.max(-MAX_VELOCITY, Math.min(MAX_VELOCITY, velocityX));
                velocityY = Math.max(-MAX_VELOCITY, Math.min(MAX_VELOCITY, velocityY));

                // Applica lo spostamento finale al nodo visivo
                pallina.setTranslateX(nextX);
                pallina.setTranslateY(nextY);
            }
        };
    }

    /**
     * Gesisce l'evento di un gol segnato.
     * Mette in pausa la fisica, aggiorna la UI e avvia un timer non bloccante per la ripresa del gioco.
     *
     * @param squadra Identificativo della squadra che ha segnato ("ROSSO" o "BLU").
     */
    private void gestisciGol(String squadra) {
        gameLoop.stop();

        if ("ROSSO".equals(squadra)) {
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

        // L'uso di PauseTransition sostituisce il Thread.sleep per non bloccare thread nativi.
        // Esegue il suo OnFinished in modo sicuro direttamente sul JavaFX Application Thread.
        PauseTransition pausaRipresa = new PauseTransition(Duration.seconds(1.5));
        pausaRipresa.setOnFinished(event -> {
            messaggioCentrale.setVisible(false);
            pallina.setTranslateX(0);
            pallina.setTranslateY(0);

            // Regola del vantaggio: il servizio va a chi ha subito il gol
            velocityX = "ROSSO".equals(squadra) ? -12.0 : 12.0;
            velocityY = (Math.random() * 10) - 5;
            
            if (isPlaying) {
                gameLoop.start();
            }
        });
        pausaRipresa.play();
    }

    /**
     * Termina ufficialmente la partita disabilitando il motore fisico e mostrando il vincitore.
     */
    private void finalizzarePartita() {
        boolean vittoriaRossa = scoreRosso >= MAX_GOL;
        String vincitore = vittoriaRossa ? "ROSSA" : "BLU";
        String coloreHex = vittoriaRossa ? "#e74c3c" : "#3498db";

        messaggioCentrale.setText("🏆 VITTORIA SQUADRA " + vincitore + " 🏆");
        messaggioCentrale.setStyle("-fx-text-fill: " + coloreHex + "; -fx-background-color: rgba(0,0,0,0.9); -fx-padding: 40 80; -fx-background-radius: 20; -fx-font-size: 70px; -fx-font-weight: bold;");

        isPlaying = false;
        btnAvviaPausa.setText("🔄 NUOVA PARTITA");
        btnAvviaPausa.setStyle("-fx-background-color: #9b59b6; -fx-text-fill: white; -fx-font-size: 20px; -fx-font-weight: bold; -fx-padding: 15 40; -fx-background-radius: 15;");
    }

    /**
     * Sincronizza i nodi grafici dei punteggi con lo stato logico corrente.
     */
    private void aggiornaTestoPunteggi() {
        punteggioRosso.setText(String.valueOf(scoreRosso));
        punteggioBlu.setText(String.valueOf(scoreBlu));
    }
}