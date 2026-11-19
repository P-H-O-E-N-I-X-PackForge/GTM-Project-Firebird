package com.gregtechceu.gtceu.common.mui;

import com.gregtechceu.gtceu.api.recipe.gui.viewer.GTViewerParts;
import com.gregtechceu.gtceu.common.machine.trait.AutoOutputTrait;

import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;

import brachy.modularui.api.IPanelHandler;
import brachy.modularui.api.drawable.IDrawable;
import brachy.modularui.api.drawable.Text;
import brachy.modularui.api.widget.Interactable;
import brachy.modularui.drawable.GuiDraw;
import brachy.modularui.screen.viewport.GuiContext;
import brachy.modularui.theme.WidgetTheme;
import brachy.modularui.value.BoolValue;
import brachy.modularui.value.sync.EnumSyncValue;
import brachy.modularui.value.sync.PanelSyncManager;
import brachy.modularui.widget.Widget;
import brachy.modularui.widgets.ToggleButton;
import brachy.modularui.widgets.layout.Flow;
import brachy.modularui.widgets.menu.AbstractMenuButton;
import brachy.modularui.widgets.menu.Menu;
import brachy.modularui.widgets.menu.MenuPanel;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

/**
 * A small menu for choosing which side a machine's auto-output works on, opened by shift-clicking its auto-output
 * button. A plain click still turns auto-output on or off.
 */
public final class AutoOutputSideMenu {

    private AutoOutputSideMenu() {}

    /**
     * An auto-output toggle that opens the side menu when shift-clicked.
     */
    public static class Button extends ToggleButton {

        private final Supplier<Menu<?>> menu;
        private IPanelHandler panelHandler;

        public Button(Supplier<Menu<?>> menu) {
            this.menu = menu;
        }

        @Override
        public @NotNull Interactable.Result onMousePressed(int mouseButton) {
            if (mouseButton == 0 && Interactable.hasShiftDown()) {
                toggleMenu();
                return Interactable.Result.SUCCESS;
            }
            return super.onMousePressed(mouseButton);
        }

        private void toggleMenu() {
            if (panelHandler == null) {
                panelHandler = IPanelHandler.simple(getPanel(), (parentPanel, player) -> {
                    Menu<?> built = menu.get();
                    built.resizer().relative(this);
                    AbstractMenuButton.Direction.RIGHT_DOWN.apply(built);
                    built.resizer().top(-12);
                    return new MenuPanel("auto_output_side", built);
                }, true);
            }
            if (panelHandler.isPanelOpen()) panelHandler.closePanel();
            else panelHandler.openPanel();
        }
    }

    /**
     * Makes the side value for one output, and registers it with the panel so the choice reaches the server.
     */
    public static EnumSyncValue<Direction> sideValue(AutoOutputTrait trait, boolean fluid,
                                                     PanelSyncManager syncManager) {
        var value = new EnumSyncValue<>(Direction.class,
                () -> {
                    Direction d = fluid ? trait.getFluidOutputDirection() : trait.getItemOutputDirection();
                    return d == null ? Direction.UP : d;
                },
                d -> {
                    if (fluid) trait.setFluidOutputDirection(d);
                    else trait.setItemOutputDirection(d);
                }).allowC2S();
        syncManager.syncValue(fluid ? "fluid_output_side" : "item_output_side", value);
        return value;
    }

    /** Where a side sits in the menu, which is laid out like the machine seen from the front. */
    private enum Place {

        TOP(0, 1),
        BACK(0, 2),
        LEFT(1, 0),
        FRONT(1, 1),
        RIGHT(1, 2),
        BOTTOM(2, 1);

        final int row;
        final int column;

        Place(int row, int column) {
            this.row = row;
            this.column = column;
        }
    }

    /** The place of a direction, seen from the front of the machine, or from the south if it has no level front. */
    private static Place placeOf(AutoOutputTrait trait, Direction direction) {
        if (direction == Direction.UP) return Place.TOP;
        if (direction == Direction.DOWN) return Place.BOTTOM;
        var machine = trait.getMachine();
        Direction front = machine.hasFrontFacing() && machine.getFrontFacing().getAxis().isHorizontal() ?
                machine.getFrontFacing() : Direction.SOUTH;
        if (direction == front) return Place.FRONT;
        if (direction == front.getOpposite()) return Place.BACK;
        // looking at the front, the machine's right is on the left of its back
        return direction == front.getOpposite().getClockWise() ? Place.RIGHT : Place.LEFT;
    }

