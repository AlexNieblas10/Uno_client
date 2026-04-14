package org.borradoruno.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import org.borradoruno.models.Carta;
import org.borradoruno.models.Partida;
import org.borradoruno.models.Jugador;
import org.borradoruno.network.ClientSocket;
import org.borradoruno.network.Mensaje;
import com.google.gson.Gson;

public class JuegoController implements ClientSocket.ServerObserver {

    @FXML
    private HBox hboxMano;
    @FXML
    private Label lblCartaActiva;
    @FXML
    private Label lblTurnoActual;
    @FXML
    private Label lblSentido;

    private Gson gson = new Gson();

    @FXML
    public void initialize() {
        ClientSocket.getInstance().addObserver(this);
        // Solicitar el estado actual al servidor para obtener las cartas
        ClientSocket.getInstance().enviar("SOLICITAR_ESTADO", null);
    }

    @Override
    public void onMensajeRecibido(Mensaje mensaje) {
        if (mensaje.getTipo().equals("ESTADO_PARTIDA")) {
            String partidaJson = gson.toJson(mensaje.getDatos());
            Partida partida = gson.fromJson(partidaJson, Partida.class);
            actualizarInterfaz(partida);

            // Verificar si el juego ha terminado
            if (partida.getEstado() == org.borradoruno.models.EstadoPartida.FINALIZADA) {
                mostrarFinDeJuego(partida);
            }
        }
    }

    private void actualizarInterfaz(Partida partida) {
        // 1. Actualizar Pila de Descarte
        Carta activa = partida.getPilaDescarte().getCartas().get(partida.getPilaDescarte().getCartas().size() - 1);
        lblCartaActiva.setText(getValorCorto(activa.getValor()));
        String colorActivaCss = mapColorToCss(activa.getColor());
        
        lblCartaActiva.setStyle(
            "-fx-background-color: " + colorActivaCss + ";" +
            "-fx-text-fill: white;" +
            "-fx-font-weight: bold;" +
            "-fx-font-size: 40;" +
            "-fx-min-width: 100;" +
            "-fx-min-height: 150;" +
            "-fx-background-radius: 15;" +
            "-fx-border-color: white;" +
            "-fx-border-radius: 15;" +
            "-fx-border-width: 4;" +
            "-fx-alignment: center;" +
            "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.6), 15, 0, 0, 5);"
        );

        // 2. Actualizar Turno y Sentido
        if (partida.getJugadores().size() > partida.getTurnoActual()) {
            String nombreTurno = partida.getJugadores().get(partida.getTurnoActual()).getNombre();
            lblTurnoActual.setText("TURNO DE: " + nombreTurno.toUpperCase());
            lblTurnoActual.setStyle("-fx-font-size: 22; -fx-font-weight: bold; -fx-text-fill: " + 
                (nombreTurno.equals(ClientSocket.getInstance().getNombreLocal()) ? "#22c55e" : "white") + ";");
        }
        lblSentido.setText("SENTIDO " + partida.getSentidoJuego());

        // 3. Actualizar Mano del Jugador Local
        hboxMano.getChildren().clear();
        String miNombre = ClientSocket.getInstance().getNombreLocal();
        
        Jugador yo = null;
        for(Jugador j : partida.getJugadores()) {
            if(j.getNombre().equals(miNombre)) {
                yo = j;
                break;
            }
        }

        if (yo != null) {
            for (Carta c : yo.getMano()) {
                Button btnCarta = new Button(getValorCorto(c.getValor()));
                String colorCss = mapColorToCss(c.getColor());
                
                btnCarta.setStyle(
                    "-fx-background-color: " + colorCss + ";" +
                    "-fx-text-fill: white;" +
                    "-fx-font-weight: bold;" +
                    "-fx-font-size: 16;" +
                    "-fx-min-width: 70;" +
                    "-fx-min-height: 110;" +
                    "-fx-background-radius: 10;" +
                    "-fx-border-color: white;" +
                    "-fx-border-radius: 10;" +
                    "-fx-border-width: 2;" +
                    "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.4), 5, 0, 0, 2);"
                );
                
                btnCarta.setOnAction(e -> onTirarCarta(c));
                hboxMano.getChildren().add(btnCarta);
            }
        }
    }

    private String getValorCorto(org.borradoruno.models.Valor v) {
        return switch (v) {
            case CERO -> "0"; case UNO -> "1"; case DOS -> "2"; case TRES -> "3";
            case CUATRO -> "4"; case CINCO -> "5"; case SEIS -> "6";
            case SIETE -> "7"; case OCHO -> "8"; case NUEVE -> "9";
            case BLOQUEO -> "🚫";
            case REVERSA -> "🔄";
            case MAS_DOS -> "+2";
            case COMODIN_COLOR -> "🎨";
            case COMODIN_MAS_CUATRO -> "+4";
        };
    }

    private String mapColorToCss(org.borradoruno.models.Color color) {
        return switch (color) {
            case ROJO -> "#ef4444";
            case AZUL -> "#3b82f6";
            case VERDE -> "#22c55e";
            case AMARILLO -> "#facc15";
            case NEGRO -> "#1f2937";
        };
    }

