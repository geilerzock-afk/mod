package de.visitedwaypoints;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class WaypointStore {
    public record Waypoint(String id, String name, double x, double y, double z, boolean yIncluded) {}

    public static class Config {
        public int radius = 5;             // Radius in Blöcken (horizontal)
        public int verticalTolerance = 6;  // erlaubter Höhenunterschied
        public boolean hud = true;
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path dir = FabricLoader.getInstance().getConfigDir().resolve("visitedwaypoints");
    private final Path sourceFile = dir.resolve("overworld.json");
    private final Path visitedFile = dir.resolve("visited.json");
    private final Path configFile = dir.resolve("config.json");
    private final Path exportFile = dir.resolve("overworld_remaining.json");

    private JsonObject root;
    private final List<Waypoint> all = new ArrayList<>();
    private final List<Waypoint> remaining = new ArrayList<>();
    private final Set<String> visited = new HashSet<>();
    private Config config = new Config();

    public Config config() { return config; }
    public int total() { return all.size(); }
    public List<Waypoint> remaining() { return Collections.unmodifiableList(remaining); }

    public void load() {
        try {
            Files.createDirectories(dir);
            if (!Files.exists(sourceFile)) {
                try (InputStream in = WaypointStore.class.getResourceAsStream("/default_overworld.json")) {
                    if (in != null) Files.copy(in, sourceFile);
                }
            }

            all.clear();
            visited.clear();
            root = null;

            if (Files.exists(sourceFile)) {
                root = JsonParser.parseString(Files.readString(sourceFile, StandardCharsets.UTF_8)).getAsJsonObject();
                for (Map.Entry<String, JsonElement> set : root.getAsJsonObject("sets").entrySet()) {
                    JsonArray arr = set.getValue().getAsJsonObject().getAsJsonArray("waypoints");
                    for (JsonElement el : arr) {
                        JsonObject o = el.getAsJsonObject();
                        if (o.has("disabled") && o.get("disabled").getAsBoolean()) continue;
                        all.add(new Waypoint(
                                o.get("id").getAsString(),
                                o.has("name") ? o.get("name").getAsString() : "?",
                                o.get("x").getAsDouble(),
                                o.get("y").getAsDouble(),
                                o.get("z").getAsDouble(),
                                !o.has("yIncluded") || o.get("yIncluded").getAsBoolean()));
                    }
                }
            }

            if (Files.exists(visitedFile)) {
                for (JsonElement el : JsonParser.parseString(Files.readString(visitedFile, StandardCharsets.UTF_8)).getAsJsonArray()) {
                    visited.add(el.getAsString());
                }
            }

            if (Files.exists(configFile)) {
                Config c = GSON.fromJson(Files.readString(configFile, StandardCharsets.UTF_8), Config.class);
                if (c != null) config = c;
            } else {
                saveConfig();
            }
        } catch (Exception e) {
            VisitedWaypointsClient.LOGGER.error("Konnte Waypoints nicht laden", e);
        }
        rebuildRemaining();
    }

    private void rebuildRemaining() {
        remaining.clear();
        for (Waypoint w : all) if (!visited.contains(w.id())) remaining.add(w);
    }

    public void markVisited(Waypoint w) {
        visited.add(w.id());
        remaining.remove(w);
        saveVisited();
    }

    public void reset() {
        visited.clear();
        rebuildRemaining();
        saveVisited();
    }

    public void saveVisited() {
        try {
            Files.createDirectories(dir);
            JsonArray arr = new JsonArray();
            for (String id : visited) arr.add(id);
            Files.writeString(visitedFile, GSON.toJson(arr), StandardCharsets.UTF_8);
        } catch (IOException e) {
            VisitedWaypointsClient.LOGGER.error("Konnte visited.json nicht speichern", e);
        }
    }

    public void saveConfig() {
        try {
            Files.createDirectories(dir);
            Files.writeString(configFile, GSON.toJson(config), StandardCharsets.UTF_8);
        } catch (IOException e) {
            VisitedWaypointsClient.LOGGER.error("Konnte config.json nicht speichern", e);
        }
    }

    /** Schreibt die Datei im Originalformat, nur mit den noch nicht besuchten Waypoints. */
    public Path exportRemaining() throws IOException {
        if (root == null) throw new IOException("Keine Waypoint-Datei geladen");
        JsonObject copy = root.deepCopy();
        for (Map.Entry<String, JsonElement> set : copy.getAsJsonObject("sets").entrySet()) {
            JsonObject setObj = set.getValue().getAsJsonObject();
            JsonArray keep = new JsonArray();
            for (JsonElement el : setObj.getAsJsonArray("waypoints")) {
                if (!visited.contains(el.getAsJsonObject().get("id").getAsString())) keep.add(el);
            }
            setObj.add("waypoints", keep);
        }
        Files.writeString(exportFile, GSON.toJson(copy), StandardCharsets.UTF_8);
        return exportFile;
    }
}
