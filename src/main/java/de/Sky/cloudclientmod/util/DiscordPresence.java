package de.Sky.cloudclientmod.util;

import dev.firstdark.rpc.DiscordRpc;
import dev.firstdark.rpc.handlers.RPCEventHandler;
import dev.firstdark.rpc.models.DiscordRichPresence;
import dev.firstdark.rpc.models.User;
import dev.firstdark.rpc.enums.ErrorCode;
import de.Sky.cloudclientmod.Main;

public final class DiscordPresence {

    // Gleiche ID wie im Launcher
    private static final String APP_ID = "1549077325050675271";

    private static final long MIN_UPDATE_INTERVAL_MS = 5_000;

    private static DiscordRpc rpc;
    private static boolean connected = false;
    private static boolean initialized = false;
    private static long startTimestamp = 0;
    private static long lastSentTime = 0;

    private static String lastState = "";
    private static String lastDetails = "";

    private DiscordPresence() {}

    public static void init() {
        if (initialized) return;
        initialized = true;
        startTimestamp = System.currentTimeMillis() / 1000;

        Thread t = new Thread(() -> {
            try {
                rpc = new DiscordRpc();
                rpc.setDebugMode(false);

                RPCEventHandler handler = new RPCEventHandler() {
                    @Override
                    public void ready(User user) {
                        connected = true;
                        Main.LOGGER.info("[Discord] Verbunden als {}", user.getUsername());
                    }

                    @Override
                    public void disconnected(ErrorCode errorCode, String message) {
                        connected = false;
                        Main.LOGGER.warn("[Discord] Getrennt: {}", message);
                    }

                    @Override
                    public void errored(ErrorCode errorCode, String message) {
                        Main.LOGGER.warn("[Discord] Fehler: {}", message);
                    }
                };

                rpc.init(APP_ID, handler, false);
            } catch (Throwable e) {
                Main.LOGGER.warn("[Discord] Konnte nicht verbinden: {}", e.getMessage());
            }
        }, "cloudclientmod-discord-init");
        t.setDaemon(true);
        t.start();

        Runtime.getRuntime().addShutdownHook(new Thread(DiscordPresence::shutdown));
    }

    public static void update(String state, String details) {
        if (!connected || rpc == null) return;
        if (state == null) state = "";
        if (details == null) details = "";

        if (state.equals(lastState) && details.equals(lastDetails)) return;

        long now = System.currentTimeMillis();
        if (now - lastSentTime < MIN_UPDATE_INTERVAL_MS) return;

        try {
            DiscordRichPresence presence = DiscordRichPresence.builder()
                    .state(state)
                    .details(details)
                    .startTimestamp(startTimestamp)
                    .largeImageKey("main_icon")
                    .largeImageText("Cloud Client")
                    .build();

            rpc.updatePresence(presence);

            lastState = state;
            lastDetails = details;
            lastSentTime = now;

            Main.LOGGER.debug("[Discord] Update: {} / {}", state, details);
        } catch (Throwable e) {
            Main.LOGGER.warn("[Discord] Update fehlgeschlagen: {}", e.getMessage());
        }
    }

    public static void shutdown() {
        if (rpc == null) return;
        try {
            rpc.shutdown();
        } catch (Throwable ignored) {}
        rpc = null;
        connected = false;
    }
}