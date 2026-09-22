package de.Sky.cloudclientmod.mixin;

import de.Sky.cloudclientmod.server.ServerListRenderer;
import de.Sky.cloudclientmod.server.ServerMapping;
import de.Sky.cloudclientmod.server.ServerMappings;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

/**
 * Hängt sich in die OnlineServerEntry-Klasse der Serverliste.
 *
 * renderContent() ist die Methode, die eine einzelne Server-Zeile malt.
 * Die Parameter i/j sind Mauspositionen — nicht die Zeilenkoordinaten!
 * Zeilenkoordinaten kommen über getContentX()/getContentY() etc.
 */
@Mixin(ServerSelectionList.OnlineServerEntry.class)
public class ServerListEntryMixin {

    @Shadow @Final private ServerData serverData;

    @Inject(method = "renderContent", at = @At("HEAD"))
    private void cloudclient$renderBanner(GuiGraphics g, int mouseX, int mouseY,
                                          boolean hovering, float partialTick,
                                          CallbackInfo ci) {
        try {
            ServerMapping mapping = lookupMapping();
            if (mapping == null) return;

            // Row-Bounds aus dem Entry
            int x = getContentX();
            int y = getContentY();
            int w = getContentWidth();
            int h = 36;  // Standard-Zeilen-Höhe der Serverliste

            ServerListRenderer.renderBanner(g, x, y, w, h, mapping);
        } catch (Exception e) {
            // Darf das Spiel nicht crashen
        }
    }

    @Inject(method = "drawIcon", at = @At("HEAD"), cancellable = true)
    private void cloudclient$renderIcon(GuiGraphics g, int x, int y,
                                        Identifier iconLocation, CallbackInfo ci) {
        try {
            ServerMapping mapping = lookupMapping();
            if (mapping == null || !mapping.hasIcon()) return;

            ServerListRenderer.renderIcon(g, x, y, 32, mapping);
            ci.cancel();
        } catch (Exception e) {
            // Bei Fehler: Vanilla rendern lassen
        }
    }

    /** Wrapper, weil getContentX()/getContentY() in der Parent-Klasse liegen. */
    private int getContentX() {
        return ((ServerSelectionList.Entry) (Object) this).getContentX();
    }

    private int getContentY() {
        return ((ServerSelectionList.Entry) (Object) this).getContentY();
    }

    private int getContentWidth() {
        return ((ServerSelectionList.Entry) (Object) this).getContentWidth();
    }

    private ServerMapping lookupMapping() {
        String ip = serverData.ip;
        if (ip == null || ip.isBlank()) return null;

        String hostOnly = ip;
        int colon = hostOnly.indexOf(':');
        if (colon > 0) hostOnly = hostOnly.substring(0, colon);

        Optional<ServerMapping> opt = ServerMappings.lookup(hostOnly);
        return opt.orElse(null);
    }
}