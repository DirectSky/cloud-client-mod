package de.Sky.cloudclientmod.gui;

import de.Sky.cloudclientmod.config.ElementConfig;
import de.Sky.cloudclientmod.config.HudConfig;
import de.Sky.cloudclientmod.features.HudFeature;
import de.Sky.cloudclientmod.gui.widgets.ColorRow;
import de.Sky.cloudclientmod.gui.widgets.SliderRow;
import de.Sky.cloudclientmod.gui.widgets.ThemedButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FeatureDetailView {

    private final int x, y, w, h;
    private final Runnable onBack;
    private HudFeature feature;

    // Scroll-State
    private double scrollOffset = 0;
    private double maxScroll = 0;
    private final List<AbstractWidget> scrollableWidgets = new ArrayList<>();
    private final Map<AbstractWidget, Integer> baseYs = new HashMap<>();

    // Layout-Konstanten
    private static final int HEADER_Y = 14;
    private static final int HEADER_HEIGHT = 20;
    private static final int PREVIEW_Y = 50;
    private static final int PREVIEW_H = 100;
    private static final int CONTENT_START_Y = 180; // ab hier scrollbar (relativ zu y)

    public FeatureDetailView(int x, int y, int w, int h, Runnable onBack) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        this.onBack = onBack;
    }

    public void setFeature(HudFeature f) {
        this.feature = f;
        this.scrollOffset = 0;
    }

    public double getScrollOffset() { return scrollOffset; }
    public double getMaxScroll() { return maxScroll; }

    public void setScrollOffset(double offset) {
        this.scrollOffset = Math.max(0, Math.min(maxScroll, offset));
        applyScroll();
    }

    private void applyScroll() {
        for (AbstractWidget widget : scrollableWidgets) {
            Integer baseY = baseYs.get(widget);
            if (baseY != null) {
                widget.setY((int) (baseY - scrollOffset));
            }
        }
    }

    public void buildWidgets(Screen screen) {
        if (feature == null || !(screen instanceof ModMenuScreen mms)) return;

        scrollableWidgets.clear();
        baseYs.clear();
        scrollOffset = 0;

        ElementConfig cfg = HudConfig.get(feature.getId());

        // Fester Zurück-Button (nicht scrollbar)
        mms.addWidgetPublic(new ThemedButton(x + 20, y + HEADER_Y, 80, HEADER_HEIGHT,
                Component.literal("← ZURÜCK"), () -> onBack.run()));

        // Scrollbare Widgets
        int sx = x + 40;
        int sy = y + CONTENT_START_Y;
        int rowH = 20;
        int spacing = 8;
        int fullW = Math.min(280, w - 80);

        addScrollable(mms, new ThemedButton(sx, sy, fullW, rowH,
                Component.literal("Aktiviert: " + (cfg.enabled ? "AN" : "AUS")),
                () -> {
                    cfg.enabled = !cfg.enabled;
                    HudConfig.markDirty();
                    mms.rebuildPublic();
                }), sy);
        sy += rowH + spacing;

        addScrollable(mms, new ThemedButton(sx, sy, fullW, rowH,
                Component.literal("Stil: " + cfg.style.displayName()),
                () -> {
                    cfg.style = cfg.style.next();
                    HudConfig.markDirty();
                    mms.rebuildPublic();
                }), sy);
        sy += rowH + spacing;

        addScrollable(mms, new SliderRow(sx, sy, fullW, rowH,
                0, 20, cfg.cornerRadius,
                v -> "Radius: " + v.intValue() + "px",
                v -> { cfg.cornerRadius = (int) Math.round(v); HudConfig.markDirty(); }), sy);
        sy += rowH + spacing;

        addScrollable(mms, new SliderRow(sx, sy, fullW, rowH,
                0, 12, cfg.padding,
                v -> "Padding: " + v.intValue() + "px",
                v -> { cfg.padding = (int) Math.round(v); HudConfig.markDirty(); }), sy);
        sy += rowH + spacing;

        addScrollable(mms, new SliderRow(sx, sy, fullW, rowH,
                0.5, 3.0, cfg.scale,
                v -> String.format("Skalierung: %.2fx", v),
                v -> { cfg.scale = v.floatValue(); HudConfig.markDirty(); }), sy);
        sy += rowH + spacing;

        addScrollable(mms, new SliderRow(sx, sy, fullW, rowH,
                0, 400, cfg.x,
                v -> "X: " + v.intValue(),
                v -> { cfg.x = (int) Math.round(v); HudConfig.markDirty(); }), sy);
        sy += rowH + spacing;

        addScrollable(mms, new SliderRow(sx, sy, fullW, rowH,
                0, 300, cfg.y,
                v -> "Y: " + v.intValue(),
                v -> { cfg.y = (int) Math.round(v); HudConfig.markDirty(); }), sy);
        sy += rowH + spacing;

        addScrollable(mms, new ColorRow(sx, sy, fullW, 16,
                () -> cfg.backgroundColor,
                c -> { cfg.backgroundColor = c; HudConfig.markDirty(); }), sy);
        sy += rowH + spacing;

        addScrollable(mms, new ColorRow(sx, sy, fullW, 16,
                () -> cfg.textColor,
                c -> { cfg.textColor = c; HudConfig.markDirty(); }), sy);
        sy += rowH + spacing + 10;

        // Reset-Button ganz unten (scrollbar)
        addScrollable(mms, new ThemedButton(sx, sy, 140, rowH,
                Component.literal("ZURÜCKSETZEN"),
                () -> {
                    cfg.resetToDefault();
                    HudConfig.markDirty();
                    mms.rebuildPublic();
                }), sy);
        sy += rowH;

        // Max-Scroll berechnen
        int viewportTop = y + CONTENT_START_Y;
        int viewportBottom = y + h - 10;
        int viewportHeight = Math.max(1, viewportBottom - viewportTop);
        int contentHeight = sy - (y + CONTENT_START_Y);
        maxScroll = Math.max(0, contentHeight - viewportHeight);
    }

    private void addScrollable(ModMenuScreen mms, AbstractWidget widget, int baseY) {
        mms.addWidgetPublic(widget);
        scrollableWidgets.add(widget);
        baseYs.put(widget, baseY);
    }

    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        if (feature == null) return;

        var font = Minecraft.getInstance().font;
        ElementConfig cfg = HudConfig.get(feature.getId());

        // Titel
        g.drawString(font, feature.getDisplayName(),
                x + 110, y + HEADER_Y + 6, 0xFFFFFFFF, false);

        // Preview-Box (fest, scrollt nicht)
        int pvX = x + 40;
        int pvY = y + PREVIEW_Y;
        int pvW = Math.min(280, w - 80);
        int pvH = PREVIEW_H;

        g.fill(pvX, pvY, pvX + pvW, pvY + pvH, 0xFF050510);
        g.fill(pvX, pvY, pvX + pvW, pvY + 1, 0xFF333355);
        g.fill(pvX, pvY + pvH - 1, pvX + pvW, pvY + pvH, 0xFF333355);
        g.fill(pvX, pvY, pvX + 1, pvY + pvH, 0xFF333355);
        g.fill(pvX + pvW - 1, pvY, pvX + pvW, pvY + pvH, 0xFF333355);

        g.pose().pushMatrix();
        int featureW = feature.getWidth(cfg);
        int featureH = feature.getHeight(cfg);
        int fpx = pvX + (pvW - (int) (featureW * cfg.scale)) / 2;
        int fpy = pvY + (pvH - (int) (featureH * cfg.scale)) / 2;
        g.pose().translate(fpx, fpy);
        g.pose().scale(cfg.scale, cfg.scale);
        try {
            feature.render(g, cfg, delta);
        } catch (Exception ignored) {}
        g.pose().popMatrix();

        // Beschreibung
        if (!feature.getDescription().isEmpty()) {
            g.drawString(font, feature.getDescription(),
                    pvX, pvY + pvH + 6, 0xFF8888AA, false);
        }

        // Scroll-Indikator rechts
        if (maxScroll > 0) {
            int barX = x + w - 12;
            int barY = y + CONTENT_START_Y;
            int barH = h - CONTENT_START_Y - 10;
            g.fill(barX, barY, barX + 4, barY + barH, 0x40FFFFFF);

            double ratio = (double) barH / (barH + maxScroll);
            int thumbH = Math.max(20, (int) (barH * ratio));
            double scrollRatio = scrollOffset / maxScroll;
            int thumbY = barY + (int) ((barH - thumbH) * scrollRatio);
            g.fill(barX, thumbY, barX + 4, thumbY + thumbH, 0xFF00B4D8);
        }
    }
}