package org.borradoruno.controller;

import javafx.application.Platform;
import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.util.Duration;
import org.borradoruno.config.ServerConfig;
import org.borradoruno.model.Avatar;
import org.borradoruno.model.EstadoCliente;
import org.borradoruno.model.Partida;
import org.borradoruno.navigation.SceneManager;
import org.borradoruno.network.ClientSocket;
import org.borradoruno.network.Mensaje;
import org.borradoruno.network.MensajeParser;

import java.io.IOException;
import java.util.Optional;

public class RegistroController implements ClientSocket.ServerObserver {

    @FXML private TextField txtApodo;
    @FXML private Button btnAvatarAzul;
    @FXML private Button btnAvatarAmarillo;
    @FXML private Button btnAvatarRojo;
    @FXML private Button btnAvatarVerde;

    private Avatar avatarSeleccionado = Avatar.AZUL;
    private boolean intentandoLogin = false;
    private PauseTransition timeoutLogin;

    @FXML
    public void initialize() {
        ClientSocket.getInstance().addObserver(this);
        actualizarBordeAvatar();
    }

    @FXML private void onSeleccionarAvatarAzul()     { avatarSeleccionado = Avatar.AZUL;     actualizarBordeAvatar(); }
    @FXML private void onSeleccionarAvatarAmarillo() { avatarSeleccionado = Avatar.AMARILLO; actualizarBordeAvatar(); }
    @FXML private void onSeleccionarAvatarRojo()     { avatarSeleccionado = Avatar.ROJO;     actualizarBordeAvatar(); }
    @FXML private void onSeleccionarAvatarVerde()    { avatarSeleccionado = Avatar.VERDE;    actualizarBordeAvatar(); }

    private void actualizarBordeAvatar() {
        String borde = "-fx-border-color: #1d4ed8; -fx-border-width: 3; -fx-border-radius: 8;";
        btnAvatarAzul.setStyle("-fx-background-color: #3b82f6; -fx-min-width: 50; -fx-min-height: 50; -fx-background-radius: 8;"
                + (avatarSeleccionado == Avatar.AZUL ? borde : ""));
        btnAvatarAmarillo.setStyle("-fx-background-color: #facc15; -fx-min-width: 50; -fx-min-height: 50; -fx-background-radius: 8;"
                + (avatarSeleccionado == Avatar.AMARILLO ? borde : ""));
        btnAvatarRojo.setStyle("-fx-background-color: #ef4444; -fx-min-width: 50; -fx-min-height: 50; -fx-background-radius: 8;"
                + (avatarSeleccionado == Avatar.ROJO ? borde : ""));
        btnAvatarVerde.setStyle("-fx-background-color: #22c55e; -fx-min-width: 50; -fx-min-height: 50; -fx-background-radius: 8;"
                + (avatarSeleccionado == Avatar.VERDE ? borde : ""));
    }

    @FXML
    private void onCrearPartida() {
        if (!validarApodo()) return;

        String apodo = txtApodo.getText().trim();
        try {
            ClientSocket.getInstance().conectar(ServerConfig.getServerIP(), ServerConfig.getServerPort());
            intentandoLogin = true;
            bloquearUI(true);
            EstadoCliente.getInstance().setNombreLocal(apodo);
            EstadoCliente.getInstance().setAvatarLocal(avatarSeleccionado);
            ClientSocket.getInstance().enviar("CREATE", new Object[]{apodo, avatarSeleccionado.name()});
            iniciarTimeoutLogin();
        } catch (IOException e) {
            mostrarError("Error de Conexión", "No se pudo conectar al servidor");
            intentandoLogin = false;
            bloquearUI(false);
        }
    }

