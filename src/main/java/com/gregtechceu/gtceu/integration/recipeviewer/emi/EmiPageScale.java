package com.gregtechceu.gtceu.integration.recipeviewer.emi;

import com.gregtechceu.gtceu.config.ConfigHolder;

import dev.emi.emi.api.widget.Bounds;
import dev.emi.emi.api.widget.Widget;
import dev.emi.emi.screen.WidgetGroup;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Shrinks an EMI recipe that is too big for the space EMI has for it, so it fits instead of running off the frame, and
 * keeps the mouse working on it. The mixins in {@code core.mixins.emi} call into this; everything here is about the
 * scale itself.
 * <p>
 * A recipe that needs scaling gets an {@link Info} for each of its widgets. A widget with one is drawn smaller, and its
 * bounds and the mouse position are converted to match, so slots, buttons and tooltips are hit where they appear.
 * The scale is about the recipe's top-left corner, with a horizontal shift that keeps it centred.
 */
public final class EmiPageScale {

    /** How a recipe is drawn: its scale, and how far right it is moved to stay centred. */
    public record Info(float scale, int offsetX) {}

    private static final Map<Widget, Info> WIDGETS = Collections.synchronizedMap(new WeakHashMap<>());

    private EmiPageScale() {}

    public static boolean enabled() {
        return ConfigHolder.getInstance().client.scaleOversizedEmiPages;
    }

    /**
     * Works out the scale for a recipe of this size in this much room, and marks the group's widgets with it.
     *
     * @return the scale used, 1 if the recipe fits
     */
    public static float apply(WidgetGroup group, int width, int height, int roomWidth, int roomHeight) {
        if (!enabled() || width <= 0 || height <= 0 || roomWidth <= 0 || roomHeight <= 0) return 1f;
        float scale = Math.min(1f, Math.min((float) roomWidth / width, (float) roomHeight / height));
        if (scale >= 0.999f) return 1f;
        float minimum = Math.max(0.1f, Math.min(1f, ConfigHolder.getInstance().client.emiPageMinScale));
        scale = Math.max(scale, minimum);
        Info info = new Info(scale, Math.round(width * (1f - scale) / 2f));
        for (Widget widget : group.widgets) {
            // the frame behind the recipe is already sized to the room EMI gave it, so it stays as it is
            if (widget.getClass().getSimpleName().equals("RecipeBackground")) continue;
            WIDGETS.put(widget, info);
        }
        return scale;
    }

    public static @Nullable Info of(Widget widget) {
        return WIDGETS.get(widget);
    }

    /** The widget's bounds as they appear on screen, relative to its group. */
    public static Bounds bounds(Widget widget, Bounds bounds) {
        Info info = WIDGETS.get(widget);
        if (info == null) return bounds;
        return new Bounds(info.offsetX() + Math.round(bounds.x() * info.scale()), Math.round(bounds.y() * info.scale()),
                Math.max(1, Math.round(bounds.width() * info.scale())),
                Math.max(1, Math.round(bounds.height() * info.scale())));
    }

    /** A mouse x relative to the group, as the widget itself sees it. */
    public static int localX(Widget widget, int x) {
        Info info = WIDGETS.get(widget);
        return info == null ? x : Math.round((x - info.offsetX()) / info.scale());
    }

    /** A mouse y relative to the group, as the widget itself sees it. */
    public static int localY(Widget widget, int y) {
        Info info = WIDGETS.get(widget);
        return info == null ? y : Math.round(y / info.scale());
    }

    /** The width of a group as it appears, for finding whether the mouse is over it. */
    public static int groupWidth(WidgetGroup group) {
        Info info = infoOf(group);
        return info == null ? group.getWidth() : info.offsetX() + Math.round(group.getWidth() * info.scale());
    }

    private static @Nullable Info infoOf(WidgetGroup group) {
        for (Widget widget : group.widgets) {
            Info info = WIDGETS.get(widget);
            if (info != null) return info;
        }
        return null;
    }

    /** EMI already limits a group's height to the room it has, which is what a scaled recipe fills. */
    public static int groupHeight(WidgetGroup group) {
        return group.getHeight();
    }
}
