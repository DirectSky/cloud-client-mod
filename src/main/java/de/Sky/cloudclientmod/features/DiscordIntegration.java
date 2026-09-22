package de.Sky.cloudclientmod.features;

import de.Sky.cloudclientmod.Main;
import de.Sky.cloudclientmod.server.InstanceState;
import de.Sky.cloudclientmod.server.ServerMapping;
import de.Sky.cloudclientmod.server.ServerMappings;
import de.Sky.cloudclientmod.util.DiscordPresence;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;

import java.util.Optional;

public final class DiscordIntegration {

    private DiscordIntegration() {}

    public static void init() {
        DiscordPresence.init();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            try {
                tick(client);
            } catch (Exception e) {
                // Integration darf das Spiel nicht crashen
            }
        });
    }

    private static void tick(Minecraft client) {
        String state = InstanceState.getInstanceName();
        String details = computeDetails(client);
        DiscordPresence.update(state, details);
    }

    private static String computeDetails(Minecraft client) {
        // Im Hauptmenü oder Ladebildschirm
        if (client.level == null || client.player == null) {
            return "In Menus";
        }

        // Singleplayer
        if (client.isLocalServer()) {
            return "Playing Singleplayer";
        }

        // Multiplayer — Server-IP auslesen
        ServerData server = client.getCurrentServer();
        if (server == null) {
            return "Playing Multiplayer";
        }

        String ip = server.ip;
        if (ip == null || ip.isBlank()) {
            return "Playing Multiplayer";
        }

        // Mappings-Lookup (entfernt Port, falls vorhanden)
        String hostOnly = ip;
        int colon = hostOnly.indexOf(':');
        if (colon > 0) hostOnly = hostOnly.substring(0, colon);

        Optional<ServerMapping> mapping = ServerMappings.lookup(hostOnly);
        if (mapping.isPresent()) {
            String name = mapping.get().displayName();
            if (name != null && !name.isBlank()) {
                return "Playing " + name;
            }
        }

        return "Playing Multiplayer";
    }
}