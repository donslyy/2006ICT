package app;

import java.util.Random;

final class SimpleAI {
    private final Random rng = new Random();

    void step(GameView.Player p) {
        // occasionally rotate/move toward center, then drop
        if (rng.nextDouble() < 0.3) p.rotate();
        int center = p.cols() / 2;
        if (p.col() < center && rng.nextDouble() < 0.6) p.moveRight();
        else if (p.col() > center && rng.nextDouble() < 0.6) p.moveLeft();
        if (rng.nextDouble() < 0.5) p.softDrop();
        if (rng.nextDouble() < 0.25) p.hardDrop();
    }
}
