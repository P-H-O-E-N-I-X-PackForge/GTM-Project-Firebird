package com.gregtechceu.gtceu.api.machine.mui;

import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.common.mui.GTGuiTextures;
import com.gregtechceu.gtceu.common.mui.GTMuiWidgets;

import net.minecraft.network.chat.Component;

import brachy.modularui.api.drawable.IDrawable;
import brachy.modularui.drawable.UITexture;
import brachy.modularui.factory.PosGuiData;
import brachy.modularui.screen.ModularPanel;
import brachy.modularui.screen.UISettings;
import brachy.modularui.theme.ThemeAPI;
import brachy.modularui.utils.Alignment;
import brachy.modularui.widget.ParentWidget;
import brachy.modularui.widgets.SlotGroupWidget;
import brachy.modularui.widgets.layout.Flow;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

public class MachineUIPanel extends ModularPanel<MachineUIPanel> {

    /** The column that holds the main contents and, under them, the player inventory when there is one. */
    @lombok.Getter
    private Flow contentsColumn;
    /**
     * Takes the UI back to its main page: what a tab of the machine's own does, for the ones an addon puts on the
     * panel.
     */
    @Setter
    private Runnable showMainPage = () -> {};

    public static final int DEFAULT_CONTENT_WIDTH = 169;
    public static final int DEFAULT_CONTENT_HEIGHT = 77;

    @Getter
    protected final Flow leftConfiguratorPanel;
    protected final Flow rightConfiguratorPanel;

    /**
     * There is no column on the right: the configurators are all in the one on the left. This is that column, so that
     * code that adds to the right one ends up on the left.
     */
    public Flow getRightConfiguratorPanel() {
        return leftConfiguratorPanel;
    }

    @Getter
    protected final ParentWidget<?> mainContents;

    /**
     * The panel is drawn smaller when it is too big for the window, so a large UI still fits on the screen. This adds
     * to
     * the scale ModularUI uses for the opening animation, and its hit testing follows it. See
     * {@code ConfigHolder#scaleOversizedMachineUis}.
     */
    @Override
    public float getScale() {
        float scale = super.getScale();
        if (!com.gregtechceu.gtceu.GTCEu.isClientSide()) return scale;
        var config = com.gregtechceu.gtceu.config.ConfigHolder.INSTANCE.client;
        if (!config.scaleOversizedMachineUis) return scale;
        var area = getArea();
        if (area.w() <= 0 || area.h() <= 0) return scale;
        var window = net.minecraft.client.Minecraft.getInstance().getWindow();
        float fit = Math.min((window.getGuiScaledWidth() - 12f) / area.w(),
                (window.getGuiScaledHeight() - 12f) / area.h());
        if (fit >= 1f) return scale;
        return scale * Math.max(fit, Math.max(0.2f, Math.min(1f, config.machineUiMinScale)));
    }

    public void showMainPage() {
        showMainPage.run();
    }

    public MachineUIPanel(MetaMachine machine, PosGuiData data, UISettings settings, boolean attachPlayerInventory,
                          boolean addTitleBar, boolean drawGTLogo, UITexture gtLogoTexture) {
        super(machine.getDefinition().getId().getPath());

        UITexture themeBackground = null;
        if (!machine.getDefinition().getThemeId().equals(ThemeAPI.DEFAULT_ID)) {
            // not always a texture, as after the resources are reloaded: then it is the default one
            themeBackground = ThemeAPI.INSTANCE.getTheme(machine.getDefinition().getThemeId())
                    .getPanelTheme()
                    .theme().getBackground() instanceof UITexture texture ? texture : null;
        }
        if (themeBackground == null) {
            themeBackground = ThemeAPI.INSTANCE.getTheme(settings.getTheme()).getPanelTheme()
                    .theme().getBackground() instanceof UITexture texture ? texture : null;
        }

        if (themeBackground == null) themeBackground = GTGuiTextures.BACKGROUND;
        leftConfiguratorPanel = Flow.col()
                .coverChildren()
                .rightRel(1.0f)
                .reverseLayout(true)
                .padding(4, 2, 4, 4)
                .bottom(16)
                .crossAxisAlignment(Alignment.CrossAxis.CENTER)
                .childPadding(2)
                .excludeAreaInRecipeViewer()
                .background(themeBackground.getSubArea(0f, 0f, 0.75f, 1.0f))
                .setEnabledIf(f -> !f.getChildren().isEmpty())
                .decoration();

        rightConfiguratorPanel = Flow.col()
                .coverChildren()
                .leftRel(1.0f)
                .reverseLayout(true)
                .padding(2, 4, 4, 4)
                .bottom(16)
                .crossAxisAlignment(Alignment.CrossAxis.CENTER)
                .childPadding(2)
                .excludeAreaInRecipeViewer()
                .background(themeBackground.getSubArea(0.25f, 0f, 1.0f, 1.0f))
                .setEnabledIf(f -> !f.getChildren().isEmpty())
                .decoration();

        child(Flow.col()
                .coverChildren()
                .leftRel(1.0f)
                .reverseLayout(true)
                .padding(2, 0, 0, 0)
                .top(2)
                .crossAxisAlignment(Alignment.CrossAxis.CENTER)
                .childPadding(2)
                .excludeAreaInRecipeViewer()
                .setEnabledIf(f -> !f.getChildren().isEmpty())
                .decoration()
                .child(GTGuiTextures.INFO.asWidget().tooltip(t -> {
                    List<Component> lines = new ArrayList<>();
                    lines.add(machine.getDefinition().asStack().getHoverName());
                    machine.getDefinition().getTooltipBuilder().accept(machine.getDefinition().asStack(), lines);
                    lines.forEach(t::addLine);
                })));

        Flow panelContents = Flow.col().coverChildren();
        this.contentsColumn = panelContents;
        panelContents.margin(4);
        mainContents = new ParentWidget<>().coverChildren(DEFAULT_CONTENT_WIDTH, DEFAULT_CONTENT_HEIGHT);

        panelContents.child(mainContents);

        if (attachPlayerInventory) {
            panelContents.childPadding(2);
            var inventory = SlotGroupWidget.playerInventory((index, slot) -> slot)
                    .marginTop(1)
                    .marginBottom(3);
            panelContents.child(inventory);
        }

        if (addTitleBar) {
            child(GTMuiWidgets.createTitleBar(machine.getDefinition(), 172).decoration());
        }

        if (drawGTLogo) {
            child(new IDrawable.DrawableWidget(gtLogoTexture)
                    .right(7).bottom(7 + (attachPlayerInventory ? 78 : 0)).decoration());
        }

        child(leftConfiguratorPanel);
        child(rightConfiguratorPanel);
        child(panelContents);
        coverChildren();
    }
}
