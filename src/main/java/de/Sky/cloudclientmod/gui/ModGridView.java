package de.Sky.cloudclientmod.gui;

import de.Sky.cloudclientmod.config.ElementConfig;
import de.Sky.cloudclientmod.config.HudConfig;
import de.Sky.cloudclientmod.features.FeatureRegistry;
import de.Sky.cloudclientmod.features.HudFeature;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import java.util.function.Consumer;

public class ModGridView {

    private static final int CARD_W = 120;
    private static final int CARD_H = 60;
    private static final int GAP = 12;

    private final int x, y, w, h;
    private final Consumer<String> onSelect;

    public ModGridView(int x, int y, int w, int h, Consumer<String> onSelect) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        this.onSelect = onSelect;
    }

    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        var font = Minecraft.getInstance().font;

        g.drawString(font, "MODS", x + 20, y + 14, 0xFF00B4D8, false);

        int startX = x + 20;
        int startY = y + 40;
        int cols = Math.max(1, (w - 40) / (CARD_W + GAP));

        int i = 0;
        for (HudFeature f : FeatureRegistry.getFeatures()) {
            int col = i % cols;
            int row = i / cols;
            int cx = startX + col * (CARD_W + GAP);
            int cy = startY + row * (CARD_H + GAP);

            boolean hover = mouseX >= cx && mouseX <= cx + CARD_W
                    && mouseY >= cy && mouseY <= cy + CARD_H;

            ElementConfig cfg = HudConfig.get(f.getId());

            g.fill(cx, cy, cx + CARD_W, cy + CARD_H, hover ? 0xFF1A1A2E : 0xFF111118);
            int border = hover ? 0xFF00B4D8 : 0xFF333355;
            g.fill(cx, cy, cx + CARD_W, cy + 1, border);
            g.fill(cx, cy + CARD_H - 1, cx + CARD_W, cy + CARD_H, border);
            g.fill(cx, cy, cx + 1, cy + CARD_H, border);
            g.fill(cx + CARD_W - 1, cy, cx + CARD_W, cy + CARD_H, border);

            String name = f.getDisplayName();
            int tw = font.width(name);
            g.drawString(font, name, cx + (CARD_W - tw) / 2, cy + CARD_H / 2 - 4,
                    cfg.enabled ? 0xFFFFFFFF : 0xFF666680, false);

            i++;
        }
    }

    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0) return false;

        int startX = x + 20;
        int startY = y + 40;
        int cols = Math.max(1, (w - 40) / (CARD_W + GAP));

        int i = 0;
        for (HudFeature f : FeatureRegistry.getFeatures()) {
            int col = i % cols;
            int row = i / cols;
            int cx = startX + col * (CARD_W + GAP);
            int cy = startY + row * (CARD_H + GAP);

            if (mx >= cx && mx <= cx + CARD_W && my >= cy && my <= cy + CARD_H) {
                onSelect.accept(f.getId());
                return true;
            }
            i++;
        }
        return false;
    }
}