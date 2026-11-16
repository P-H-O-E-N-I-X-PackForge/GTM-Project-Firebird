package com.gregtechceu.gtceu.integration.kjs.viewer;

import com.gregtechceu.gtceu.api.recipe.gui.viewer.GTViewerParts;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

import brachy.modularui.api.drawable.Text;
import brachy.modularui.api.widget.IWidget;
import brachy.modularui.integration.recipeviewer.RecipeSlotRole;
import brachy.modularui.integration.recipeviewer.RecipeViewerSlotWidget;
import brachy.modularui.widgets.ButtonWidget;
import brachy.modularui.widgets.layout.Flow;

import java.util.function.Supplier;

/**
 * Widgets for scripts to build the parts of recipe viewer pages and multiblock previews with, without handling
 * ModularUI. Bound to scripts as {@code GTViewerWidgets}. All of them return an ordinary widget that goes where the
 * script's function is asked for one.
 */
public final class GTViewerWidgets {

    private GTViewerWidgets() {}

    /** A line of text. */
    public static IWidget text(String text) {
        return Text.str(text).asWidget();
    }

    /** A line of translated text. */
    public static IWidget lang(String key, Object... args) {
        return Text.lang(key, args).asWidget();
    }

    /** A line of text that is worked out again each time it is drawn, so it can follow something that changes. */
    public static IWidget dynamicText(Supplier<Object> text) {
        return Text.dynamic(() -> Component.literal(String.valueOf(text.get()))).asWidget();
    }

    /** Widgets one under the other. */
    public static IWidget column(IWidget... children) {
        Flow column = Flow.col().coverChildren().childPadding(2);
        for (IWidget child : children) column.child(GTViewerParts.unique(child));
        return column;
    }

    /** Widgets side by side. */
    public static IWidget row(IWidget... children) {
        Flow row = Flow.row().coverChildren().childPadding(2);
        for (IWidget child : children) row.child(GTViewerParts.unique(child));
        return row;
    }

    /** A bold title with the widgets underneath, the way GT's own panels group things. */
    public static IWidget section(String title, IWidget... children) {
        return GTViewerParts.section(Text.str(title), children);
    }

    /** Empty space. */
    public static IWidget gap(int height) {
        return GTViewerParts.gap(height);
    }

    /** An item slot that only shows the stack, with the usual recipe viewer behaviour when clicked. */
    public static IWidget itemSlot(ItemStack stack) {
        return RecipeViewerSlotWidget.create(ItemStack.class).recipeSlotRole(RecipeSlotRole.RENDER_ONLY).value(stack);
    }

    /** A fluid slot that only shows the stack. */
    public static IWidget fluidSlot(FluidStack stack) {
        return RecipeViewerSlotWidget.create(FluidStack.class).recipeSlotRole(RecipeSlotRole.RENDER_ONLY)
                .value(stack);
    }

    /** A button with a text label and a tooltip, that runs the function when clicked. */
    public static IWidget button(String label, String tooltip, Runnable onClick) {
        return new ButtonWidget<>()
                .size(Math.max(18, label.length() * 6 + 8), 14)
                .overlay(Text.str(label).asIcon())
                .tooltip(r -> r.addLine(Text.str(tooltip)))
                .onMousePressed((context, button) -> {
                    onClick.run();
                    return true;
                });
    }
}
