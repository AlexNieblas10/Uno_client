package org.borradoruno.controllers;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.BorderPane;
import javafx.util.Duration;
import org.borradoruno.models.Carta;
import org.borradoruno.models.Partida;
import org.borradoruno.models.Jugador;
import org.borradoruno.models.Color;
import org.borradoruno.models.Valor;
import org.borradoruno.models.EstadoPartida;
import org.borradoruno.network.ClientSocket;
import org.borradoruno.network.Mensaje;
import com.google.gson.Gson;

import java.util.List;
import java.util.Objects;

public class JuegoController implements ClientSocket.ServerObserver {

    @FXML private HBox hboxMano;
    @FXML private Label lblCartaActiva;
    @FXML private Label lblTurnoActual;
    @FXML private Label lblSentido;
    @FXML private VBox overlayColor;
    @FXML private VBox overlayTurno;
    @FXML private Label lblError;
    @FXML private BorderPane mainPane;

    private Gson gson = new Gson();
    private Carta cartaComodinPendiente;
    private boolean esMiTurno = false;
    private String ultimaManoHash = "";
    private String ultimaCartaActivaHash = "";

    @FXML
    public void initialize() {
        ClientSocket.getInstance().addObserver(this);
        ClientSocket.getInstance().enviar("SOLICITAR_ESTADO", null);
    }

    @Override
    public void onMensajeRecibido(Mensaje mensaje) {
        switch (mensaje.getTipo()) {
            case "ESTADO_PARTIDA":
                String partidaJson = gson.toJson(mensaje.getDatos());
                Partida partida = gson.fromJson(partidaJson, Partida.class);
                actualizarInterfaz(partida);
                if (partida.getEstado() == EstadoPartida.FINALIZADA) {
                    mostrarFinDeJuego(partida);
                }
                break;
            case "ERROR":
                mostrarError(mensaje.getDatos().toString());
                break;
        }
    }

    private void actualizarInterfaz(Partida partida) {
        actualizarMesa(partida);
        actualizarTurno(partida);
        actualizarMano(partida);
    }

    private void actualizarMesa(Partida partida) {
        Carta activa = partida.getPilaDescarte().getCartas().get(partida.getPilaDescarte().getCartas().size() - 1);
        Color colorActivo = partida.getPilaDescarte().getColorActivo();
        String currentHash = colorActivo + "_" + activa.getValor() + "_" + partida.getSentidoJuego();
        
        if (currentHash.equals(ultimaCartaActivaHash)) return;
        ultimaCartaActivaHash = currentHash;

        lblCartaActiva.setText(getValorCorto(activa.getValor()));
        String colorActivaCss = mapColorToCss(colorActivo);
        
        lblCartaActiva.setStyle(
            "-fx-background-color: " + colorActivaCss + ";" +
            "-fx-text-fill: white;" +
            "-fx-font-weight: bold;" +
            "-fx-font-size: 40;" +
            "-fx-min-width: 100;" +
            "-fx-min-height: 150;" +
            "-fx-background-radius: 15;" +
            "-fx-border-color: white;" +
            "-fx-border-radius: 15;" +
            "-fx-border-width: 4;" +
            "-fx-alignment: center;" +
            "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.6), 15, 0, 0, 5);"
        );
        lblSentido.setText("SENTIDO " + partida.getSentidoJuego());
    }

