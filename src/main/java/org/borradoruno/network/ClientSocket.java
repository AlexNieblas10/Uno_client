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
    private Gson gson;
    private List<ServerObserver> observers;

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

        this.socket = new Socket(host, puerto);
        this.out = new PrintWriter(socket.getOutputStream(), true);
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        new Thread(this::escuchar).start();
    }

    private void escuchar() {
        try {
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                String finalInputLine = inputLine;
                Platform.runLater(() -> notificar(finalInputLine));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void enviar(String tipo, Object datos) {
        if (out == null) {
            System.err.println("Error: No se pueden enviar datos, no hay conexión con el servidor.");
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
