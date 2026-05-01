package com.bitpub.controllers;

import com.bitpub.network.RestClient;
import com.bitpub.network.SessionManager;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.util.*;

/**
 * Controller per la gestione del calciobalilla lato utente.
 */
public class CalciobalillaUtenteController {

    @FXML private VBox boxRisultatiPartita;
    @FXML private Label lblPunteggio;
    @FXML private Label lblStatistiche;
    @FXML private TextField txtNomeSquadra;
    @FXML private ListView<String> listSquadre;
    @FXML private VBox boxTorneo;

    private Timeline gameLoop;
    private int goalS1 = 0;
    private int goalS2 = 0;
    private int secondiTrascorsi = 0;

    /**
     * Avvia una partita simulata che dura max 2 minuti o fino a 10 goal.
     */
    @FXML
    void giocaPartitaSimulata(ActionEvent event) {
        resetGame();
        boxRisultatiPartita.setVisible(true);

        // Timeline configurata per 1 secondo (1000ms)
        gameLoop = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            secondiTrascorsi++;
            
            // Simula un'azione ogni 4 secondi
            if (secondiTrascorsi % 4 == 0) {
                if (new Random().nextBoolean()) goalS1++; else goalS2++;
                
                // Sincronizzazione Cloud obbligatoria
                Map<String, Integer> score = new HashMap<>();
                score.put("s1", goalS1);
                score.put("s2", goalS2);
                RestClient.getInstance().putAsync("/api/v1/calciobalilla/punteggio", score, null);
            }

            aggiornaUI();

            // Condizioni di uscita: 10 goal o 120 secondi (2 minuti)
            if (goalS1 >= 10 || goalS2 >= 10 || secondiTrascorsi >= 120) {
                terminaESalvaPartita();
            }
        }));
        gameLoop.setCycleCount(Timeline.INDEFINITE);
        gameLoop.play();
    }

    private void terminaESalvaPartita() {
        gameLoop.stop();
        // Chiamata finale al cloud per salvare le statistiche
        RestClient.getInstance().postAsync("/api/v1/calciobalilla/concludi", null, response -> {
            System.out.println("Partita salvata nel database cloud.");
        });
    }

    private void logicaPartita() {
        Random r = new Random();
        // Simula un'azione ogni 3 secondi mediamente
        if (secondiTrascorsi % 3 == 0 && r.nextBoolean()) {
            if (r.nextBoolean()) goalS1++; else goalS2++;
            
            // Invia aggiornamento goal al Cloud
            Map<String, Integer> score = new HashMap<>();
            score.put("goalS1", goalS1);
            score.put("goalS2", goalS2);
            RestClient.getInstance().putAsync("/api/v1/calciobalilla/aggiorna-punteggio", score, null);
        }
    }

    private void concludiPartita() {
        gameLoop.stop();
        RestClient.getInstance().postAsync("/api/v1/calciobalilla/termina", null, res -> {
            System.out.println("Partita salvata definitivamente");
        });
        lblStatistiche.setText(lblStatistiche.getText() + "\nPARTITA TERMINATA");
    }

    /**
     * Gestisce l'iscrizione a un torneo da 16 squadre.
     */
    @FXML
    void iscrivitiTorneo(ActionEvent event) {
        String miaSquadra = txtNomeSquadra.getText().trim();
        if (miaSquadra.isEmpty()) return;

        List<String> nomiFinti = Arrays.asList("Real Pub", "Atletico Birra", "Dinamo Bar", "I Corsari", 
            "Le Aquile", "Team Alpha", "I Luppoli", "FC Divano", "Sbronza Team", "Gladiatori", 
            "I Titani", "I Falchi", "I Lupi", "Zena FC", "Spartan");
        
        List<String> partecipanti = new ArrayList<>();
        partecipanti.add("⭐ " + miaSquadra + " (Tu)");
        partecipanti.addAll(nomiFinti);

        Map<String, Object> data = new HashMap<>();
        data.put("nomeTorneo", "Torneo BitPub Primavera");
        data.put("squadra", miaSquadra);
        data.put("partecipanti", partecipanti);

        RestClient.getInstance().postAsync("/api/v1/tornei/calciobalilla/iscriviti", data, res -> {
            listSquadre.setItems(FXCollections.observableArrayList(partecipanti));
            boxTorneo.setVisible(true);
            txtNomeSquadra.setDisable(true);
        });
    }

    @FXML
    void tornaAllaDashboard(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/DashboardUtenteView.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root, 900, 600));
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void aggiornaUI() {
        lblPunteggio.setText("Punteggio: " + goalS1 + " - " + goalS2);
        lblStatistiche.setText("Tempo: " + secondiTrascorsi + "s / 120s");
    }

    private void resetGame() {
        goalS1 = 0; goalS2 = 0; secondiTrascorsi = 0;
        if (gameLoop != null) gameLoop.stop();
    }
}