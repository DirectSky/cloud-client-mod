package de.Sky.cloudclientmod.config;

public class ElementConfig {

    public boolean enabled = true;

    public int x = 4;
    public int y = 4;
    public float scale = 1.0f;

    public HudStyle style = HudStyle.MODERN;
    public int cornerRadius = 4;
    public int padding = 4;

    public int backgroundColor = 0xB4141420;
    public int textColor = 0xFFFFFFFF;
    public int borderColor = 0x26FFFFFF;
    public int accentColor = 0xFF00B4D8;

    public ElementConfig() {}

    public ElementConfig(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public ElementConfig copy() {
        ElementConfig c = new ElementConfig(x, y);
        c.enabled = enabled;
        c.scale = scale;
        c.style = style;
        c.cornerRadius = cornerRadius;
        c.padding = padding;
        c.backgroundColor = backgroundColor;
        c.textColor = textColor;
        c.borderColor = borderColor;
        c.accentColor = accentColor;
        return c;
    }

    public void resetToDefault() {
        ElementConfig d = new ElementConfig();
        this.enabled = d.enabled;
        this.scale = d.scale;
        this.style = d.style;
        this.cornerRadius = d.cornerRadius;
        this.padding = d.padding;
        this.backgroundColor = d.backgroundColor;
        this.textColor = d.textColor;
        this.borderColor = d.borderColor;
        this.accentColor = d.accentColor;
    }
}