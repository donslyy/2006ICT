package app;

import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.*;

public class GameView extends ScreenBase {

    private int COLS, ROWS;
    private static final int CELL = 24;
    private static final int LOCK_DELAY_MS = 500;
    private static final int CLEAR_FLASH_MS = 350;

    private int[][] board;
    private final Color[] colors = {
            Color.TRANSPARENT,
            Color.CYAN, Color.YELLOW, Color.PURPLE, Color.LIMEGREEN,
            Color.RED, Color.BLUE, Color.ORANGE
    };

    private Tetromino piece;
    private int pr, pc, rotation;

    private boolean paused = false;
    private boolean isClearing = false;
    private Timeline gravity;
    private PauseTransition lockTimer = null;

    private Canvas canvas;
    private Canvas previewCanvas;

    private int score = 0;
    private int level = 1;
    private int initLevel = 1;
    private int lines = 0;

    private Label scoreLbl;
    private Label infoPlayer;
    private Label infoType;
    private Label infoInitLevel;
    private Label infoLevel;
    private Label infoLines;
    private Label infoHighScore;

    private final Deque<Tetromino> bag = new ArrayDeque<>();
    private final Random rng = new Random();
    private int[] clearingRows = new int[0];

    private Stage stage;

    public Scene create(Stage stage) {
        this.stage = stage;

        applyConfigAndResize();

        scoreLbl = new Label();
        scoreLbl.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        infoPlayer = new Label("Player: 1");
        infoType = new Label("Type: " + ConfigService.getInstance().getPlayer1Type().name());
        infoInitLevel = new Label();
        infoLevel = new Label();
        infoLines = new Label();
        infoHighScore = new Label("High Score: " + topScore());

        VBox infoBox = new VBox(6, infoPlayer, infoType, infoInitLevel, infoLevel, infoLines, infoHighScore);
        infoBox.setAlignment(Pos.TOP_CENTER);

        previewCanvas = new Canvas(6 * CELL, 6 * CELL);
        Label nextLbl = new Label("Next Piece");
        VBox nextBox = new VBox(6, nextLbl, previewCanvas);
        nextBox.setAlignment(Pos.TOP_CENTER);

        VBox sidebar = new VBox(12, infoBox, new Separator(), nextBox);
        sidebar.setAlignment(Pos.TOP_CENTER);
        sidebar.setPadding(new Insets(8));

        Button back = new Button("Back");
        back.setFocusTraversable(false);
        back.setOnAction(e -> stage.setScene(Main.buildMenuScene(stage)));

        HBox playRow = new HBox(12, canvas, sidebar);
        playRow.setAlignment(Pos.CENTER);

        VBox content = new VBox(12, scoreLbl, playRow, back);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(18));
        StackPane center = new StackPane(content);
        center.setPadding(new Insets(12));

        Scene scene = sceneWithTopTitleAndCenter(center, "Play");

        scene.setOnKeyPressed(e -> {
            if (paused || isClearing) {
                if (e.getCode() == KeyCode.P) togglePause();
                return;
            }
            switch (e.getCode()) {
                case LEFT, A  -> moved(tryMove(0, -1));
                case RIGHT, D -> moved(tryMove(0,  1));
                case DOWN, S  -> softDropOne();
                case UP, W    -> moved(tryRotate());
                case SPACE    -> hardDrop();
                case P        -> togglePause();
                default -> {}
            }
            draw();
        });
        scene.setOnMouseClicked(ev -> canvas.requestFocus());

        ConfigService.getInstance().addListener(c -> {
            int w = c.getFieldWidth(), h = c.getFieldHeight();
            if (w != COLS || h != ROWS) {
                COLS = w; ROWS = h;
                applyConfigAndResize();
                newGame();
                draw();
            }
            infoType.setText("Type: " + c.getPlayer1Type().name());
        });

