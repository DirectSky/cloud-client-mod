package de.Sky.cloudclientmod.features.impl;

import de.Sky.cloudclientmod.config.ElementConfig;
import de.Sky.cloudclientmod.features.HudFeature;
import de.Sky.cloudclientmod.features.HudRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public class FpsFeature implements HudFeature {

    @Override public String getId() { return "fps"; }
    @Override public String getDisplayName() { return "FPS"; }
    @Override public String getDescription() { return "Zeigt die aktuellen Frames pro Sekunde."; }

    @Override
    public void render(GuiGraphics g, ElementConfig cfg, float delta) {
        int fps = Minecraft.getInstance().getFps();
        String text = fps + " FPS";

        int pad = cfg.padding;
        int textW = HudRenderer.textWidth(text);
        int textH = HudRenderer.textHeight();

        int w = textW + pad * 2;
        int h = textH + pad * 2;

        HudRenderer.drawBox(g, cfg, w, h);
        HudRenderer.drawText(g, text, pad, pad, cfg.textColor);
    }

    @Override
    public int getWidth(ElementConfig cfg) {
        return HudRenderer.textWidth("120 FPS") + cfg.padding * 2;
    }

    @Override
    public int getHeight(ElementConfig cfg) {
        return HudRenderer.textHeight() + cfg.padding * 2;
    }
}