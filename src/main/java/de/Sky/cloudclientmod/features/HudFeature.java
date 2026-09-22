package de.Sky.cloudclientmod.features;

import de.Sky.cloudclientmod.config.ElementConfig;
import net.minecraft.client.gui.GuiGraphics;

public interface HudFeature {

    String getId();

    String getDisplayName();

    default String getDescription() { return ""; }

    void render(GuiGraphics g, ElementConfig cfg, float delta);

    int getWidth(ElementConfig cfg);

    int getHeight(ElementConfig cfg);
}