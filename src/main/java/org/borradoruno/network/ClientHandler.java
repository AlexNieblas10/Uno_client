package org.borradoruno.network;

import com.google.gson.Gson;
import org.borradoruno.logic.JuegoManager;
import org.borradoruno.models.Jugador;
import org.borradoruno.models.Partida;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private Socket socket;
    private Server server;
    private PrintWriter out;
    private BufferedReader in;
    private Gson gson;
    private Jugador jugador;

    public ClientHandler(Socket socket, Server server) {
        this.socket = socket;
        this.server = server;
        this.gson = new Gson();
    }

    @Override
    public void run() {
        try {
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                Mensaje mensaje = gson.fromJson(inputLine, Mensaje.class);
                procesarMensaje(mensaje);
            }
        } catch (IOException e) {
            System.out.println("Cliente desconectado");
        } finally {
            server.removerCliente(this);
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void procesarMensaje(Mensaje mensaje) {
        System.out.println("Comando recibido: " + mensaje.getTipo() + " con datos: " + mensaje.getDatos());
        try {
            switch (mensaje.getTipo()) {
                case "CREATE":
                    String nomC = (String) mensaje.getDatos();
                    // Si no hay jugadores, reiniciamos la partida para una nueva sesión limpia
                    if (JuegoManager.getInstance().getPartidaActual().getJugadores().isEmpty()) {
                        JuegoManager.getInstance().iniciarPartida(); // Esto reinicia mazo y pila pero limpiaremos jugadores
                        JuegoManager.getInstance().getPartidaActual().getJugadores().clear();
                        JuegoManager.getInstance().getPartidaActual().setEstado(org.borradoruno.models.EstadoPartida.ESPERANDO_JUGADORES);
                    }
                    this.jugador = new Jugador(nomC, socket.getRemoteSocketAddress().toString());
                    JuegoManager.getInstance().agregarJugador(this.jugador);
                    server.broadcast(gson.toJson(new Mensaje("ESTADO_PARTIDA", JuegoManager.getInstance().getPartidaActual())));
                    break;
                case "JOIN":
                    String nomJ = (String) mensaje.getDatos();
                    Partida p = JuegoManager.getInstance().getPartidaActual();
                    if (p.getJugadores().size() >= p.getMaxJugadores()) {
                        enviar(gson.toJson(new Mensaje("ERROR", "La sala está llena")));
                        return;
                    }
                    this.jugador = new Jugador(nomJ, socket.getRemoteSocketAddress().toString());
                    JuegoManager.getInstance().agregarJugador(this.jugador);
                    server.broadcast(gson.toJson(new Mensaje("ESTADO_PARTIDA", JuegoManager.getInstance().getPartidaActual())));
                    break;
                case "SET_MAX_JUGADORES":
                    // GSON a veces envía números como Double o Integer, manejamos ambos
                    int max = 4;
                    if (mensaje.getDatos() instanceof Double) max = ((Double) mensaje.getDatos()).intValue();
                    else if (mensaje.getDatos() instanceof Integer) max = (Integer) mensaje.getDatos();
                    
                    JuegoManager.getInstance().setMaxJugadores(max);
                    System.out.println("Nuevo límite de jugadores: " + max);
                    server.broadcast(gson.toJson(new Mensaje("ESTADO_PARTIDA", JuegoManager.getInstance().getPartidaActual())));
                    break;
                case "INICIAR_PARTIDA":
                    JuegoManager.getInstance().iniciarPartida();
                    server.broadcast(gson.toJson(new Mensaje("ESTADO_PARTIDA", JuegoManager.getInstance().getPartidaActual())));
                    break;
                case "TIRAR_CARTA":
                    // Convertimos explícitamente los datos a un objeto Carta
                    String cJson = gson.toJson(mensaje.getDatos());
                    org.borradoruno.models.Carta cartaTirada = gson.fromJson(cJson, org.borradoruno.models.Carta.class);

                    if (JuegoManager.getInstance().procesarJugada(this.jugador, cartaTirada)) {
                        server.broadcast(gson.toJson(new Mensaje("ESTADO_PARTIDA", JuegoManager.getInstance().getPartidaActual())));
                    } else {
                        enviar(gson.toJson(new Mensaje("ERROR", "Movimiento inválido")));
                    }
                    break;
                case "TIRAR_COMODIN":
                    // El mensaje contiene [Carta, Color] en un objeto List
                    java.util.List<?> listaComodin = (java.util.List<?>) mensaje.getDatos();
                    String jsonCarta = gson.toJson(listaComodin.get(0));
                    String jsonColor = gson.toJson(listaComodin.get(1));
                    
                    org.borradoruno.models.Carta comodin = gson.fromJson(jsonCarta, org.borradoruno.models.Carta.class);
                    org.borradoruno.models.Color colorElegido = gson.fromJson(jsonColor, org.borradoruno.models.Color.class);

                    if (comodin != null && colorElegido != null && JuegoManager.getInstance().procesarJugadaComodin(this.jugador, comodin, colorElegido)) {
                        server.broadcast(gson.toJson(new Mensaje("ESTADO_PARTIDA", JuegoManager.getInstance().getPartidaActual())));
                    } else {
                        enviar(gson.toJson(new Mensaje("ERROR", "Movimiento inválido con comodín")));
                    }
                    break;
                case "ROBAR_CARTA":
                    JuegoManager.getInstance().robarCarta(this.jugador);
                    server.broadcast(gson.toJson(new Mensaje("ESTADO_PARTIDA", JuegoManager.getInstance().getPartidaActual())));
                    break;
                case "DECIR_UNO":
                    JuegoManager.getInstance().marcarUno(this.jugador);
                    server.broadcast(gson.toJson(new Mensaje("ESTADO_PARTIDA", JuegoManager.getInstance().getPartidaActual())));
                    System.out.println(this.jugador.getNombre() + " dijo UNO!");
                    break;
                case "ABANDONAR_SALA":
                    JuegoManager.getInstance().removerJugador(this.jugador);
                    server.broadcast(gson.toJson(new Mensaje("ESTADO_PARTIDA", JuegoManager.getInstance().getPartidaActual())));
                    break;
                case "SOLICITAR_ESTADO":
                    enviar(gson.toJson(new Mensaje("ESTADO_PARTIDA", JuegoManager.getInstance().getPartidaActual())));
                    break;
            }
        } catch (Exception e) {
            System.err.println("Error procesando mensaje: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void enviar(String mensajeJson) {
        if (out != null) {
            out.println(mensajeJson);
        }
    }
}