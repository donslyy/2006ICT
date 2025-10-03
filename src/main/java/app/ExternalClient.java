package app;

import javafx.application.Platform;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

final class ExternalClient implements Runnable {
    private final String host;
    private final int port;
    private final Consumer<String> onCommand;
    private final Consumer<Boolean> onConnectionChange; // true=connected, false=disconnected
    private final Supplier<String> initialStateSupplier;

    private volatile boolean running = true;
    private volatile PrintWriter out;

    ExternalClient(String host, int port,
                   Consumer<String> onCommand,
                   Consumer<Boolean> onConnectionChange,
                   Supplier<String> initialStateSupplier) {
        this.host = Objects.requireNonNull(host);
        this.port = port;
        this.onCommand = onCommand;
        this.onConnectionChange = onConnectionChange;
        this.initialStateSupplier = initialStateSupplier;
    }

    public void stop() { running = false; }

    public void sendJson(String jsonLine) {
        try {
            PrintWriter w = this.out;
            if (w != null) {
                synchronized (this) {
                    w.println(jsonLine);
                    w.flush();
                }
                String preview = jsonLine.substring(0, Math.min(200, jsonLine.length()));
                System.out.println("[EXT->SRV] sent " + jsonLine.length() + " bytes: " +
                        preview + (jsonLine.length() > 200 ? "..." : ""));
            } else {
                System.out.println("[EXT->SRV] sendJson skipped (socket not connected yet)");
            }
        } catch (Exception ex) {
            System.out.println("[EXT->SRV] sendJson error: " + ex);
        }
    }


    @Override public void run() {
        while (running) {
            try (Socket socket = new Socket(host, port);
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                 PrintWriter outW = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8)) {

                this.out = outW;
                Platform.runLater(() -> onConnectionChange.accept(true));
                System.out.println("[ExternalClient] Connected to " + host + ":" + port);

                if (initialStateSupplier != null) {
                    String init = initialStateSupplier.get();
                    if (init != null && !init.isBlank()) {
                        outW.println(init);
                        outW.flush();
                        System.out.println("[ExternalClient] SENT(initial): " +
                                init.substring(0, Math.min(200, init.length())) + "...");
                    } else {
                        System.out.println("[ExternalClient] WARNING: initial snapshot is null/blank (nothing sent).");
                    }
                }

                String line;
                while (running && (line = in.readLine()) != null) {
                    String cmd = line.trim().toUpperCase();
                    switch (cmd) {
                        case "LEFT", "RIGHT", "DOWN", "ROTATE", "DROP", "PAUSE" -> onCommand.accept(cmd);
                        case "L" -> onCommand.accept("LEFT");
                        case "R" -> onCommand.accept("RIGHT");
                        case "D" -> onCommand.accept("DOWN");
                        case "U" -> onCommand.accept("ROTATE");
                        case "SPACE", "HARD", "HARDDROP" -> onCommand.accept("DROP");
                        case "P" -> onCommand.accept("PAUSE");
                        default -> { /* ignore unknown */ }
                    }
                }
                System.out.println("[ExternalClient] Disconnected (socket closed).");
            } catch (Exception e) {
                this.out = null;
                Platform.runLater(() -> onConnectionChange.accept(false));
                System.out.println("[ExternalClient] Connection error: " + e.getMessage());
                try { Thread.sleep(1200); } catch (InterruptedException ignored) {}
            } finally {
                this.out = null;
            }
        }
        Platform.runLater(() -> onConnectionChange.accept(false));
        System.out.println("[ExternalClient] Stopped.");
    }
}
