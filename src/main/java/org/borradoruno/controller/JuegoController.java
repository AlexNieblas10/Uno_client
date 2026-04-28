package org.borradoruno.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import org.borradoruno.model.Carta;
import org.borradoruno.model.Color;
import org.borradoruno.model.EstadoCliente;
import org.borradoruno.model.EstadoPartida;
import org.borradoruno.model.Jugador;
import org.borradoruno.model.Partida;
import org.borradoruno.model.Valor;
import org.borradoruno.navigation.SceneManager;
import org.borradoruno.network.ClientSocket;
import org.borradoruno.network.Mensaje;
import org.borradoruno.network.MensajeParser;

public class JuegoController implements ClientSocket.ServerObserver {

    private static final String COLOR_ROJO = "#ef4444";
    private static final String COLOR_AZUL = "#3b82f6";
    private static final String COLOR_VERDE = "#22c55e";
    private static final String COLOR_AMARILLO = "#facc15";
    private static final String COLOR_NEGRO = "#1f2937";

    private static final String ESTILO_CARTA_BASE =
            "-fx-text-fill: white;" +
            "-fx-font-weight: bold;" +
            "-fx-min-width: 70;" +
            "-fx-min-height: 110;" +
            "-fx-background-radius: 10;" +
            "-fx-border-color: white;" +
            "-fx-border-width: 2;";

    @FXML private HBox hboxMano;
    @FXML private Label lblCartaActiva;
    @FXML private Label lblTurnoActual;
    @FXML private Label lblSentido;

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

