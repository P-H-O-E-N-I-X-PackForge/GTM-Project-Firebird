package com.gregtechceu.gtceu.common.mui;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.feature.IMuiMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.part.MultiblockPartMachine;
import com.gregtechceu.gtceu.api.mui.factory.MachineUIFactory;

import net.minecraft.network.chat.Component;

import brachy.modularui.api.drawable.IDrawable;
import brachy.modularui.api.drawable.Text;
import brachy.modularui.drawable.DynamicDrawable;
import brachy.modularui.drawable.ItemDrawable;
import brachy.modularui.widget.ParentWidget;
import brachy.modularui.widget.ScrollWidget;
import brachy.modularui.widget.Widget;
import brachy.modularui.widget.scroll.VerticalScrollData;
import brachy.modularui.widgets.ButtonWidget;
import brachy.modularui.widgets.layout.Flow;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The navigation of a machine UI as GregTech 7.5.3's had it: a title bar over the panel with the current page's icon
 * and
 * title, a back button when there is a page to go back to, and a "+" button for the page switcher, which lists the
 * controller and the parts of a multiblock to go to. At the left are the tabs of the machine's own pages: its main
 * page and the sides page.
 */
public final class GTMuiNav {

    public static final int MAIN = 0;
    public static final int SIDES = 1;
    public static final int PAGES = 2;

    private static final int TITLE_HEIGHT = 16;
    private static final int MARGIN = 8;
    private static final int BORDER = 3;
    private static final int BUTTON_WIDTH = 18;
    private static final int PAGES_PER_ROW = 5;

    private final MetaMachine machine;
    private final @Nullable MultiblockControllerMachine controller;
    private final boolean hasSides;
    private final brachy.modularui.value.sync.PanelSyncManager syncManager;
    private final Deque<Integer> history = new ArrayDeque<>();
    private int page = MAIN;
    private Runnable onPageChanged = () -> {};

    public GTMuiNav(MetaMachine machine, boolean hasSides,
                    brachy.modularui.value.sync.PanelSyncManager syncManager) {
        this.machine = machine;
        this.syncManager = syncManager;
        this.hasSides = hasSides;
        if (machine instanceof MultiblockControllerMachine c) {
            this.controller = c;
        } else if (machine instanceof MultiblockPartMachine part && !part.getControllers().isEmpty()) {
            this.controller = part.getControllers().first();
        } else {
            this.controller = null;
        }
        // opening another machine's UI is the server's to do, which closes this one and opens that one
        syncManager.registerSyncedAction("nav_open", false, true, buf -> {
            var pos = buf.readBlockPos();
            com.gregtechceu.gtceu.GTCEu.LOGGER.info("[nav_open] request for {} from {} (controller {})", pos,
                    syncManager.getPlayer(), this.controller);
            if (!(syncManager.getPlayer() instanceof net.minecraft.server.level.ServerPlayer player) ||
                    this.controller == null) {
                return;
            }
            var target = MetaMachine.getMachine(player.level(), pos);
            boolean sameMultiblock = target == this.controller ||
                    (target instanceof MultiblockPartMachine part && part.getControllers().contains(this.controller));
            com.gregtechceu.gtceu.GTCEu.LOGGER.info("[nav_open] target {} sameMultiblock {}", target, sameMultiblock);
            if (sameMultiblock) {
                // the open one is closed first, and the other opens once the client has closed it, or its closing
                // takes the new one with it
                player.closeContainer();
                var server = player.server;
                java.util.concurrent.CompletableFuture.delayedExecutor(250, java.util.concurrent.TimeUnit.MILLISECONDS)
                        .execute(() -> server.execute(() -> {
                            com.gregtechceu.gtceu.GTCEu.LOGGER.info("[nav_open] opening {}", pos);
                            MachineUIFactory.INSTANCE.open(player, pos);
                        }));
            }
        });
    }

    /** Called when the page changes, for the layout to be made again with the pages that are on now. */
    public void onPageChanged(Runnable listener) {
        this.onPageChanged = listener;
    }

    public int page() {
        return page;
    }

    // ---- navigation

    public void toMain() {
        history.clear();
        page = MAIN;
        onPageChanged.run();
    }

    public void toSides() {
        if (page == SIDES) return;
        if (!history.isEmpty() && history.peek() == SIDES) history.pop();
        else history.push(page);
        page = SIDES;
        onPageChanged.run();
    }

    public void back() {
        if (!history.isEmpty()) page = history.pop();
        onPageChanged.run();
    }

    public void openPages() {
        history.clear();
        history.push(MAIN);
        page = PAGES;
        onPageChanged.run();
    }

    private boolean canGoBack() {
        return !history.isEmpty();
    }

    private boolean hasPages() {
        return controller != null && machine.isRemote();
    }

    private void goTo(MetaMachine target) {
        com.gregtechceu.gtceu.GTCEu.LOGGER.info("[nav] page switcher click: {} at {}", target, target.getBlockPos());
        if (target == machine) {
            toMain();
        } else {
            GTMuiSync.call(syncManager, "nav_open", buf -> buf.writeBlockPos(target.getBlockPos()));
        }
    }

    // ---- the title bar, over the panel

