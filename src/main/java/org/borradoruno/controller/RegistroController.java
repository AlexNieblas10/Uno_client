package org.borradoruno.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.TextField;
import org.borradoruno.config.ServerConfig;
import org.borradoruno.model.Avatar;
import org.borradoruno.model.EstadoCliente;
import org.borradoruno.navigation.SceneManager;
import org.borradoruno.network.ClientSocket;
import org.borradoruno.network.Mensaje;
import org.borradoruno.network.MensajeParser;
import org.borradoruno.sound.MusicManager;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class RegistroController implements ClientSocket.ServerObserver {

    @FXML private TextField txtNombre;
    @FXML private Button btnAvatarAzul, btnAvatarAmarillo, btnAvatarRojo, btnAvatarVerde;

    private Avatar avatarSeleccionado = Avatar.AZUL;

    @FXML
    public void initialize() {
        MusicManager.getInstance().play(MusicManager.MUSIC_MENU);
        EstadoCliente.getInstance().limpiar();
        seleccionarAvatar(Avatar.AZUL);
    }

    @FXML private void onAvatarAzul()     { seleccionarAvatar(Avatar.AZUL); }
    @FXML private void onAvatarAmarillo() { seleccionarAvatar(Avatar.AMARILLO); }
    @FXML private void onAvatarRojo()     { seleccionarAvatar(Avatar.ROJO); }
    @FXML private void onAvatarVerde()    { seleccionarAvatar(Avatar.VERDE); }

    private void seleccionarAvatar(Avatar avatar) {
        avatarSeleccionado = avatar;
        String sel   = "-fx-min-width: 60; -fx-min-height: 60; -fx-background-radius: 30; -fx-border-color: #1d4ed8; -fx-border-width: 3; -fx-border-radius: 30;";
        String nosel = "-fx-min-width: 60; -fx-min-height: 60; -fx-background-radius: 30;";
        btnAvatarAzul.setStyle     ("-fx-background-color: #3b82f6; " + (avatar == Avatar.AZUL     ? sel : nosel));
        btnAvatarAmarillo.setStyle ("-fx-background-color: #facc15; " + (avatar == Avatar.AMARILLO ? sel : nosel));
        btnAvatarRojo.setStyle     ("-fx-background-color: #ef4444; " + (avatar == Avatar.ROJO     ? sel : nosel));
        btnAvatarVerde.setStyle    ("-fx-background-color: #22c55e; " + (avatar == Avatar.VERDE    ? sel : nosel));
    }

    @FXML
    private void onCrearPartida() {
        String nombre = txtNombre.getText().trim();
        if (nombre.isEmpty()) {
            mostrarAlerta("Nombre requerido", "Ingresa tu nombre para continuar.");
            return;
        }
        conectarYEnviar(nombre, "CREAR_PARTIDA", null);
    }

    @FXML
    private void onUnirsePartida() {
        String nombre = txtNombre.getText().trim();
        if (nombre.isEmpty()) {
            mostrarAlerta("Nombre requerido", "Ingresa tu nombre para continuar.");
            return;
        }
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Unirse a partida");
        dialog.setHeaderText(null);
        dialog.setContentText("Código de sala:");
        dialog.showAndWait().ifPresent(codigo -> {
            String c = codigo.trim();
            if (!c.isEmpty()) conectarYEnviar(nombre, "UNIRSE_PARTIDA", c);
        });
    }

    private void conectarYEnviar(String nombre, String tipo, String codigoSala) {
        EstadoCliente.getInstance().setNombreLocal(nombre);
        EstadoCliente.getInstance().setAvatarLocal(avatarSeleccionado);

        new Thread(() -> {
            ClientSocket socket = ClientSocket.getInstance();
            try {
                socket.addObserver(this);
                if (!socket.isConectado()) {
                    socket.conectar(ServerConfig.getServerIP(), ServerConfig.getServerPort());
                }
                Map<String, Object> payload = new HashMap<>();
                payload.put("nombre", nombre);
                payload.put("avatar", avatarSeleccionado.name());
                if (codigoSala != null) payload.put("codigoSala", codigoSala);
                socket.enviar(tipo, payload);
            } catch (IOException e) {
                socket.removeObserver(this);
                Platform.runLater(() ->
                    mostrarAlerta("Error de conexión", "No se pudo conectar al servidor:\n" + e.getMessage()));
            }
        }).start();
    }

    @Override
    public void onMensajeRecibido(Mensaje mensaje) {
        switch (mensaje.getTipo()) {
            case "ESTADO_PARTIDA" -> {
                var partida = MensajeParser.parsearPartida(mensaje.getDatos());
                EstadoCliente.getInstance().setPartidaActual(partida);
                ClientSocket.getInstance().removeObserver(this);
                Platform.runLater(() -> SceneManager.getInstance().cambiarVista(SceneManager.VIEW_SALA));
            }
            case "ERROR" -> {
                String error = MensajeParser.parsearString(mensaje.getDatos());
                Platform.runLater(() -> mostrarAlerta("Error", error));
            }
        }
    }

    private void mostrarAlerta(String titulo, String contenido) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(contenido);
        alert.showAndWait();
    }
}
