package app;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public final class ExternalControllerWindow {

    private static final String HOST = "localhost";
    private static final int PORT = 3000;

    private static Stage stage;           // singleton window
    private static Socket socket;
    private static PrintWriter out;
    private static Label status;

    private static void sendJsonCmd(String cmd) {
        try {
            if (out != null) {
                // {"cmd":"LEFT"}
                out.println("{\"cmd\":\"" + cmd + "\"}");
            }
        } catch (Exception ignored) {}
    }


    public static void show() {
        if (stage != null) {
            stage.toFront();
            stage.requestFocus();
            return;
        }
        stage = new Stage();
        status = new Label("Disconnected");

        var left   = new Button("LEFT");
        var right  = new Button("RIGHT");
        var down   = new Button("DOWN");
        var rotate = new Button("ROTATE");
        var drop   = new Button("DROP");
        var pause  = new Button("PAUSE");
        var connectBtn = new Button("Connect");
        var closeBtn   = new Button("Disconnect");

        left.setOnAction(e -> sendJsonCmd("LEFT"));
        right.setOnAction(e -> sendJsonCmd("RIGHT"));
        down.setOnAction(e -> sendJsonCmd("DOWN"));
        rotate.setOnAction(e -> sendJsonCmd("ROTATE"));
        drop.setOnAction(e -> sendJsonCmd("DROP"));
        pause.setOnAction(e -> sendJsonCmd("PAUSE"));


        connectBtn.setOnAction(e -> connect());
        closeBtn.setOnAction(e -> disconnect());

        var root = new VBox(
                10,
                centered(new Label("External Controller (localhost:3000)")),
                centered(status),
                centered(new HBox(8, left, right, down, rotate, drop, pause)),
                centered(new HBox(8, connectBtn, closeBtn))
        );
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(16));

        var scene = new Scene(root, 460, 200);
        scene.setOnKeyPressed(ev -> {
            switch (ev.getCode()) {
                case LEFT  -> sendJsonCmd("LEFT");
                case RIGHT -> sendJsonCmd("RIGHT");
                case DOWN  -> sendJsonCmd("DOWN");
                case UP    -> sendJsonCmd("ROTATE");
                case SPACE -> sendJsonCmd("DROP");
                case P     -> sendJsonCmd("PAUSE");
                case ESCAPE -> disconnect();
                default -> {}
            }
        });

        stage.setTitle("Tetris External Controller");
        stage.setScene(scene);
        stage.setOnCloseRequest(e -> {
            disconnect();
            stage = null;   // allow reopening later
        });
        stage.show();
    }

    private static HBox centered(javafx.scene.Node n) {
        var h = new HBox(n);
        h.setAlignment(Pos.CENTER);
        return h;
    }

    private static void connect() {
        disconnect(); // safety
        new Thread(() -> {
            try {
                Socket s = new Socket(HOST, PORT);
                PrintWriter w = new PrintWriter(new OutputStreamWriter(s.getOutputStream(), StandardCharsets.UTF_8), true);
                socket = s;
                out = w;
                Platform.runLater(() -> status.setText("Connected"));
                if (out != null) out.println("{\"role\":\"controller\",\"hello\":true}");
            } catch (Exception ex) {
                Platform.runLater(() -> status.setText("Failed to connect"));
            }
        }, "ExtCtrl-Connect").start();
    }

    private static void disconnect() {
        try { if (out != null) out.flush(); } catch (Exception ignored) {}
        try { if (socket != null) socket.close(); } catch (Exception ignored) {}
        out = null;
        socket = null;
        if (status != null) status.setText("Disconnected");
    }

    private static void send(String line) {
        try { if (out != null) out.println(line); } catch (Exception ignored) {}
    }
}
