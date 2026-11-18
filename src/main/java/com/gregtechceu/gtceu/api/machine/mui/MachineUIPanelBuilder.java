package com.gregtechceu.gtceu.api.machine.mui;

import com.gregtechceu.gtceu.api.capability.IControllable;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IDistinctPart;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.steam.SimpleSteamMachine;
import com.gregtechceu.gtceu.api.machine.trait.feature.IAttachConfiguratorsTrait;
import com.gregtechceu.gtceu.api.recipe.modifier.RecipeModifierList;
import com.gregtechceu.gtceu.common.data.GTRecipeModifiers;
import com.gregtechceu.gtceu.common.mui.GTGuiTextures;
import com.gregtechceu.gtceu.common.mui.GTMuiWidgets;
import com.gregtechceu.gtceu.common.mui.widgets.SteamDialWidget;
import com.gregtechceu.gtceu.utils.FormattingUtil;

import net.minecraft.network.chat.Component;

import brachy.modularui.drawable.UITexture;
import brachy.modularui.factory.PosGuiData;
import brachy.modularui.factory.SidedPosGuiData;
import brachy.modularui.screen.UISettings;
import brachy.modularui.utils.Color;
import brachy.modularui.value.sync.DoubleSyncValue;
import brachy.modularui.value.sync.IntSyncValue;
import brachy.modularui.value.sync.PanelSyncManager;
import brachy.modularui.widget.ParentWidget;
import brachy.modularui.widget.Widget;
import brachy.modularui.widgets.layout.Flow;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.Arrays;
import java.util.function.Consumer;

@Accessors(fluent = true)
@Setter
public class MachineUIPanelBuilder {

    /**
     * Should the GregTech logo be drawn in the bottom right corner of the panel.
     */
    private boolean drawGTLogo = false;
    /**
     * The texture to use for the GregTech logo.
     */
    private UITexture gtLogoTexture = GTGuiTextures.GREGTECH_LOGO;
    /**
     * Should the player inventory be attached to the bottom of the panel.
     */
    private boolean attachInventory = true;
    /**
     * Should a fancy title bar be created for this panel.
     */
    private boolean addTitleBar = true;
    /**
     * If machine traits should be allowed to attach configurators to the sides of the panel.
     */
    private boolean addTraitConfigurators = true;
    /**
     * If the default configurators (circuit slot, battery slot, power button) should be added to this machine, provided
     * the machine supports them.
     */
    private boolean addDefaultConfigurators = true;
    /**
     * Whether a multiblock controller's UI reports why it has not formed, under its main contents, the way the
     * standard multiblock UIs do. Standard multiblocks already do this in their main contents; this is for UIs that
     * build their own.
     */
    private boolean showPatternErrors = false;
    private final MetaMachine machine;

    @Getter
    private Consumer<Flow> leftConfigurators = (f) -> {};
    @Getter
    private Consumer<Flow> rightConfigurators = (f) -> {};
    private Consumer<ParentWidget<?>> mainContents = (p) -> {};

    protected MachineUIPanelBuilder(MetaMachine machine) {
        this.machine = machine;
    }

