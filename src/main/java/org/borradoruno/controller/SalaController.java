package org.borradoruno.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.borradoruno.model.EstadoCliente;
import org.borradoruno.model.EstadoPartida;
import org.borradoruno.model.Jugador;
import org.borradoruno.model.Partida;
import org.borradoruno.navigation.SceneManager;
import org.borradoruno.network.ClientSocket;
import org.borradoruno.network.Mensaje;
import org.borradoruno.network.MensajeParser;

public class SalaController implements ClientSocket.ServerObserver {

    @FXML private VBox vboxJugadores;
    @FXML private VBox vboxConfig;
    @FXML private Button btnAccion;
    @FXML private Label lblEstado;
    @FXML private Label lblContador;
    @FXML private TextField txtCodigoSala;
    @FXML private Button btnMax2, btnMax3, btnMax4;

    @FXML
    public void initialize() {
        ClientSocket.getInstance().addObserver(this);
        ClientSocket.getInstance().enviar("SOLICITAR_ESTADO", null);
    }

    @Override
    public void onMensajeRecibido(Mensaje mensaje) {
        if (mensaje.getTipo().equals("ESTADO_PARTIDA")) {
            Partida partida = MensajeParser.parsearPartida(mensaje.getDatos());
            EstadoCliente.getInstance().setPartidaActual(partida);
            Platform.runLater(() -> actualizarInterfaz(partida));

            if (partida.getEstado() == EstadoPartida.EN_CURSO) {
                cambiarVista(SceneManager.VIEW_JUEGO);
            }
        }
    }

    private void actualizarInterfaz(Partida partida) {
        vboxJugadores.getChildren().clear();
        String miNombre = EstadoCliente.getInstance().getNombreLocal();
        boolean soyAnfitrion = false;

        for (Jugador j : partida.getJugadores()) {
            if (j.getNombre().equals(miNombre) && j.isEsAnfitrion()) {
                soyAnfitrion = true;
            }

            HBox item = new HBox(10);
            item.setStyle("-fx-background-color: white; -fx-padding: 10; -fx-background-radius: 5; -fx-alignment: center-left; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 1);");

            Label name = new Label(j.getNombre());
            name.setStyle("-fx-font-weight: bold; -fx-font-size: 14;");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Label role = new Label(j.isEsAnfitrion() ? "Anfitrión" : "Listo!");
            role.setStyle("-fx-text-fill: " + (j.isEsAnfitrion() ? "#ef4444" : "#22c55e") + "; -fx-font-weight: bold;");

            item.getChildren().addAll(name, spacer, role);
            vboxJugadores.getChildren().add(item);
        }

        if (lblContador != null) {
            lblContador.setText("(" + partida.getJugadores().size() + "/" + partida.getMaxJugadores() + ")");
        }
        if (txtCodigoSala != null) {
            txtCodigoSala.setText(partida.getCodigoSala());
        }

        actualizarEstiloBotonesMax(partida.getMaxJugadores());

        if (vboxConfig != null) {
            vboxConfig.setVisible(soyAnfitrion);
            vboxConfig.setManaged(soyAnfitrion);
        }

        if (soyAnfitrion) {
            btnAccion.setText("INICIAR PARTIDA");
            btnAccion.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-font-weight: bold; -fx-min-width: 250; -fx-background-radius: 10;");
        } else {
            btnAccion.setText("ESTOY LISTO!!");
            btnAccion.setStyle("-fx-background-color: #22c55e; -fx-text-fill: white; -fx-font-weight: bold; -fx-min-width: 250; -fx-background-radius: 10;");
        }
    }

    private void actualizarEstiloBotonesMax(int max) {
        String base = "-fx-min-width: 40; -fx-background-radius: 5; -fx-font-weight: bold;";
        String selected = base + "-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-border-color: #1d4ed8; -fx-border-width: 2;";
        String unselected = base + "-fx-background-color: #e5e7eb; -fx-text-fill: #4b5563;";

        if (btnMax2 != null) btnMax2.setStyle(max == 2 ? selected : unselected);
        if (btnMax3 != null) btnMax3.setStyle(max == 3 ? selected : unselected);
        if (btnMax4 != null) btnMax4.setStyle(max == 4 ? selected : unselected);
    }

    @FXML private void onSetMax2() { ClientSocket.getInstance().enviar("SET_MAX_JUGADORES", 2); }
    @FXML private void onSetMax3() { ClientSocket.getInstance().enviar("SET_MAX_JUGADORES", 3); }
    @FXML private void onSetMax4() { ClientSocket.getInstance().enviar("SET_MAX_JUGADORES", 4); }

    @FXML
    private void onAbandonar() {
        ClientSocket.getInstance().enviar("ABANDONAR_SALA", null);
        cambiarVista(SceneManager.VIEW_REGISTRO);
    }

    @FXML
    private void onAccionPrincipal() {
        ClientSocket.getInstance().enviar("INICIAR_PARTIDA", null);
    }

    private void cambiarVista(String vista) {
        ClientSocket.getInstance().removeObserver(this);
        SceneManager.getInstance().cambiarVista(vista);
    }
}