        newGame();
        draw();
        canvas.requestFocus();
        return scene;
    }

    private void applyConfigAndResize() {
        var cfg = ConfigService.getInstance();
        COLS = cfg.getFieldWidth();
        ROWS = cfg.getFieldHeight();
        board = new int[ROWS][COLS];
        if (canvas == null) canvas = new Canvas();
        canvas.setWidth(COLS * CELL);
        canvas.setHeight(ROWS * CELL);
        canvas.setFocusTraversable(true);
    }

    private void newGame() {
        for (var row : board) Arrays.fill(row, 0);
        score = 0;
        lines = 0;
        initLevel = ConfigService.getInstance().getStartLevel();
        level = initLevel;
        updateHud();
        isClearing = false;
        cancelLockTimer();
        bag.clear();
        refillBagIfNeeded();
        spawnFromBag();
        if (gravity != null) gravity.stop();
        gravity = new Timeline(new KeyFrame(Duration.millis(gravityMs(level)), e -> {
            if (!paused && !isClearing) { tick(); draw(); }
        }));
        gravity.setCycleCount(Timeline.INDEFINITE);
        gravity.playFromStart();
    }

    private int gravityMs(int lvl) {
        int base = 700;
        int ms = base - (lvl - 1) * 60;
        if (ms < 100) ms = 100;
        return ms;
    }

    private void moved(boolean didMove) {
        if (didMove && lockTimer != null) {
            if (canPlace(pr + 1, pc, rotation)) cancelLockTimer();
        }
    }

    private void softDropOne() {
        if (paused || isClearing) return;
        if (canPlace(pr + 1, pc, rotation)) {
            pr++;
            addScore(1);
        } else {
            startLockDelay();
        }
    }

    private void hardDrop() {
        if (paused || isClearing) return;
        int dist = 0;
        while (canPlace(pr + 1, pc, rotation)) { pr++; dist++; }
        if (dist > 0) addScore(dist * 2);
        lockNowOrClear();
    }

    private void tick() {
        if (canPlace(pr + 1, pc, rotation)) {
            pr++;
        } else {
            startLockDelay();
        }
    }

    private void startLockDelay() {
        if (lockTimer != null) return;
        lockTimer = new PauseTransition(Duration.millis(LOCK_DELAY_MS));
        lockTimer.setOnFinished(ev -> { lockTimer = null; lockNowOrClear(); });
        lockTimer.playFromStart();
    }

    private void cancelLockTimer() {
        if (lockTimer != null) { lockTimer.stop(); lockTimer = null; }
    }

    private void lockNowOrClear() {
        boolean aboveTop = lockPieceAndCheckAboveTop();
        if (aboveTop) { gameOver(); return; }

        int[] full = scanFullRows();
        if (full.length > 0) {
            startClearFlash(full);
        } else {
            if (!spawnFromBag()) { gameOver(); return; }
        }
    }

    private boolean spawnFromBag() {
        refillBagIfNeeded();
        piece = bag.removeFirst();
        rotation = 0; pr = 0; pc = Math.max(0, (COLS / 2) - 2);
        return canPlace(pr, pc, rotation);
    }

    private void refillBagIfNeeded() {
        if (!bag.isEmpty()) return;
        List<Tetromino> all = new ArrayList<>(Arrays.asList(Tetromino.values()));
        all.remove(Tetromino.NONE);
        Collections.shuffle(all, rng);
        bag.addAll(all);
    }

    private Optional<Tetromino> nextPiece() {
        if (bag.isEmpty()) refillBagIfNeeded();
        return Optional.ofNullable(bag.peekFirst());
    }

    private boolean tryMove(int dr, int dc) {
        int nr = pr + dr, nc = pc + dc;
        if (canPlace(nr, nc, rotation)) { pr = nr; pc = nc; return true; }
        return false;
    }

    private boolean tryRotate() {
        int nr = (rotation + 1) % 4;
        if (canPlace(pr, pc, nr)) { rotation = nr; return true; }
        return false;
    }

    private boolean canPlace(int r0, int c0, int rot) {
        int[][] s = piece.shape(rot);
        for (int r = 0; r < 4; r++) for (int c = 0; c < 4; c++) {
            if (s[r][c] == 0) continue;
            int rr = r0 + r, cc = c0 + c;
            if (cc < 0 || cc >= COLS || rr >= ROWS) return false;
            if (rr >= 0 && board[rr][cc] != 0) return false;
        }
        return true;
    }

    private boolean lockPieceAndCheckAboveTop() {
        boolean aboveTop = false;
        int id = piece.id();
        int[][] s = piece.shape(rotation);
        for (int r = 0; r < 4; r++) for (int c = 0; c < 4; c++) {
            if (s[r][c] == 0) continue;
            int rr = pr + r, cc = pc + c;
            if (rr < 0) { aboveTop = true; continue; }
            if (rr >= 0 && rr < ROWS && cc >= 0 && cc < COLS) board[rr][cc] = id;
        }
        return aboveTop;
    }

    private int[] scanFullRows() {
        List<Integer> full = new ArrayList<>();
        for (int r = 0; r < ROWS; r++) {
            boolean all = true;
            for (int c = 0; c < COLS; c++) if (board[r][c] == 0) { all = false; break; }
            if (all) full.add(r);
        }
        return full.stream().mapToInt(Integer::intValue).toArray();
    }

    private void startClearFlash(int[] rows) {
        isClearing = true;
        clearingRows = rows;
        draw();
        PauseTransition flash = new PauseTransition(Duration.millis(CLEAR_FLASH_MS));
        flash.setOnFinished(ev -> {
            applyRowClear(rows);
            isClearing = false;
            clearingRows = new int[0];
            if (!spawnFromBag()) { gameOver(); return; }
            draw();
        });
        flash.playFromStart();
        addScore(scoreForLines(rows.length));
        lines += rows.length;
        levelUpIfNeeded();
        updateHud();
        rebuildGravityIfNeeded();
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

    private void levelUpIfNeeded() {
        int target = initLevel + (lines / 10);
        if (target > level) level = target;
    }

    private void rebuildGravityIfNeeded() {
        if (gravity != null) {
            gravity.stop();
            gravity.getKeyFrames().setAll(new KeyFrame(Duration.millis(gravityMs(level)), e -> {
                if (!paused && !isClearing) { tick(); draw(); }
            }));
            gravity.playFromStart();
        }
    }

    private void applyRowClear(int[] rows) {
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

    private void gameOver() {
        if (gravity != null) gravity.stop();
        cancelLockTimer();
        var dialog = new javafx.scene.control.TextInputDialog("Devlin Hampson");
        dialog.setTitle("Game Over");
        dialog.setHeaderText("Game Over — Score: " + score);
        dialog.setContentText("Enter your name:");
        String name = dialog.showAndWait().orElse("").trim();
        HighScores.add(name, score);
        stage.setScene(HighScoresView.create(stage));
    }

    private void togglePause() {
        paused = !paused;
        if (paused) { if (gravity != null) gravity.pause(); cancelLockTimer(); }
        else if (gravity != null) gravity.play();
        draw();
    }

    private void draw() {
        GraphicsContext g = canvas.getGraphicsContext2D();
        g.setFill(Color.web("#111418"));
        g.fillRect(0, 0, COLS * CELL, ROWS * CELL);

        for (int r = 0; r < ROWS; r++) for (int c = 0; c < COLS; c++) {
            Color color = colors[board[r][c]];
            if (isRowClearing(r)) color = Color.WHITE;
            drawCell(g, c, r, color);
        }

        if (!isClearing) {
            int[][] s = piece.shape(rotation);
            for (int r = 0; r < 4; r++) for (int c = 0; c < 4; c++) {
                if (s[r][c] == 0) continue;
                int rr = pr + r, cc = pc + c;
                if (rr >= 0) drawCell(g, cc, rr, colors[piece.id()]);
            }
        }

        drawPreview();
    }

    private void updateHud() {
        scoreLbl.setText("Score: " + score);
        infoInitLevel.setText("Initial Level: " + initLevel);
        infoLevel.setText("Current Level: " + level);
        infoLines.setText("Lines Cleared: " + lines);
        infoHighScore.setText("High Score: " + topScore());
    }

    private int topScore() {
        var list = HighScores.top();
        return list.isEmpty() ? 0 : list.get(0).score();
    }

    private boolean isRowClearing(int r) {
        for (int x : clearingRows) if (x == r) return true;
        return false;
    }

    private void drawCell(GraphicsContext g, int c, int r, Color color) {
        int x = c * CELL, y = r * CELL;
        g.setStroke(Color.web("#2a2f3a"));
        g.strokeRect(x, y, CELL, CELL);
        if (color == Color.TRANSPARENT) return;
        var cell = new Cell(r, c, color);
        g.setFill(cell.color());
        g.fillRect(x + 1, y + 1, CELL - 2, CELL - 2);
    }

    private void drawPreview() {
        var pg = previewCanvas.getGraphicsContext2D();
        pg.setFill(Color.web("#1a1f27"));
        pg.fillRect(0, 0, previewCanvas.getWidth(), previewCanvas.getHeight());
        Optional<Tetromino> np = nextPiece();
        if (np.isEmpty()) return;
        Tetromino t = np.get();
        int[][] s = t.shape(0);
        int cell = CELL;
        int w = 4 * cell, h = 4 * cell;
        int offsetX = (int)((previewCanvas.getWidth() - w) / 2);
        int offsetY = (int)((previewCanvas.getHeight() - h) / 2);
        for (int r = 0; r < 4; r++) for (int c = 0; c < 4; c++) {
            if (s[r][c] == 0) continue;
            int x = offsetX + c * cell;
            int y = offsetY + r * cell;
            pg.setStroke(Color.web("#2a2f3a"));
            pg.strokeRect(x, y, cell, cell);
            pg.setFill(colors[t.id()]);
            pg.fillRect(x + 1, y + 1, cell - 2, cell - 2);
        }
    }

    private void addScore(int delta) { score += delta; updateHud(); }

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
