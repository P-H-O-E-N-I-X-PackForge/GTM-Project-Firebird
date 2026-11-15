package com.gregtechceu.gtceu.api.recipe.gui.viewer;

import com.gregtechceu.gtceu.api.GTValues;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import brachy.modularui.api.drawable.Text;
import brachy.modularui.api.widget.IWidget;
import brachy.modularui.utils.Alignment;
import brachy.modularui.widget.Widget;
import brachy.modularui.widgets.layout.Flow;

import java.util.function.Supplier;

/**
 * The pieces GT's own recipe panels are made of, so a custom panel built from them looks like it belongs. Each returns
 * an ordinary widget to add to a panel or to {@code GTRecipeViewerWidget#textComponents}.
 */
public final class GTViewerParts {

    private GTViewerParts() {}

    /** A line of text. */
    public static IWidget textRow(Text text) {
        return text.asWidget();
    }

    /** {@code label  value}, the value following the label on one line. */
    public static IWidget labelValueRow(Text label, Supplier<Component> value) {
        return Flow.row().coverChildren().childPadding(3).child(label.asWidget()).child(Text.dynamic(value).asWidget());
    }

    /** A titled group of rows: a bold title with the rows underneath. */
    public static Flow section(Text title, IWidget... rows) {
        Flow column = Flow.col().widthRel(1f).coverChildrenHeight().childPadding(1)
                .crossAxisAlignment(Alignment.CrossAxis.START)
                .child(title.style(ChatFormatting.BOLD).asWidget());
        for (IWidget row : rows) column.child(row);
        return column;
    }

    /** The recipe's duration in seconds, as GT writes it. */
    public static IWidget durationRow(int ticks) {
        return Text.lang("gtceu.recipe.duration", ticks / 20.0).asWidget();
    }

    /** A voltage tier's short coloured name, for example the overclock tier. */
    public static Component tierName(int tier) {
        return Component.literal(GTValues.VNF[Math.max(0, Math.min(tier, GTValues.VNF.length - 1))]);
    }

    /** A chance as a percentage, for example for a byproduct. */
    public static Component chance(float chance) {
        return Component.literal(String.format("%.0f%%", chance * 100f));
    }

    /** Empty space between sections, to keep panels evenly spaced. */
    public static IWidget gap(int height) {
        return new Widget<>().size(1, height);
    }
}
