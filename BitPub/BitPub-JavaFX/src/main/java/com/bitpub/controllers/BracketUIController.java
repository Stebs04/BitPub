package com.bitpub.controllers;

import com.bitpub.model.MatchModel;
import com.bitpub.model.TournamentModel;
import com.bitpub.network.RestClient;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class BracketUIController {

    @FXML private Label lblTournamentName;
    @FXML private HBox bracketContainer;

    private TournamentModel tournament;

    public void setTournament(TournamentModel tournament) {
        this.tournament = tournament;
        lblTournamentName.setText("Bracket: " + tournament.getName());
        loadBracket();
    }

    @FXML
    private void loadBracket() {
        if (tournament == null) return;
        new Thread(() -> {
            try {
                String response = RestClient.getInstance().get("/tournaments/" + tournament.getId() + "/matches");
                Type listType = new TypeToken<List<MatchModel>>(){}.getType();
                List<MatchModel> list = RestClient.getInstance().getGson().fromJson(response, listType);
                Platform.runLater(() -> renderBracket(list));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void renderBracket(List<MatchModel> matches) {
        bracketContainer.getChildren().clear();
        
        Map<Integer, List<MatchModel>> matchesByRound = matches.stream()
                .collect(Collectors.groupingBy(MatchModel::getRound));

        int maxRound = matchesByRound.keySet().stream().max(Integer::compareTo).orElse(0);

        for (int i = 1; i <= maxRound; i++) {
            List<MatchModel> roundMatches = matchesByRound.get(i);
            if(roundMatches != null) {
                roundMatches.sort((m1, m2) -> Integer.compare(m1.getMatchIndex(), m2.getMatchIndex()));
                
                VBox roundCol = new VBox(20);
                roundCol.setAlignment(Pos.CENTER);
                
                for (MatchModel match : roundMatches) {
                    VBox matchBox = createMatchBox(match);
                    roundCol.getChildren().add(matchBox);
                }
                
                bracketContainer.getChildren().add(roundCol);
            }
        }
    }

    private VBox createMatchBox(MatchModel match) {
        VBox box = new VBox(5);
        box.setStyle("-fx-border-color: #555; -fx-border-radius: 5; -fx-padding: 10; -fx-background-color: #f9f9f9;");
        box.setPrefWidth(150);

        String teamAName = match.getTeamA() != null ? match.getTeamA().getName() : "TBD";
        String teamBName = match.getTeamB() != null ? match.getTeamB().getName() : "TBD";
        
        Label lblA = new Label(teamAName + " - " + match.getScoreA());
        Label lblB = new Label(teamBName + " - " + match.getScoreB());

        if (match.getWinner() != null) {
            if (match.getWinner().getId().equals(match.getTeamA() != null ? match.getTeamA().getId() : null)) {
                lblA.setStyle("-fx-font-weight: bold; -fx-text-fill: green;");
            } else if (match.getWinner().getId().equals(match.getTeamB() != null ? match.getTeamB().getId() : null)) {
                lblB.setStyle("-fx-font-weight: bold; -fx-text-fill: green;");
            }
        }

        box.getChildren().addAll(lblA, lblB);

        // Optional: on click open dialog to set score if user is admin
        box.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && "SCHEDULED".equals(match.getStatus())) {
                // Here we would open a dialog to enter score.
                System.out.println("Doppio clic sul match: " + match.getId());
            }
        });

        return box;
    }
}
