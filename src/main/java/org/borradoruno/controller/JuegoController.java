package org.borradoruno.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import org.borradoruno.model.Avatar;
import org.borradoruno.model.Carta;
import org.borradoruno.model.Color;
import org.borradoruno.model.EstadoCliente;
import org.borradoruno.model.EstadoPartida;
import org.borradoruno.model.Jugador;
import org.borradoruno.model.Partida;
import org.borradoruno.model.Sentido;
import org.borradoruno.model.Valor;
import org.borradoruno.navigation.SceneManager;
import org.borradoruno.network.ClientSocket;
import org.borradoruno.network.Mensaje;
import org.borradoruno.network.MensajeParser;
import org.borradoruno.sound.SoundManager;

import java.util.List;
import java.util.stream.Collectors;

public class JuegoController implements ClientSocket.ServerObserver {

    private static final String COLOR_ROJO     = "#ef4444";
    private static final String COLOR_AZUL     = "#3b82f6";
    private static final String COLOR_VERDE    = "#22c55e";
    private static final String COLOR_AMARILLO = "#facc15";

    private static final double CARD_WIDTH   = 70;
    private static final double CARD_HEIGHT  = 110;
    private static final double CARD_OVERLAP = 35;
    private static final double HOVER_LIFT   = 25;

    // Pila y turno
    @FXML private Pane  paneMano;
    @FXML private Label lblCartaActiva;
    @FXML private Label lblTurnoActual;
    @FXML private Label lblSentido;
    @FXML private Label lblFlechaArriba;
    @FXML private Label lblFlechaAbajo;

