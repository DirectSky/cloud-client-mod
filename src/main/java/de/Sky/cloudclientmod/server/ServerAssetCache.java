package de.Sky.cloudclientmod.server;

import de.Sky.cloudclientmod.Main;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lädt und cached die Bilder (Icons, Banner) aus dem Mappings-Repo.
 * Speichert einmal heruntergeladene Assets dauerhaft, um wiederholte
 * Downloads zu vermeiden.
 *
 * Der In-Memory-Cache hält nur Bytes, keine GPU-Texturen — die werden
 * im Renderer (Schritt 3) lazy aus den Bytes gebaut.
 */
public final class ServerAssetCache {

    private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(15);

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(HTTP_TIMEOUT)
            .build();

    /** Relativer Pfad (z.B. "assets/hypixel-icon.png") -> Bytes. */
    private static final Map<String, byte[]> memoryCache = new ConcurrentHashMap<>();

    private ServerAssetCache() {}

    /**
     * Liefert die Bytes eines Assets. Erst synchron prüfen ob im RAM,
     * sonst aus dem lokalen Disk-Cache, sonst herunterladen (async).
     *
     * @param relativePath z.B. "assets/hypixel-icon.png"
     * @return Optional mit Bytes falls sofort verfügbar
     */
    public static Optional<byte[]> get(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) return Optional.empty();

        // 1) RAM
        byte[] cached = memoryCache.get(relativePath);
        if (cached != null) return Optional.of(cached);

        // 2) Disk
        Path diskFile = diskPathFor(relativePath);
        if (Files.exists(diskFile)) {
            try {
                byte[] bytes = Files.readAllBytes(diskFile);
                memoryCache.put(relativePath, bytes);
                return Optional.of(bytes);
            } catch (IOException e) {
                Main.LOGGER.warn("[Assets] Cache-Datei kaputt: {}", diskFile);
            }
        }

        // 3) Async download
        CompletableFuture.runAsync(() -> downloadAndCache(relativePath));

        return Optional.empty();
    }

    /** Wird im Renderer aufgerufen um zu prüfen ob wir schon ein Asset haben. */
    public static boolean isReady(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) return false;
        if (memoryCache.containsKey(relativePath)) return true;
        return Files.exists(diskPathFor(relativePath));
    }

    /** Löscht den gesamten Cache (RAM + Disk). Für /cloudclient clearcache. */
    public static void clearAll() {
        memoryCache.clear();
        Path dir = cacheRoot();
        if (!Files.exists(dir)) return;
        try (var stream = Files.walk(dir)) {
            stream.filter(Files::isRegularFile).forEach(p -> {
                try { Files.delete(p); } catch (IOException ignored) {}
            });
        } catch (IOException e) {
            Main.LOGGER.warn("[Assets] Cache konnte nicht geleert werden: {}", e.getMessage());
        }
    }

    // =================================================================
    // Intern
    // =================================================================

    private static void downloadAndCache(String relativePath) {
        try {
            String url = ServerMappings.baseAssetUrl() + relativePath;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(HTTP_TIMEOUT)
                    .header("User-Agent", "CloudClientMod/1.0")
                    .GET()
                    .build();

            HttpResponse<byte[]> response = HTTP.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() != 200) {
                Main.LOGGER.warn("[Assets] Download {} -> HTTP {}", relativePath, response.statusCode());
                return;
            }

            byte[] bytes = response.body();
            memoryCache.put(relativePath, bytes);

            Path diskFile = diskPathFor(relativePath);
            Files.createDirectories(diskFile.getParent());
            Files.write(diskFile, bytes);

            Main.LOGGER.debug("[Assets] {} heruntergeladen ({} bytes)", relativePath, bytes.length);
        } catch (Exception e) {
            Main.LOGGER.warn("[Assets] Download von {} fehlgeschlagen: {}", relativePath, e.getMessage());
        }
    }

    private static Path cacheRoot() {
        return FabricLoader.getInstance().getConfigDir()
                .resolve("cloudclientmod")
                .resolve("asset-cache");
    }

    private static Path diskPathFor(String relativePath) {
        // Schrägstriche in Ordner-Struktur umwandeln
        String clean = relativePath.replace("\\", "/");
        if (clean.startsWith("/")) clean = clean.substring(1);
        return cacheRoot().resolve(clean);
    }
}