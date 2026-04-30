package org.borradoruno.navigation;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.IOException;

public class SceneManager {
    private static SceneManager instance;
    private Stage primaryStage;

    public static final String VIEW_REGISTRO = "/org/borradoruno/registro-view.fxml";
    public static final String VIEW_SALA = "/org/borradoruno/sala-view.fxml";
    public static final String VIEW_JUEGO = "/org/borradoruno/juego-view.fxml";

    private static final int DEFAULT_WIDTH = 800;
    private static final int DEFAULT_HEIGHT = 600;

    private SceneManager() {}

    public static synchronized SceneManager getInstance() {
        if (instance == null) {
            instance = new SceneManager();
        }
        return instance;
    }

    public void setPrimaryStage(Stage stage) {
        this.primaryStage = stage;
    }

    public void cambiarVista(String fxmlPath) {
        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
                Parent root = loader.load();
                Scene scene = new Scene(root, DEFAULT_WIDTH, DEFAULT_HEIGHT);

                Stage stage = primaryStage;
                if (stage == null && !Window.getWindows().isEmpty()) {
                    stage = (Stage) Window.getWindows().get(0);
                }

                if (stage == null) {
                    System.err.println("SceneManager: no hay stage disponible");
                    return;
                }

                stage.setScene(scene);
            } catch (IOException e) {
                System.err.println("Error cargando vista " + fxmlPath + ": " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
}
