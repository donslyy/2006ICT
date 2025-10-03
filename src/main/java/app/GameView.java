package app;

import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.*;

public class GameView extends ScreenBase {
    interface Player {
        int cols();
        int rows();
        int[][] board();
        int pieceId();
        int pieceRow();
        int pieceCol();
        int pieceRot();
        int[][] pieceShape(int rot);
        boolean canPlace(int row, int col, int rot);
        void moveLeft();
        void moveRight();
        void softDrop();
        void hardDrop();
        void rotate();
    }


    private int COLS, ROWS;
    private static final int CELL = 24;
    private static final int LOCK_DELAY_MS = 500;
    private static final int CLEAR_FLASH_MS = 350;

    private static final Color BG = Color.web("#111418");
    private static final Color GRID = Color.web("#2a2f3a");

    private final Color[] colors = {
            Color.TRANSPARENT,
            Color.CYAN, Color.YELLOW, Color.PURPLE, Color.LIMEGREEN,
            Color.RED, Color.BLUE, Color.ORANGE
    };

    private Stage stage;
    private boolean paused = false;
    private boolean active = false;
    private Scene sceneRef = null;
    private boolean gameOverShown = false;



    private final List<Tetromino> seq = new ArrayList<>();
    private int seqWritePtr = 0;

    private PState p1, p2;

    private Canvas playCanvas1, playCanvas2, preview1, preview2;
    private Label topScoreLbl;

    private Timeline gravityTick;
    private final SimpleAI ai = new SimpleAI();

    private boolean isExtendTwoPlayer() {
        var cfg = ConfigService.getInstance();
        return cfg.isExtendedMode() && cfg.getMode() == ConfigService.Mode.TWO_PLAYER;
    }

    public Scene create(Stage stage) {
        this.stage = stage;

        applyConfig();

        var cfg = ConfigService.getInstance();
        p1 = new PState(1, cfg.getPlayer1Type(), COLS, ROWS);
        p2 = isExtendTwoPlayer() ? new PState(2, cfg.getPlayer2Type(), COLS, ROWS) : null;

        BorderPane root = new BorderPane();

        topScoreLbl = new Label("Scores");
        topScoreLbl.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        var header = new HBox(topScoreLbl);
        header.setAlignment(Pos.CENTER);
        header.setPadding(new Insets(10, 0, 0, 0));

        var fieldsRow = new HBox(16);
        fieldsRow.setAlignment(Pos.CENTER);
        fieldsRow.setPadding(new Insets(10));

        playCanvas1 = new Canvas(COLS * CELL, ROWS * CELL);
        preview1 = new Canvas(6 * CELL, 6 * CELL);
        VBox side1 = buildSidebar("Player 1", p1, preview1);
        HBox p1Block = new HBox(10, playCanvas1, side1);
        p1Block.setAlignment(Pos.CENTER_LEFT);
        fieldsRow.getChildren().add(p1Block);

        boolean twoPlayer = (p2 != null);
        if (twoPlayer) {
            playCanvas2 = new Canvas(COLS * CELL, ROWS * CELL);
            preview2 = new Canvas(6 * CELL, 6 * CELL);
            VBox side2 = buildSidebar("Player 2", p2, preview2);
            HBox p2Block = new HBox(10, playCanvas2, side2);
            p2Block.setAlignment(Pos.CENTER_LEFT);
            fieldsRow.getChildren().add(new Separator());
            fieldsRow.getChildren().add(p2Block);
        }

        Button back = new Button("Back");
        back.setFocusTraversable(false);
        back.setDefaultButton(false);
        back.setOnAction(e -> {
            active = false;
            if (gravityTick != null) gravityTick.stop();
            if (p1 != null && p1.lockTimer != null) { p1.lockTimer.stop(); p1.lockTimer = null; }
            if (p2 != null && p2.lockTimer != null) { p2.lockTimer.stop(); p2.lockTimer = null; }
            stopExternal(p1);
            stopExternal(p2);
            stage.setScene(Main.buildMenuScene(stage));
        });
        back.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, ev -> {
            if (ev.getCode() == KeyCode.SPACE) ev.consume();
        });
        var footer = new HBox(back);
        footer.setAlignment(Pos.CENTER);
        footer.setPadding(new Insets(10));

        root.setTop(header);
        root.setCenter(fieldsRow);
        root.setBottom(footer);
        root.setPrefSize(twoPlayer ? 1000 : 600, 720);

