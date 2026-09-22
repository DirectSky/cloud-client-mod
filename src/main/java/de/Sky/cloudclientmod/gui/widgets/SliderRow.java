package de.Sky.cloudclientmod.gui.widgets;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;
import java.util.function.Function;

public class SliderRow extends AbstractSliderButton {

    private final double min, max;
    private final Function<Double, String> labelFormatter;
    private final Consumer<Double> onChange;
    private boolean updating = false;

    public SliderRow(int x, int y, int w, int h,
                     double min, double max, double initial,
                     Function<Double, String> labelFormatter,
                     Consumer<Double> onChange) {
        super(x, y, w, h, Component.empty(),
                (initial - min) / (max - min));
        this.min = min;
        this.max = max;
        this.labelFormatter = labelFormatter;
        this.onChange = onChange;
        updateMessage();
    }

    public double getValue() {
        return min + (max - min) * this.value;
    }

    @Override
    protected void updateMessage() {
        setMessage(Component.literal(labelFormatter.apply(getValue())));
    }

    @Override
    protected void applyValue() {
        if (updating) return;
        onChange.accept(getValue());
    }

    public void setValueExternal(double v) {
        updating = true;
        this.value = (v - min) / (max - min);
        updateMessage();
        updating = false;
    }

    @Override
    public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float delta) {
        int bx = getX();
        int by = getY();
        int bw = width;
        int bh = height;

        g.fill(bx, by, bx + bw, by + bh, 0xFF16161F);
        g.fill(bx, by, bx + bw, by + 1, 0xFF333355);
        g.fill(bx, by + bh - 1, bx + bw, by + bh, 0xFF333355);
        g.fill(bx, by, bx + 1, by + bh, 0xFF333355);
        g.fill(bx + bw - 1, by, bx + bw, by + bh, 0xFF333355);

        int filled = (int) (bw * this.value);
        g.fill(bx + 1, by + 1, bx + filled, by + bh - 1, 0x8800B4D8);

        var font = Minecraft.getInstance().font;
        int tw = font.width(getMessage());
        g.drawString(font, getMessage(), bx + (bw - tw) / 2,
                by + (bh - 8) / 2, 0xFFFFFFFF, false);
    }
}