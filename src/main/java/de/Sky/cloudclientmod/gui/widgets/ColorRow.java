package de.Sky.cloudclientmod.gui.widgets;

import de.Sky.cloudclientmod.features.HudRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;
import java.util.function.IntSupplier;

public class ColorRow extends AbstractWidget {

    private final IntSupplier getter;
    private final Consumer<Integer> setter;

    private static final int FULLY_TRANSPARENT = 0x00000000;
    private static final int DEFAULT_BG        = 0xB4141420;  // leicht transparentes Schwarz

    private final int[] presets = {
            DEFAULT_BG,              // Standard (halbtransparentes Schwarz)
            FULLY_TRANSPARENT,       // komplett transparent
            0xFFFFFFFF, 0xFF00B4D8, 0xFF4A9A4A, 0xFFFFB703,
            0xFFCC4444, 0xFF9D4EDD, 0xFF000000, 0xFF888888
    };
    private int presetIndex = 0;

    public ColorRow(int x, int y, int w, int h, IntSupplier getter, Consumer<Integer> setter) {
        super(x, y, w, h, Component.empty());
        this.getter = getter;
        this.setter = setter;
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float delta) {
        int color = getter.getAsInt();

        int sw = height;
        drawSwatch(g, getX(), getY(), sw, height, color);

        String label;
        if (color == FULLY_TRANSPARENT) label = "TRANSPARENT";
        else if (color == DEFAULT_BG)  label = "DEFAULT";
        else                           label = String.format("#%08X", color);
        HudRenderer.drawText(g, label, getX() + sw + 6, getY() + (height - 8) / 2, 0xFFFFFFFF);

        int px = getX() + width - presets.length * (height + 2);
        for (int i = 0; i < presets.length; i++) {
            int cx = px + i * (height + 2);
            drawSwatch(g, cx, getY(), height, height, presets[i]);
        }
    }

    /** Zeichnet einen Farb-Swatch. Bei voller Transparenz → Schachbrett, sonst Farbe. */
    private void drawSwatch(GuiGraphics g, int x, int y, int w, int h, int color) {
        if ((color >>> 24) == 0) {
            // Schachbrett (nur bei voller Transparenz)
            for (int yy = 0; yy < h; yy += 4) {
                for (int xx = 0; xx < w; xx += 4) {
                    boolean light = ((xx / 4) + (yy / 4)) % 2 == 0;
                    int fill = light ? 0xFF666666 : 0xFF333333;
                    g.fill(x + xx, y + yy,
                            Math.min(x + xx + 4, x + w),
                            Math.min(y + yy + 4, y + h), fill);
                }
            }
        } else {
            g.fill(x, y, x + w, y + h, color);
        }

        // Weißer Rahmen
        g.fill(x, y, x + w, y + 1, 0xFFFFFFFF);
        g.fill(x, y + h - 1, x + w, y + h, 0xFFFFFFFF);
        g.fill(x, y, x + 1, y + h, 0xFFFFFFFF);
        g.fill(x + w - 1, y, x + w, y + h, 0xFFFFFFFF);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x();
        double my = event.y();
        if (!isMouseOver(mx, my) || event.button() != 0) return false;

        int px = getX() + width - presets.length * (height + 2);
        for (int i = 0; i < presets.length; i++) {
            int cx = px + i * (height + 2);
            if (mx >= cx && mx <= cx + height && my >= getY() && my <= getY() + height) {
                setter.accept(presets[i]);
                presetIndex = i;
                return true;
            }
        }

        presetIndex = (presetIndex + 1) % presets.length;
        setter.accept(presets[presetIndex]);
        return true;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narration) {}
}