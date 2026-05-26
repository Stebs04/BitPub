package com.bitpub.controllers;

import com.bitpub.core.UIState;
import com.bitpub.models.Locale;
import com.bitpub.models.Utente;
import com.bitpub.viewmodels.AdminDashboardViewModel;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.List;

/**
 * Controller per la Dashboard Amministratore dedicata alla gestione dei Locali.
 *
 * Pattern: MVVM
 */
public class AdminDashboardController {

    @FXML private TableView<Locale> localiTable;
    @FXML private TableColumn<Locale, Long> colId;
    @FXML private TableColumn<Locale, String> colNome;
    @FXML private TableColumn<Locale, String> colCitta;
    @FXML private TableColumn<Locale, String> colIndirizzo;

    @FXML private Button btnModifica;
    @FXML private Button btnElimina;
    @FXML private ProgressIndicator progressIndicator;

    private final AdminDashboardViewModel viewModel;

    public AdminDashboardController(AdminDashboardViewModel viewModel) {
        this.viewModel = viewModel;
    }

    @FXML
    public void initialize() {
        configuraTabella();
        setupBindings();
        
        // Controllo contestuale degli stati: abilita i pulsanti solo se una riga è selezionata
        localiTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            boolean selezionato = (newVal != null);
            btnModifica.setDisable(!selezionato);
            btnElimina.setDisable(!selezionato);
        });

        viewModel.loadLocali();
    }

    private void configuraTabella() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNome.setCellValueFactory(new PropertyValueFactory<>("name"));
        colCitta.setCellValueFactory(new PropertyValueFactory<>("citta"));
        colIndirizzo.setCellValueFactory(new PropertyValueFactory<>("indirizzo"));
        localiTable.setItems(viewModel.getLocali());
    }

    private void setupBindings() {
        viewModel.stateProperty().addListener((obs, oldState, newState) -> {
            Platform.runLater(() -> progressIndicator.setVisible(newState == UIState.LOADING));
        });
    }

    @FXML
    public void caricaDati() {
        viewModel.loadLocali();
    }

    @FXML
    public void handleNuovoLocale() {
        viewModel.startNuovoLocale(this::mostraDialogCreazione);
    }

    private void mostraDialogCreazione(List<Utente> gestori) {
        // [Logica Dialog omessa per brevità, simulata con un mock]
        // Se avessi un dialog vero, gestirei qui il form per creare il Locale.
        // Simulazione creazione nuovo locale.
        Locale nuovoLocale = new Locale();
        nuovoLocale.setName("Nuovo Locale MVVM");
        // ...
        viewModel.createLocale(nuovoLocale);
    }

    @FXML
    public void handleModifica() {
        Locale selezionato = localiTable.getSelectionModel().getSelectedItem();
        if (selezionato == null) return;
        
        // Simulo modifica
        viewModel.updateLocale(selezionato);
    }

    @FXML
    public void handleElimina() {
        Locale selezionato = localiTable.getSelectionModel().getSelectedItem();
        if (selezionato == null) return;

        Alert conferma = new Alert(Alert.AlertType.CONFIRMATION, "Eliminare " + selezionato.getName() + "?");
        conferma.showAndWait().ifPresent(risposta -> {
            if (risposta == ButtonType.OK) {
                viewModel.deleteLocale(selezionato);
            }
        });
    }
}
