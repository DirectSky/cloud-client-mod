package de.Sky.cloudclientmod.config;

import java.util.HashMap;
import java.util.Map;

public class ProfileData {

    public String name = "Neues Profil";
    public String icon = "◆";

    public Map<String, ElementConfig> elements = new HashMap<>();

    public ProfileData() {}

    public ProfileData(String name) {
        this.name = name;
    }

    public ElementConfig get(String id) {
        return elements.computeIfAbsent(id, k -> new ElementConfig());
    }

    public ProfileData copy(String newName) {
        ProfileData p = new ProfileData(newName);
        p.icon = this.icon;
        for (Map.Entry<String, ElementConfig> e : elements.entrySet()) {
            p.elements.put(e.getKey(), e.getValue().copy());
        }
        return p;
    }
}