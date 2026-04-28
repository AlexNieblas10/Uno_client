package org.borradoruno.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import org.borradoruno.config.ServerConfig;
import org.borradoruno.model.EstadoCliente;
import org.borradoruno.navigation.SceneManager;
import org.borradoruno.network.ClientSocket;
import org.borradoruno.network.Mensaje;

import java.io.IOException;

public class RegistroController implements ClientSocket.ServerObserver {

    @FXML private TextField txtApodo;
    @FXML private TextField txtIp;

    private boolean intentandoLogin = false;

    @FXML
    public void initialize() {
        ClientSocket.getInstance().addObserver(this);

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
        if (!validarEntradas()) return;

        String apodo = txtApodo.getText().trim();
        String ip = txtIp.getText().trim();

        try {
            conectarAlServidor();
            intentandoLogin = true;
            EstadoCliente.getInstance().setNombreLocal(apodo);
            ClientSocket.getInstance().enviar("CREATE", apodo);
            ServerConfig.setServerIP(ip);
        } catch (IOException e) {
            mostrarError("Error de Conexión", "No se pudo conectar al servidor en " + ip);
            intentandoLogin = false;
        }
    }

    @FXML
    private void onUnirsePartida() {
        if (!validarEntradas()) return;

        String apodo = txtApodo.getText().trim();
        String ip = txtIp.getText().trim();

        try {
            conectarAlServidor();
            intentandoLogin = true;
            EstadoCliente.getInstance().setNombreLocal(apodo);
            ClientSocket.getInstance().enviar("JOIN", apodo);
            ServerConfig.setServerIP(ip);
        } catch (IOException e) {
            mostrarError("Error de Conexión", "No se pudo conectar al servidor en " + ip);
            intentandoLogin = false;
        }
    }

    @Override
    public void onMensajeRecibido(Mensaje mensaje) {
        if (mensaje.getTipo().equals("ESTADO_PARTIDA") && intentandoLogin) {
            cambiarVista(SceneManager.VIEW_SALA);
            intentandoLogin = false;
        } else if (mensaje.getTipo().equals("ERROR") && intentandoLogin) {
            String errorMsg = mensaje.getDatos() != null ? mensaje.getDatos().toString() : "Error desconocido";
            mostrarError("Error del Servidor", errorMsg);
            intentandoLogin = false;
        }
    }

    private boolean validarEntradas() {
        String apodo = txtApodo.getText().trim();
        String ip = txtIp.getText().trim();

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
        if (ip.isEmpty()) {
            mostrarError("Dirección Inválida", "La dirección del servidor no puede estar vacía");
            return false;
        }
        if (ip.equals("localhost") || ip.equals("127.0.0.1")) {
            return true;
        }
        String ipv4Pattern = "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}"
                + "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";
        String hostnamePattern = "^([a-zA-Z0-9]([a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?\\.)+[a-zA-Z]{2,}$";
        if (!ip.matches(ipv4Pattern) && !ip.matches(hostnamePattern)) {
            mostrarError("Dirección Inválida",
                    "Formato inválido.\nUsa: 192.168.1.1, localhost o example.com");
            return false;
        }
        return true;
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

    private void cambiarVista(String vista) {
        ClientSocket.getInstance().removeObserver(this);
        SceneManager.getInstance().cambiarVista(vista);
    }
}
