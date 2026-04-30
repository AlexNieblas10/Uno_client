package org.borradoruno.logic;

import org.borradoruno.models.Partida;
import org.borradoruno.models.Jugador;
import java.util.UUID;

public class JuegoManager {
    private static JuegoManager instance;
    private Partida partidaActual;

    private JuegoManager() {
        this.partidaActual = new Partida(UUID.randomUUID().toString());
    }

    public static synchronized JuegoManager getInstance() {
        if (instance == null) {
            instance = new JuegoManager();
        }
        return instance;
    }

    public void inicializarMazo() {
        org.borradoruno.models.Mazo mazo = partidaActual.getMazo();
        mazo.getCartas().clear();
        for (org.borradoruno.models.Color c : org.borradoruno.models.Color.values()) {
            if (c == org.borradoruno.models.Color.NEGRO) continue;
            for (org.borradoruno.models.Valor v : org.borradoruno.models.Valor.values()) {
                if (v == org.borradoruno.models.Valor.COMODIN_COLOR || v == org.borradoruno.models.Valor.COMODIN_MAS_CUATRO) continue;
                // UNO tiene dos de cada especial y un 0
                mazo.getCartas().add(new org.borradoruno.models.Carta(c, v, false, 0));
                if (v != org.borradoruno.models.Valor.CERO) {
                    mazo.getCartas().add(new org.borradoruno.models.Carta(c, v, false, 0));
                }
            }
        }
        // Comodines
        for (int i = 0; i < 4; i++) {
            mazo.getCartas().add(new org.borradoruno.models.Carta(org.borradoruno.models.Color.NEGRO, org.borradoruno.models.Valor.COMODIN_COLOR, true, 50));
            mazo.getCartas().add(new org.borradoruno.models.Carta(org.borradoruno.models.Color.NEGRO, org.borradoruno.models.Valor.COMODIN_MAS_CUATRO, true, 50));
        }
        mazo.barajar();
    }

    public synchronized void iniciarPartida() {
        if (partidaActual.getJugadores().size() < 2) return;
        
        inicializarMazo();
        partidaActual.setEstado(org.borradoruno.models.EstadoPartida.EN_CURSO);
        
        // Repartir 7 cartas a cada uno
        for (Jugador j : partidaActual.getJugadores()) {
            for (int i = 0; i < 7; i++) {
                j.getMano().add(partidaActual.getMazo().robar());
            }
        }
        
        // Primera carta a la pila
        partidaActual.getPilaDescarte().agregarCarta(partidaActual.getMazo().robar());
    }

    public synchronized boolean validarJugada(Jugador jugador, org.borradoruno.models.Carta carta) {
        // 1. Validar que sea el turno del jugador
        int indiceJugador = partidaActual.getJugadores().indexOf(jugador);
        if (indiceJugador != partidaActual.getTurnoActual()) {
            System.out.println("Jugada rechazada: No es el turno de " + jugador.getNombre());
            return false;
        }

        org.borradoruno.models.PilaDescarte pila = partidaActual.getPilaDescarte();
        // 2. Si es comodín negro, es válido
        if (carta.getColor() == org.borradoruno.models.Color.NEGRO) return true;
        
        // 3. Si la carta en la pila es negra (comodín recién tirado), permitir cualquier carta
        if (pila.getColorActivo() == org.borradoruno.models.Color.NEGRO) return true;

        // 4. Si coincide color o valor
        boolean coincide = carta.getColor() == pila.getColorActivo() || carta.getValor() == pila.getValorActivo();
        if (!coincide) System.out.println("Jugada rechazada: La carta " + carta.getValor() + " no coincide con la pila");
        return coincide;
    }

    public synchronized boolean procesarJugada(Jugador jugador, org.borradoruno.models.Carta carta) {
        if (!validarJugada(jugador, carta)) return false;

        // Validar posesión: solo proceder si la carta realmente estaba en la mano
        boolean removida = jugador.getMano().removeIf(c -> 
            c.getColor() == carta.getColor() && c.getValor() == carta.getValor());
        
        if (!removida) {
            System.err.println("Error: El jugador " + jugador.getNombre() + " intentó tirar una carta que no tiene: " + carta.getValor());
            return false;
        }

        // Agregar a la pila
        partidaActual.getPilaDescarte().agregarCarta(carta);

        // Si es comodín, asignamos un color por defecto
        if (carta.getColor() == org.borradoruno.models.Color.NEGRO) {
            partidaActual.getPilaDescarte().setColorActivo(org.borradoruno.models.Color.ROJO);
        }

        aplicarEfectos(carta);
        verificarGanador(jugador);
        avanzarTurno();
        System.out.println("Jugada exitosa de " + jugador.getNombre() + ". Siguiente turno: " + partidaActual.getTurnoActual());
        return true;
    }