    private void onTirarCarta(Carta carta) {
        // Si es un comodín, primero seleccionar el color
        if (carta.getColor() == org.borradoruno.models.Color.NEGRO) {
            mostrarSelectorColor(carta);
        } else {
            ClientSocket.getInstance().enviar("TIRAR_CARTA", carta);
        }
    }

    private void mostrarSelectorColor(Carta comodin) {
        javafx.application.Platform.runLater(() -> {
            javafx.scene.control.Dialog<org.borradoruno.models.Color> dialog = new javafx.scene.control.Dialog<>();
            dialog.setTitle("Seleccionar Color");
            dialog.setHeaderText("Elige el color para el comodín");

            javafx.scene.layout.HBox coloresBox = new javafx.scene.layout.HBox(15);
            coloresBox.setAlignment(javafx.geometry.Pos.CENTER);
            coloresBox.setStyle("-fx-padding: 20;");

            // Crear botones para cada color
            javafx.scene.control.Button btnRojo = crearBotonColor("ROJO", "#ef4444");
            javafx.scene.control.Button btnAzul = crearBotonColor("AZUL", "#3b82f6");
            javafx.scene.control.Button btnVerde = crearBotonColor("VERDE", "#22c55e");
            javafx.scene.control.Button btnAmarillo = crearBotonColor("AMARILLO", "#facc15");

            coloresBox.getChildren().addAll(btnRojo, btnAzul, btnVerde, btnAmarillo);
            dialog.getDialogPane().setContent(coloresBox);

            // Configurar los resultados del diálogo
            final org.borradoruno.models.Color[] colorSeleccionado = {null};

            btnRojo.setOnAction(e -> {
                colorSeleccionado[0] = org.borradoruno.models.Color.ROJO;
                dialog.setResult(org.borradoruno.models.Color.ROJO);
                dialog.close();
            });
            btnAzul.setOnAction(e -> {
                colorSeleccionado[0] = org.borradoruno.models.Color.AZUL;
                dialog.setResult(org.borradoruno.models.Color.AZUL);
                dialog.close();
            });
            btnVerde.setOnAction(e -> {
                colorSeleccionado[0] = org.borradoruno.models.Color.VERDE;
                dialog.setResult(org.borradoruno.models.Color.VERDE);
                dialog.close();
            });
            btnAmarillo.setOnAction(e -> {
                colorSeleccionado[0] = org.borradoruno.models.Color.AMARILLO;
                dialog.setResult(org.borradoruno.models.Color.AMARILLO);
                dialog.close();
            });

            // Agregar un botón de cancelar
            javafx.scene.control.ButtonType cancelButtonType = javafx.scene.control.ButtonType.CANCEL;
            dialog.getDialogPane().getButtonTypes().add(cancelButtonType);

            dialog.showAndWait().ifPresent(color -> {
                // Enviar el comodín con el color seleccionado
                ClientSocket.getInstance().enviar("TIRAR_COMODIN", new Object[]{comodin, color});
            });
        });
    }

    private javafx.scene.control.Button crearBotonColor(String nombre, String colorCss) {
        javafx.scene.control.Button btn = new javafx.scene.control.Button(nombre);
        btn.setStyle(
            "-fx-background-color: " + colorCss + ";" +
            "-fx-text-fill: white;" +
            "-fx-font-weight: bold;" +
            "-fx-font-size: 18;" +
            "-fx-min-width: 100;" +
            "-fx-min-height: 100;" +
            "-fx-background-radius: 15;" +
            "-fx-cursor: hand;"
        );
        return btn;
    }

    @FXML
    private void onRobarCarta() {
        ClientSocket.getInstance().enviar("ROBAR_CARTA", null);
    }

    @FXML
    private void onDecirUno() {
        ClientSocket.getInstance().enviar("DECIR_UNO", null);
    }

    @FXML
    private void onAbandonar() {
        ClientSocket.getInstance().enviar("ABANDONAR_SALA", null);
        regresarARegistro();
    }

    private void regresarARegistro() {
        javafx.application.Platform.runLater(() -> {
            try {
                javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/org/borradoruno/registro-view.fxml"));
                javafx.scene.Parent root = loader.load();
                javafx.scene.Scene scene = new javafx.scene.Scene(root, 800, 600);
                javafx.stage.Stage stage = (javafx.stage.Stage) lblTurnoActual.getScene().getWindow();
                stage.setScene(scene);
            } catch (java.io.IOException e) {
                e.printStackTrace();
            }
        });
    }

    private void mostrarFinDeJuego(Partida partida) {
        javafx.application.Platform.runLater(() -> {
            // Encontrar al ganador (el que tiene 0 cartas)
            String ganador = "Desconocido";
            for (Jugador j : partida.getJugadores()) {
                if (j.getMano().isEmpty()) {
                    ganador = j.getNombre();
                    break;
                }
            }

            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
            alert.setTitle("Fin del Juego");
            alert.setHeaderText("¡EL JUEGO HA TERMINADO!");
            alert.setContentText("El ganador es: " + ganador + "\n\n¡Felicidades!");

            // Personalizar el diálogo
            alert.getDialogPane().setStyle("-fx-font-size: 14; -fx-font-weight: bold;");

            alert.showAndWait();

            // Volver al registro después de cerrar el diálogo
            regresarARegistro();
        });
    }
}