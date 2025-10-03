package app;

import javafx.application.Application;
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

public class ExternalControllerTool extends Application {

    private static final String HOST = "localhost";
    private static final int PORT = 3000;

    private volatile Socket socket;
    private volatile PrintWriter out;
    private final Label status = new Label("Disconnected");

    @Override
    public void start(Stage stage) {
        var left   = new Button("LEFT");
        var right  = new Button("RIGHT");
        var down   = new Button("DOWN");
        var rotate = new Button("ROTATE");
        var drop   = new Button("DROP");
        var pause  = new Button("PAUSE");
        var connectBtn = new Button("Connect");
        var closeBtn   = new Button("Disconnect");

        left.setOnAction(e -> send("LEFT"));
        right.setOnAction(e -> send("RIGHT"));
        down.setOnAction(e -> send("DOWN"));
        rotate.setOnAction(e -> send("ROTATE"));
        drop.setOnAction(e -> send("DROP"));
        pause.setOnAction(e -> send("PAUSE"));

        connectBtn.setOnAction(e -> connect());
        closeBtn.setOnAction(e -> disconnect());

        // Keyboard shortcuts
        // Arrows/Space/P map to server commands (Esc disconnects)
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
            KeyCode c = ev.getCode();
            switch (c) {
                case LEFT -> send("LEFT");
                case RIGHT -> send("RIGHT");
                case DOWN -> send("DOWN");
                case UP -> send("ROTATE");
                case SPACE -> send("DROP");
                case P -> send("PAUSE");
                case ESCAPE -> disconnect();
                default -> {}
            }
        });

        stage.setTitle("Tetris External Controller");
        stage.setScene(scene);
        stage.setOnCloseRequest(e -> disconnect());
        stage.show();
    }

    private static HBox centered(javafx.scene.Node n) {
        var h = new HBox(n);
        h.setAlignment(Pos.CENTER);
        return h;
    }

    private void connect() {
        disconnect(); // safety
        new Thread(() -> {
            try {
                Socket s = new Socket(HOST, PORT);
                PrintWriter w = new PrintWriter(new OutputStreamWriter(s.getOutputStream(), StandardCharsets.UTF_8), true);
                socket = s;
                out = w;
                Platform.runLater(() -> {
                    status.setText("Connected");
                    System.out.println("[CTRL] connected");
                });
            } catch (Exception ex) {
                Platform.runLater(() -> status.setText("Failed to connect"));
            }
        }, "ExtCtrl-Connect").start();
    }


    private void disconnect() {
        try { if (out != null) out.flush(); } catch (Exception ignored) {}
        try { if (socket != null) socket.close(); } catch (Exception ignored) {}
        out = null;
        socket = null;
        status.setText("Disconnected");
    }

    private void send(String line) {
        try {
            if (out != null) { out.println(line); }
        } catch (Exception ignored) {}
    }

    public static void main(String[] args) { launch(args); }
}
