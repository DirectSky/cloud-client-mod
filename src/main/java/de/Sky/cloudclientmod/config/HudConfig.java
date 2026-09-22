package de.Sky.cloudclientmod.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.Sky.cloudclientmod.Main;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class HudConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final long SAVE_DEBOUNCE_MS = 500;

    private static final Map<String, ProfileData> PROFILES = new LinkedHashMap<>();
    private static String activeProfileId = null;
    private static boolean saveScheduled = false;

    private static Path configPath() {
        return FabricLoader.getInstance().getConfigDir()
                .resolve("cloudclientmod").resolve("hud.json");
    }

    public static String getActiveProfileId() {
        return activeProfileId;
    }

    public static ProfileData getActiveProfile() {
        if (activeProfileId == null || !PROFILES.containsKey(activeProfileId)) {
            if (PROFILES.isEmpty()) {
                createDefaultProfile();
            }
            activeProfileId = PROFILES.keySet().iterator().next();
        }
        return PROFILES.get(activeProfileId);
    }

    public static ProfileData getProfile(String id) {
        return PROFILES.get(id);
    }

    public static Map<String, ProfileData> getAllProfiles() {
        return PROFILES;
    }

    public static ElementConfig get(String featureId) {
        return getActiveProfile().get(featureId);
    }

    public static ElementConfig get(String profileId, String featureId) {
        ProfileData p = PROFILES.get(profileId);
        if (p == null) return new ElementConfig();
        return p.get(featureId);
    }

    public static void setActiveProfile(String id) {
        if (PROFILES.containsKey(id)) {
            activeProfileId = id;
            markDirty();
        }
    }

    public static void markDirty() {
        if (!saveScheduled) {
            saveScheduled = true;
            new Thread(() -> {
                try {
                    Thread.sleep(SAVE_DEBOUNCE_MS);
                } catch (InterruptedException ignored) {}
                save();
                saveScheduled = false;
            }, "cloudclientmod-config-save").start();
        }
    }

    public static String createProfile(String name) {
        String id = UUID.randomUUID().toString().substring(0, 8);
        ProfileData p = new ProfileData(name);
        applyDefaultPositions(p);
        PROFILES.put(id, p);
        markDirty();
        return id;
    }

    public static String createProfileFrom(String name, String sourceId) {
        ProfileData source = PROFILES.get(sourceId);
        if (source == null) return createProfile(name);
        String id = UUID.randomUUID().toString().substring(0, 8);
        PROFILES.put(id, source.copy(name));
        markDirty();
        return id;
    }

    public static void deleteProfile(String id) {
        if (PROFILES.size() <= 1) return;
        PROFILES.remove(id);
        if (activeProfileId == null || !PROFILES.containsKey(activeProfileId)) {
            activeProfileId = PROFILES.keySet().iterator().next();
        }
        markDirty();
    }

    public static void renameProfile(String id, String newName) {
        ProfileData p = PROFILES.get(id);
        if (p == null) return;
        p.name = newName;
        markDirty();
    }

    public static void load() {
        PROFILES.clear();
        activeProfileId = null;

        Path path = configPath();
        if (!Files.exists(path)) {
            createDefaultProfile();
            save();
            return;
        }

        try {
            String json = Files.readString(path);
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();

            if (root.has("activeProfile")) {
                activeProfileId = root.get("activeProfile").getAsString();
            }

            if (root.has("profiles")) {
                JsonObject profilesJson = root.getAsJsonObject("profiles");
                for (String id : profilesJson.keySet()) {
                    ProfileData p = GSON.fromJson(profilesJson.get(id), ProfileData.class);
                    if (p.elements == null) p.elements = new java.util.HashMap<>();
                    PROFILES.put(id, p);
                }
            }

            if (PROFILES.isEmpty()) {
                createDefaultProfile();
            }
            if (activeProfileId == null || !PROFILES.containsKey(activeProfileId)) {
                activeProfileId = PROFILES.keySet().iterator().next();
            }

            Main.LOGGER.info("[Cloud Client] Config geladen: " + PROFILES.size() + " Profil(e).");
        } catch (Exception e) {
            Main.LOGGER.error("[Cloud Client] Config konnte nicht geladen werden", e);
            createDefaultProfile();
        }
    }

    public static void save() {
        try {
            JsonObject root = new JsonObject();
            root.addProperty("activeProfile", activeProfileId);

            JsonObject profilesJson = new JsonObject();
            for (Map.Entry<String, ProfileData> e : PROFILES.entrySet()) {
                profilesJson.add(e.getKey(), GSON.toJsonTree(e.getValue()));
            }
            root.add("profiles", profilesJson);

            Files.createDirectories(configPath().getParent());
            Files.writeString(configPath(), GSON.toJson(root));
        } catch (Exception e) {
            Main.LOGGER.error("[Cloud Client] Config konnte nicht gespeichert werden", e);
        }
    }

    private static void createDefaultProfile() {
        String id = "default";
        ProfileData p = new ProfileData("Standard");
        p.icon = "◆";
        applyDefaultPositions(p);
        PROFILES.put(id, p);
        activeProfileId = id;
    }

    private static void applyDefaultPositions(ProfileData p) {
        p.get("fps").x = 4;           p.get("fps").y = 4;
        p.get("keystrokes").x = 4;    p.get("keystrokes").y = 60;
        p.get("cps").x = 70;          p.get("cps").y = 4;
        p.get("coordinates").x = 4;   p.get("coordinates").y = 20;
        p.get("ping").x = 130;        p.get("ping").y = 4;
        p.get("armor").x = 4;         p.get("armor").y = 110;
    }
}