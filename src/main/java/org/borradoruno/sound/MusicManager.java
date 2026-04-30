package org.borradoruno.sound;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;

import java.util.HashMap;
import java.util.Map;

/**
 * Gestor de música de fondo.
 * Usa MediaPlayer (no AudioClip) para loop infinito y archivos largos (.mp3).
 */
public class MusicManager {

    public static final String MUSIC_MENU = "menu";
    public static final String MUSIC_GAME = "game";

    private static MusicManager instance;
    private final Map<String, MediaPlayer> tracks = new HashMap<>();
    private MediaPlayer reproductorActual;
    private double volumen = 0.4;
    private boolean enabled = true;

    private MusicManager() {
        precargarMusica();
    }

    public static synchronized MusicManager getInstance() {
        if (instance == null) {
            instance = new MusicManager();
        }
        return instance;
    }

    private void precargarMusica() {
        cargar(MUSIC_MENU);
        cargar(MUSIC_GAME);
    }

    private void cargar(String nombre) {
        try {
            java.net.URL url = getClass().getResource("/org/borradoruno/music/" + nombre + ".mp3");
            if (url != null) {
                Media media = new Media(url.toExternalForm());
                MediaPlayer player = new MediaPlayer(media);
                player.setCycleCount(MediaPlayer.INDEFINITE);
                player.setVolume(volumen);
                tracks.put(nombre, player);
                System.out.println("[Music] Cargada: " + nombre + ".mp3");
            } else {
                System.out.println("[Music] No encontrada (omitida): " + nombre + ".mp3");
            }
        } catch (Exception e) {
            System.err.println("[Music] Error cargando " + nombre + ": " + e.getMessage());
        }
    }

    public void play(String nombre) {
        if (!enabled) return;

        MediaPlayer nueva = tracks.get(nombre);
        if (nueva == null) return;

        if (reproductorActual == nueva
                && reproductorActual.getStatus() == MediaPlayer.Status.PLAYING) {
            return;
        }

        if (reproductorActual != null) {
            reproductorActual.stop();
        }

        reproductorActual = nueva;
        reproductorActual.setVolume(volumen);
        reproductorActual.seek(Duration.ZERO);
        reproductorActual.play();
        System.out.println("[Music] Reproduciendo: " + nombre);
    }

    public void stop() {
        if (reproductorActual != null) {
            reproductorActual.stop();
            reproductorActual = null;
        }
    }

    public void pause() {
        if (reproductorActual != null) {
            reproductorActual.pause();
        }
    }

    public void resume() {
        if (reproductorActual != null && reproductorActual.getStatus() == MediaPlayer.Status.PAUSED) {
            reproductorActual.play();
        }
    }

    public void setVolumen(double vol) {
        this.volumen = Math.max(0, Math.min(1, vol));
        if (reproductorActual != null) {
            reproductorActual.setVolume(this.volumen);
        }
    }

    public double getVolumen() {
        return volumen;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (!enabled) stop();
    }

    public boolean isEnabled() {
        return enabled;
    }
}
