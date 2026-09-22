package de.Sky.cloudclientmod.util;

import de.Sky.cloudclientmod.Main;
import de.Sky.cloudclientmod.server.InstanceState;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Heartbeat-Lock: signalisiert dem Launcher, dass eine Mod-Instanz läuft.
 * Der Launcher überspringt seine Discord-Presence-Updates, solange ein
 * frisches Lock-File existiert.
 */
public final class GameLock {

    private static final long HEARTBEAT_INTERVAL_SEC = 10;

    private static Path lockFile = null;
    private static ScheduledExecutorService scheduler = null;

    private GameLock() {}

    public static void start() {
        Path launcherHome = findLauncherHome();
        if (launcherHome == null) {
            Main.LOGGER.info("[GameLock] Kein Launcher-Home gefunden — Lock wird übersprungen.");
            return;
        }

        long pid = ProcessHandle.current().pid();
        Path locksDir = launcherHome.resolve("locks");
        lockFile = locksDir.resolve(pid + ".json");

        try {
            Files.createDirectories(locksDir);
            writeLock();
            Main.LOGGER.info("[GameLock] Lock angelegt: {}", lockFile);
        } catch (IOException e) {
            Main.LOGGER.warn("[GameLock] Konnte Lock nicht schreiben: {}", e.getMessage());
            lockFile = null;
            return;
        }

        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "cloudclientmod-heartbeat");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(() -> {
            try {
                writeLock();
            } catch (Exception e) {
                Main.LOGGER.warn("[GameLock] Heartbeat fehlgeschlagen: {}", e.getMessage());
            }
        }, HEARTBEAT_INTERVAL_SEC, HEARTBEAT_INTERVAL_SEC, TimeUnit.SECONDS);

        Runtime.getRuntime().addShutdownHook(new Thread(GameLock::stop));
    }

    public static void stop() {
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
        if (lockFile != null) {
            try {
                Files.deleteIfExists(lockFile);
                Main.LOGGER.info("[GameLock] Lock entfernt.");
            } catch (IOException ignored) {}
            lockFile = null;
        }
    }

    // =================================================================
    // Intern
    // =================================================================

    private static void writeLock() throws IOException {
        long pid = ProcessHandle.current().pid();
        String instance = InstanceState.getInstanceName();
        long now = System.currentTimeMillis();

        String json = "{\n"
                + "  \"instance\": \"" + escape(instance) + "\",\n"
                + "  \"pid\": " + pid + ",\n"
                + "  \"started\": " + now + ",\n"
                + "  \"lastBeat\": " + now + "\n"
                + "}";

        Files.writeString(lockFile, json);
    }

    private static Path findLauncherHome() {
        // 1) Bevorzugt: Env-Var vom Launcher
        String envHome = System.getenv("CLOUDCLIENT_HOME");
        if (envHome != null && !envHome.isBlank()) {
            try {
                Path p = Path.of(envHome);
                if (Files.exists(p)) return p;
            } catch (Exception ignored) {}
        }

        // 2) Aus --gameDir ableiten
        String gameDir = InstanceState.getGameDir();
        if (gameDir != null) {
            try {
                Path gd = Path.of(gameDir);
                Path instancesDir = gd.getParent();
                if (instancesDir != null && "instances".equals(instancesDir.getFileName().toString())) {
                    return instancesDir.getParent();
                }
            } catch (Exception ignored) {}
        }

        // 3) Fallback auf Standard-Home
        Path fallback = Path.of(System.getProperty("user.home"), ".skylauncher");
        if (Files.exists(fallback)) return fallback;
        return null;
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}