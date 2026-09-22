package de.Sky.cloudclientmod.features;

import de.Sky.cloudclientmod.config.ElementConfig;
import de.Sky.cloudclientmod.config.HudConfig;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;

public class HudRenderer {

    public static final Identifier LAYER_ID =
            Identifier.fromNamespaceAndPath("cloudclientmod", "hud_main");

    public static void init() {
        HudElementRegistry.attachElementBefore(
                VanillaHudElements.CHAT,
                LAYER_ID,
                (graphics, deltaTracker) -> {
                    Minecraft mc = Minecraft.getInstance();
                    if (mc.player == null || mc.options.hideGui) return;

                    float delta = deltaTracker.getGameTimeDeltaPartialTick(false);

                    for (HudFeature feature : FeatureRegistry.getFeatures()) {
                        ElementConfig cfg = HudConfig.get(feature.getId());
                        if (!cfg.enabled) continue;

                        graphics.pose().pushMatrix();
                        graphics.pose().translate(cfg.x, cfg.y);
                        graphics.pose().scale(cfg.scale, cfg.scale);
                        try {
                            feature.render(graphics, cfg, delta);
                        } catch (Exception e) {
                            // Feature darf das HUD nicht crashen
                        }
                        graphics.pose().popMatrix();
                    }
                }
        );
    }

    public static void drawBox(GuiGraphics g, ElementConfig cfg, int w, int h) {
        switch (cfg.style) {
            case MODERN -> drawModern(g, cfg, w, h);
            case RETRO  -> drawRetro(g, cfg, w, h);
            case GLASS  -> drawGlass(g, cfg, w, h);
        }
    }

    private static void drawModern(GuiGraphics g, ElementConfig cfg, int w, int h) {
        int radius = cfg.cornerRadius;
        if (radius <= 0) {
            g.fill(0, 0, w, h, cfg.backgroundColor);
        } else {
            fillRounded(g, 0, 0, w, h, radius, cfg.backgroundColor);
        }
        if ((cfg.borderColor >>> 24) > 0) {
            drawRoundedBorder(g, 0, 0, w, h, radius, cfg.borderColor);
        }
    }

    private static void drawRetro(GuiGraphics g, ElementConfig cfg, int w, int h) {
        g.fill(0, 0, w, h, 0xFF000000);
        drawBorder1px(g, 0, 0, w, h, 0xFFFFFFFF);
    }

    private static void drawGlass(GuiGraphics g, ElementConfig cfg, int w, int h) {
        int bg = (cfg.backgroundColor & 0x00FFFFFF) | 0x50000000;
        fillRounded(g, 0, 0, w, h, Math.max(cfg.cornerRadius, 6), bg);
        if ((cfg.borderColor >>> 24) > 0) {
            drawRoundedBorder(g, 0, 0, w, h, Math.max(cfg.cornerRadius, 6), cfg.borderColor);
        }
    }

    private static void fillRounded(GuiGraphics g, int x, int y, int w, int h, int r, int color) {
        if (r > h / 2) r = h / 2;
        if (r > w / 2) r = w / 2;

        g.fill(x + r, y, x + w - r, y + h, color);
        g.fill(x, y + r, x + r, y + h - r, color);
        g.fill(x + w - r, y + r, x + w, y + h - r, color);

        for (int cy = 0; cy < r; cy++) {
            for (int cx = 0; cx < r; cx++) {
                int dx = r - cx;
                int dy = r - cy;
                if (dx * dx + dy * dy <= r * r) {
                    g.fill(x + cx, y + cy, x + cx + 1, y + cy + 1, color);
                    g.fill(x + w - cx - 1, y + cy, x + w - cx, y + cy + 1, color);
                    g.fill(x + cx, y + h - cy - 1, x + cx + 1, y + h - cy, color);
                    g.fill(x + w - cx - 1, y + h - cy - 1, x + w - cx, y + h - cy, color);
                }
            }
        }
    }

    private static void drawRoundedBorder(GuiGraphics g, int x, int y, int w, int h, int r, int color) {
        if (r > h / 2) r = h / 2;
        if (r > w / 2) r = w / 2;

        g.fill(x + r, y, x + w - r, y + 1, color);
        g.fill(x + r, y + h - 1, x + w - r, y + h, color);
        g.fill(x, y + r, x + 1, y + h - r, color);
        g.fill(x + w - 1, y + r, x + w, y + h - r, color);

        for (int cy = 0; cy < r; cy++) {
            for (int cx = 0; cx < r; cx++) {
                int dx = r - cx;
                int dy = r - cy;
                int d2 = dx * dx + dy * dy;
                int r2 = r * r;
                int rInner = (r - 1) * (r - 1);
                if (d2 <= r2 && d2 > rInner) {
                    g.fill(x + cx, y + cy, x + cx + 1, y + cy + 1, color);
                    g.fill(x + w - cx - 1, y + cy, x + w - cx, y + cy + 1, color);
                    g.fill(x + cx, y + h - cy - 1, x + cx + 1, y + h - cy, color);
                    g.fill(x + w - cx - 1, y + h - cy - 1, x + w - cx, y + h - cy, color);
                }
            }
        }
    }

    private static void drawBorder1px(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + 1, color);
        g.fill(x, y + h - 1, x + w, y + h, color);
        g.fill(x, y, x + 1, y + h, color);
        g.fill(x + w - 1, y, x + w, y + h, color);
    }

    public static void drawText(GuiGraphics g, String text, int x, int y, int color) {
        g.drawString(Minecraft.getInstance().font, text, x, y, color, true);
    }

    public static int textWidth(String text) {
        return Minecraft.getInstance().font.width(text);
    }

    public static int textHeight() {
        return Minecraft.getInstance().font.lineHeight;
    }
}