    // Esquinas con otros jugadores
    @FXML private HBox  hboxJugadorIzquierda;
    @FXML private HBox  hboxJugadorDerecha;
    @FXML private HBox  hboxJugadorAbajoDerecha;
    @FXML private Label lblJugadorTu;

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
        actualizarPilaDescarte(partida);
        actualizarTurnoYSentido(partida);
        actualizarFlechasSentido(partida);
        renderizarJugadores(partida);
        renderizarMano(EstadoCliente.getInstance().getJugadorLocal());
    }

    // ── Pila central ─────────────────────────────────────────────────────────

    private void actualizarPilaDescarte(Partida partida) {
        Carta activa = partida.getPilaDescarte().getCartas()
                .get(partida.getPilaDescarte().getCartas().size() - 1);

        Color colorMostrado;
        if (activa.getColor() == Color.NEGRO) {
            colorMostrado = partida.getPilaDescarte().getColorActivo();
            if (colorMostrado == null || colorMostrado == Color.NEGRO) colorMostrado = Color.ROJO;
        } else {
            colorMostrado = activa.getColor();
        }

        String simbolo = activa.getColor() == Color.NEGRO
                ? getValorCorto(activa.getValor()) + "★"
                : getValorCorto(activa.getValor());

        lblCartaActiva.setText(simbolo);
        lblCartaActiva.setStyle(
                "-fx-background-color: " + mapColorToCss(colorMostrado) + ";"
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
    }

    // ── Turno y sentido ───────────────────────────────────────────────────────

    private void actualizarTurnoYSentido(Partida partida) {
        if (partida.getJugadores().size() > partida.getTurnoActual()) {
            String nombreTurno = partida.getJugadores().get(partida.getTurnoActual()).getNombre();
            String miNombre = EstadoCliente.getInstance().getNombreLocal();
            lblTurnoActual.setText("TURNO DE: " + nombreTurno.toUpperCase());
            lblTurnoActual.setStyle("-fx-font-size: 22; -fx-font-weight: bold; -fx-text-fill: "
                    + (nombreTurno.equals(miNombre) ? COLOR_VERDE : "white") + ";");
        }
    }

    private void actualizarFlechasSentido(Partida partida) {
        boolean horario = partida.getSentidoJuego() == Sentido.HORARIO;
        lblFlechaArriba.setText(horario ? "▶" : "◀");
        lblFlechaAbajo.setText(horario ? "◀" : "▶");
        lblSentido.setText("SENTIDO " + (horario ? "HORARIO" : "ANTIHORARIO"));
    }

    // ── Jugadores en las esquinas ─────────────────────────────────────────────

    private void renderizarJugadores(Partida partida) {
        hboxJugadorIzquierda.getChildren().clear();
        hboxJugadorDerecha.getChildren().clear();
        hboxJugadorAbajoDerecha.getChildren().clear();

        String miNombre = EstadoCliente.getInstance().getNombreLocal();

        Jugador jugadorEnTurno = (partida.getTurnoActual() < partida.getJugadores().size())
                ? partida.getJugadores().get(partida.getTurnoActual())
                : null;

        List<Jugador> otros = partida.getJugadores().stream()
                .filter(j -> !j.getNombre().equals(miNombre))
                .collect(Collectors.toList());

        switch (otros.size()) {
            case 1 -> colocarJugadorEn(hboxJugadorIzquierda, otros.get(0), jugadorEnTurno);
            case 2 -> {
                colocarJugadorEn(hboxJugadorIzquierda, otros.get(0), jugadorEnTurno);
                colocarJugadorEn(hboxJugadorDerecha,   otros.get(1), jugadorEnTurno);
            }
            case 3 -> {
                colocarJugadorEn(hboxJugadorIzquierda,    otros.get(0), jugadorEnTurno);
                colocarJugadorEn(hboxJugadorDerecha,      otros.get(1), jugadorEnTurno);
                colocarJugadorEn(hboxJugadorAbajoDerecha, otros.get(2), jugadorEnTurno);
            }
        }

        // Etiqueta "Tú" en la esquina inferior izquierda
        Jugador yo = partida.getJugadores().stream()
                .filter(j -> j.getNombre().equals(miNombre))
                .findFirst().orElse(null);

        if (yo != null) {
            boolean miTurno = jugadorEnTurno != null && jugadorEnTurno.getNombre().equals(miNombre);
            lblJugadorTu.setText("Tú · " + yo.getMano().size() + " cartas");
            lblJugadorTu.setStyle(
                    "-fx-background-color: " + (miTurno ? "#22c55e" : "white") + ";"
                    + "-fx-text-fill: " + (miTurno ? "white" : "black") + ";"
                    + "-fx-font-weight: bold;"
                    + "-fx-padding: 8 12;"
                    + "-fx-background-radius: 20;"
                    + "-fx-font-size: 14;"
                    + (miTurno ? "-fx-effect: dropshadow(gaussian, #22c55e, 15, 0.6, 0, 0);" : "")
            );
            lblJugadorTu.setVisible(true);
            lblJugadorTu.setManaged(true);
        }
    }

    private void colocarJugadorEn(HBox contenedor, Jugador jugador, Jugador jugadorEnTurno) {
        boolean esSuTurno = jugadorEnTurno != null
                && jugadorEnTurno.getNombre().equals(jugador.getNombre());

        Region avatar = new Region();
        avatar.setMinSize(30, 30);
        avatar.setMaxSize(30, 30);
        avatar.setStyle(
                "-fx-background-color: " + mapAvatarToCss(jugador.getAvatar()) + ";"
                + "-fx-background-radius: 30;"
        );

        Label info = new Label(jugador.getNombre() + " · " + jugador.getMano().size());
        info.setStyle(
                "-fx-background-color: " + (esSuTurno ? "#22c55e" : "rgba(255,255,255,0.9)") + ";"
                + "-fx-text-fill: " + (esSuTurno ? "white" : "black") + ";"
                + "-fx-font-weight: bold;"
                + "-fx-padding: 6 12;"
                + "-fx-background-radius: 20;"
                + "-fx-font-size: 13;"
                + (esSuTurno ? "-fx-effect: dropshadow(gaussian, #22c55e, 12, 0.5, 0, 0);" : "")
        );

        contenedor.getChildren().addAll(avatar, info);

        if (jugador.getMano().size() == 1 && jugador.isDijoUNO()) {
            Label unoTag = new Label("¡UNO!");
            unoTag.setStyle(
                    "-fx-background-color: #ef4444;"
                    + "-fx-text-fill: white;"
                    + "-fx-font-weight: bold;"
                    + "-fx-padding: 4 8;"
                    + "-fx-background-radius: 15;"
                    + "-fx-font-size: 11;"
            );
            contenedor.getChildren().add(unoTag);
        }

        contenedor.setVisible(true);
        contenedor.setManaged(true);
    }

    private String mapAvatarToCss(Avatar avatar) {
        if (avatar == null) return "#9ca3af";
        return switch (avatar) {
            case AZUL     -> "#3b82f6";
            case AMARILLO -> "#facc15";
            case ROJO     -> "#ef4444";
            case VERDE    -> "#22c55e";
        };
    }

    // ── Mano del jugador local ────────────────────────────────────────────────

    private void renderizarMano(Jugador yo) {
        paneMano.getChildren().clear();
        if (yo == null || yo.getMano().isEmpty()) return;

        List<Carta> mano = yo.getMano();
        int total = mano.size();

        double anchoTotal = CARD_WIDTH + (total - 1) * (CARD_WIDTH - CARD_OVERLAP);
        double paneWidth  = paneMano.getWidth() > 0 ? paneMano.getWidth() : 800;
        double startX     = (paneWidth - anchoTotal) / 2;

        for (int i = 0; i < total; i++) {
            Carta c = mano.get(i);
            Button btn = crearBotonCarta(c);

            btn.setLayoutX(startX + i * (CARD_WIDTH - CARD_OVERLAP));
            btn.setLayoutY(20);

            btn.setOnMouseEntered(e -> {
                javafx.animation.TranslateTransition tt = new javafx.animation.TranslateTransition(
                        javafx.util.Duration.millis(150), btn);
                tt.setToY(-HOVER_LIFT);
                tt.play();
                btn.toFront();
            });

            btn.setOnMouseExited(e -> {
                javafx.animation.TranslateTransition tt = new javafx.animation.TranslateTransition(
                        javafx.util.Duration.millis(150), btn);
                tt.setToY(0);
                tt.play();
            });

            btn.setOnAction(e -> {
                SoundManager.getInstance().play(SoundManager.SOUND_CARD_PLAY);
                onTirarCarta(c);
            });

            paneMano.getChildren().add(btn);
        }
    }

    private Button crearBotonCarta(Carta c) {
        Button btn = new Button(getValorCorto(c.getValor()));

        String bg = (c.getColor() == Color.NEGRO)
                ? "-fx-background-color: linear-gradient(to bottom right, "
                        + "#ef4444 0%, #ef4444 25%, "
                        + "#facc15 25%, #facc15 50%, "
                        + "#22c55e 50%, #22c55e 75%, "
                        + "#3b82f6 75%, #3b82f6 100%);"
                : "-fx-background-color: " + mapColorToCss(c.getColor()) + ";";

        btn.setStyle(bg
                + "-fx-text-fill: white;"
                + "-fx-font-weight: bold;"
                + "-fx-font-size: 18;"
                + "-fx-min-width: " + CARD_WIDTH + ";"
                + "-fx-pref-width: " + CARD_WIDTH + ";"
                + "-fx-min-height: " + CARD_HEIGHT + ";"
                + "-fx-pref-height: " + CARD_HEIGHT + ";"
                + "-fx-background-radius: 10;"
                + "-fx-border-color: white;"
                + "-fx-border-radius: 10;"
                + "-fx-border-width: 3;"
                + "-fx-cursor: hand;"
                + "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.5), 8, 0, 0, 4);"
        );
        return btn;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String getValorCorto(Valor v) {
        return switch (v) {
            case CERO -> "0"; case UNO -> "1"; case DOS -> "2"; case TRES -> "3";
            case CUATRO -> "4"; case CINCO -> "5"; case SEIS -> "6"; case SIETE -> "7";
            case OCHO -> "8"; case NUEVE -> "9";
            case BLOQUEO -> "🚫"; case REVERSA -> "🔄"; case MAS_DOS -> "+2";
            case COMODIN_COLOR -> "🎨"; case COMODIN_MAS_CUATRO -> "+4";
        };
    }

    private String mapColorToCss(Color color) {
        return switch (color) {
            case ROJO -> COLOR_ROJO; case AZUL -> COLOR_AZUL;
            case VERDE -> COLOR_VERDE; case AMARILLO -> COLOR_AMARILLO;
            case NEGRO -> "#1f2937";
        };
    }

    // ── Acciones ──────────────────────────────────────────────────────────────

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

            javafx.scene.control.Button btnRojo     = crearBotonColor("ROJO",     COLOR_ROJO);
            javafx.scene.control.Button btnAzul     = crearBotonColor("AZUL",     COLOR_AZUL);
            javafx.scene.control.Button btnVerde    = crearBotonColor("VERDE",    COLOR_VERDE);
            javafx.scene.control.Button btnAmarillo = crearBotonColor("AMARILLO", COLOR_AMARILLO);

            coloresBox.getChildren().addAll(btnRojo, btnAzul, btnVerde, btnAmarillo);
            dialog.getDialogPane().setContent(coloresBox);
            dialog.getDialogPane().getButtonTypes().add(javafx.scene.control.ButtonType.CANCEL);

            btnRojo.setOnAction(e     -> { dialog.setResult(Color.ROJO);     dialog.close(); });
            btnAzul.setOnAction(e     -> { dialog.setResult(Color.AZUL);     dialog.close(); });
            btnVerde.setOnAction(e    -> { dialog.setResult(Color.VERDE);    dialog.close(); });
            btnAmarillo.setOnAction(e -> { dialog.setResult(Color.AMARILLO); dialog.close(); });

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

    @FXML
    private void onRobarCarta() {
        SoundManager.getInstance().play(SoundManager.SOUND_CARD_DRAW);
        ClientSocket.getInstance().enviar("ROBAR_CARTA", null);
    }

    @FXML
    private void onDecirUno() {
        SoundManager.getInstance().play(SoundManager.SOUND_UNO_ALERT);
        ClientSocket.getInstance().enviar("DECIR_UNO", null);
    }

    @FXML
    private void onAbandonar() {
        ClientSocket.getInstance().enviar("ABANDONAR_SALA", null);
        new Thread(() -> {
            try { Thread.sleep(100); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            ClientSocket.getInstance().desconectar();
            Platform.runLater(() -> cambiarVista(SceneManager.VIEW_REGISTRO));
        }).start();
    }

    private void mostrarFinDeJuego(Partida partida) {
        SoundManager.getInstance().play(SoundManager.SOUND_WIN);
        Platform.runLater(() -> {
            String ganador = "Desconocido";
            for (Jugador j : partida.getJugadores()) {
                if (j.getMano().isEmpty()) { ganador = j.getNombre(); break; }
            }

            String miNombre = EstadoCliente.getInstance().getNombreLocal();
            boolean ganaste = ganador.equals(miNombre);

            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                    javafx.scene.control.Alert.AlertType.INFORMATION);
            alert.setTitle("Fin del Juego");
            alert.setHeaderText(ganaste ? "¡GANASTE! 🎉" : "¡" + ganador + " ha ganado!");
            alert.setContentText(ganaste
                    ? "¡Felicidades, has ganado la partida!\n\nVolverás a la sala."
                    : "El ganador fue " + ganador + ".\n\nVolverás a la sala.");
            alert.getDialogPane().setStyle("-fx-font-size: 14;");
            alert.showAndWait();

            cambiarVista(SceneManager.VIEW_SALA);
        });
    }

    private void cambiarVista(String vista) {
        ClientSocket.getInstance().removeObserver(this);
        SceneManager.getInstance().cambiarVista(vista);
    }
}
