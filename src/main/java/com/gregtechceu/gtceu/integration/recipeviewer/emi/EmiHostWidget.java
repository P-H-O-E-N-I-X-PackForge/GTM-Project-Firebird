package com.gregtechceu.gtceu.integration.recipeviewer.emi;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import brachy.modularui.widget.Widget;
import dev.emi.emi.api.widget.DrawableWidget;
import dev.emi.emi.api.widget.WidgetHolder;

import java.util.List;
import java.util.function.Consumer;

/**
 * Reserves a rectangle in a GT recipe panel for EMI's own widgets. The panel lays the rectangle out like any other
 * widget, so it grows with the content around it, and when EMI shows the recipe the content is added to EMI's
 * {@link WidgetHolder} at the rectangle's real position. Slots, tooltips, buttons and textures added this way are
 * ordinary EMI widgets, with everything EMI does for them.
 * <p>
 * It draws nothing in ModularUI, and the content is not shown in JEI, so give it a {@code background(...)} if JEI
 * should show something there. Referencing this class loads EMI classes: use it only where EMI is present.
 *
 * <pre>{@code
 * widget.additionalRecipeContent.child(new EmiHostWidget(60, 20, host -> {
 *     host.holder().addTexture(ICON, host.x(), host.y(), 16, 16, 0, 0);
 *     host.holder().addText(Component.literal("Heat"), host.x() + 20, host.y() + 4, 0xFF404040, false);
 * }));
 * }</pre>
 */
public class EmiHostWidget extends Widget<EmiHostWidget> {

    /**
     * Where the content goes: {@code x} and {@code y} are the top-left corner of the reserved rectangle in the
     * recipe's own coordinates, so widgets are added at {@code host.x() + dx}.
     */
    public record Host(WidgetHolder holder, int x, int y, int width, int height) {}

    /**
     * A drawn rectangle with EMI's own drawable widget: {@code draw} paints at 0,0, the top-left of the rectangle.
     */
    public static EmiHostWidget drawable(int width, int height, DrawableWidget.DrawableWidgetConsumer draw) {
        return new EmiHostWidget(width, height,
                host -> host.holder().addDrawable(host.x(), host.y(), width, height, draw));
    }

    /**
     * A tinted label with a frame, the way GT-style pages mark something, for example "Only for sculk". The text sets
     * the width. Hovering shows {@code tooltip}, if given.
     */
    public static EmiHostWidget pill(String text, int color, List<Component> tooltip) {
        var font = Minecraft.getInstance().font;
        int width = font.width(text) + 8;
        int height = 11;
        return new EmiHostWidget(width, height, host -> {
            host.holder().addDrawable(host.x(), host.y(), width, height, (graphics, mouseX, mouseY, delta) -> {
                graphics.fill(0, 0, width, height, withAlpha(color, 0x38));
                graphics.renderOutline(0, 0, width, height, withAlpha(color, 0xA0));
                graphics.drawString(font, text, 4, 2, color, false);
            });
            if (tooltip != null && !tooltip.isEmpty()) {
                host.holder().addTooltipText(tooltip, host.x(), host.y(), width, height);
            }
        });
    }

    public static EmiHostWidget pill(String text, int color) {
        return pill(text, color, List.of());
    }

    private static int withAlpha(int color, int alpha) {
        return (alpha << 24) | (color & 0xFFFFFF);
    }

    private final Consumer<Host> content;

    public EmiHostWidget(int width, int height, Consumer<Host> content) {
        this.content = content;
        size(width, height);
    }

    /** Adds the content to the holder. {@code x} and {@code y} are where this widget sits in the recipe. */
    @org.jetbrains.annotations.ApiStatus.Internal
    public void emit(WidgetHolder holder, int x, int y) {
        var area = getArea();
        content.accept(new Host(holder, x, y, area.width, area.height));
    }
}
