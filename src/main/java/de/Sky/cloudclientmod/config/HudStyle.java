package de.Sky.cloudclientmod.config;

public enum HudStyle {
    MODERN,
    RETRO,
    GLASS;

    public HudStyle next() {
        HudStyle[] all = values();
        return all[(ordinal() + 1) % all.length];
    }

    public String displayName() {
        return switch (this) {
            case MODERN -> "Modern";
            case RETRO  -> "Retro";
            case GLASS  -> "Glass";
        };
    }
}