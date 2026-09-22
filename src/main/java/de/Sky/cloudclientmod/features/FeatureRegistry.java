package de.Sky.cloudclientmod.features;

import de.Sky.cloudclientmod.features.impl.FpsFeature;

import java.util.ArrayList;
import java.util.List;

public class FeatureRegistry {

    private static final List<HudFeature> FEATURES = new ArrayList<>();

    public static void registerAll() {
        register(new FpsFeature());
    }

    public static void register(HudFeature feature) {
        FEATURES.add(feature);
    }

    public static List<HudFeature> getFeatures() {
        return FEATURES;
    }

    public static HudFeature getById(String id) {
        for (HudFeature f : FEATURES) {
            if (f.getId().equals(id)) return f;
        }
        return null;
    }
}