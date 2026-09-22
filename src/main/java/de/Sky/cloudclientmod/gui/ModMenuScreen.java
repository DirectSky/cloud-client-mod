package de.Sky.cloudclientmod.gui;

import de.Sky.cloudclientmod.config.HudConfig;
import de.Sky.cloudclientmod.config.ProfileData;
import de.Sky.cloudclientmod.features.FeatureRegistry;
import de.Sky.cloudclientmod.features.HudFeature;
import de.Sky.cloudclientmod.gui.widgets.ThemedButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.Map;

public class ModMenuScreen extends Screen {

    private static final int SIDEBAR_WIDTH = 130;

    private String selectedFeatureId = null;
    private ModGridView gridView;
    private FeatureDetailView detailView;

    public ModMenuScreen() {
        super(Component.literal("Cloud Client"));
    }

    public void addWidgetPublic(AbstractWidget w) {
        this.addRenderableWidget(w);
    }

    public void rebuildPublic() {
        this.rebuildWidgets();
    }

    @Override
    protected void init() {
        int contentX = SIDEBAR_WIDTH;
        int contentW = this.width - SIDEBAR_WIDTH;

        gridView = new ModGridView(contentX, 0, contentW, this.height, this::openFeature);
        detailView = new FeatureDetailView(contentX, 0, contentW, this.height, this::closeFeature);

        buildProfileSidebar();

        // WICHTIG: Detail-View-Widgets nach einem rebuildWidgets() wiederherstellen
        if (selectedFeatureId != null) {
            HudFeature f = FeatureRegistry.getById(selectedFeatureId);
            if (f != null) {
                detailView.setFeature(f);
                detailView.buildWidgets(this);
            } else {
                selectedFeatureId = null;
            }
        }
    }

    private void buildProfileSidebar() {
        int y = 20;
        int tileSize = 100;
        int tileX = 15;
        int tileSpacing = 6;

        for (Map.Entry<String, ProfileData> e : HudConfig.getAllProfiles().entrySet()) {
            final String pid = e.getKey();
            ProfileData p = e.getValue();
            boolean active = pid.equals(HudConfig.getActiveProfileId());

            ThemedButton btn = new ThemedButton(tileX, y, tileSize, 30,
                    Component.literal((active ? "● " : "○ ") + p.name),
                    () -> {
                        if (!pid.equals(HudConfig.getActiveProfileId())) {
                            HudConfig.setActiveProfile(pid);
                            Minecraft.getInstance().setScreen(new ModMenuScreen());
                        }
                    });
            addRenderableWidget(btn);
            y += 30 + tileSpacing;
        }

        ThemedButton newBtn = new ThemedButton(tileX, y, tileSize, 26,
                Component.literal("+ NEUES PROFIL"), () -> {
            String id = HudConfig.createProfile("Profil " + (HudConfig.getAllProfiles().size() + 1));
            HudConfig.setActiveProfile(id);
            Minecraft.getInstance().setScreen(new ModMenuScreen());
        });
        addRenderableWidget(newBtn);
    }

    private void openFeature(String id) {
        selectedFeatureId = id;
        rebuildPublic();
    }

    private void closeFeature() {
        selectedFeatureId = null;
        rebuildPublic();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        g.fill(0, 0, SIDEBAR_WIDTH, height, 0xC0101020);
        g.fill(SIDEBAR_WIDTH - 1, 0, SIDEBAR_WIDTH, height, 0xFF333355);

        var font = Minecraft.getInstance().font;
        g.drawString(font, "PROFILE", 15, 6, 0xFF00B4D8, false);

        if (selectedFeatureId == null) {
            if (gridView != null) gridView.render(g, mouseX, mouseY, delta);
        } else {
            if (detailView != null) detailView.render(g, mouseX, mouseY, delta);
        }

        super.render(g, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0 && selectedFeatureId == null && gridView != null) {
            if (gridView.mouseClicked(event.x(), event.y(), event.button())) return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (selectedFeatureId != null && detailView != null && detailView.getMaxScroll() > 0) {
            detailView.setScrollOffset(detailView.getScrollOffset() - scrollY * 20);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        HudConfig.save();
        Minecraft.getInstance().setScreen(new HudEditorScreen());
    }
}