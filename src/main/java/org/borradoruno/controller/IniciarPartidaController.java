package org.borradoruno.controller;

import javafx.application.Platform;
import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.util.Duration;
import org.borradoruno.model.EstadoCliente;
import org.borradoruno.model.EstadoPartida;
import org.borradoruno.model.Jugador;
import org.borradoruno.model.Partida;
import org.borradoruno.navigation.SceneManager;
import org.borradoruno.network.ClientSocket;
import org.borradoruno.network.Mensaje;
import org.borradoruno.network.MensajeParser;
import org.borradoruno.sound.MusicManager;
import org.borradoruno.sound.SoundManager;

public class IniciarPartidaController implements ClientSocket.ServerObserver {

    @FXML private Label lblCodigoSala;
    @FXML private Label lblContadorJugadores;
    @FXML private Label lblEstadoListos;
    @FXML private Button btnConfirmar;
    @FXML private Button btnCancelar;

    private boolean iniciandoPartida = false;
    private PauseTransition timeoutInicio;

    @FXML
    public void initialize() {
        ClientSocket.getInstance().addObserver(this);
        MusicManager.getInstance().play(MusicManager.MUSIC_MENU);

        Partida partida = EstadoCliente.getInstance().getPartidaActual();
        if (partida != null) {
            Platform.runLater(() -> actualizarInfo(partida));
        }
    }

    @FXML
    private void onConfirmarInicio() {
        SoundManager.getInstance().play(SoundManager.SOUND_CLICK);
        if (!validarPartida()) return;

        iniciandoPartida = true;
        bloquearUI(true);
        ClientSocket.getInstance().enviar("INICIAR_PARTIDA", null);
        iniciarTimeoutInicio();
    }

    @FXML
    private void onCancelar() {
        SoundManager.getInstance().play(SoundManager.SOUND_CLICK);
        cancelarTimeoutInicio();
        ClientSocket.getInstance().removeObserver(this);
        SceneManager.getInstance().cambiarVista(SceneManager.VIEW_SALA);
    }

    private void iniciarTimeoutInicio() {
        cancelarTimeoutInicio();

        timeoutInicio = new PauseTransition(Duration.seconds(8));
        timeoutInicio.setOnFinished(e -> {
            if (iniciandoPartida) {
                iniciandoPartida = false;
                bloquearUI(false);
                mostrarError("Servidor sin respuesta",
                    "El servidor no respondió a tiempo.\n\nPosibles causas:\n" +
                    "• Tu conexión se interrumpió\n" +
                    "• El servidor está saturado\n" +
                    "• Otro jugador abandonó la sala\n\n" +
                    "Intenta de nuevo en unos segundos.");
            }
        });
        timeoutInicio.play();
    }

    private void cancelarTimeoutInicio() {
        if (timeoutInicio != null) {
            timeoutInicio.stop();
            timeoutInicio = null;
        }
    }

    private void bloquearUI(boolean bloquear) {
        Platform.runLater(() -> {
            btnConfirmar.setDisable(bloquear);
            btnCancelar.setDisable(bloquear);
            if (bloquear) {
                btnConfirmar.setText("INICIANDO...");
            } else {
                btnConfirmar.setText("INICIAR PARTIDA");
            }
        });
    }

    private boolean validarPartida() {
        Partida partida = EstadoCliente.getInstance().getPartidaActual();
        Jugador yo = EstadoCliente.getInstance().getJugadorLocal();

        if (partida == null || yo == null) {
            mostrarError("Estado Inválido", "No hay una partida activa");
            return false;
        }
        if (!yo.isEsAnfitrion()) {
            mostrarError("Acción no permitida", "Solo el anfitrión puede iniciar la partida");
            return false;
        }
        if (partida.getJugadores().size() < 2) {
            mostrarError("Faltan jugadores", "Se necesitan al menos 2 jugadores para iniciar");
            return false;
        }
        boolean todosListos = partida.getJugadores().stream()
                .filter(j -> !j.isEsAnfitrion())
                .allMatch(Jugador::isListo);
        if (!todosListos) {
            mostrarError("Esperando jugadores", "Aún hay jugadores que no han marcado 'Listo'");
            return false;
        }
        return true;
    }

    private void actualizarInfo(Partida partida) {
        if (lblCodigoSala != null) {
            lblCodigoSala.setText(partida.getCodigoSala());
        }
        if (lblContadorJugadores != null) {
            lblContadorJugadores.setText(partida.getJugadores().size() + "/" + partida.getMaxJugadores());
        }
        if (lblEstadoListos != null) {
            long listos = partida.getJugadores().stream()
                    .filter(j -> j.isEsAnfitrion() || j.isListo())
                    .count();
            lblEstadoListos.setText(listos + " de " + partida.getJugadores().size() + " listos");
        }
    }

    @Override
    public void onMensajeRecibido(Mensaje mensaje) {
        if (mensaje.getTipo().equals("ESTADO_PARTIDA") && iniciandoPartida) {
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

            if (partida == null) {
                return;
            }

            EstadoCliente.getInstance().setPartidaActual(partida);

            if (partida.getEstado() == EstadoPartida.EN_CURSO) {
                cancelarTimeoutInicio();
                iniciandoPartida = false;
                SoundManager.getInstance().play(SoundManager.SOUND_SHUFFLE);
                ClientSocket.getInstance().removeObserver(this);
                SceneManager.getInstance().cambiarVista(SceneManager.VIEW_JUEGO);
            } else {
                Platform.runLater(() -> actualizarInfo(partida));
            }

        } else if (mensaje.getTipo().equals("ERROR") && iniciandoPartida) {
            cancelarTimeoutInicio();
            bloquearUI(false);
            String msg = mensaje.getDatos() != null ? mensaje.getDatos().toString() : "Error desconocido";
            mostrarError("Error del Servidor", msg);
            iniciandoPartida = false;
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
