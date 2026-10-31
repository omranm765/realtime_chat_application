package de.htwsaar.messagebroker.desktopclient;

import javafx.application.Application;
import javafx.stage.Stage;

public class DesktopClientApplication extends Application {
    @Override
    public void start(Stage stage) {
        new ClientWindow().show(stage);
    }

    @Override
    public void stop() {
        ClientWindow.shutdownAll();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
