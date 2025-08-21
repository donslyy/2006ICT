package app;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public final class HighScores {
    private static final Path FILE = Paths.get("scores.csv");
    private static final int MAX = 10;
    private static final List<Score> list = new ArrayList<>();

    static { load(); }
    private HighScores() {}

    public static synchronized List<Score> top() {
        return List.copyOf(list);
    }

    public static synchronized void add(String name, int score) {
        if (name == null || name.isBlank()) name = "Anonymous";
        list.add(new Score(name.trim(), score));
        list.sort(Comparator.comparingInt(Score::score).reversed());
        if (list.size() > MAX) list.subList(MAX, list.size()).clear();
        save();
    }

    private static void load() {
        list.clear();
        if (Files.exists(FILE)) {
            try {
                for (String line : Files.readAllLines(FILE, StandardCharsets.UTF_8)) {
                    String[] p = line.split(",", 2);
                    if (p.length == 2) {
                        try {
                            list.add(new Score(p[0], Integer.parseInt(p[1].trim())));
                        } catch (NumberFormatException ignored) {}
                    }
                }
            } catch (IOException ignored) {}
            list.sort(Comparator.comparingInt(Score::score).reversed());
            if (list.size() > MAX) list.subList(MAX, list.size()).clear();
        }
    }

    private static void save() {
        try (var w = Files.newBufferedWriter(FILE, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            for (Score s : list) {
                w.write(s.name() + "," + s.score());
                w.newLine();
            }
        } catch (IOException ignored) {}
    }

    public record Score(String name, int score) {}
}