    /**
     * The menu itself: the machine as a diagram, one button on each side the output can go to, with a picture of that
     * side. Their names are in the tooltips.
     */
    public static Menu<?> buildMenu(AutoOutputTrait trait, EnumSyncValue<Direction> side) {
        Direction[][] cells = new Direction[3][3];
        for (Direction direction : Direction.values()) {
            if (trait.getMachine().hasFrontFacing() && direction == trait.getMachine().getFrontFacing()) continue;
            Place place = placeOf(trait, direction);
            cells[place.row][place.column] = direction;
        }
        Flow grid = Flow.col().coverChildren().childPadding(1);
        for (int r = 0; r < 3; r++) {
            Flow row = Flow.row().coverChildren().childPadding(1);
            for (int c = 0; c < 3; c++) {
                Direction direction = cells[r][c];
                Place place = null;
                for (Place candidate : Place.values()) {
                    if (candidate.row == r && candidate.column == c) place = candidate;
                }
                if (direction != null) {
                    Place shown = place;
                    row.child(new ToggleButton()
                            .size(16, 16)
                            .value(new BoolValue.Dynamic(() -> side.getValue() == direction,
                                    selected -> side.setValue(direction)))
                            .overlay(new SideIcon(shown))
                            .tooltip(r2 -> r2.addLine(Text.of(label(trait, direction)))));
                } else if (place == Place.FRONT) {
                    // the front of a machine that has one: not a place for the output, but it shows which way is which
                    row.child(GTViewerParts.unique(new Widget<>().size(16, 16).overlay(new SideIcon(Place.FRONT))));
                } else {
                    // widgets with the same settings count as one in ModularUI, so each blank needs a name of its own
                    row.child(GTViewerParts.unique(new Widget<>().size(16, 16)));
                }
            }
            grid.child(row);
        }
        return new Menu<>().coverChildren().padding(2).child(grid);
    }

    /** A small picture of a machine with its side marked, drawn from rectangles so it needs no texture. */
    private record SideIcon(Place place) implements IDrawable {

        private static final int BODY = 0xFF8B8B8B;
        private static final int EDGE = 0xFF373737;
        private static final int MARK = 0xFFFFC800;

        @Override
        public void draw(GuiContext context, int x, int y, int width, int height, WidgetTheme widgetTheme) {
            var g = context.getGraphics();
            // the machine is the 6 by 6 square in the middle of the 14 by 14 picture
            int left = x + (width - 14) / 2;
            int top = y + (height - 14) / 2;
            switch (place) {
                case TOP -> {
                    GuiDraw.drawRect(g, left + 4, top + 1, 6, 2, MARK);
                    body(g, left + 4, top + 4);
                }
                case BOTTOM -> {
                    body(g, left + 4, top + 4);
                    GuiDraw.drawRect(g, left + 4, top + 11, 6, 2, MARK);
                }
                case LEFT -> {
                    GuiDraw.drawRect(g, left + 1, top + 4, 2, 6, MARK);
                    body(g, left + 4, top + 4);
                }
                case RIGHT -> {
                    body(g, left + 4, top + 4);
                    GuiDraw.drawRect(g, left + 11, top + 4, 2, 6, MARK);
                }
                case BACK -> {
                    // behind the machine: a marked square showing up behind and above its corner
                    GuiDraw.drawRect(g, left + 6, top + 2, 6, 6, MARK);
                    body(g, left + 2, top + 6);
                }
                case FRONT -> {
                    body(g, left + 4, top + 4);
                    GuiDraw.drawRect(g, left + 6, top + 6, 2, 2, MARK);
                }
            }
        }

        private static void body(net.minecraft.client.gui.GuiGraphics g, int x, int y) {
            GuiDraw.drawRect(g, x, y, 6, 6, EDGE);
            GuiDraw.drawRect(g, x + 1, y + 1, 4, 4, BODY);
        }
    }

    /** A side's name: as seen from the front of the machine, if it has one that is level, and otherwise plain. */
    private static Component label(AutoOutputTrait trait, Direction direction) {
        var machine = trait.getMachine();
        if (machine.hasFrontFacing() && machine.getFrontFacing().getAxis().isHorizontal()) {
            Direction front = machine.getFrontFacing();
            if (direction == front.getOpposite()) return Component.translatable("gtceu.gui.output_side.back");
            if (direction == front.getOpposite().getClockWise()) {
                return Component.translatable("gtceu.gui.output_side.right");
            }
            if (direction == front.getOpposite().getCounterClockWise()) {
                return Component.translatable("gtceu.gui.output_side.left");
            }
        }
        if (direction == Direction.UP) return Component.translatable("gtceu.gui.output_side.top");
        if (direction == Direction.DOWN) return Component.translatable("gtceu.gui.output_side.bottom");
        return Component.literal(capitalise(direction.getName()));
    }

    private static String capitalise(String name) {
        List<String> parts = new ArrayList<>();
        for (String part : name.split("_")) {
            if (!part.isEmpty()) parts.add(part.substring(0, 1).toUpperCase(Locale.ROOT) + part.substring(1));
        }
        return String.join(" ", parts);
    }
}