    @FXML
    private void onUnirsePartida() {
        if (!validarApodo()) return;

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Unirse a partida");
        dialog.setHeaderText("Código de la sala");
        dialog.setContentText("Ingresa el código (ej. UNO-1234):");

        Optional<String> resultado = dialog.showAndWait();
        if (resultado.isEmpty()) return;

        String codigo = resultado.get().trim().toUpperCase();
        if (codigo.isEmpty()) {
            mostrarError("Código inválido", "Debes ingresar un código de sala");
            return;
        }

        String apodo = txtApodo.getText().trim();
        try {
            ClientSocket.getInstance().conectar(ServerConfig.getServerIP(), ServerConfig.getServerPort());
            intentandoLogin = true;
            bloquearUI(true);
            EstadoCliente.getInstance().setNombreLocal(apodo);
            EstadoCliente.getInstance().setAvatarLocal(avatarSeleccionado);
            ClientSocket.getInstance().enviar("JOIN", new Object[]{apodo, codigo, avatarSeleccionado.name()});
            iniciarTimeoutLogin();
        } catch (IOException e) {
            mostrarError("Error de Conexión", "No se pudo conectar al servidor");
            intentandoLogin = false;
            bloquearUI(false);
        }
    }

    private void iniciarTimeoutLogin() {
        cancelarTimeoutLogin();

        timeoutLogin = new PauseTransition(Duration.seconds(8));
        timeoutLogin.setOnFinished(e -> {
            if (intentandoLogin) {
                intentandoLogin = false;
                bloquearUI(false);
                mostrarError("Servidor sin respuesta",
                    "El servidor no respondió a tiempo.\n\nPosibles causas:\n" +
                    "• Tu red bloquea el puerto 5050\n" +
                    "• El servidor está saturado\n" +
                    "• Tu conexión es muy lenta\n\n" +
                    "Intenta de nuevo en unos segundos.");
                try {
                    ClientSocket.getInstance().desconectar();
                } catch (Exception ex) { /* ignore */ }
            }
        });
        timeoutLogin.play();
    }

    private void cancelarTimeoutLogin() {
        if (timeoutLogin != null) {
            timeoutLogin.stop();
            timeoutLogin = null;
        }
    }

    private void bloquearUI(boolean bloquear) {
        Platform.runLater(() -> {
            txtApodo.setDisable(bloquear);
            btnAvatarAzul.setDisable(bloquear);
            btnAvatarAmarillo.setDisable(bloquear);
            btnAvatarRojo.setDisable(bloquear);
            btnAvatarVerde.setDisable(bloquear);
        });
    }

    private boolean validarApodo() {
        String apodo = txtApodo.getText().trim();
        if (apodo.isEmpty()) {
            mostrarError("Apodo Inválido", "El apodo no puede estar vacío");
            return false;
        }
        if (apodo.length() < 3 || apodo.length() > 20) {
            mostrarError("Apodo Inválido", "El apodo debe tener entre 3 y 20 caracteres");
            return false;
        }
        if (!apodo.matches("^[a-zA-Z0-9_-]+$")) {
            mostrarError("Apodo Inválido", "Solo letras, números, guión y guión bajo");
            return false;
        }
        return true;
    }

    @Override
    public void onMensajeRecibido(Mensaje mensaje) {
        if (mensaje.getTipo().equals("ESTADO_PARTIDA") && intentandoLogin) {
            if (mensaje.getDatos() == null) {
                return;
            }

            Partida partida;
            try {
                partida = MensajeParser.parsearPartida(mensaje.getDatos());
            } catch (Exception e) {
                System.err.println("Error parseando partida: " + e.getMessage());
                return;
            }

            if (partida == null || partida.getCodigoSala() == null
                    || partida.getJugadores() == null || partida.getJugadores().isEmpty()) {
                System.out.println("Estado de partida incompleto, esperando...");
                return;
            }

            cancelarTimeoutLogin();
            EstadoCliente.getInstance().setPartidaActual(partida);
            intentandoLogin = false;
            ClientSocket.getInstance().removeObserver(this);
            SceneManager.getInstance().cambiarVista(SceneManager.VIEW_SALA);

        } else if (mensaje.getTipo().equals("ERROR") && intentandoLogin) {
            cancelarTimeoutLogin();
            bloquearUI(false);
            String msg = mensaje.getDatos() != null ? mensaje.getDatos().toString() : "Error desconocido";
            mostrarError("Error del Servidor", msg);
            intentandoLogin = false;
        }
    }

    private void mostrarError(String titulo, String mensaje) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(titulo);
            alert.setHeaderText(null);
            alert.setContentText(mensaje);
            alert.showAndWait();
        });
    }
}
