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
import org.borradoruno.model.Avatar;
import org.borradoruno.model.EstadoCliente;
import org.borradoruno.sound.SoundManager;
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
            if (j.getNombre().equals(miNombre) && j.isEsAnfitrion()) soyAnfitrion = true;

            HBox item = new HBox(10);
            item.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            item.setStyle("-fx-background-color: #f3f4f6; -fx-padding: 12; -fx-background-radius: 30;");

            Region avatarCircle = new Region();
            avatarCircle.setMinSize(35, 35);
            avatarCircle.setMaxSize(35, 35);
            avatarCircle.setStyle("-fx-background-color: " + mapAvatarToCss(j.getAvatar()) + "; -fx-background-radius: 30;");

            Label name = new Label(j.getNombre());
            name.setStyle("-fx-font-weight: bold; -fx-font-size: 14;");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            String estadoTexto;
            String estadoColor;
            if (j.isEsAnfitrion()) {
                estadoTexto = "Anfitrión";
                estadoColor = "#ef4444";
            } else if (j.isListo()) {
                estadoTexto = "Listo!";
                estadoColor = "#22c55e";
            } else {
                estadoTexto = "Pendiente";
                estadoColor = "#9ca3af";
            }
            Label role = new Label(estadoTexto);
            role.setStyle("-fx-text-fill: " + estadoColor + "; -fx-font-weight: bold;");

            item.getChildren().addAll(avatarCircle, name, spacer, role);
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

        boolean partidaFinalizada = partida.getEstado() == EstadoPartida.FINALIZADA;

        if (partidaFinalizada && soyAnfitrion) {
            btnAccion.setText("NUEVA PARTIDA");
            btnAccion.setStyle("-fx-background-color: #f59e0b; -fx-text-fill: white; -fx-font-weight: bold; -fx-min-width: 250; -fx-min-height: 45; -fx-background-radius: 10; -fx-font-size: 14;");
            btnAccion.setDisable(false);
        } else if (partidaFinalizada) {
            btnAccion.setText("ESPERANDO ANFITRIÓN...");
            btnAccion.setStyle("-fx-background-color: #9ca3af; -fx-text-fill: white; -fx-font-weight: bold; -fx-min-width: 250; -fx-min-height: 45; -fx-background-radius: 10; -fx-font-size: 14;");
            btnAccion.setDisable(true);
        } else if (soyAnfitrion) {
            btnAccion.setText("INICIAR PARTIDA");
            boolean todosListos = partida.getJugadores().stream()
                    .filter(j -> !j.isEsAnfitrion())
                    .allMatch(Jugador::isListo);
            boolean suficientes = partida.getJugadores().size() >= 2;
            boolean puedeIniciar = todosListos && suficientes;
            if (puedeIniciar) {
                btnAccion.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-font-weight: bold; -fx-min-width: 250; -fx-min-height: 45; -fx-background-radius: 10; -fx-font-size: 14;");
                btnAccion.setDisable(false);
            } else {
                btnAccion.setStyle("-fx-background-color: #9ca3af; -fx-text-fill: white; -fx-font-weight: bold; -fx-min-width: 250; -fx-min-height: 45; -fx-background-radius: 10; -fx-font-size: 14;");
                btnAccion.setDisable(true);
            }
        } else {
            Jugador yo = partida.getJugadores().stream()
                    .filter(j -> j.getNombre().equals(miNombre))
                    .findFirst().orElse(null);
            boolean estoyListo = yo != null && yo.isListo();
            if (estoyListo) {
                btnAccion.setText("¡LISTO!");
                btnAccion.setStyle("-fx-background-color: #16a34a; -fx-text-fill: white; -fx-font-weight: bold; -fx-min-width: 250; -fx-min-height: 45; -fx-background-radius: 10; -fx-font-size: 14;");
            } else {
                btnAccion.setText("ESTOY LISTO!!");
                btnAccion.setStyle("-fx-background-color: #22c55e; -fx-text-fill: white; -fx-font-weight: bold; -fx-min-width: 250; -fx-min-height: 45; -fx-background-radius: 10; -fx-font-size: 14;");
            }
            btnAccion.setDisable(false);
        }
    }

    private String mapAvatarToCss(Avatar avatar) {
        if (avatar == null) return "#9ca3af";
        return switch (avatar) {
            case AZUL -> "#3b82f6";
            case AMARILLO -> "#facc15";
            case ROJO -> "#ef4444";
            case VERDE -> "#22c55e";
        };
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
        new Thread(() -> {
            try { Thread.sleep(100); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            ClientSocket.getInstance().desconectar();
            Platform.runLater(() -> cambiarVista(SceneManager.VIEW_REGISTRO));
        }).start();
    }

    @FXML
    private void onAccionPrincipal() {
        SoundManager.getInstance().play(SoundManager.SOUND_CLICK);
        Jugador yo = EstadoCliente.getInstance().getJugadorLocal();
        Partida partida = EstadoCliente.getInstance().getPartidaActual();
        if (yo == null || partida == null) return;

        // Partida terminada: solo el anfitrión puede reiniciar
        if (partida.getEstado() == EstadoPartida.FINALIZADA && yo.isEsAnfitrion()) {
            ClientSocket.getInstance().enviar("REINICIAR_PARTIDA", null);
            return;
        }

        if (yo.isEsAnfitrion()) {
            boolean todosListos = partida.getJugadores().stream()
                    .filter(j -> !j.isEsAnfitrion())
                    .allMatch(Jugador::isListo);

            if (!todosListos) {
                mostrarAlerta("Esperando jugadores", "Aún hay jugadores que no han marcado 'Listo'");
                return;
            }
            if (partida.getJugadores().size() < 2) {
                mostrarAlerta("Faltan jugadores", "Se necesitan al menos 2 jugadores para iniciar");
                return;
            }
            ClientSocket.getInstance().enviar("INICIAR_PARTIDA", null);
        } else {
            ClientSocket.getInstance().enviar("MARCAR_LISTO", null);
        }
    }

    private void mostrarAlerta(String titulo, String mensaje) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.WARNING);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    private void cambiarVista(String vista) {
        ClientSocket.getInstance().removeObserver(this);
        SceneManager.getInstance().cambiarVista(vista);
    }
}