    public MachineUIPanel build(PosGuiData data, PanelSyncManager syncManager, UISettings settings) {
        boolean hasSides = addDefaultConfigurators && com.gregtechceu.gtceu.common.mui.GTMuiSides.applies(machine);
        // the title bar over the panel is the navigation's, with the back button and the page switcher
        var nav = hasSides ? new com.gregtechceu.gtceu.common.mui.GTMuiNav(machine, true, syncManager) : null;
        var panel = new MachineUIPanel(machine, data, settings, attachInventory, addTitleBar && nav == null,
                drawGTLogo,
                gtLogoTexture);

        var attachLeft = panel.getLeftConfiguratorPanel();
        // all the side columns are on the left; the right column is where the Wiki's HUD tabs used to be too
        var attachRight = attachLeftForRight(panel);
        var attachMain = panel.getMainContents();

        if (addDefaultConfigurators) {
            if (machine instanceof WorkableMultiblockMachine workableMultiblockMachine) {
                attachRight.child(GTMuiWidgets.createPowerButton(workableMultiblockMachine));

                if (machine.getDefinition().getBaseRecipeModifier() instanceof RecipeModifierList rml &&
                        Arrays.stream(rml.getModifiers()).anyMatch(m -> m == GTRecipeModifiers.BATCH_MODE) &&
                        machine instanceof WorkableElectricMultiblockMachine workableElectric) {
                    attachRight.child(GTMuiWidgets.createBatchModeButton(workableElectric));
                }

                attachRight.child(GTMuiWidgets.createVoidingButton(workableMultiblockMachine));

                if (machine.getDefinition().getRecipeTypes().length > 1 &&
                        machine instanceof WorkableElectricMultiblockMachine workableMachine) {
                    attachRight.child(GTMuiWidgets.createRecipeTypeButton(workableMachine, syncManager));
                }

            } else {
                if (machine instanceof IControllable controllable) {
                    attachRight.child(GTMuiWidgets.createPowerButton(controllable));
                }
                if (machine instanceof IDistinctPart distinctPart) {
                    attachRight.childIf(distinctPart.supportsDistinct(),
                            () -> GTMuiWidgets.createDistinctnessButton(distinctPart));
                }
            }
        }

        leftConfigurators.accept(attachLeft);
        rightConfigurators.accept(attachRight);
        mainContents.accept(attachMain);

        if (nav != null) {
            // the sides page and the page switcher take the place of the main contents while they are on
            var column = panel.getContentsColumn();
            final int inventoryHeight = attachInventory ? 0 : 78;
            final int mainPage = com.gregtechceu.gtceu.common.mui.GTMuiNav.MAIN;
            ParentWidget<?> sidesInner = null;
            Widget<?> sidesPage = null;
            if (hasSides) {
                sidesInner = com.gregtechceu.gtceu.common.mui.GTMuiSides.page(machine, data, syncManager, settings);
                sidesPage = sidesInner;
                if (!attachInventory) {
                    // a machine UI without the player inventory (the HUD ones) gets one under the sides page
                    sidesPage = Flow.col().coverChildren().childPadding(2).child(sidesInner)
                            .child(brachy.modularui.widgets.SlotGroupWidget.playerInventory((index, slot) -> slot)
                                    .marginTop(1).marginBottom(3));
                }
                sidesPage.setEnabledIf(
                        w -> !machine.isRemote() || nav.page() == com.gregtechceu.gtceu.common.mui.GTMuiNav.SIDES);
            }
            ParentWidget<?> pagesPage = nav.pagesPage();
            pagesPage.setEnabledIf(
                    w -> !machine.isRemote() || nav.page() == com.gregtechceu.gtceu.common.mui.GTMuiNav.PAGES);

            // the pages are as big as the main contents they replace, as the tabs were in the old machine UI
            int[] mainSize = { 0, 0 };
            int[] asked = { 0, 0, 0, 0 };
            final ParentWidget<?> sizedSides = sidesInner;
            panel.child(new Widget<>().size(0, 0).onUpdateListener(w -> {
                if (nav.page() == mainPage) {
                    var area = attachMain.getArea();
                    if (area.w() > 0 && area.h() > 0) {
                        mainSize[0] = area.w();
                        mainSize[1] = area.h();
                    }
                }
                if (mainSize[0] <= 0) return;
                // asked for once for each size: a page that is off keeps the area it had, so its area is no guide to
                // this
                int sidesHeight = Math.max(MachineUIPanel.DEFAULT_CONTENT_HEIGHT, mainSize[1] - inventoryHeight);
                if (sizedSides != null && (asked[0] != mainSize[0] || asked[1] != sidesHeight)) {
                    asked[0] = mainSize[0];
                    asked[1] = sidesHeight;
                    sizedSides.size(mainSize[0], sidesHeight);
                    sizedSides.scheduleResize();
                }
                // the page switcher has no player inventory, so it takes the room of it
                int pagesHeight = mainSize[1] + (attachInventory ? 78 : 0);
                if (asked[2] != mainSize[0] || asked[3] != pagesHeight) {
                    asked[2] = mainSize[0];
                    asked[3] = pagesHeight;
                    pagesPage.size(mainSize[0], pagesHeight);
                    pagesPage.scheduleResize();
                }
            }));
            attachMain.setEnabledIf(w -> !machine.isRemote() || nav.page() == mainPage);
            column.collapseDisabledChildren();
            // what is enabled is laid out again when the page changes, or a tab that was off is left with no room
            panel.setShowMainPage(nav::toMain);
            nav.onPageChanged(() -> {
                column.scheduleResize();
                attachRight.scheduleResize();
                panel.scheduleResize();
            });
            int at = Math.min(1, column.getChildren().size());
            if (sidesPage != null) column.child(at, sidesPage);
            column.child(at, pagesPage);
            if (attachInventory) {
                for (var child : column.getChildren()) {
                    if (child instanceof brachy.modularui.widgets.SlotGroupWidget) {
                        ((Widget<?>) child).setEnabledIf(
                                w -> !machine.isRemote() ||
                                        nav.page() != com.gregtechceu.gtceu.common.mui.GTMuiNav.PAGES);
                    }
                }
            }
            // added last, so it is the top one of the left column: the main tab over the sides tab
            var sidesTab = nav.sidesTab();
            if (sidesTab != null) attachRight.child(sidesTab);
            attachRight.child(nav.mainTab());
            if (machine.isRemote()) panel.child(nav.titleBar());
        }

        if (showPatternErrors &&
                machine instanceof com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine controller) {
            // under the main contents, above the player inventory; the widget takes no room while there are no errors
            panel.getContentsColumn().child(1,
                    com.gregtechceu.gtceu.common.mui.GTMultiblockTextUtil.addUnformedWarning(controller, syncManager));
        }

        if (addTraitConfigurators) {
            for (var attachConfiguratorsTrait : machine.getTraitHolder()
                    .getTraitsByInterface(IAttachConfiguratorsTrait.class)) {
                attachConfiguratorsTrait.attachLeftConfigurators(attachLeft, panel, syncManager);
                attachConfiguratorsTrait.attachRightConfigurators(attachRight, panel, syncManager);
            }
        }

        if (machine instanceof SimpleSteamMachine steamMachine) {
            IntSyncValue steamAmount = syncManager.getOrCreateSyncHandler("steamTank", IntSyncValue.class,
                    () -> new IntSyncValue(() -> steamMachine.steamTank.getFluidInTank(0).getAmount()));
            IntSyncValue steamCapacity = syncManager.getOrCreateSyncHandler("steamCapacity", IntSyncValue.class,
                    () -> new IntSyncValue(() -> steamMachine.steamTank.getTankCapacity(0)));

            DoubleSyncValue steamProgress = syncManager.getOrCreateSyncHandler("steamTankRatio", DoubleSyncValue.class,
                    () -> new DoubleSyncValue(() -> steamMachine.steamTank.getFluidInTank(0).getAmount() /
                            (float) steamMachine.steamTank.getTankCapacity(0)));
            final int dialWidth = 4;
            final int dialHeight = 12;
            UITexture background = steamMachine.isHighPressure() ? GTGuiTextures.STEAM_DIAL_STEEL :
                    GTGuiTextures.STEAM_DIAL_BRONZE;
            attachMain.child(new ParentWidget<>()
                    .child(background.asWidget()
                            .size(32, 32)
                            .tooltipAutoUpdate(true)
                            .tooltipDynamic(r -> r.addLine(Component.translatable("gtceu.multiblock.steam.steam_stored",
                                    FormattingUtil.formatNumbers(steamAmount.getIntValue()),
                                    FormattingUtil.formatNumbers(steamCapacity.getIntValue())))))
                    .child(new SteamDialWidget(steamProgress)
                            .setMinAngle((float) Math.PI)
                            .setMaxAngle((float) 0.0f)
                            .setColor(Color.BLACK.brighterSafe(4))
                            .asWidget().decoration()

                            .size(dialHeight, dialWidth)
                            .left(16)
                            .top(16)
                            .tooltipAutoUpdate(true)
                            .tooltipDynamic(r -> r.addLine(Component.translatable("gtceu.multiblock.steam.steam_stored",
                                    FormattingUtil.formatNumbers(steamAmount.getIntValue()),
                                    FormattingUtil.formatNumbers(steamCapacity.getIntValue())))))
                    .leftRel(0.0f).left(-36).top(4)
                    .excludeAreaInRecipeViewer());
        }

        for (var cover : machine.getCoverContainer().getCovers()) {
            GTMuiWidgets.createCoverWidget(attachLeft, cover,
                    new SidedPosGuiData(data.getPlayer(), data.getBlockPos(), cover.attachedSide), syncManager,
                    settings);
        }

        MachineUIExtensions.apply(machine, syncManager, panel);

        return panel;
    }

    /**
     * Makes the panel report a multiblock controller's pattern errors under its main contents. See
     * {@code MultiblockControllerMachine#getErrorReportStructures}.
     */
    public MachineUIPanelBuilder showPatternErrors() {
        this.showPatternErrors = true;
        return this;
    }

    private static Flow attachLeftForRight(MachineUIPanel panel) {
        return panel.getLeftConfiguratorPanel();
    }

    public static MachineUIPanelBuilder panelBuilder(MetaMachine machine) {
        return new MachineUIPanelBuilder(machine);
    }

    public static MachineUIPanelBuilder defaultSteamMachinePanelBuilder(MetaMachine machine) {
        return new MachineUIPanelBuilder(machine).addDefaultConfigurators(false)
                .addTraitConfigurators(false);
    }
}
