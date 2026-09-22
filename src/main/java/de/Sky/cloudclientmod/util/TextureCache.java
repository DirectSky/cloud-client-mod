package de.Sky.cloudclientmod.util;

import com.mojang.blaze3d.platform.NativeImage;
import de.Sky.cloudclientmod.Main;
import de.Sky.cloudclientmod.server.ServerAssetCache;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import java.io.ByteArrayInputStream;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class TextureCache {

    private static final Map<String, Identifier> locationByPath = new ConcurrentHashMap<>();
    private static final Map<Identifier, DynamicTexture> textures = new ConcurrentHashMap<>();

    private TextureCache() {}

    public static Optional<Identifier> getLocation(String assetPath) {
        if (assetPath == null || assetPath.isBlank()) return Optional.empty();

        Identifier existing = locationByPath.get(assetPath);
        if (existing != null) return Optional.of(existing);

        Optional<byte[]> bytesOpt = ServerAssetCache.get(assetPath);
        if (bytesOpt.isEmpty()) return Optional.empty();

        try {
            byte[] bytes = bytesOpt.get();
            NativeImage image = NativeImage.read(new ByteArrayInputStream(bytes));

            String slug = assetPath.replace("assets/", "")
                    .replace(".png", "")
                    .replace(".jpg", "")
                    .replaceAll("[^a-zA-Z0-9_./-]", "_")
                    .toLowerCase();
            Identifier id = Identifier.fromNamespaceAndPath("cloudclientmod", "dynamic/" + slug);

            // NEU: Label-Supplier als erster Parameter
            DynamicTexture texture = new DynamicTexture(() -> id.toString(), image);
            Minecraft.getInstance().getTextureManager().register(id, texture);

            locationByPath.put(assetPath, id);
            textures.put(id, texture);

            Main.LOGGER.info("[TextureCache] {} -> {}", assetPath, id);
            return Optional.of(id);
        } catch (Exception e) {
            Main.LOGGER.warn("[TextureCache] Konnte {} nicht laden: {}", assetPath, e.getMessage());
            return Optional.empty();
        }
    }

    public static void clearAll() {
        for (Map.Entry<Identifier, DynamicTexture> e : textures.entrySet()) {
            try {
                e.getValue().close();
            } catch (Exception ignored) {}
        }
        textures.clear();
        locationByPath.clear();
    }
}