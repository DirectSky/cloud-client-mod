package de.Sky.cloudclientmod.server;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

public final class InstanceState {

    private static String cachedName = null;
    private static String cachedGameDir = null;

    private InstanceState() {}

    public static String getInstanceName() {
        if (cachedName != null) return cachedName;

        // 1) Bevorzugt: Env-Var vom Launcher
        String env = System.getenv("CLOUDCLIENT_INSTANCE");
        if (env != null && !env.isBlank()) {
            cachedName = env;
            return cachedName;
        }

        // 2) Fallback: --gameDir parsen
        cachedName = extractName(getGameDir());
        return cachedName;
    }

    public static String getGameDir() {
        if (cachedGameDir != null) return cachedGameDir.isEmpty() ? null : cachedGameDir;

        try {
            List<String> args = ProcessHandle.current().info().arguments()
                    .map(List::of)
                    .orElse(Collections.emptyList());

            for (int i = 0; i < args.size() - 1; i++) {
                if ("--gameDir".equals(args.get(i))) {
                    cachedGameDir = args.get(i + 1);
                    return cachedGameDir;
                }
            }
        } catch (Exception ignored) {}

        cachedGameDir = "";
        return null;
    }

    private static String extractName(String gameDir) {
        if (gameDir == null || gameDir.isBlank()) return "Development";

        String norm = gameDir.replace("\\", "/").replace("\"", "");
        int idx = norm.lastIndexOf("/instances/");
        if (idx < 0) return "Development";

        String after = norm.substring(idx + "/instances/".length());
        int slash = after.indexOf('/');
        if (slash > 0) after = after.substring(0, slash);
        return after.isEmpty() ? "Minecraft" : after;
    }
}