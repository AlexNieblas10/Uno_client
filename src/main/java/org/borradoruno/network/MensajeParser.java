package org.borradoruno.network;

import com.google.gson.Gson;
import org.borradoruno.model.Partida;

public class MensajeParser {
    private static final Gson gson = new Gson();

    public static Partida parsearPartida(Object datos) {
        return gson.fromJson(gson.toJson(datos), Partida.class);
    }

    public static String parsearString(Object datos) {
        return datos != null ? datos.toString() : "";
    }
}