        Scene scene = sceneWithTopTitleAndCenter(new StackPane(root), "Play");
        this.sceneRef = scene;
        this.active = true;
        scene.windowProperty().addListener((obs, oldWin, newWin) -> {
            if (newWin != null) {
                newWin.setOnHidden(ev -> active = false);
            }
        });

        playCanvas1.setFocusTraversable(true);
        playCanvas1.setOnMouseClicked(ev -> playCanvas1.requestFocus());
        if (playCanvas2 != null) {
            playCanvas2.setFocusTraversable(true);
            playCanvas2.setOnMouseClicked(ev -> playCanvas2.requestFocus());
        }
        Platform.runLater(() -> {
            if (playCanvas1 != null) playCanvas1.requestFocus();
        });

        // input mapping
        scene.setOnKeyPressed(e -> {
            var cfgNow = ConfigService.getInstance();
            // global toggles
            switch (e.getCode()) {
                case P -> { togglePause(); return; }
                case S -> { cfgNow.setSfxEnabled(!cfgNow.isSfxEnabled()); JsonConfigRepository.save(cfgNow); return; }
                case M -> { cfgNow.setMusicEnabled(!cfgNow.isMusicEnabled()); JsonConfigRepository.save(cfgNow); return; }
                default -> {}
            }
            if (paused) return;

            if (isExtendTwoPlayer()) {
                handleP1_Controls_Extend(e.getCode());
                if (p2 != null) handleP2_Controls_Extend(e.getCode());
            } else {
                handleP1_Controls_Single(e.getCode());
            }

            redrawAll();
        });

        var cs = ConfigService.getInstance();
        cs.addListener(c -> {
            if (!active || stage.getScene() != scene) return;

            boolean sizeChanged = (c.getFieldWidth() != COLS || c.getFieldHeight() != ROWS);
            boolean wantP2 = isExtendTwoPlayer();
            boolean haveP2 = (p2 != null);

            if (sizeChanged) {
                applyConfig();
            }

            if (wantP2 != haveP2) {
                stage.setScene(create(stage));
                return;
            }

            if (sizeChanged) {
                playCanvas1.setWidth(COLS * CELL);
                playCanvas1.setHeight(ROWS * CELL);
                if (playCanvas2 != null) {
                    playCanvas2.setWidth(COLS * CELL);
                    playCanvas2.setHeight(ROWS * CELL);
                }
                newGame();
            }
        });

