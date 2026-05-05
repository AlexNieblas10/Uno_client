package org.borradoruno.controller;

import javafx.fxml.FXML;
import org.borradoruno.network.ClientSocket;
import org.borradoruno.network.Mensaje;
import org.borradoruno.sound.MusicManager;

/**
 * STUB — Caso de uso "Registrar Jugador" movido a la rama feature/registrar-jugador.
 * Esta clase es un placeholder para mantener la compilación en main.
 * Ver implementación completa en: feature/registrar-jugador
 */
public class RegistroController implements ClientSocket.ServerObserver {

    @FXML
    public void initialize() {
        MusicManager.getInstance().play(MusicManager.MUSIC_MENU);
    }

    @FXML
    private void onCrearPartida() {
        // Implementación en rama feature/registrar-jugador
    }

    @FXML
    private void onUnirsePartida() {
        // Implementación en rama feature/registrar-jugador
    }

    @Override
    public void onMensajeRecibido(Mensaje mensaje) {
        // Implementación en rama feature/registrar-jugador
    }
}
