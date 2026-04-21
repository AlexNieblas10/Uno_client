package org.borradoruno.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import org.borradoruno.config.ServerConfig;
import org.borradoruno.network.ClientSocket;
import org.borradoruno.network.Mensaje;
import java.io.IOException;

public class RegistroController implements ClientSocket.ServerObserver {

    @FXML
    private TextField txtApodo;
    @FXML
    private TextField txtIp;

    private boolean intentandoLogin = false;

    @FXML
    public void initialize() {
        ClientSocket.getInstance().addObserver(this);

        // Cargar IP guardada previamente
        String savedIP = ServerConfig.getServerIP();
        if (savedIP != null && !savedIP.isEmpty()) {
            txtIp.setText(savedIP);
        }
    }

    private void conectarAlServidor() throws IOException {
        String ip = txtIp.getText().trim();
        int port = ServerConfig.getServerPort();
        ClientSocket.getInstance().conectar(ip, port);
    }

    @FXML
    private void onCrearPartida() {
        if (!validarEntradas()) {
            return;
        }

        String apodo = txtApodo.getText().trim();
        String ip = txtIp.getText().trim();

        try {
            conectarAlServidor();
            intentandoLogin = true;
            ClientSocket.getInstance().setNombreLocal(apodo);
            ClientSocket.getInstance().enviar("CREATE", apodo);

            // Guardar IP exitosa
            ServerConfig.setServerIP(ip);
        } catch (IOException e) {
            mostrarError("Error de Conexión", "No se pudo conectar al servidor en " + ip);
            intentandoLogin = false;
        }
    }

    @FXML
    private void onUnirsePartida() {
        if (!validarEntradas()) {
            return;
        }

        String apodo = txtApodo.getText().trim();
        String ip = txtIp.getText().trim();

        try {
            conectarAlServidor();
            intentandoLogin = true;
            ClientSocket.getInstance().setNombreLocal(apodo);
            ClientSocket.getInstance().enviar("JOIN", apodo);

            // Guardar IP exitosa
            ServerConfig.setServerIP(ip);
        } catch (IOException e) {
            mostrarError("Error de Conexión", "No se pudo conectar al servidor en " + ip);
            intentandoLogin = false;
        }
    }

    @Override
    public void onMensajeRecibido(Mensaje mensaje) {
        if (mensaje.getTipo().equals("ESTADO_PARTIDA") && intentandoLogin) {
            cambiarASala();
            intentandoLogin = false;
        } else if (mensaje.getTipo().equals("ERROR") && intentandoLogin) {
            String errorMsg = mensaje.getDatos() != null ? mensaje.getDatos().toString() : "Error desconocido";
            mostrarError("Error del Servidor", errorMsg);
            intentandoLogin = false;
        }
    }

    /**
     * Valida las entradas del usuario antes de intentar conectar
     */
    private boolean validarEntradas() {
        String apodo = txtApodo.getText().trim();
        String ip = txtIp.getText().trim();

        // Validar apodo
        if (apodo.isEmpty()) {
            mostrarError("Apodo Inválido", "El apodo no puede estar vacío");
            return false;
        }

        if (apodo.length() < 3 || apodo.length() > 20) {
            mostrarError("Apodo Inválido", "El apodo debe tener entre 3 y 20 caracteres");
            return false;
        }

        if (!apodo.matches("^[a-zA-Z0-9_-]+$")) {
            mostrarError("Apodo Inválido",
                "El apodo solo puede contener letras, números, guión (-) y guión bajo (_)");
            return false;
        }

        // Validar IP/Hostname
        if (ip.isEmpty()) {
            mostrarError("Dirección Inválida", "La dirección del servidor no puede estar vacía");
            return false;
        }

        // Permitir localhost
        if (ip.equals("localhost") || ip.equals("127.0.0.1")) {
            return true;
        }

        // Validar formato IPv4
        String ipv4Pattern = "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}" +
                           "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";

        // Validar formato de hostname/dominio (ej: example.com, server.railway.app)
        String hostnamePattern = "^([a-zA-Z0-9]([a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?\\.)+[a-zA-Z]{2,}$";

        // Aceptar si es IPv4 O hostname válido
        if (!ip.matches(ipv4Pattern) && !ip.matches(hostnamePattern)) {
            mostrarError("Dirección Inválida",
                "Formato inválido.\nUsa: 192.168.1.1, localhost o example.com");
            return false;
        }

        return true;
    }

    /**
     * Muestra un diálogo de error al usuario
     */
    private void mostrarError(String titulo, String mensaje) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(titulo);
            alert.setHeaderText(null);
            alert.setContentText(mensaje);
            alert.showAndWait();
        });
    }

    private void cambiarASala() {
        Platform.runLater(() -> {
            try {
                javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/org/borradoruno/sala-view.fxml"));
                javafx.scene.Parent root = loader.load();
                javafx.scene.Scene scene = new javafx.scene.Scene(root, 800, 600);

                // Forma segura de obtener el stage
                javafx.stage.Stage stage = (javafx.stage.Stage) javafx.stage.Window.getWindows().get(0);
                stage.setScene(scene);
            } catch (java.io.IOException e) {
                e.printStackTrace();
            }
        });
    }
}