        newGame();
        redrawAll();
        return scene;
    }

    private VBox buildSidebar(String title, PState target, Canvas previewCanvas) {
        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        Label typeLbl    = new Label();
        Label initLvlLbl = new Label();
        Label lvlLbl     = new Label();
        Label linesLbl   = new Label();
        Label scoreLbl   = new Label();
        Label hiLbl      = new Label("High Score: " + topScore());

        VBox info = new VBox(6, titleLbl, typeLbl, initLvlLbl, lvlLbl, linesLbl, scoreLbl, hiLbl);
        info.setAlignment(Pos.TOP_CENTER);

        if (target.type == ConfigService.PlayerType.EXTERNAL) {
            Label connLbl = new Label("External: disconnected (start server on localhost:3000)");
            info.getChildren().add(1, connLbl); // insert right under the title
            target.uiConn = connLbl;
        }

        Label nextLbl = new Label("Next Piece");
        VBox next = new VBox(6, nextLbl, previewCanvas);
        next.setAlignment(Pos.TOP_CENTER);

        VBox box = new VBox(10, info, new Separator(), next);
        box.setAlignment(Pos.TOP_CENTER);
        box.setPadding(new Insets(6));

        target.uiType  = typeLbl;
        target.uiInit  = initLvlLbl;
        target.uiLevel = lvlLbl;
        target.uiLines = linesLbl;
        target.uiScore = scoreLbl;
        target.uiHigh  = hiLbl;

        return box;
    }

    private void applyConfig() {
        var cfg = ConfigService.getInstance();
        COLS = cfg.getFieldWidth();
        ROWS = cfg.getFieldHeight();
    }

    private void handleP1_Controls_Single(KeyCode code) {
        if (p1.type != ConfigService.PlayerType.HUMAN) return;
        switch (code) {
            case COMMA, LEFT -> p1.moveLeft();
            case PERIOD, RIGHT -> p1.moveRight();
            case SPACE, DOWN -> p1.softDrop();
            case L, UP -> p1.rotate();
            default -> {}
        }
    }

    private void handleP1_Controls_Extend(KeyCode code) {
        if (p1.type != ConfigService.PlayerType.HUMAN) return;
        switch (code) {
            case COMMA -> p1.moveLeft();
            case PERIOD -> p1.moveRight();
            case SPACE -> p1.softDrop();
            case L -> p1.rotate();
            default -> {}
        }
    }

    private void handleP2_Controls_Extend(KeyCode code) {
        if (p2 == null || p2.type != ConfigService.PlayerType.HUMAN) return;
        switch (code) {
            case LEFT  -> p2.moveLeft();
            case RIGHT -> p2.moveRight();
            case DOWN, SPACE -> p2.softDrop();
            case UP    -> p2.rotate();
            default -> {}
        }
    }


    private void newGame() {
        gameOverShown = false;
        ensureSeq();

        resetPlayer(p1);
        if (p2 != null) resetPlayer(p2);

        spawn(p1);
        if (p2 != null) spawn(p2);

        stopExternal(p1);
        stopExternal(p2);

        startExternal(p1);
        startExternal(p2);

        if (gravityTick != null) gravityTick.stop();
        gravityTick = new Timeline(new KeyFrame(Duration.millis(gravMs(p1.level)), e -> {
            if (!paused) {
                tick(p1);
                if (p1.type == ConfigService.PlayerType.AI) ai.step(p1);
                if (p2 != null) {
                    tick(p2);
                    if (p2.type == ConfigService.PlayerType.AI) ai.step(p2);
                }
                redrawAll();
                if (p1.type == ConfigService.PlayerType.EXTERNAL && p1.client != null) {
                    p1.client.sendJson(snapshotJson(p1));
                }
                if (p2 != null && p2.type == ConfigService.PlayerType.EXTERNAL && p2.client != null) {
                    p2.client.sendJson(snapshotJson(p2));
                }
            }
        }));
        gravityTick.setCycleCount(Timeline.INDEFINITE);
        gravityTick.playFromStart();

        if (p1.type == ConfigService.PlayerType.EXTERNAL && p1.client != null) {
            p1.client.sendJson(snapshotJson(p1));
        }
        if (p2 != null && p2.type == ConfigService.PlayerType.EXTERNAL && p2.client != null) {
            p2.client.sendJson(snapshotJson(p2));
        }

        updateAllLabels();
    }

    private void startExternal(PState p) {
        if (p == null || p.type != ConfigService.PlayerType.EXTERNAL) return;
        if (p.clientThread != null && p.clientThread.isAlive()) return;

        System.out.println("[EXT] startExternal P" + p.id + " connecting...");

        p.client = new ExternalClient(
                "localhost",
                3000,
                cmd -> Platform.runLater(() -> {
                    if (p.dead || p.clearing) return;
                    switch (cmd) {
                        case "LEFT"   -> p.moveLeft();
                        case "RIGHT"  -> p.moveRight();
                        case "DOWN"   -> p.softDrop();
                        case "ROTATE" -> p.rotate();
                        case "DROP"   -> p.hardDrop();
                        case "PAUSE"  -> togglePause();
                        default -> {}
                    }
                    redrawAll();
                    if (p.client != null) p.client.sendJson(snapshotJson(p));
                }),
                connected -> Platform.runLater(() -> {
                    if (p.uiConn != null) {
                        p.uiConn.setText(connected ? "External: connected" : "External: disconnected");
                    }
                    System.out.println("[EXT] P" + p.id + " connected=" + connected);
                }),
                () -> snapshotJson(p)
        );

        p.clientThread = new Thread(p.client, "ExternalClient-P" + p.id);
        p.clientThread.setDaemon(true);
        p.clientThread.start();
    }

    private void stopExternal(PState p) {
        if (p == null) return;
        try { if (p.client != null) p.client.stop(); } catch (Exception ignored) {}
        p.client = null;
        p.clientThread = null;
    }



    private void resetPlayer(PState p) {
        p.dead = false;
        p.clearing = false;
        p.clearingRows = new int[0];
        for (int r = 0; r < ROWS; r++) Arrays.fill(p.board[r], 0);
        p.score = 0;
        p.lines = 0;
        p.seqIdx = 0;
        p.lockTimer = null;
        p.initLevel = ConfigService.getInstance().getStartLevel();
        p.level = p.initLevel;
    }

    private void togglePause() {
        paused = !paused;
        if (gravityTick != null) {
            if (paused) gravityTick.pause();
            else gravityTick.play();
        }
        redrawAll();
    }

    private void spawn(PState p) {
        ensureSeq();
        if (p.seqIdx >= seq.size()) ensureSeq();
        p.piece = seq.get(p.seqIdx++);
        p.rot = 0;
        p.row = 0;
        p.col = Math.max(0, (COLS / 2) - 2);
        if (!canPlace(p, p.row, p.col, p.rot)) {
            onGameOver(p);
        }
        updateAllLabels();
    }

    private void ensureSeq() {
        int farthestIdx = Math.max(p1 == null ? 0 : p1.seqIdx, p2 == null ? 0 : p2.seqIdx);
        if (seqWritePtr - farthestIdx < 14) {
            List<Tetromino> bag = new ArrayList<>(Arrays.asList(Tetromino.values()));
            bag.remove(Tetromino.NONE);
            Collections.shuffle(bag, new Random());
            seq.addAll(bag);
            seqWritePtr += bag.size();
        }
    }

    private void tick(PState p) {
        if (p.clearing || p.dead) return;
        if (canPlace(p, p.row + 1, p.col, p.rot)) {
            p.row++;
        } else {
            if (p.lockTimer == null) {
                p.lockTimer = new PauseTransition(Duration.millis(LOCK_DELAY_MS));
                p.lockTimer.setOnFinished(e -> { p.lockTimer = null; lockNowOrClear(p); });
                p.lockTimer.playFromStart();
            }
        }
    }

    private void lockNowOrClear(PState p) {
        boolean aboveTop = lockAndCheckAboveTop(p);
        if (aboveTop) { onGameOver(p); return; }

        int[] full = scanFullRows(p.board);
        if (full.length > 0) {
            startFlashThenClear(p, full);
        } else {
            spawn(p);
        }
    }

    private boolean lockAndCheckAboveTop(PState p) {
        boolean aboveTop = false;
        int id = p.piece.id();
        int[][] s = p.piece.shape(p.rot);
        for (int r = 0; r < 4; r++) for (int c = 0; c < 4; c++) {
            if (s[r][c] == 0) continue;
            int rr = p.row + r, cc = p.col + c;
            if (rr < 0) { aboveTop = true; continue; }
            if (rr >= 0 && rr < ROWS && cc >= 0 && cc < COLS) p.board[rr][cc] = id;
        }
        return aboveTop;
    }

    private void startFlashThenClear(PState p, int[] rows) {
        p.clearing = true;
        p.clearingRows = rows;
        redrawAll();
        PauseTransition flash = new PauseTransition(Duration.millis(CLEAR_FLASH_MS));
        flash.setOnFinished(ev -> {
            applyRowClear(p.board, rows);
            p.clearing = false;
            p.clearingRows = new int[0];
            int add = scoreForLines(rows.length);
            p.score += add;
            p.lines += rows.length;
            int target = p.initLevel + (p.lines / 10);
            if (target > p.level) {
                p.level = target;
                rebuildGravity(); // speeds up
            }
            updateAllLabels();
            spawn(p);
            redrawAll();
        });
        flash.playFromStart();
    }

    private void rebuildGravity() {
        if (gravityTick == null) return;
        gravityTick.stop();
        gravityTick.getKeyFrames().setAll(new KeyFrame(Duration.millis(gravMs(p1.level)), e -> {
            if (!paused) {
                tick(p1);
                if (p1.type == ConfigService.PlayerType.AI) ai.step(p1);
                if (p2 != null) {
                    tick(p2);
                    if (p2.type == ConfigService.PlayerType.AI) ai.step(p2);
                }
                redrawAll();
            }
        }));
        gravityTick.playFromStart();
    }

    private int gravMs(int lvl) {
        int base = 700;
        int ms = base - (lvl - 1) * 60;
        return Math.max(100, ms);
    }

    private void onGameOver(PState p) {
        p.dead = true;

        if (!active || stage.getScene() != sceneRef) return;

        if (!(p1.dead && (p2 == null || p2.dead))) return;

        if (gameOverShown) return;
        gameOverShown = true;

        if (gravityTick != null) gravityTick.stop();
        if (p1.lockTimer != null) { p1.lockTimer.stop(); p1.lockTimer = null; }
        if (p2 != null && p2.lockTimer != null) { p2.lockTimer.stop(); p2.lockTimer = null; }

        Platform.runLater(() -> {
            if (!active || stage.getScene() != sceneRef) return;
            int best = Math.max(p1.score, p2 == null ? 0 : p2.score);
            var dialog = new javafx.scene.control.TextInputDialog("Devlin Hampson");
            dialog.setTitle("Game Over");
            dialog.setHeaderText("Game Over — Best Score: " + best);
            dialog.setContentText("Enter your name:");
            String name = dialog.showAndWait().orElse("").trim();
            HighScores.add(name, best);
            stage.setScene(HighScoresView.create(stage));
        });
    }

    private void redrawAll() {
        drawField(p1, playCanvas1.getGraphicsContext2D());
        drawPreview(p1, preview1.getGraphicsContext2D(), preview1);
        if (p2 != null) {
            drawField(p2, playCanvas2.getGraphicsContext2D());
            drawPreview(p2, preview2.getGraphicsContext2D(), preview2);
        }
    }

    private void drawField(PState p, GraphicsContext g) {
        g.setFill(BG);
        g.fillRect(0, 0, COLS * CELL, ROWS * CELL);

        for (int r = 0; r < ROWS; r++) for (int c = 0; c < COLS; c++) {
            Color color = colors[p.board[r][c]];
            if (isRowClearing(p, r)) color = Color.WHITE;
            drawCell(g, c, r, color);
        }

        if (!p.clearing && !p.dead) {
            int[][] s = p.piece.shape(p.rot);
            for (int r = 0; r < 4; r++) for (int c = 0; c < 4; c++) {
                if (s[r][c] == 0) continue;
                int rr = p.row + r, cc = p.col + c;
                if (rr >= 0) drawCell(g, cc, rr, colors[p.piece.id()]);
            }
        }
    }

    private boolean isRowClearing(PState p, int r) {
        for (int x : p.clearingRows) if (x == r) return true;
        return false;
    }

    private void drawCell(GraphicsContext g, int c, int r, Color color) {
        int x = c * CELL, y = r * CELL;
        g.setStroke(GRID);
        g.strokeRect(x, y, CELL, CELL);
        if (color == Color.TRANSPARENT) return;
        g.setFill(color);
        g.fillRect(x + 1, y + 1, CELL - 2, CELL - 2);
    }

    private void drawPreview(PState p, GraphicsContext pg, Canvas c) {
        pg.setFill(Color.web("#1a1f27"));
        pg.fillRect(0, 0, c.getWidth(), c.getHeight());
        Tetromino n = peekNext(p);
        if (n == null) return;
        int[][] s = n.shape(0);
        int cell = CELL;
        int w = 4 * cell, h = 4 * cell;
        int offsetX = (int)((c.getWidth() - w) / 2);
        int offsetY = (int)((c.getHeight() - h) / 2);
        for (int r = 0; r < 4; r++) for (int cc = 0; cc < 4; cc++) {
            if (s[r][cc] == 0) continue;
            int x = offsetX + cc * cell;
            int y = offsetY + r * cell;
            pg.setStroke(GRID);
            pg.strokeRect(x, y, cell, cell);
            pg.setFill(colors[n.id()]);
            pg.fillRect(x + 1, y + 1, cell - 2, cell - 2);
        }
    }

    private Tetromino peekNext(PState p) {
        ensureSeq();
        int idx = p.seqIdx;
        if (idx >= seq.size()) return null;
        return seq.get(idx);
    }

    private String snapshotJson(PState p) {
        int[][] grid = new int[ROWS][COLS];
        for (int r = 0; r < ROWS; r++) {
            System.arraycopy(p.board[r], 0, grid[r], 0, COLS);
        }
        if (!p.clearing && !p.dead && p.piece != null) {
            int[][] s = p.piece.shape(p.rot);
            int pid = p.piece.id();
            for (int r = 0; r < 4; r++) for (int c = 0; c < 4; c++) {
                if (s[r][c] == 0) continue;
                int rr = p.row + r, cc = p.col + c;
                if (rr >= 0 && rr < ROWS && cc >= 0 && cc < COLS) grid[rr][cc] = pid;
            }
        }

        int total = ROWS * COLS;
        int[] flat = new int[total];
        int i = 0;
        for (int r = 0; r < ROWS; r++) for (int c = 0; c < COLS; c++) flat[i++] = grid[r][c];

        StringBuilder sb = new StringBuilder(64 + total * 3);
        sb.append('{')
                .append("\"type\":\"state\",")
                .append("\"player\":").append(p.id).append(',')
                .append("\"cols\":").append(COLS).append(',')
                .append("\"rows\":").append(ROWS).append(',')
                .append("\"boxes\":[");
        for (int k = 0; k < flat.length; k++) {
            if (k > 0) sb.append(',');
            sb.append(flat[k]);
        }
        sb.append("]}");
        String json = sb.toString();

        System.out.println("[SNAP] P" + p.id + " rows=" + ROWS + " cols=" + COLS +
                " flatLen=" + flat.length + " jsonLen=" + json.length());

        return json;
    }


    private void updateAllLabels() {
        topScoreLbl.setText(
                String.format("P1 Score %d | P1 Level %d | P1 Lines %d%s",
                        p1.score, p1.level, p1.lines,
                        (p2 != null ? String.format("   ||   P2 Score %d | P2 Level %d | P2 Lines %d", p2.score, p2.level, p2.lines) : "")
                )
        );

        if (p1.uiType != null) {
            p1.uiType.setText("Type: " + p1.type.name());
            p1.uiInit.setText("Initial Level: " + p1.initLevel);
            p1.uiLevel.setText("Current Level: " + p1.level);
            p1.uiLines.setText("Lines: " + p1.lines);
            p1.uiScore.setText("Score: " + p1.score);
            p1.uiHigh.setText("High Score: " + topScore());
        }
        if (p2 != null && p2.uiType != null) {
            p2.uiType.setText("Type: " + p2.type.name());
            p2.uiInit.setText("Initial Level: " + p2.initLevel);
            p2.uiLevel.setText("Current Level: " + p2.level);
            p2.uiLines.setText("Lines: " + p2.lines);
            p2.uiScore.setText("Score: " + p2.score);
            p2.uiHigh.setText("High Score: " + topScore());
        }
    }

    private int scoreForLines(int n) {
        return switch (n) {
            case 1 -> 100;
            case 2 -> 300;
            case 3 -> 600;
            case 4 -> 1000;
            default -> 0;
        };
    }

    private int topScore() {
        var list = HighScores.top();
        return list.isEmpty() ? 0 : list.get(0).score();
    }

    private boolean canPlace(PState p, int r0, int c0, int rot) {
        int[][] s = p.piece.shape(rot);
        for (int r = 0; r < 4; r++) for (int c = 0; c < 4; c++) {
            if (s[r][c] == 0) continue;
            int rr = r0 + r, cc = c0 + c;
            if (cc < 0 || cc >= COLS || rr >= ROWS) return false;
            if (rr >= 0 && p.board[rr][cc] != 0) return false;
        }
        return true;
    }

    private int[] scanFullRows(int[][] board) {
        List<Integer> full = new ArrayList<>();
        for (int r = 0; r < ROWS; r++) {
            boolean all = true;
            for (int c = 0; c < COLS; c++) if (board[r][c] == 0) { all = false; break; }
            if (all) full.add(r);
        }
        return full.stream().mapToInt(Integer::intValue).toArray();
    }

    private void applyRowClear(int[][] board, int[] rows) {
        Set<Integer> toClear = new HashSet<>();
        for (int r : rows) toClear.add(r);
        int write = ROWS - 1;
        for (int r = ROWS - 1; r >= 0; r--) {
            if (!toClear.contains(r)) {
                if (write != r) System.arraycopy(board[r], 0, board[write], 0, COLS);
                write--;
            }
        }
        for (int r = write; r >= 0; r--) Arrays.fill(board[r], 0);
    }

    private final class PState implements Player {
        final int id;
        final ConfigService.PlayerType type;
        final int[][] board;

        Tetromino piece;
        int row, col, rot;

        int score = 0, lines = 0, level, initLevel;
        boolean clearing = false, dead = false;
        int[] clearingRows = new int[0];

        int seqIdx = 0;
        PauseTransition lockTimer;

        Label uiType, uiInit, uiLevel, uiLines, uiScore, uiHigh, uiConn;

        java.util.concurrent.ConcurrentLinkedQueue<String> extQueue = new java.util.concurrent.ConcurrentLinkedQueue<>();
        ExternalClient client;
        Thread clientThread;

        PState(int id, ConfigService.PlayerType type, int cols, int rows) {
            this.id = id;
            this.type = type;
            this.board = new int[rows][cols];
            this.initLevel = ConfigService.getInstance().getStartLevel();
            this.level = this.initLevel;
        }

        @Override public int cols() { return COLS; }
        @Override public int rows() { return ROWS; }
        @Override public int[][] board() { return board; }
        @Override public int pieceId() { return piece == null ? 0 : piece.id(); }
        @Override public int pieceRow() { return row; }
        @Override public int pieceCol() { return col; }
        @Override public int pieceRot() { return rot; }
        @Override public int[][] pieceShape(int r) { return piece == null ? new int[4][4] : piece.shape(r); }
        @Override public boolean canPlace(int r0, int c0, int r) { return GameView.this.canPlace(this, r0, c0, r); }

        @Override public void moveLeft()  { tryMove(this, 0, -1); }
        @Override public void moveRight() { tryMove(this, 0,  1); }
        @Override public void softDrop()  { softDropOne(this); }
        @Override public void hardDrop()  { hardDropNow(this); }
        @Override public void rotate()    { tryRotate(this); }
    }


    // movement helpers
    private boolean tryMove(PState p, int dr, int dc) {
        int nr = p.row + dr, nc = p.col + dc;
        if (canPlace(p, nr, nc, p.rot)) {
            p.row = nr; p.col = nc;
            if (p.lockTimer != null && canPlace(p, p.row + 1, p.col, p.rot)) {
                p.lockTimer.stop(); p.lockTimer = null;
            }
            return true;
        }
        return false;
    }
    private boolean tryRotate(PState p) {
        int nr = (p.rot + 1) % 4;
        if (canPlace(p, p.row, p.col, nr)) {
            p.rot = nr;
            if (p.lockTimer != null && canPlace(p, p.row + 1, p.col, p.rot)) {
                p.lockTimer.stop(); p.lockTimer = null;
            }
            return true;
        }
        return false;
    }
    private void softDropOne(PState p) {
        if (canPlace(p, p.row + 1, p.col, p.rot)) p.row++; else startLock(p);
    }
    private void hardDropNow(PState p) {
        if (p.dead || p.clearing) return;

        if (p.lockTimer != null) { p.lockTimer.stop(); p.lockTimer = null; }

        int dist = 0;
        while (canPlace(p, p.row + 1, p.col, p.rot)) { p.row++; dist++; }
        if (dist > 0) p.score += dist * 2;

        lockNowOrClear(p);
    }

    private void startLock(PState p) {
        if (p.lockTimer != null) return;
        p.lockTimer = new PauseTransition(Duration.millis(LOCK_DELAY_MS));
        p.lockTimer.setOnFinished(ev -> { p.lockTimer = null; lockNowOrClear(p); });
        p.lockTimer.playFromStart();
    }

    private enum Tetromino implements Piece {
        NONE(0, new int[][]{{0,0,0,0},{0,0,0,0},{0,0,0,0},{0,0,0,0}}),
        I(1, new int[][]{{0,0,0,0},{1,1,1,1},{0,0,0,0},{0,0,0,0}}),
        O(2, new int[][]{{0,1,1,0},{0,1,1,0},{0,0,0,0},{0,0,0,0}}),
        T(3, new int[][]{{0,1,0,0},{1,1,1,0},{0,0,0,0},{0,0,0,0}}),
        S(4, new int[][]{{0,1,1,0},{1,1,0,0},{0,0,0,0},{0,0,0,0}}),
        Z(5, new int[][]{{1,1,0,0},{0,1,1,0},{0,0,0,0},{0,0,0,0}}),
        J(6, new int[][]{{1,0,0,0},{1,1,1,0},{0,0,0,0},{0,0,0,0}}),
        L(7, new int[][]{{0,0,1,0},{1,1,1,0},{0,0,0,0},{0,0,0,0}});

        private final int id;
        private final int[][] base;
        Tetromino(int id, int[][] base) { this.id = id; this.base = base; }
        public int id() { return id; }
        public int[][] shape(int rot) {
            int[][] m = base;
            for (int i = 0; i < rot; i++) m = rotCW(m);
            return m;
        }
        private static int[][] rotCW(int[][] m) {
            int[][] r = new int[4][4];
            for (int i = 0; i < 4; i++) for (int j = 0; j < 4; j++) r[j][3 - i] = m[i][j];
            return r;
        }
    }
}
