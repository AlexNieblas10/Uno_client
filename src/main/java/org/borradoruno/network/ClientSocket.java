package org.borradoruno.network;

import com.google.gson.Gson;
import javafx.application.Platform;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ClientSocket {

    private static ClientSocket instance;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private final Gson gson;
    private final List<ServerObserver> observers;
    private Thread heartbeatThread;
    private volatile boolean conectado = false;

    private ClientSocket() {
        this.gson = new Gson();
        this.observers = new ArrayList<>();
    }

    public static synchronized ClientSocket getInstance() {
        if (instance == null) {
            instance = new ClientSocket();
        }
        return instance;
    }

    public void conectar(String host, int puerto) throws IOException {
        if (socket != null && !socket.isClosed()) {
            return;
        }

        IOException ultimoError = null;
        int[] intervalos = {0, 1000, 2000}; // ms de espera antes de cada reintento

        for (int intento = 0; intento < 3; intento++) {
            if (intervalos[intento] > 0) {
                try {
                    Thread.sleep(intervalos[intento]);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                System.out.println("[ClientSocket] Reintento de conexión #" + intento + "...");
            }

            try {
                this.socket = new Socket();
                this.socket.connect(new java.net.InetSocketAddress(host, puerto), 5000);
                this.out = new PrintWriter(socket.getOutputStream(), true);
                this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                this.conectado = true;

                Mensaje handshake = new Mensaje("HANDSHAKE", "UNO-CLIENT-V1");
                out.println(gson.toJson(handshake));

                new Thread(this::escuchar).start();
                iniciarHeartbeat();

                System.out.println("[ClientSocket] Conexión establecida en intento #" + (intento + 1));
                return;
            } catch (IOException e) {
                ultimoError = e;
                System.err.println("[ClientSocket] Intento " + (intento + 1) + " falló: " + e.getMessage());
                if (socket != null) {
                    try { socket.close(); } catch (Exception ignored) {}
                }
            }
        }

        throw ultimoError != null ? ultimoError : new IOException("No se pudo conectar después de 3 intentos");
    }

    private void iniciarHeartbeat() {
        heartbeatThread = new Thread(() -> {
            while (conectado && socket != null && !socket.isClosed()) {
                try {
                    Thread.sleep(10_000);
                    if (conectado && out != null && !socket.isClosed()) {
                        enviar("PING", null);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    break;
                }
            }
        });
        heartbeatThread.setDaemon(true);
        heartbeatThread.start();
    }

    public void desconectar() {
        if (!conectado) return;

        System.out.println("[ClientSocket] Desconectando...");
        conectado = false;

        if (heartbeatThread != null) heartbeatThread.interrupt();

        try { if (out != null) out.close(); } catch (Exception ignored) {}
        try { if (in != null) in.close(); } catch (Exception ignored) {}
        try { if (socket != null && !socket.isClosed()) socket.close(); } catch (Exception ignored) {}

        observers.clear();
        System.out.println("[ClientSocket] Desconectado");
    }

    public boolean isConectado() {
        return conectado && socket != null && !socket.isClosed();
    }

    private void escuchar() {
        try {
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                String finalInputLine = inputLine;
                Platform.runLater(() -> notificar(finalInputLine));
            }
            System.out.println("[ClientSocket] Stream cerrado por el servidor");
        } catch (IOException e) {
            System.out.println("[ClientSocket] Conexión perdida: " + e.getMessage());
        }
    }

    public void enviar(String tipo, Object datos) {
        if (out == null) {
            System.err.println("[ClientSocket] Error: no hay conexión con el servidor.");
            return;
        }
        Mensaje mensaje = new Mensaje(tipo, datos);
        out.println(gson.toJson(mensaje));
    }

    public void addObserver(ServerObserver observer) {
        observers.add(observer);
    }

    public void removeObserver(ServerObserver observer) {
        observers.remove(observer);
    }

    private void notificar(String mensajeJson) {
        Mensaje mensaje = gson.fromJson(mensajeJson, Mensaje.class);
        for (ServerObserver observer : new ArrayList<>(observers)) {
            observer.onMensajeRecibido(mensaje);
        }
    }

    public interface ServerObserver {
        void onMensajeRecibido(Mensaje mensaje);
    }
}
