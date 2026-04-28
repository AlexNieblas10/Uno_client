package org.borradoruno.model;

public class EstadoCliente {
    private static EstadoCliente instance;

    private String nombreLocal;
    private Partida partidaActual;

    private EstadoCliente() {}

    public static synchronized EstadoCliente getInstance() {
        if (instance == null) {
            instance = new EstadoCliente();
        }
        return instance;
    }

    public String getNombreLocal() { return nombreLocal; }
    public void setNombreLocal(String nombreLocal) { this.nombreLocal = nombreLocal; }

    public Partida getPartidaActual() { return partidaActual; }
    public void setPartidaActual(Partida partida) { this.partidaActual = partida; }

    public Jugador getJugadorLocal() {
        if (partidaActual == null || nombreLocal == null) return null;
        return partidaActual.getJugadores().stream()
            .filter(j -> j.getNombre().equals(nombreLocal))
            .findFirst()
            .orElse(null);
    }

    public boolean esMiTurno() {
        if (partidaActual == null) return false;
        Jugador yo = getJugadorLocal();
        if (yo == null) return false;
        int miIndice = partidaActual.getJugadores().indexOf(yo);
        return miIndice == partidaActual.getTurnoActual();
    }

    public void limpiar() {
        this.partidaActual = null;
    }
}
