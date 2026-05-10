package org.borradoruno.controller;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.util.Duration;
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
import org.borradoruno.sound.MusicManager;
import org.borradoruno.sound.SoundManager;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
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

    private static final double TIEMPO_POR_TURNO_SEGUNDOS = 10.0;

    // ── FXML ─────────────────────────────────────────────────────────────────

    @FXML private Pane        paneMano;
    @FXML private Label       lblCartaActiva;
    @FXML private Label       lblTurnoActual;
    @FXML private Label       lblSentido;
    @FXML private Label       lblFlechaArriba;
    @FXML private Label       lblFlechaAbajo;
    @FXML private ProgressBar barraTiempo;
    @FXML private Label       lblTiempoRestante;
    @FXML private Button      btnRobar;

    @FXML private HBox  hboxJugadorIzquierda;
    @FXML private HBox  hboxJugadorDerecha;
    @FXML private HBox  hboxJugadorAbajoDerecha;
    @FXML private Label lblJugadorTu;

    // ── Estado interno ────────────────────────────────────────────────────────

    private Timeline timeline;
    private final Set<String> unoYaNotificados = new HashSet<>();
    private boolean timerExpiredActed = false;

    // ── Ciclo de vida ─────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        ClientSocket.getInstance().addObserver(this);
        ClientSocket.getInstance().enviar("SOLICITAR_ESTADO", null);
        iniciarTimerVisual();
        MusicManager.getInstance().play(MusicManager.MUSIC_GAME);
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

    // ── Actualización principal ───────────────────────────────────────────────

    private void actualizarInterfaz(Partida partida) {
        // Cada vez que el servidor manda estado nuevo, el turno se reinició → resetear flag
        timerExpiredActed = false;
        actualizarPilaDescarte(partida);
        actualizarTurnoYSentido(partida);
        actualizarFlechasSentido(partida);
        renderizarJugadores(partida);
        renderizarMano(EstadoCliente.getInstance().getJugadorLocal());
        detectarAvisoUno(partida);

        boolean esMiTurno = EstadoCliente.getInstance().esMiTurno();
        if (btnRobar != null) {
            btnRobar.setDisable(!esMiTurno);
            btnRobar.setOpacity(esMiTurno ? 1.0 : 0.5);
        }
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

    // ── Timer visual ─────────────────────────────────────────────────────────

    private void iniciarTimerVisual() {
        if (timeline != null) timeline.stop();

        timeline = new Timeline(new KeyFrame(Duration.millis(100), e -> actualizarBarraTiempo()));
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();
    }

    private void actualizarBarraTiempo() {
        Partida partida = EstadoCliente.getInstance().getPartidaActual();
        if (partida == null || partida.getTurnoIniciadoEn() == 0) {
            barraTiempo.setProgress(1.0);
            lblTiempoRestante.setText("--");
            return;
        }

        long ahora = System.currentTimeMillis();
        double transcurrido = (ahora - partida.getTurnoIniciadoEn()) / 1000.0;
        double restante = Math.max(0, TIEMPO_POR_TURNO_SEGUNDOS - transcurrido);
        double progress = Math.max(0, restante / TIEMPO_POR_TURNO_SEGUNDOS);

        barraTiempo.setProgress(progress);
        lblTiempoRestante.setText(restante > 0 ? String.format("%.0fs", Math.ceil(restante)) : "0s");

        String colorBarra;
        if (progress > 0.5)       colorBarra = "#22c55e";
        else if (progress > 0.25) colorBarra = "#f59e0b";
        else                      colorBarra = "#ef4444";
        barraTiempo.setStyle("-fx-accent: " + colorBarra + ";");

        if (restante <= 3 && restante > 2.9 && EstadoCliente.getInstance().esMiTurno()) {
            SoundManager.getInstance().play(SoundManager.SOUND_TURN_WARNING);
        }

        // Tiempo agotado: si es mi turno, robar automáticamente (solo una vez por turno)
        if (restante <= 0 && EstadoCliente.getInstance().esMiTurno() && !timerExpiredActed) {
            timerExpiredActed = true;
            SoundManager.getInstance().play(SoundManager.SOUND_CARD_DRAW);
            ClientSocket.getInstance().enviar("ROBAR_CARTA", null);
        }
    }

    private void detenerTimer() {
        if (timeline != null) {
            timeline.stop();
            timeline = null;
        }
    }

    // ── Alerta UNO ────────────────────────────────────────────────────────────

    private void detectarAvisoUno(Partida partida) {
        for (Jugador j : partida.getJugadores()) {
            if (j.isDijoUNO() && j.getMano().size() == 1
                    && !unoYaNotificados.contains(j.getNombre())) {
                unoYaNotificados.add(j.getNombre());
                mostrarAlertaUno(j);
            }
            if (j.getMano().size() != 1) {
                unoYaNotificados.remove(j.getNombre());
            }
        }
    }

    private void mostrarAlertaUno(Jugador jugador) {
        SoundManager.getInstance().play(SoundManager.SOUND_UNO_ALERT);

        Platform.runLater(() -> {
            Label toast = new Label("¡" + jugador.getNombre() + " dijo UNO! 🎴");
            toast.setStyle(
                    "-fx-background-color: #ef4444;"
                    + "-fx-text-fill: white;"
                    + "-fx-font-size: 24;"
                    + "-fx-font-weight: bold;"
                    + "-fx-padding: 20 40;"
                    + "-fx-background-radius: 30;"
                    + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 20, 0.5, 0, 5);"
            );

            javafx.scene.Parent root = paneMano.getScene().getRoot();
            if (root instanceof javafx.scene.layout.BorderPane bp) {
                javafx.scene.Node center = bp.getCenter();
                if (center instanceof javafx.scene.layout.StackPane sp) {
                    sp.getChildren().add(toast);

                    javafx.animation.FadeTransition fadeIn = new javafx.animation.FadeTransition(
                            Duration.millis(300), toast);
                    fadeIn.setFromValue(0);
                    fadeIn.setToValue(1);

                    javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(
                            Duration.millis(1500));

                    javafx.animation.FadeTransition fadeOut = new javafx.animation.FadeTransition(
                            Duration.millis(300), toast);
                    fadeOut.setFromValue(1);
                    fadeOut.setToValue(0);
                    fadeOut.setOnFinished(e -> sp.getChildren().remove(toast));

                    new javafx.animation.SequentialTransition(fadeIn, pause, fadeOut).play();
                }
            }
        });
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
        } else if (jugador.getMano().size() == 1 && !jugador.isDijoUNO()) {
            Button btnAtrapar = new Button("¡ATRAPAR!");
            btnAtrapar.setStyle(
                    "-fx-background-color: #f97316;"
                    + "-fx-text-fill: white;"
                    + "-fx-font-weight: bold;"
                    + "-fx-padding: 4 8;"
                    + "-fx-background-radius: 15;"
                    + "-fx-font-size: 11;"
                    + "-fx-cursor: hand;"
            );
            final String nombreVictima = jugador.getNombre();
            btnAtrapar.setOnAction(e -> onAtraparUno(nombreVictima));
            contenedor.getChildren().add(btnAtrapar);
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

        boolean esMiTurno = EstadoCliente.getInstance().esMiTurno();
        Partida partida = EstadoCliente.getInstance().getPartidaActual();

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

            boolean jugadaValida = esMiTurno && partida != null && esJugadaValida(c, partida);

            if (jugadaValida) {
                btn.setOnMouseEntered(e -> {
                    javafx.animation.TranslateTransition tt = new javafx.animation.TranslateTransition(
                            Duration.millis(150), btn);
                    tt.setToY(-HOVER_LIFT);
                    tt.play();
                    btn.toFront();
                });

                btn.setOnMouseExited(e -> {
                    javafx.animation.TranslateTransition tt = new javafx.animation.TranslateTransition(
                            Duration.millis(150), btn);
                    tt.setToY(0);
                    tt.play();
                });

                btn.setOnAction(e -> {
                    SoundManager.getInstance().play(SoundManager.SOUND_CARD_PLAY);
                    onTirarCarta(c);
                });
            } else {
                btn.setDisable(true);
                btn.setOpacity(0.4);
            }

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

    private boolean esJugadaValida(Carta jugada, Partida partida) {
        if (jugada.getColor() == Color.NEGRO) return true;
        Color colorActivo = partida.getPilaDescarte().getColorActivo();
        Valor valorActivo = partida.getPilaDescarte().getValorActivo();
        if (colorActivo == Color.NEGRO) return true;
        return jugada.getColor() == colorActivo || jugada.getValor() == valorActivo;
    }

    // ── Acciones ──────────────────────────────────────────────────────────────

    private void onTirarCarta(Carta carta) {
        System.out.println("[RealizarJugada] " + EstadoCliente.getInstance().getNombreLocal()
                + " tirando carta: " + carta.getColor() + " " + carta.getValor());
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
        System.out.println("[RealizarJugada] " + EstadoCliente.getInstance().getNombreLocal() + " roba carta");
        SoundManager.getInstance().play(SoundManager.SOUND_CARD_DRAW);
        ClientSocket.getInstance().enviar("ROBAR_CARTA", null);
    }

    @FXML
    private void onDecirUno() {
        SoundManager.getInstance().play(SoundManager.SOUND_UNO_ALERT);
        ClientSocket.getInstance().enviar("DECIR_UNO", null);
    }

    private void onAtraparUno(String nombreVictima) {
        ClientSocket.getInstance().enviar("ATRAPAR_UNO", nombreVictima);
    }

    @FXML
    private void onAbandonar() {
        detenerTimer();
        ClientSocket.getInstance().enviar("ABANDONAR_SALA", null);
        new Thread(() -> {
            try { Thread.sleep(100); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            ClientSocket.getInstance().desconectar();
            Platform.runLater(() -> cambiarVista(SceneManager.VIEW_REGISTRO));
        }).start();
    }

    private void mostrarFinDeJuego(Partida partida) {
        detenerTimer();
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
