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

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import com.google.gson.Gson;
import com.bitpub.models.PartitaCalciobalilla;
import javafx.application.Platform;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Controller per la gestione del calciobalilla lato utente.
 */
public class CalciobalillaUtenteController implements MqttCallback {

    @FXML private VBox boxRisultatiPartita;
    @FXML private Label lblPunteggio;
    @FXML private Label lblStatistiche;
    @FXML private TextField txtNomeSquadra;
    @FXML private ListView<String> listSquadre;
    @FXML private VBox boxTorneo;

    private MqttClient localMqttClient;

    @FXML
    public void initialize() {
        try {
            // L'Edge Node (o in questo caso il client UI della LAN) si iscrive al broker locale
            localMqttClient = new MqttClient("tcp://localhost:1883", "JavaFX-Calciobalilla-UI");
            localMqttClient.setCallback(this);
            localMqttClient.connect();
            localMqttClient.subscribe("bitpub/locali/+/calciobalilla/+/eventi");
            System.out.println("[Calciobalilla UI] In ascolto degli eventi locali per latenza nulla.");
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void connectionLost(Throwable cause) {
        System.err.println("Connessione persa al broker locale: " + cause.getMessage());
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        String payload = new String(message.getPayload(), StandardCharsets.UTF_8);
        try {
            PartitaCalciobalilla evento = new Gson().fromJson(payload, PartitaCalciobalilla.class);
            // ⚡ View in Tempo Reale pilotata direttamente tramite Platform.runLater
            Platform.runLater(() -> {
                boxRisultatiPartita.setVisible(true);
                lblPunteggio.setText("Punteggio: " + evento.getGoalRossi() + " - " + evento.getGoalBlu());
                // Mostra il tempo. Le statistiche o rullate etc...
                lblStatistiche.setText("Rullate totali: " + evento.getTotaleRullate() + " | Durata: " + evento.getDurataMediaPallinaSecondi() + "s");
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
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
            Parent root = FXMLLoader.load(getClass().getResource("/DashboardView.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root, 1024, 768)); // Adjust to main dashboard size
        } catch (IOException e) { e.printStackTrace(); }
    }
}