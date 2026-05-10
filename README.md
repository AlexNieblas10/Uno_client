# UNO Online - Proyecto académico

Implementación cliente-servidor del juego UNO en Java + JavaFX con servidor TCP.

## Casos de uso destacados

Este proyecto implementa los 6 casos de uso del juego, con énfasis particular en:

### ⭐ Configurar Partida (caso de uso principal)
El **anfitrión** de la sala puede configurar el máximo de jugadores admitidos
(2, 3 o 4) antes de iniciar la partida. El sistema valida:
- Que solo el anfitrión pueda configurar
- Que el valor esté en rango válido (2-10)
- Que la partida no haya iniciado todavía
- Que no haya más jugadores conectados que el nuevo límite

### ⭐ Realizar Jugada (caso de uso principal)
Durante la partida, cada jugador en su turno puede tirar una carta válida
(coincide en color, valor o es comodín), robar del mazo, o decir "UNO" cuando
le queda una sola carta. El sistema aplica los efectos de cartas especiales
(REVERSA, BLOQUEO, +2, +4, COMODIN) y avanza turnos automáticamente.

### Casos de uso de soporte
Los siguientes casos están implementados al mínimo viable necesario:
- Registrar usuario
- Unirse a partida
- Iniciar partida
- Abandonar partida

## Patrones de diseño aplicados
- **Singleton:** JuegoManager, TurnoScheduler, EstadoCliente, ClientSocket
- **Observer:** PartidaPublisher + PartidaObserver
- **Command (simplificado):** GameController.procesarMensaje
- **Facade:** JuegoManager sobre Mazo, Pila, Partida, Jugador
- **MVC:** Vista FXML / Controller JavaFX / Modelo de dominio

## Arquitectura
Cliente-servidor sobre TCP plano con protocolo JSON.
- Cliente: JavaFX 21 con Maven
- Servidor: Java 21 puro, desplegado en VPS Oracle Cloud con Docker

## Cómo ejecutar

```bash
mvn clean javafx:run
```

Requiere Java 21 y Maven 3.8+.

## Flujo de prueba end-to-end

1. Cliente A (anfitrión) crea partida → ve código de sala y botones 2/3/4
2. **Configurar:** Cliente A da clic en botón "3" → log `[ConfigurarPartida]` en servidor
3. Cliente B y Cliente C se unen con el código de sala
4. **Iniciar:** Cliente A da clic en "INICIAR PARTIDA" (sin esperar marcado de listos)
5. **Realizar jugada:** los 3 clientes juegan turnos → log `[RealizarJugada]` en servidor
6. Verificar cartas especiales: REVERSA, +2, COMODIN, BLOQUEO
7. **Error de configuración:** intentar cambiar max jugadores con partida iniciada → rechaza con mensaje
8. **Abandonar:** un cliente da clic en "ABANDONAR SALA" → vuelve a registro inmediatamente
