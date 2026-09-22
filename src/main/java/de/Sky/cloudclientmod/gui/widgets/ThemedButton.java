package de.Sky.cloudclientmod.gui.widgets;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class ThemedButton extends AbstractWidget {

    public interface OnPress { void run(); }

    private final OnPress onPress;

    public ThemedButton(int x, int y, int w, int h, Component label, OnPress onPress) {
        super(x, y, w, h, label);
        this.onPress = onPress;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (this.active && this.visible
                && this.isMouseOver(event.x(), event.y())
                && event.button() == 0) {
            onPress.run();
            return true;
        }
        return false;
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float delta) {
        boolean hover = isMouseOver(mouseX, mouseY);
        int bg = hover ? 0xFF1A1A2E : 0xFF111118;
        int border = 0xFF333355;

        g.fill(getX(), getY(), getX() + width, getY() + height, bg);
        g.fill(getX(), getY(), getX() + width, getY() + 1, border);
        g.fill(getX(), getY() + height - 1, getX() + width, getY() + height, border);
        g.fill(getX(), getY(), getX() + 1, getY() + height, border);
        g.fill(getX() + width - 1, getY(), getX() + width, getY() + height, border);

        var font = Minecraft.getInstance().font;
        int tw = font.width(getMessage());
        g.drawString(font, getMessage(), getX() + (width - tw) / 2,
                getY() + (height - 8) / 2, 0xFFFFFFFF, false);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narration) {}
}