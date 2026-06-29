package it.uniupo.pissir.bitpub.javafx.controller;

import it.uniupo.pissir.bitpub.javafx.model.GameStateDto;
import it.uniupo.pissir.bitpub.javafx.mqtt.MqttSubscriberService;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Duration;

import java.util.Arrays;
import java.util.List;

public class KioskController {

    @FXML
    private Label gameTitleLabel;
    @FXML
    private Label scoreTeamALabel;
    @FXML
    private Label scoreTeamBLabel;
    @FXML
    private Label timerLabel;
    @FXML
    private Label statusLabel;
    @FXML
    private Label eventMessageLabel;
    @FXML
    private ImageView promoImageView;

    private MqttSubscriberService mqttService;
    
    // Lista di URL (o path locali) per le immagini promozionali
    private final List<String> promoImages = Arrays.asList(
        "https://images.unsplash.com/photo-1575361204480-aadea25e6e68?w=800&q=80",
        "https://images.unsplash.com/photo-1540348737522-86ec163f9a74?w=800&q=80",
        "https://images.unsplash.com/photo-1511512578047-dfb367046420?w=800&q=80"
    );
    private int currentImageIndex = 0;

    @FXML
    public void initialize() {
        // Setup initial text
        gameTitleLabel.setText("BitPub Kiosk");
        scoreTeamALabel.setText("0");
        scoreTeamBLabel.setText("0");
        timerLabel.setText("00:00");
        statusLabel.setText("WAITING");
        eventMessageLabel.setText("");

        // Setup promo carousel
        setupPromoCarousel();

        // TODO: Read localeId and gameInstanceId from config/properties in a real app
        String brokerUrl = "tcp://localhost:1883";
        String localeId = "LOC-1";
        String gameInstanceId = "CALCIO-1";
        
        mqttService = new MqttSubscriberService(brokerUrl, localeId, gameInstanceId, this);
        mqttService.start();
    }

    private void setupPromoCarousel() {
        if (!promoImages.isEmpty()) {
            loadImage(currentImageIndex);
            Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(10), event -> {
                currentImageIndex = (currentImageIndex + 1) % promoImages.size();
                loadImage(currentImageIndex);
            }));
            timeline.setCycleCount(Timeline.INDEFINITE);
            timeline.play();
        }
    }
    
    private void loadImage(int index) {
        try {
            Image image = new Image(promoImages.get(index), true);
            promoImageView.setImage(image);
        } catch (Exception e) {
            System.err.println("Failed to load image: " + promoImages.get(index));
        }
    }

    public void updateGameState(GameStateDto state) {
        if (state == null) return;
        
        scoreTeamALabel.setText(String.valueOf(state.getScoreTeamA()));
        scoreTeamBLabel.setText(String.valueOf(state.getScoreTeamB()));
        
        int minutes = state.getTimeRemainingSeconds() / 60;
        int seconds = state.getTimeRemainingSeconds() % 60;
        timerLabel.setText(String.format("%02d:%02d", minutes, seconds));
        
        if (state.getStatus() != null) {
            statusLabel.setText(state.getStatus());
        }
        
        if (state.getCurrentEventMessage() != null) {
            eventMessageLabel.setText(state.getCurrentEventMessage());
        }
    }
    
    public void shutdown() {
        if (mqttService != null) {
            mqttService.stop();
        }
    }
}