    public ParentWidget<?> titleBar() {
        ParentWidget<?> bar = new ParentWidget<>().widthRel(1f).height(TITLE_HEIGHT).topRel(0f, 0, 1f);

        bar.child(new ButtonWidget<>().left(MARGIN).top(BORDER).size(BUTTON_WIDTH + 2, TITLE_HEIGHT - BORDER)
                .background(GTGuiTextures.TITLE_BAR_BACKGROUND)
                .overlay(Text.str(" <").asIcon())
                .tooltip(r -> r.addLine(Component.translatable("gtceu.gui.title_bar.back")))
                .setEnabledIf(w -> canGoBack())
                .onMousePressed((c, b) -> {
                    back();
                    return true;
                }));
        bar.child(new ButtonWidget<>().right(MARGIN).top(BORDER).size(BUTTON_WIDTH + 2, TITLE_HEIGHT - BORDER)
                .background(GTGuiTextures.TITLE_BAR_BACKGROUND)
                .overlay(Text.str("+").asIcon())
                .tooltip(r -> r.addLine(Component.translatable("gtceu.gui.title_bar.page_switcher")))
                .setEnabledIf(w -> hasPages() && page != PAGES)
                .onMousePressed((c, b) -> {
                    openPages();
                    return true;
                }));

        ParentWidget<?> main = new ParentWidget<>().left(MARGIN + BUTTON_WIDTH).right(MARGIN + BUTTON_WIDTH)
                .height(TITLE_HEIGHT)
                .background(GTGuiTextures.TITLE_BAR_BACKGROUND);
        int inner = TITLE_HEIGHT - BORDER;
        main.child(new Widget<>().left(BORDER + 1).top(BORDER + 1).size(inner - 2, inner - 2)
                .background(new DynamicDrawable(this::icon)));
        main.child(Text.dynamic(this::title).asWidget().left(BORDER + inner).right(BORDER).top(BORDER + 1)
                .height(inner));
        bar.child(main);
        return bar;
    }

    private IDrawable icon() {
        int size = TITLE_HEIGHT - BORDER - 2;
        return switch (page) {
            case SIDES -> GTGuiTextures.TOOL_COVER_SETTINGS.asIcon().size(size);
            case PAGES -> Text.str("+").asIcon();
            default -> new ItemDrawable(machine.getDefinition().asStack()).asIcon().size(size);
        };
    }

    private Component title() {
        return switch (page) {
            case SIDES -> Component.translatable("gtceu.gui.directional_setting.title");
            case PAGES -> Component.translatable("gtceu.gui.title_bar.page_switcher");
            default -> Component.translatable(machine.getDefinition().getDescriptionId());
        };
    }

    // ---- the tabs at the left

    public Widget<?> mainTab() {
        return tab(MAIN, new ItemDrawable(machine.getDefinition().asStack()).asIcon().size(16),
                Component.translatable(machine.getDefinition().getDescriptionId()), this::toMain);
    }

    public @Nullable Widget<?> sidesTab() {
        if (!hasSides) return null;
        return tab(SIDES, GTGuiTextures.TOOL_COVER_SETTINGS.asIcon().size(16),
                Component.translatable("gtceu.gui.directional_setting.tab_tooltip"), this::toSides);
    }

    /** A tab of the machine's own pages: pressed in while it is the page that is open. */
    private Widget<?> tab(int tabPage, IDrawable icon, Component tooltip, Runnable open) {
        return new ButtonWidget<>().size(18, 18)
                .background(new DynamicDrawable(() -> page == tabPage ? GTGuiTextures.BACKGROUND_INVERSE :
                        GTGuiTextures.BUTTON))
                .overlay(icon)
                .tooltip(r -> r.addLine(tooltip))
                .setEnabledIf(w -> page != PAGES)
                .onMousePressed((c, b) -> {
                    open.run();
                    return true;
                });
    }

    // ---- the page switcher

    /**
     * The page that lists the controller and the parts of a multiblock; empty where there is no client to list them.
     */
    public ParentWidget<?> pagesPage() {
        ParentWidget<?> container = new ParentWidget<>();
        if (!hasPages()) return container;

        List<MetaMachine> all = new ArrayList<>();
        all.add(controller);
        for (var part : controller.getParts()) {
            if (part instanceof IMuiMachine || part.getDefinition().getUI() != null) all.add(part);
        }

        // ungrouped first, then the inputs, then the outputs
        Map<String, List<MetaMachine>> groups = new LinkedHashMap<>();
        groups.put("", new ArrayList<>());
        groups.put("Inputs", new ArrayList<>());
        groups.put("Outputs", new ArrayList<>());
        for (var m : all) {
            String key = "";
            if (m instanceof MultiblockPartMachine part) {
                for (var handlers : part.getRecipeHandlers()) {
                    IO io = handlers.getHandlerIO();
                    if (io == IO.IN) key = "Inputs";
                    else if (io == IO.OUT) key = "Outputs";
                    break;
                }
            }
            groups.get(key).add(m);
        }

        Flow content = Flow.col().widthRel(1f).coverChildrenHeight().childPadding(2)
                .crossAxisAlignment(brachy.modularui.utils.Alignment.CrossAxis.START);
        for (var entry : groups.entrySet()) {
            if (entry.getValue().isEmpty()) continue;
            if (!entry.getKey().isEmpty()) {
                content.child(Text.str(entry.getKey()).color(0xFF1FC41F).asWidget().height(10));
            }
            Flow row = null;
            int index = 0;
            for (var m : entry.getValue()) {
                if (index % PAGES_PER_ROW == 0) {
                    row = Flow.row().coverChildren().childPadding(5)
                            .crossAxisAlignment(brachy.modularui.utils.Alignment.CrossAxis.START);
                    content.child(row);
                }
                row.child(new ButtonWidget<>().size(25, 25)
                        .background(GTGuiTextures.BUTTON)
                        .overlay(new ItemDrawable(m.getDefinition().asStack()).asIcon().size(17))
                        .tooltip(r -> r.addLine(Component.translatable(m.getDefinition().getDescriptionId())))
                        .onMousePressed((c, b) -> {
                            goTo(m);
                            return true;
                        }));
                index++;
            }
        }
        container.child(new ScrollWidget<>(new VerticalScrollData()).left(6).right(6).top(6).bottom(6)
                .child(content));
        return container;
    }
}
