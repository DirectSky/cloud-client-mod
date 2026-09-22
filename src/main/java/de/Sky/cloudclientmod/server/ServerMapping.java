package de.Sky.cloudclientmod.server;

import java.util.ArrayList;
import java.util.List;

/**
 * Ein einzelner Server-Eintrag aus dem Mappings-Bundle.
 * Enthält den Anzeigenamen (wie er im Discord/UI auftaucht), die Patterns
 * (Hostnamen, die auf diesen Server matchen) und relative Asset-Pfade.
 */
public class ServerMapping {

    public String name = "";
    public List<String> patterns = new ArrayList<>();

    /** Relativer Pfad, z.B. "assets/hypixel-icon.png". Kann null sein. */
    public String icon;
    /** Relativer Pfad, z.B. "assets/hypixel-banner.png". Kann null sein. */
    public String banner;

    public ServerMapping() {}

    public ServerMapping(String name, List<String> patterns) {
        this.name = name;
        this.patterns = patterns;
    }

    public boolean hasIcon()   { return icon != null && !icon.isBlank(); }
    public boolean hasBanner() { return banner != null && !banner.isBlank(); }

    /** Liefert die bevorzugte Anzeige-Form: Name oder null wenn leer. */
    public String displayName() {
        return (name != null && !name.isBlank()) ? name : null;
    }
}