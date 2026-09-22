package de.Sky.cloudclientmod.server;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.Sky.cloudclientmod.Main;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Lädt die zentralen Server-Mappings von GitHub, cached sie lokal
 * und bietet einen schnellen Lookup für Hostnamen.
 *
 * Ablauf:
 *   1. Beim ersten Aufruf: prüfen ob lokaler Cache existiert und < 24h alt ist
 *   2. Falls nicht: von GitHub laden, Cache schreiben
 *   3. Falls GitHub nicht erreichbar: Fallback (lokale Bundle im JAR)
 *   4. Alle 24h im Hintergrund aktualisieren
 */
public final class ServerMappings {

    // GitHub-URL zur auto-generierten dist/mappings.json
    private static final String BUNDLE_URL =
            "https://raw.githubusercontent.com/DirectSky/cloud-client-mod/main/mappings/dist/mappings.json";

    private static final String BASE_ASSET_URL =
            "https://raw.githubusercontent.com/DirectSky/cloud-client-mod/main/mappings/";

    private static final Duration CACHE_TTL = Duration.ofHours(24);
    private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(15);

    private static final Gson GSON = new Gson();
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(HTTP_TIMEOUT)
            .build();

    /** Pattern -> Mapping (String-Keys sind bereits lowercase). */
    private static volatile Map<String, ServerMapping> flatMappings = Collections.emptyMap();

    /** Hostname-Cache für wiederholte Lookups (z.B. pro Tick). */
    private static final Map<String, Optional<ServerMapping>> lookupCache = new HashMap<>();

    private static volatile boolean initialized = false;
    private static volatile Instant lastFetch = Instant.EPOCH;

    private ServerMappings() {}

    // =================================================================
    // Initialisierung
    // =================================================================

    /** Synchron initialisieren. Wird von Main aufgerufen. */
    public static void init() {
        if (initialized) return;

        // 1) Versuch: lokaler Cache
        Path cache = cacheFile();
        if (Files.exists(cache) && !isCacheStale(cache)) {
            try {
                loadFromJson(Files.readString(cache));
                initialized = true;
                Main.LOGGER.info("[Mappings] Aus Cache geladen ({} Einträge)", flatMappings.size());
                asyncRefresh();
                return;
            } catch (Exception e) {
                Main.LOGGER.warn("[Mappings] Cache kaputt, wird neu geladen: {}", e.getMessage());
            }
        }

        // 2) Versuch: von GitHub laden
        try {
            String json = fetchUrl(BUNDLE_URL);
            loadFromJson(json);
            Files.createDirectories(cache.getParent());
            Files.writeString(cache, json);
            lastFetch = Instant.now();
            initialized = true;
            Main.LOGGER.info("[Mappings] Von GitHub geladen ({} Einträge)", flatMappings.size());
            return;
        } catch (Exception e) {
            Main.LOGGER.warn("[Mappings] GitHub nicht erreichbar: {}", e.getMessage());
        }

        // 3) Fallback: eingebettete Bundle
        try (InputStream in = ServerMappings.class.getResourceAsStream(
                "/assets/cloudclientmod/mappings-fallback.json")) {
            if (in != null) {
                String json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                loadFromJson(json);
                initialized = true;
                Main.LOGGER.info("[Mappings] Fallback geladen ({} Einträge)", flatMappings.size());
            } else {
                Main.LOGGER.error("[Mappings] Kein Fallback im JAR gefunden.");
                flatMappings = Collections.emptyMap();
                initialized = true;
            }
        } catch (Exception e) {
            Main.LOGGER.error("[Mappings] Fallback fehlgeschlagen", e);
            flatMappings = Collections.emptyMap();
            initialized = true;
        }
    }

    /** Lädt im Hintergrund neu, falls Cache > 24h alt. */
    private static void asyncRefresh() {
        if (Duration.between(lastFetch, Instant.now()).compareTo(CACHE_TTL) < 0) return;

        CompletableFuture.runAsync(() -> {
            try {
                String json = fetchUrl(BUNDLE_URL);
                loadFromJson(json);
                Files.createDirectories(cacheFile().getParent());
                Files.writeString(cacheFile(), json);
                lastFetch = Instant.now();
                Main.LOGGER.info("[Mappings] Im Hintergrund aktualisiert ({} Einträge)", flatMappings.size());
            } catch (Exception e) {
                Main.LOGGER.debug("[Mappings] Hintergrund-Refresh fehlgeschlagen: {}", e.getMessage());
            }
        });
    }

    // =================================================================
    // Lookup
    // =================================================================

    /**
     * Sucht ein Mapping für einen Hostnamen.
     * Reihenfolge: exakter Match -> Wildcard-Match (*.domain.tld) -> leer.
     */
    public static Optional<ServerMapping> lookup(String hostname) {
        if (hostname == null || hostname.isBlank()) return Optional.empty();
        String host = hostname.toLowerCase().trim();

        Optional<ServerMapping> cached = lookupCache.get(host);
        if (cached != null) return cached;

        Optional<ServerMapping> result = Optional.empty();

        // 1) Exakter Match
        ServerMapping exact = flatMappings.get(host);
        if (exact != null) {
            result = Optional.of(exact);
        } else {
            // 2) Wildcard-Match
            for (Map.Entry<String, ServerMapping> e : flatMappings.entrySet()) {
                String pattern = e.getKey();
                if (pattern.startsWith("*.")) {
                    String suffix = pattern.substring(1); // ".hypixel.net"
                    if (host.endsWith(suffix) || host.equals(pattern.substring(2))) {
                        result = Optional.of(e.getValue());
                        break;
                    }
                }
            }
        }

        lookupCache.put(host, result);
        return result;
    }

    public static boolean isInitialized() {
        return initialized;
    }

    public static int size() {
        return flatMappings.size();
    }

    public static String baseAssetUrl() {
        return BASE_ASSET_URL;
    }

    // =================================================================
    // Intern
    // =================================================================

    private static void loadFromJson(String json) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        JsonObject servers = root.has("servers")
                ? root.getAsJsonObject("servers") : new JsonObject();

        Map<String, ServerMapping> map = new HashMap<>();
        for (String pattern : servers.keySet()) {
            ServerMapping m = GSON.fromJson(servers.get(pattern), ServerMapping.class);
            if (m == null) continue;
            map.put(pattern.toLowerCase(), m);
        }
        flatMappings = map;
        lookupCache.clear();
    }

    private static String fetchUrl(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(HTTP_TIMEOUT)
                .header("User-Agent", "CloudClientMod/1.0")
                .GET()
                .build();

        HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("HTTP " + response.statusCode());
        }
        return response.body();
    }

    private static Path cacheFile() {
        return FabricLoader.getInstance().getConfigDir()
                .resolve("cloudclientmod")
                .resolve("mappings-cache.json");
    }

    private static boolean isCacheStale(Path file) {
        try {
            Instant modified = Files.getLastModifiedTime(file).toInstant();
            return Duration.between(modified, Instant.now()).compareTo(CACHE_TTL) > 0;
        } catch (IOException e) {
            return true;
        }
    }
}