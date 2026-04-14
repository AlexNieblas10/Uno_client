package org.borradoruno.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
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
    }

    private void conectarAlServidor() {
        try {
            String ip = txtIp.getText();
            ClientSocket.getInstance().conectar(ip, 12345);
        } catch (IOException e) {
            System.err.println("No se pudo conectar al servidor en " + txtIp.getText());
        }
    }

    @FXML
    private void onCrearPartida() {
        String apodo = txtApodo.getText();
        if (!apodo.isEmpty()) {
            conectarAlServidor();
            intentandoLogin = true;
            ClientSocket.getInstance().setNombreLocal(apodo);
            ClientSocket.getInstance().enviar("CREATE", apodo);
        }
    }

    @FXML
    private void onUnirsePartida() {
        String apodo = txtApodo.getText();
        if (!apodo.isEmpty()) {
            conectarAlServidor();
            intentandoLogin = true;
            ClientSocket.getInstance().setNombreLocal(apodo);
            ClientSocket.getInstance().enviar("JOIN", apodo);
        }
    }

    @Override
    public void onMensajeRecibido(Mensaje mensaje) {
        if (mensaje.getTipo().equals("ESTADO_PARTIDA") && intentandoLogin) {
            cambiarASala();
            intentandoLogin = false; 
        } else if (mensaje.getTipo().equals("ERROR") && intentandoLogin) {
            System.err.println("Error del servidor: " + mensaje.getDatos());
            intentandoLogin = false;
        }
    }

    private void cambiarASala() {
        javafx.application.Platform.runLater(() -> {
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