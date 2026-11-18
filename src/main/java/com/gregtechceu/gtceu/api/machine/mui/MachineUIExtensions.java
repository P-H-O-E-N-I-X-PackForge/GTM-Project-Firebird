package com.gregtechceu.gtceu.api.machine.mui;

import com.gregtechceu.gtceu.api.machine.MetaMachine;

import brachy.modularui.api.widget.IWidget;
import brachy.modularui.value.sync.PanelSyncManager;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

/**
 * Lets addons put their own widgets into the UI of machines they did not make, without wrapping the machine's UI
 * builder or mixing into it: a status line under the main contents, a button in a side column, a banner above.
 * <p>
 * A widget is registered for a {@link Slot} and built each time a machine's UI is built, on both the server and the
 * client, so it has to come out the same on both sides. It applies to every UI made with
 * {@link MachineUIPanelBuilder}, which is what GT's machines and the Wiki's HUD use.
 *
 * <pre>{@code
 * MachineUIExtensions.register(Slot.BELOW, m -> m instanceof MyReactor,
 *         (machine, sync, panel) -> Text.lang("mymod.reactor.hint").asWidget());
 * }</pre>
 */
public final class MachineUIExtensions {

    /** Where in the machine's UI a widget goes. */
    public enum Slot {

        /** Above the main contents. */
        ABOVE,
        /** Under the main contents, over the player inventory. */
        BELOW,
        /** The column of configurators to the left of the panel, such as the circuit slot. */
        LEFT,
        /** The column of configurators to the right of the panel, such as the power button. */
        RIGHT
    }

    @FunctionalInterface
    public interface Builder {

        /** @return the widget to add, or null to add nothing for this machine */
        @Nullable
        IWidget build(MetaMachine machine, PanelSyncManager syncManager, MachineUIPanel panel);
    }

    private record Entry(Slot slot, Predicate<MetaMachine> appliesTo, Builder builder) {}

    private static final List<Entry> ENTRIES = new CopyOnWriteArrayList<>();

    private MachineUIExtensions() {}

    /** Adds a widget to the UI of every machine. */
    public static void register(Slot slot, Builder builder) {
        register(slot, machine -> true, builder);
    }

    /** Adds a widget to the UI of the machines {@code appliesTo} accepts. */
    public static void register(Slot slot, Predicate<MetaMachine> appliesTo, Builder builder) {
        ENTRIES.add(new Entry(slot, appliesTo, builder));
    }

    /** Adds the registered widgets to a machine's panel. Called by the panel builder. */
    static void apply(MetaMachine machine, PanelSyncManager syncManager, MachineUIPanel panel) {
        for (Entry entry : ENTRIES) {
            if (!entry.appliesTo().test(machine)) continue;
            IWidget widget = entry.builder().build(machine, syncManager, panel);
            if (widget == null) continue;
            switch (entry.slot()) {
                case ABOVE -> panel.getContentsColumn().child(0, widget);
                case BELOW -> panel.getContentsColumn().child(
                        Math.min(1, panel.getContentsColumn().getChildren().size()),
                        widget);
                case LEFT -> panel.getLeftConfiguratorPanel().child(widget);
                case RIGHT -> panel.getRightConfiguratorPanel().child(widget);
            }
        }
    }
}
