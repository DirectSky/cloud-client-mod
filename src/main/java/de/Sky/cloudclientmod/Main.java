package de.Sky.cloudclientmod;

import com.mojang.blaze3d.platform.InputConstants;
import de.Sky.cloudclientmod.config.HudConfig;
import de.Sky.cloudclientmod.features.FeatureRegistry;
import de.Sky.cloudclientmod.features.HudRenderer;
import de.Sky.cloudclientmod.gui.ModMenuScreen;
import de.Sky.cloudclientmod.gui.HudEditorScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main implements ClientModInitializer {

    public static final String MOD_ID = "cloudclientmod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static final KeyMapping.Category CATEGORY =
            new KeyMapping.Category(Identifier.fromNamespaceAndPath("cloudclientmod", "main"));

    public static KeyMapping openMenuKey;

    @Override
    public void onInitializeClient() {
        LOGGER.info("[Cloud Client] Initialisiere Mod...");

        HudConfig.load();
        FeatureRegistry.registerAll();
        HudRenderer.init();

        openMenuKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.cloudclientmod.open_menu",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                CATEGORY
        ));


        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openMenuKey.consumeClick()) {
                if (client.screen == null) {
                    client.setScreen(new HudEditorScreen());
                }
            }
        });

        LOGGER.info("[Cloud Client] Initialisiert.");
    }
}