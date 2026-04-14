package org.borradoruno.network;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Server {
    private static final int PORT = 12345;
    private List<ClientHandler> clientes;

    public Server() {
        this.clientes = new ArrayList<>();
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Servidor de UNO iniciado en el puerto " + PORT);
            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("Nuevo cliente conectado: " + socket.getInetAddress());
                ClientHandler handler = new ClientHandler(socket, this);
                clientes.add(handler);
                new Thread(handler).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized void broadcast(String mensajeJson) {
        for (ClientHandler cliente : clientes) {
            cliente.enviar(mensajeJson);
        }
    }

    public synchronized void removerCliente(ClientHandler handler) {
        clientes.remove(handler);
    }

    public static void main(String[] args) {
        new Server().start();
    }
}