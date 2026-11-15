package com.gregtechceu.gtceu.integration.recipeviewer.widgets;

import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;

import brachy.modularui.api.widget.IWidget;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Lets addons put their own widgets into the multiblock preview, the one in the recipe viewers and in the terminal, and
 * react to the structure changing.
 * <p>
 * A widget is registered for a {@link Slot} and built each time a preview is created, so it can read that preview: its
 * definition, schema, renderer and selected block. The widgets are ordinary ModularUI widgets, so
 * {@code EmiHostWidget} works in them too.
 *
 * <pre>{@code
 * MultiblockPreviewExtensions.register(Slot.BELOW, def -> def == MyMultiblocks.REACTOR,
 *         preview -> Text.lang("mymod.reactor.hint").asWidget());
 * }</pre>
 */
public final class MultiblockPreviewExtensions {

    /** Where in the preview a widget goes. */
    public enum Slot {

        /** The row of buttons across the top, after the flip and layer buttons. */
        TOOLBAR,
        /** A column to the left of the 3D view, beside the selected block's menu. */
        LEFT,
        /** A column to the right of the parts list. */
        RIGHT,
        /** Under the 3D view and the parts list. */
        BELOW,
        /**
         * Over the 3D view. The widgets are placed from its top-left corner, so give them an explicit position.
         */
        OVERLAY
    }

    @FunctionalInterface
    public interface Builder {

        /** @return the widget to add, or null to add nothing for this preview */
        @Nullable
        IWidget build(MultiblockPreviewWidget preview);
    }

    private record Entry(Slot slot, Predicate<MultiblockMachineDefinition> appliesTo, Builder builder) {}

    private static final List<Entry> ENTRIES = new CopyOnWriteArrayList<>();
    private static final List<Consumer<MultiblockPreviewWidget>> SCHEMA_LISTENERS = new CopyOnWriteArrayList<>();

    private MultiblockPreviewExtensions() {}

    /** Adds a widget to the preview of every multiblock. */
    public static void register(Slot slot, Builder builder) {
        register(slot, def -> true, builder);
    }

    /** Adds a widget to the preview of the multiblocks {@code appliesTo} accepts. */
    public static void register(Slot slot, Predicate<MultiblockMachineDefinition> appliesTo, Builder builder) {
        ENTRIES.add(new Entry(slot, appliesTo, builder));
    }

    /**
     * Called whenever a preview's structure is rebuilt: when it is first shown, flipped, or its layer or a block
     * choice changes. Use it to refresh anything you draw from the schema.
     */
    public static void onSchemaChanged(Consumer<MultiblockPreviewWidget> listener) {
        SCHEMA_LISTENERS.add(listener);
    }

    static boolean has(Slot slot, MultiblockMachineDefinition definition) {
        for (Entry entry : ENTRIES) {
            if (entry.slot() == slot && entry.appliesTo().test(definition)) return true;
        }
        return false;
    }

    static List<IWidget> build(Slot slot, MultiblockPreviewWidget preview) {
        List<IWidget> widgets = new ArrayList<>();
        for (Entry entry : ENTRIES) {
            if (entry.slot() != slot || !entry.appliesTo().test(preview.getMultiblockDefinition())) continue;
            IWidget widget = entry.builder().build(preview);
            if (widget != null) widgets.add(widget);
        }
        return widgets;
    }

    static void fireSchemaChanged(MultiblockPreviewWidget preview) {
        for (var listener : SCHEMA_LISTENERS) listener.accept(preview);
    }
}
