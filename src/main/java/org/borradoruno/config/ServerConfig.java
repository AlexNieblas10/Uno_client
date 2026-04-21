package org.borradoruno.config;

import java.util.prefs.Preferences;

/**
 * Configuración del servidor para el cliente.
 * Guarda la IP y puerto del servidor usando Java Preferences API.
 */
public class ServerConfig {
    private static final Preferences prefs = Preferences.userNodeForPackage(ServerConfig.class);
    private static final String KEY_SERVER_IP = "server_ip";
    private static final String KEY_SERVER_PORT = "server_port";
    private static final String DEFAULT_IP = "shinkansen.proxy.rlwy.net";
    private static final int DEFAULT_PORT = 53312;

    /**
     * Obtiene la IP del servidor guardada o retorna localhost
     */
    public static String getServerIP() {
        return prefs.get(KEY_SERVER_IP, DEFAULT_IP);
    }

    /**
     * Guarda la IP del servidor para futuras conexiones
     */
    public static void setServerIP(String ip) {
        if (ip != null && !ip.trim().isEmpty()) {
            prefs.put(KEY_SERVER_IP, ip);
        }
    }

    /**
     * Obtiene el puerto del servidor guardado o retorna 12345
     */
    public static int getServerPort() {
        return prefs.getInt(KEY_SERVER_PORT, DEFAULT_PORT);
    }

    /**
     * Guarda el puerto del servidor para futuras conexiones
     */
    public static void setServerPort(int port) {
        prefs.putInt(KEY_SERVER_PORT, port);
    }
}
