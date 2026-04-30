package org.borradoruno;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.borradoruno.navigation.SceneManager;
import org.borradoruno.network.ClientSocket;

import java.io.IOException;

public class ClientApp extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        SceneManager.getInstance().setPrimaryStage(stage);

        FXMLLoader fxmlLoader = new FXMLLoader(ClientApp.class.getResource("registro-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 800, 600);
        stage.setTitle("UNO Online");
        stage.setScene(scene);

        stage.setOnCloseRequest(event -> ClientSocket.getInstance().desconectar());

        stage.show();
    }

    @Override
    public void stop() throws Exception {
        ClientSocket.getInstance().desconectar();
        super.stop();
    }
}
