package it.uniupo.pissir.bitpub.javafx;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import it.uniupo.pissir.bitpub.javafx.controller.KioskController;

import java.net.URL;

public class BitPubKioskApplication extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        URL fxmlLocation = getClass().getResource("/fxml/kiosk.fxml");
        if (fxmlLocation == null) {
            throw new IllegalStateException("Cannot find /fxml/kiosk.fxml");
        }
        
        FXMLLoader loader = new FXMLLoader(fxmlLocation);
        Parent root = loader.load();
        
        KioskController controller = loader.getController();
        primaryStage.setOnCloseRequest(event -> controller.shutdown());

        Scene scene = new Scene(root, 1024, 768);
        
        URL cssLocation = getClass().getResource("/css/style.css");
        if (cssLocation != null) {
            scene.getStylesheets().add(cssLocation.toExternalForm());
        }

        primaryStage.setTitle("BitPub Kiosk");
        primaryStage.setScene(scene);
        primaryStage.setMaximized(true);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