    public synchronized boolean procesarJugadaComodin(Jugador jugador, org.borradoruno.models.Carta comodin, org.borradoruno.models.Color colorElegido) {
        if (!validarJugada(jugador, comodin)) return false;

        // Validar posesión
        boolean removida = jugador.getMano().removeIf(c -> 
            c.getColor() == comodin.getColor() && c.getValor() == comodin.getValor());

        if (!removida) {
            System.err.println("Error: El jugador " + jugador.getNombre() + " intentó tirar un comodín que no tiene: " + comodin.getValor());
            return false;
        }

        // Agregar a la pila
        partidaActual.getPilaDescarte().agregarCarta(comodin);
        partidaActual.getPilaDescarte().setColorActivo(colorElegido);

        aplicarEfectos(comodin);
        verificarGanador(jugador);
        avanzarTurno();
        System.out.println("Jugada exitosa de " + jugador.getNombre() + " con comodín. Color elegido: " + colorElegido + ". Siguiente turno: " + partidaActual.getTurnoActual());
        return true;
    }

    private void aplicarEfectos(org.borradoruno.models.Carta carta) {
        if (carta == null || carta.getValor() == null) {
            System.err.println("Error: Se intentó aplicar efectos a una carta nula o con valor nulo.");
            return;
        }
        
        switch (carta.getValor()) {
            case REVERSA:
                partidaActual.setSentidoJuego(
                    partidaActual.getSentidoJuego() == org.borradoruno.models.Sentido.HORARIO ? 
                    org.borradoruno.models.Sentido.ANTIHORARIO : org.borradoruno.models.Sentido.HORARIO
                );
                break;
            case MAS_DOS:
                // Castigo automático: el siguiente jugador roba 2 y pierde su turno
                avanzarTurno();
                Jugador victimaDos = partidaActual.getJugadores().get(partidaActual.getTurnoActual());
                System.out.println(victimaDos.getNombre() + " roba 2 automáticamente y salta su turno");
                for (int i = 0; i < 2; i++) {
                    if (partidaActual.getMazo().getCartasRestantes() == 0) reciclarMazo();
                    victimaDos.getMano().add(partidaActual.getMazo().robar());
                }
                break;
            case BLOQUEO:
                // Saltamos un turno extra antes de avanzar el normal
                avanzarTurno();
                break;
            case COMODIN_MAS_CUATRO:
                // Castigo automático: el siguiente jugador roba 4 y pierde su turno
                avanzarTurno();
                Jugador victimaCuatro = partidaActual.getJugadores().get(partidaActual.getTurnoActual());
                System.out.println(victimaCuatro.getNombre() + " roba 4 automáticamente y salta su turno");
                for (int i = 0; i < 4; i++) {
                    if (partidaActual.getMazo().getCartasRestantes() == 0) reciclarMazo();
                    victimaCuatro.getMano().add(partidaActual.getMazo().robar());
                }
                break;
            case COMODIN_COLOR:
                // No tiene efectos directos en el turno
                break;
        }
    }

    private void avanzarTurno() {
        int total = partidaActual.getJugadores().size();
        int paso = partidaActual.getSentidoJuego() == org.borradoruno.models.Sentido.HORARIO ? 1 : -1;
        partidaActual.setTurnoActual((partidaActual.getTurnoActual() + paso + total) % total);
    }

    public synchronized void robarCarta(Jugador jugador) {
        // En este sistema de castigos automáticos, robarCarta siempre roba exactamente 1.
        if (partidaActual.getMazo().getCartasRestantes() == 0) {
            reciclarMazo();
        }
        jugador.getMano().add(partidaActual.getMazo().robar());
        avanzarTurno();
    }

    private void reciclarMazo() {
        // Lógica para pasar cartas de PilaDescarte a Mazo (excepto la última)
        org.borradoruno.models.PilaDescarte pila = partidaActual.getPilaDescarte();
        java.util.List<org.borradoruno.models.Carta> viejas = pila.getCartas();
        org.borradoruno.models.Carta actual = viejas.remove(viejas.size() - 1);
        
        partidaActual.getMazo().getCartas().addAll(viejas);
        partidaActual.getMazo().barajar();
        viejas.clear();
        viejas.add(actual);
    }

    public Partida getPartidaActual() {
        return partidaActual;
    }

    public synchronized void agregarJugador(Jugador jugador) {
        if (partidaActual.getJugadores().size() == 0) {
            jugador.setEsAnfitrion(true);
        }
        partidaActual.getJugadores().add(jugador);
    }

    public synchronized void setMaxJugadores(int max) {
        this.partidaActual.setMaxJugadores(max);
    }

    public synchronized void removerJugador(Jugador jugador) {
        boolean eraAnfitrion = jugador.isEsAnfitrion();
        partidaActual.getJugadores().remove(jugador);

        // Reasignar Anfitrión si se fue el actual (Diagrama 3)
        if (eraAnfitrion && !partidaActual.getJugadores().isEmpty()) {
            partidaActual.getJugadores().get(0).setEsAnfitrion(true);
        }
    }

    public synchronized void marcarUno(Jugador jugador) {
        if (jugador.getMano().size() == 1) {
            jugador.setDijoUNO(true);
            System.out.println(jugador.getNombre() + " marcó UNO correctamente (tiene 1 carta)");
        } else {
            System.out.println(jugador.getNombre() + " dijo UNO pero tiene " + jugador.getMano().size() + " cartas");
        }
    }

    private void verificarGanador(Jugador jugador) {
        if (jugador.getMano().isEmpty()) {
            System.out.println("¡" + jugador.getNombre() + " ha ganado la partida!");
            partidaActual.setEstado(org.borradoruno.models.EstadoPartida.FINALIZADA);
        }
    }

    // Aquí se implementarán más métodos de lógica según los diagramas de flujo
}