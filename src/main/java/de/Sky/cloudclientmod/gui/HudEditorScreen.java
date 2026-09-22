package de.Sky.cloudclientmod.gui;

import de.Sky.cloudclientmod.config.ElementConfig;
import de.Sky.cloudclientmod.config.HudConfig;
import de.Sky.cloudclientmod.features.FeatureRegistry;
import de.Sky.cloudclientmod.features.HudFeature;
import de.Sky.cloudclientmod.gui.widgets.ThemedButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class HudEditorScreen extends Screen {

    private HudFeature dragging = null;
    private int dragOffsetX, dragOffsetY;

    public HudEditorScreen() {
        super(Component.literal("HUD Editor"));
    }

    @Override
    protected void init() {
        int btnW = 180;
        int btnH = 26;
        int btnX = (this.width - btnW) / 2;
        int btnY = (this.height - btnH) / 2;

        ThemedButton openMenuBtn = new ThemedButton(btnX, btnY, btnW, btnH,
                Component.literal("⚙  MOD MENÜ ÖFFNEN"),
                () -> Minecraft.getInstance().setScreen(new ModMenuScreen()));
        addRenderableWidget(openMenuBtn);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        // HUD-Elemente rendern
        for (HudFeature f : FeatureRegistry.getFeatures()) {
            ElementConfig cfg = HudConfig.get(f.getId());
            if (!cfg.enabled) continue;

            g.pose().pushMatrix();
            g.pose().translate(cfg.x, cfg.y);
            g.pose().scale(cfg.scale, cfg.scale);
            try {
                f.render(g, cfg, delta);
            } catch (Exception ignored) {}
            g.pose().popMatrix();

            // Hover-Highlight
            if (isMouseOverElement(f, cfg, mouseX, mouseY)) {
                int w = (int) (f.getWidth(cfg) * cfg.scale);
                int h = (int) (f.getHeight(cfg) * cfg.scale);
                drawOutline(g, cfg.x, cfg.y, cfg.x + w, cfg.y + h, 0xFF00B4D8);
            }
        }

        // Hinweis unten
        var font = Minecraft.getInstance().font;
        String hint = "Ziehe HUD-Elemente mit der Maus · ESC zum Schließen";
        g.drawString(font, hint,
                (this.width - font.width(hint)) / 2,
                this.height - 20, 0xFFAAAAAA, true);

        super.render(g, mouseX, mouseY, delta);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float delta) {
        // Kein Blur, kein Darkening — Welt bleibt sichtbar
    }

    private boolean isMouseOverElement(HudFeature f, ElementConfig cfg, int mx, int my) {
        int w = (int) (f.getWidth(cfg) * cfg.scale);
        int h = (int) (f.getHeight(cfg) * cfg.scale);
        return mx >= cfg.x && mx <= cfg.x + w && my >= cfg.y && my <= cfg.y + h;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            for (HudFeature f : FeatureRegistry.getFeatures()) {
                ElementConfig cfg = HudConfig.get(f.getId());
                if (!cfg.enabled) continue;
                if (isMouseOverElement(f, cfg, (int) event.x(), (int) event.y())) {
                    dragging = f;
                    dragOffsetX = (int) event.x() - cfg.x;
                    dragOffsetY = (int) event.y() - cfg.y;
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (dragging != null && event.button() == 0) {
            ElementConfig cfg = HudConfig.get(dragging.getId());
            cfg.x = Math.max(0, Math.min(this.width - 4, (int) event.x() - dragOffsetX));
            cfg.y = Math.max(0, Math.min(this.height - 4, (int) event.y() - dragOffsetY));
            HudConfig.markDirty();
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (dragging != null) {
            dragging = null;
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static void drawOutline(GuiGraphics g, int x1, int y1, int x2, int y2, int color) {
        g.fill(x1, y1, x2, y1 + 1, color);
        g.fill(x1, y2 - 1, x2, y2, color);
        g.fill(x1, y1, x1 + 1, y2, color);
        g.fill(x2 - 1, y1, x2, y2, color);
    }
}