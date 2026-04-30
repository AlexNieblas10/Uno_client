package org.borradoruno.sound;

import javafx.scene.media.AudioClip;
import java.util.HashMap;
import java.util.Map;

/**
 * Gestor centralizado de efectos de sonido.
 * Si un archivo .wav no existe, play() no hace nada — la app funciona sin sonidos.
 */
public class SoundManager {

    private static SoundManager instance;

    public static final String SOUND_CARD_PLAY     = "card_play";
    public static final String SOUND_CARD_DRAW     = "card_draw";
    public static final String SOUND_SHUFFLE       = "shuffle";
    public static final String SOUND_UNO_ALERT     = "uno_alert";
    public static final String SOUND_WIN           = "win";
    public static final String SOUND_CLICK         = "click";
    public static final String SOUND_TURN_WARNING  = "turn_warning";

    private final Map<String, AudioClip> clips = new HashMap<>();
    private double volumen = 0.7;
    private boolean enabled = true;

    private SoundManager() {
        precargarSonidos();
    }

    public static synchronized SoundManager getInstance() {
        if (instance == null) {
            instance = new SoundManager();
        }
        return instance;
    }

    private void precargarSonidos() {
        cargar(SOUND_CARD_PLAY);
        cargar(SOUND_CARD_DRAW);
        cargar(SOUND_SHUFFLE);
        cargar(SOUND_UNO_ALERT);
        cargar(SOUND_WIN);
        cargar(SOUND_CLICK);
        cargar(SOUND_TURN_WARNING);
    }

    private void cargar(String nombre) {
        try {
            java.net.URL url = getClass().getResource("/org/borradoruno/sounds/" + nombre + ".wav");
            if (url != null) {
                clips.put(nombre, new AudioClip(url.toExternalForm()));
                System.out.println("[Sound] Cargado: " + nombre);
            } else {
                System.out.println("[Sound] No encontrado (omitido): " + nombre + ".wav");
            }
        } catch (Exception e) {
            System.err.println("[Sound] Error cargando " + nombre + ": " + e.getMessage());
        }
    }

    public void play(String nombre) {
        if (!enabled) return;
        AudioClip clip = clips.get(nombre);
        if (clip != null) {
            clip.setVolume(volumen);
            clip.play();
        }
    }

    public void setVolumen(double volumen) { this.volumen = Math.max(0, Math.min(1, volumen)); }
    public double getVolumen() { return volumen; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public boolean isEnabled() { return enabled; }
}
