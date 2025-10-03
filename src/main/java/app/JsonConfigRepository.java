package app;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;

public final class JsonConfigRepository {
    private static final Path FILE = Paths.get("config.json");

    public static void save(ConfigService c) {
        String json = "{\n" +
                " \"fieldWidth\": " + c.getFieldWidth() + ",\n" +
                " \"fieldHeight\": " + c.getFieldHeight() + ",\n" +
                " \"blockSize\": " + c.getBlockSize() + ",\n" +
                " \"startLevel\": " + c.getStartLevel() + ",\n" +
                " \"musicEnabled\": " + c.isMusicEnabled() + ",\n" +
                " \"sfxEnabled\": " + c.isSfxEnabled() + ",\n" +
                " \"aiPlay\": " + c.isAiPlay() + ",\n" +
                " \"extendedMode\": " + c.isExtendedMode() + ",\n" +
                " \"mode\": \"" + c.getMode().name() + "\",\n" +
                " \"player1Type\": \"" + c.getPlayer1Type().name() + "\",\n" +
                " \"player2Type\": \"" + c.getPlayer2Type().name() + "\",\n" +
                " \"serverHost\": \"" + esc(c.getServerHost()) + "\",\n" +
                " \"serverPort\": " + c.getServerPort() + "\n" +
                "}\n";
        try {
            Files.writeString(FILE, json, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException ignored) {}
    }

    public static void loadInto(ConfigService c) {
        if (!Files.exists(FILE)) return;
        try {
            String json = Files.readString(FILE, StandardCharsets.UTF_8);
            Map<String,String> m = flat(json);
            if (m.containsKey("fieldWidth"))  c.setFieldWidth(i(m.get("fieldWidth"), c.getFieldWidth()));
            if (m.containsKey("fieldHeight")) c.setFieldHeight(i(m.get("fieldHeight"), c.getFieldHeight()));
            if (m.containsKey("blockSize"))   c.setBlockSize(i(m.get("blockSize"), c.getBlockSize()));
            if (m.containsKey("startLevel"))  c.setStartLevel(i(m.get("startLevel"), c.getStartLevel()));
            if (m.containsKey("musicEnabled"))c.setMusicEnabled(Boolean.parseBoolean(m.get("musicEnabled")));
            if (m.containsKey("sfxEnabled"))  c.setSfxEnabled(Boolean.parseBoolean(m.get("sfxEnabled")));
            if (m.containsKey("aiPlay"))      c.setAiPlay(Boolean.parseBoolean(m.get("aiPlay")));
            if (m.containsKey("extendedMode"))c.setExtendedMode(Boolean.parseBoolean(m.get("extendedMode")));
            if (m.containsKey("mode"))        c.setMode(ConfigService.Mode.valueOf(m.get("mode")));
            if (m.containsKey("player1Type")) c.setPlayer1Type(ConfigService.PlayerType.valueOf(m.get("player1Type")));
            if (m.containsKey("player2Type")) c.setPlayer2Type(ConfigService.PlayerType.valueOf(m.get("player2Type")));
            if (m.containsKey("serverHost"))  c.setServerHost(m.get("serverHost"));
            if (m.containsKey("serverPort"))  c.setServerPort(i(m.get("serverPort"), c.getServerPort()));
        } catch (IOException ignored) {}
    }

    private static String esc(String s){ return s.replace("\"","\\\""); }
    private static int i(String s, int def){ try { return Integer.parseInt(s); } catch(Exception e){ return def; } }

    private static Map<String,String> flat(String json){
        Map<String,String> r = new HashMap<>();
        String body = json.trim();
        if (body.startsWith("{")) body = body.substring(1);
        if (body.endsWith("}")) body = body.substring(0, body.length()-1);
        for (String part : body.split(",\\s*\\n?")) {
            int k = part.indexOf(':'); if (k < 0) continue;
            String key = unq(part.substring(0, k).trim());
            String val = unq(part.substring(k + 1).trim());
            r.put(key, val);
        }
        return r;
    }
    private static String unq(String s){
        s = s.trim();
        if (s.startsWith("\"") && s.endsWith("\"") && s.length() >= 2)
            return s.substring(1, s.length()-1).replace("\\\"","\"");
        return s;
    }
}