    private void actualizarTurno(Partida partida) {
        if (partida.getJugadores().size() <= partida.getTurnoActual()) return;

        String miNombre = ClientSocket.getInstance().getNombreLocal();
        String nombreTurno = partida.getJugadores().get(partida.getTurnoActual()).getNombre();
        boolean eraMiTurnoAnterior = esMiTurno;
        esMiTurno = nombreTurno.equals(miNombre);

        lblTurnoActual.setText("TURNO DE: " + nombreTurno.toUpperCase());
        lblTurnoActual.setStyle("-fx-font-size: 22; -fx-font-weight: bold; -fx-text-fill: " + 
            (esMiTurno ? "#22c55e" : "white") + ";");

        // Mostrar overlay si acaba de empezar mi turno
        if (esMiTurno && !eraMiTurnoAnterior) {
            mostrarOverlayTurno();
        }

        // Efecto visual en el panel principal si es mi turno
        mainPane.setStyle("-fx-background-color: linear-gradient(to bottom, " + 
            (esMiTurno ? "#fca5a5, #f87171" : "#d1d5db, #9ca3af") + ");" +
            (esMiTurno ? "-fx-border-color: #22c55e; -fx-border-width: 5; -fx-border-style: solid;" : ""));
    }

    private void actualizarMano(Partida partida) {
        String miNombre = ClientSocket.getInstance().getNombreLocal();
        Jugador yo = partida.getJugadores().stream()
                .filter(j -> j.getNombre().equals(miNombre))
                .findFirst().orElse(null);

        if (yo == null) return;

        // Verificar si la mano cambió para evitar rebuild innecesario
        String manoHash = yo.getMano().size() + "_" + esMiTurno + "_" + ultimaCartaActivaHash;
        if (manoHash.equals(ultimaManoHash)) return;
        ultimaManoHash = manoHash;

        hboxMano.getChildren().clear();
        Color colorActivo = partida.getPilaDescarte().getColorActivo();
        Valor valorActivo = partida.getPilaDescarte().getValorActivo();

        for (Carta c : yo.getMano()) {
            Button btnCarta = new Button(getValorCorto(c.getValor()));
            boolean valida = esMiTurno && esJugadaValida(c, colorActivo, valorActivo);
            
            String colorCss = mapColorToCss(c.getColor());
            btnCarta.setStyle(
                "-fx-background-color: " + colorCss + ";" +
                "-fx-text-fill: white;" +
                "-fx-font-weight: bold;" +
                "-fx-font-size: 16;" +
                "-fx-min-width: 70;" +
                "-fx-min-height: 110;" +
                "-fx-background-radius: 10;" +
                "-fx-border-color: white;" +
                "-fx-border-radius: 10;" +
                "-fx-border-width: 2;" +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.4), 5, 0, 0, 2);" +
                (valida ? "" : "-fx-opacity: 0.5;")
            );
            
            if (valida) {
                btnCarta.setOnAction(e -> onTirarCarta(c));
                btnCarta.setOnMouseEntered(e -> btnCarta.setStyle(btnCarta.getStyle() + "-fx-translate-y: -10;"));
                btnCarta.setOnMouseExited(e -> btnCarta.setStyle(btnCarta.getStyle().replace("-fx-translate-y: -10;", "")));
            } else {
                btnCarta.setDisable(true);
            }
            
