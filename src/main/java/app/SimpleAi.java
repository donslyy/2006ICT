package app;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

final class SimpleAI {
    private static final class Plan { int rot; int col; int pieceId; }
    private final Random rng = new Random();
    private final Map<GameView.Player, Plan> plans = new HashMap<>();

    void step(GameView.Player p) {
        if (p.pieceId() == 0) return;
        Plan plan = plans.get(p);
        if (plan == null || plan.pieceId != p.pieceId() || p.pieceRow() <= 1) {
            plan = choosePlan(p);
            plans.put(p, plan);
        }
        if (plan == null) {
            p.softDrop();
            return;
        }
        if (p.pieceRot() != plan.rot) {
            p.rotate();
        } else if (p.pieceCol() < plan.col) {
            p.moveRight();
        } else if (p.pieceCol() > plan.col) {
            p.moveLeft();
        }
        p.softDrop();
    }

    private Plan choosePlan(GameView.Player p) {
        int bestRot = 0, bestCol = 0;
        double bestScore = Double.NEGATIVE_INFINITY;
        int[][] baseBoard = p.board();
        for (int rot = 0; rot < 4; rot++) {
            int[][] shape = p.pieceShape(rot);
            int minC = -minShapeC(shape);
            int maxC = p.cols() - 1 - maxShapeC(shape);
            for (int col = minC; col <= maxC; col++) {
                int landingRow = dropRow(p, baseBoard, rot, col);
                if (landingRow == Integer.MIN_VALUE) continue;
                EvalResult er = evaluateAfterPlace(baseBoard, shape, landingRow, col);
                double score = score(er);
                if (score > bestScore || (score == bestScore && rng.nextBoolean())) {
                    bestScore = score;
                    bestRot = rot;
                    bestCol = col;
                }
            }
        }
        Plan plan = new Plan();
        plan.rot = bestRot;
        plan.col = bestCol;
        plan.pieceId = p.pieceId();
        return plan;
    }

    private int dropRow(GameView.Player p, int[][] board, int rot, int col) {
        int r = -3;
        while (true) {
            if (!canOnBoard(p, board, rot, r + 1, col)) break;
            r++;
            if (r > p.rows()) return Integer.MIN_VALUE;
        }
        if (!canOnBoard(p, board, rot, r, col)) return Integer.MIN_VALUE;
        return r;
    }

    private boolean canOnBoard(GameView.Player p, int[][] board, int rot, int row, int col) {
        int[][] s = p.pieceShape(rot);
        for (int r = 0; r < 4; r++) for (int c = 0; c < 4; c++) {
            if (s[r][c] == 0) continue;
            int rr = row + r, cc = col + c;
            if (cc < 0 || cc >= p.cols() || rr >= p.rows()) return false;
            if (rr >= 0 && board[rr][cc] != 0) return false;
        }
        return true;
    }

    private static final class EvalResult {
        int linesCleared;
        int holes;
        int aggHeight;
        int bumpiness;
    }

    private EvalResult evaluateAfterPlace(int[][] board, int[][] shape, int row, int col) {
        int rows = board.length, cols = board[0].length;
        int[][] b = new int[rows][cols];
        for (int r = 0; r < rows; r++) System.arraycopy(board[r], 0, b[r], 0, cols);
        for (int r = 0; r < 4; r++) for (int c = 0; c < 4; c++) {
            if (shape[r][c] == 0) continue;
            int rr = row + r, cc = col + c;
            if (rr >= 0 && rr < rows && cc >= 0 && cc < cols) b[rr][cc] = 7;
        }
        int linesCleared = 0;
        for (int r = 0; r < rows; r++) {
            boolean full = true;
            for (int c = 0; c < cols; c++) if (b[r][c] == 0) { full = false; break; }
            if (full) { linesCleared++; for (int c = 0; c < cols; c++) b[r][c] = 0; }
        }
        if (linesCleared > 0) {
            int write = rows - 1;
            for (int r = rows - 1; r >= 0; r--) {
                boolean empty = true;
                for (int c = 0; c < cols; c++) if (b[r][c] != 0) { empty = false; break; }
                if (!empty) {
                    if (write != r) System.arraycopy(b[r], 0, b[write], 0, cols);
                    write--;
                }
            }
            for (int r = write; r >= 0; r--) for (int c = 0; c < cols; c++) b[r][c] = 0;
        }
        int[] heights = new int[cols];
        for (int c = 0; c < cols; c++) {
            heights[c] = 0;
            for (int r = 0; r < rows; r++) {
                if (b[r][c] != 0) { heights[c] = rows - r; break; }
            }
        }
        int holes = 0;
        for (int c = 0; c < cols; c++) {
            boolean seen = false;
            for (int r = 0; r < rows; r++) {
                if (b[r][c] != 0) seen = true;
                else if (seen) holes++;
            }
        }
        int aggHeight = 0;
        for (int h : heights) aggHeight += h;
        int bumpiness = 0;
        for (int c = 0; c + 1 < cols; c++) bumpiness += Math.abs(heights[c] - heights[c + 1]);

        EvalResult er = new EvalResult();
        er.linesCleared = linesCleared;
        er.holes = holes;
        er.aggHeight = aggHeight;
        er.bumpiness = bumpiness;
        return er;
    }

    private double score(EvalResult e) {
        return e.linesCleared * 10000.0
                - e.holes * 150.0
                - e.aggHeight * 5.0
                - e.bumpiness * 15.0;
    }

    private int minShapeC(int[][] s) {
        int m = 3;
        for (int r = 0; r < 4; r++) for (int c = 0; c < 4; c++) if (s[r][c] != 0) m = Math.min(m, c);
        return m;
    }

    private int maxShapeC(int[][] s) {
        int m = 0;
        for (int r = 0; r < 4; r++) for (int c = 0; c < 4; c++) if (s[r][c] != 0) m = Math.max(m, c);
        return m;
    }
}