            if (partida.getEstado() == EstadoPartida.FINALIZADA) {
                mostrarFinDeJuego(partida);
            }
        }
    }

    private void actualizarInterfaz(Partida partida) {
        // Actualizar Pila de Descarte
        Carta activa = partida.getPilaDescarte().getCartas().get(partida.getPilaDescarte().getCartas().size() - 1);
        lblCartaActiva.setText(getValorCorto(activa.getValor()));

        lblCartaActiva.setStyle(
                "-fx-background-color: " + mapColorToCss(activa.getColor()) + ";"
                + "-fx-text-fill: white;"
                + "-fx-font-weight: bold;"
                + "-fx-font-size: 40;"
                + "-fx-min-width: 100;"
                + "-fx-min-height: 150;"
                + "-fx-background-radius: 15;"
                + "-fx-border-color: white;"
                + "-fx-border-radius: 15;"
                + "-fx-border-width: 4;"
                + "-fx-alignment: center;"
                + "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.6), 15, 0, 0, 5);"
        );

        // Actualizar Turno y Sentido
        if (partida.getJugadores().size() > partida.getTurnoActual()) {
            String nombreTurno = partida.getJugadores().get(partida.getTurnoActual()).getNombre();
            String miNombre = EstadoCliente.getInstance().getNombreLocal();
            lblTurnoActual.setText("TURNO DE: " + nombreTurno.toUpperCase());
            lblTurnoActual.setStyle("-fx-font-size: 22; -fx-font-weight: bold; -fx-text-fill: "
                    + (nombreTurno.equals(miNombre) ? COLOR_VERDE : "white") + ";");
        }
        lblSentido.setText("SENTIDO " + partida.getSentidoJuego());

        // Actualizar Mano del Jugador Local
        hboxMano.getChildren().clear();
        Jugador yo = EstadoCliente.getInstance().getJugadorLocal();

        if (yo != null) {
            for (Carta c : yo.getMano()) {
                Button btnCarta = new Button(getValorCorto(c.getValor()));
                btnCarta.setStyle(
                        "-fx-background-color: " + mapColorToCss(c.getColor()) + ";"
                        + ESTILO_CARTA_BASE
                        + "-fx-font-size: 16;"
                        + "-fx-border-radius: 10;"
                        + "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.4), 5, 0, 0, 2);"
                );
                btnCarta.setOnAction(e -> onTirarCarta(c));
                hboxMano.getChildren().add(btnCarta);
            }
        }
    }

    private String getValorCorto(Valor v) {
        return switch (v) {
            case CERO -> "0";
            case UNO -> "1";
            case DOS -> "2";
            case TRES -> "3";
            case CUATRO -> "4";
            case CINCO -> "5";
            case SEIS -> "6";
            case SIETE -> "7";
            case OCHO -> "8";
            case NUEVE -> "9";
            case BLOQUEO -> "🚫";
            case REVERSA -> "🔄";
            case MAS_DOS -> "+2";
            case COMODIN_COLOR -> "🎨";
            case COMODIN_MAS_CUATRO -> "+4";
        };
    }

    private String mapColorToCss(Color color) {
        return switch (color) {
            case ROJO -> COLOR_ROJO;
            case AZUL -> COLOR_AZUL;
            case VERDE -> COLOR_VERDE;
            case AMARILLO -> COLOR_AMARILLO;
            case NEGRO -> COLOR_NEGRO;
        };
    }

    private void onTirarCarta(Carta carta) {
        if (carta.getColor() == Color.NEGRO) {
            mostrarSelectorColor(carta);
        } else {
            ClientSocket.getInstance().enviar("TIRAR_CARTA", carta);
        }
    }

    private void mostrarSelectorColor(Carta comodin) {
        Platform.runLater(() -> {
            javafx.scene.control.Dialog<Color> dialog = new javafx.scene.control.Dialog<>();
            dialog.setTitle("Seleccionar Color");
            dialog.setHeaderText("Elige el color para el comodín");

            javafx.scene.layout.HBox coloresBox = new javafx.scene.layout.HBox(15);
            coloresBox.setAlignment(javafx.geometry.Pos.CENTER);
            coloresBox.setStyle("-fx-padding: 20;");

            javafx.scene.control.Button btnRojo = crearBotonColor("ROJO", COLOR_ROJO);
            javafx.scene.control.Button btnAzul = crearBotonColor("AZUL", COLOR_AZUL);
            javafx.scene.control.Button btnVerde = crearBotonColor("VERDE", COLOR_VERDE);
            javafx.scene.control.Button btnAmarillo = crearBotonColor("AMARILLO", COLOR_AMARILLO);

            coloresBox.getChildren().addAll(btnRojo, btnAzul, btnVerde, btnAmarillo);
            dialog.getDialogPane().setContent(coloresBox);

            final Color[] colorSeleccionado = {null};
            btnRojo.setOnAction(e -> { colorSeleccionado[0] = Color.ROJO; dialog.setResult(Color.ROJO); dialog.close(); });
            btnAzul.setOnAction(e -> { colorSeleccionado[0] = Color.AZUL; dialog.setResult(Color.AZUL); dialog.close(); });
            btnVerde.setOnAction(e -> { colorSeleccionado[0] = Color.VERDE; dialog.setResult(Color.VERDE); dialog.close(); });
            btnAmarillo.setOnAction(e -> { colorSeleccionado[0] = Color.AMARILLO; dialog.setResult(Color.AMARILLO); dialog.close(); });

            dialog.getDialogPane().getButtonTypes().add(javafx.scene.control.ButtonType.CANCEL);

            dialog.showAndWait().ifPresent(color ->
                ClientSocket.getInstance().enviar("TIRAR_COMODIN", new Object[]{comodin, color})
            );
        });
    }

    private javafx.scene.control.Button crearBotonColor(String nombre, String colorCss) {
        javafx.scene.control.Button btn = new javafx.scene.control.Button(nombre);
        btn.setStyle(
                "-fx-background-color: " + colorCss + ";"
                + "-fx-text-fill: white;"
                + "-fx-font-weight: bold;"
                + "-fx-font-size: 18;"
                + "-fx-min-width: 100;"
                + "-fx-min-height: 100;"
                + "-fx-background-radius: 15;"
                + "-fx-cursor: hand;"
        );
        return btn;
    }

    @FXML private void onRobarCarta() { ClientSocket.getInstance().enviar("ROBAR_CARTA", null); }
    @FXML private void onDecirUno() { ClientSocket.getInstance().enviar("DECIR_UNO", null); }

    @FXML
    private void onAbandonar() {
        ClientSocket.getInstance().enviar("ABANDONAR_SALA", null);
        cambiarVista(SceneManager.VIEW_REGISTRO);
    }

    private void mostrarFinDeJuego(Partida partida) {
        Platform.runLater(() -> {
            String ganador = "Desconocido";
            for (Jugador j : partida.getJugadores()) {
                if (j.getMano().isEmpty()) {
                    ganador = j.getNombre();
                    break;
                }
            }

            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
            alert.setTitle("Fin del Juego");
            alert.setHeaderText("¡EL JUEGO HA TERMINADO!");
            alert.setContentText("El ganador es: " + ganador + "\n\n¡Felicidades!");
            alert.getDialogPane().setStyle("-fx-font-size: 14; -fx-font-weight: bold;");
            alert.showAndWait();

            cambiarVista(SceneManager.VIEW_REGISTRO);
        });
    }

    private void cambiarVista(String vista) {
        ClientSocket.getInstance().removeObserver(this);
        SceneManager.getInstance().cambiarVista(vista);
    }
}