            hboxMano.getChildren().add(btnCarta);
        }
    }

    private boolean esJugadaValida(Carta jugada, Color colorActivo, Valor valorActivo) {
        return jugada.getColor() == Color.NEGRO || 
               jugada.getColor() == colorActivo || 
               jugada.getValor() == valorActivo;
    }

    private void onTirarCarta(Carta carta) {
        if (carta.getColor() == Color.NEGRO) {
            cartaComodinPendiente = carta;
            overlayColor.setVisible(true);
        } else {
            ClientSocket.getInstance().enviar("TIRAR_CARTA", carta);
        }
    }

    @FXML
    private void onColorSelected(javafx.event.ActionEvent event) {
        Button btn = (Button) event.getSource();
        Color color = Color.valueOf(btn.getUserData().toString());
        overlayColor.setVisible(false);
        if (cartaComodinPendiente != null) {
            ClientSocket.getInstance().enviar("TIRAR_COMODIN", new Object[]{cartaComodinPendiente, color});
            cartaComodinPendiente = null;
        }
    }

    @FXML
    private void onCancelarColor() {
        overlayColor.setVisible(false);
        cartaComodinPendiente = null;
    }

    private void mostrarOverlayTurno() {
        overlayTurno.setVisible(true);
        FadeTransition ft = new FadeTransition(Duration.millis(500), overlayTurno);
        ft.setFromValue(0.0);
        ft.setToValue(1.0);
        ft.play();

        PauseTransition pause = new PauseTransition(Duration.seconds(1.5));
        pause.setOnFinished(e -> {
            FadeTransition out = new FadeTransition(Duration.millis(500), overlayTurno);
            out.setFromValue(1.0);
            out.setToValue(0.0);
            out.setOnFinished(ev -> overlayTurno.setVisible(false));
            out.play();
        });
        pause.play();
    }

    private void mostrarError(String mensaje) {
        Platform.runLater(() -> {
            lblError.setText(mensaje.toUpperCase());
            lblError.setVisible(true);
            
            FadeTransition ft = new FadeTransition(Duration.millis(300), lblError);
            ft.setFromValue(0.0);
            ft.setToValue(1.0);
            ft.play();

            PauseTransition pause = new PauseTransition(Duration.seconds(3));
            pause.setOnFinished(e -> {
                FadeTransition out = new FadeTransition(Duration.millis(500), lblError);
                out.setFromValue(1.0);
                out.setToValue(0.0);
                out.setOnFinished(ev -> lblError.setVisible(false));
                out.play();
            });
            pause.play();
        });
    }

    private String getValorCorto(Valor v) {
        return switch (v) {
            case CERO -> "0"; case UNO -> "1"; case DOS -> "2"; case TRES -> "3";
            case CUATRO -> "4"; case CINCO -> "5"; case SEIS -> "6";
            case SIETE -> "7"; case OCHO -> "8"; case NUEVE -> "9";
            case BLOQUEO -> "🚫"; case REVERSA -> "🔄"; case MAS_DOS -> "+2";
            case COMODIN_COLOR -> "🎨"; case COMODIN_MAS_CUATRO -> "+4";
        };
    }

    private String mapColorToCss(Color color) {
        return switch (color) {
            case ROJO -> "#ef4444";
            case AZUL -> "#3b82f6";
            case VERDE -> "#22c55e";
            case AMARILLO -> "#facc15";
            case NEGRO -> "#1f2937";
        };
    }

    @FXML private void onRobarCarta() { ClientSocket.getInstance().enviar("ROBAR_CARTA", null); }
    @FXML private void onDecirUno() { ClientSocket.getInstance().enviar("DECIR_UNO", null); }
    @FXML private void onAbandonar() {
        ClientSocket.getInstance().enviar("ABANDONAR_SALA", null);
        regresarARegistro();
    }

    private void regresarARegistro() {
        Platform.runLater(() -> {
            try {
                javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/org/borradoruno/registro-view.fxml"));
                javafx.scene.Parent root = loader.load();
                javafx.scene.Scene scene = new javafx.scene.Scene(root, 800, 600);
                javafx.stage.Stage stage = (javafx.stage.Stage) lblTurnoActual.getScene().getWindow();
                stage.setScene(scene);
            } catch (java.io.IOException e) { e.printStackTrace(); }
        });
    }

    private void mostrarFinDeJuego(Partida partida) {
        Platform.runLater(() -> {
            String ganador = partida.getJugadores().stream()
                    .filter(j -> j.getMano().isEmpty())
                    .map(Jugador::getNombre)
                    .findFirst().orElse("Desconocido");

            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
            alert.setTitle("Fin del Juego");
            alert.setHeaderText("¡EL JUEGO HA TERMINADO!");
            alert.setContentText("El ganador es: " + ganador + "\n\n¡Felicidades!");
            alert.getDialogPane().setStyle("-fx-font-size: 14; -fx-font-weight: bold;");
            alert.showAndWait();
            regresarARegistro();
        });
    }
}