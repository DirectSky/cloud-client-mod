package de.Sky.cloudclientmod.server;

import de.Sky.cloudclientmod.Main;
import de.Sky.cloudclientmod.util.TextureCache;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

import java.util.Optional;

public final class ServerListRenderer {

    private ServerListRenderer() {}

    public static void renderBanner(GuiGraphics g, int x, int y, int width, int height,
                                    ServerMapping mapping) {
        if (mapping == null || !mapping.hasBanner()) return;

        Optional<Identifier> textureOpt = TextureCache.getLocation(mapping.banner);
        if (textureOpt.isEmpty()) return;

        try {
            g.blit(RenderPipelines.GUI_TEXTURED, textureOpt.get(),
                    x, y,
                    0.0F, 0.0F,
                    width, height,
                    width, height);
        } catch (Exception e) {
            Main.LOGGER.debug("[ServerList] Banner-Render fehlgeschlagen: {}", e.getMessage());
        }
    }

    public static void renderIcon(GuiGraphics g, int x, int y, int size, ServerMapping mapping) {
        if (mapping == null || !mapping.hasIcon()) return;

        Optional<Identifier> textureOpt = TextureCache.getLocation(mapping.icon);
        if (TextureCache.getLocation(mapping.icon).isEmpty()) return;

        try {
            g.blit(RenderPipelines.GUI_TEXTURED, textureOpt.get(),
                    x, y,
                    0.0F, 0.0F,
                    size, size,
                    size, size);
        } catch (Exception e) {
            Main.LOGGER.debug("[ServerList] Icon-Render fehlgeschlagen: {}", e.getMessage());
        }
    }
}