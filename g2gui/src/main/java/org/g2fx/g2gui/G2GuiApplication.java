package org.g2fx.g2gui;

import g2lib.state.Devices;
import g2lib.state.SynthSettings;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;


public class G2GuiApplication extends Application {

    public final Label welcomeText = new Label();

    private Devices devices;

    @Override
    public void init() throws Exception {
        devices = new Devices();
        devices.addListener(d -> {
            Platform.runLater(() -> {
                SynthSettings ss = d.getSynthSettings();
                welcomeText.setText(ss == null ? "no settings" : ss.getDeviceName());
            });
        });
    }

    @Override
    public void start(Stage stage) throws IOException {

        Button helloButton = new Button("Hello!");
        helloButton.setOnAction(event -> onHelloButtonClick());

        VBox root = new VBox(20, welcomeText, helloButton);
        root.setAlignment(javafx.geometry.Pos.CENTER);
        root.setPadding(new Insets(20));

        Scene scene = new Scene(root, 320, 240);
        stage.setTitle("Hello!");
        stage.setScene(scene);
        stage.show();
        devices.start();
    }

    protected void onHelloButtonClick() {
        welcomeText.setText("Welcome to JavaFX Application!");
    }

    @Override
    public void stop() throws Exception {
        devices.shutdown();
    }

    public static void main(String[] args) {
        launch();
    }
}
