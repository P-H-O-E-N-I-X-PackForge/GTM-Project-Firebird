package com.gregtechceu.gtceu.common.commands;

import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.api.multiblock.pattern.PatternState;
import com.gregtechceu.gtceu.api.sync_system.ClassSyncData;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

/**
 * What a machine looks like to one side, for finding out why the server and the client disagree about it: its synced
 * fields and, for a multiblock controller, each of its structures. The same report is made by the server command
 * {@code /gtceu debug machine} and the client command {@code /gtceu debug client_machine}, for the block being looked
 * at. Run both and compare them.
 */
public final class MachineDebugReport {

    private MachineDebugReport() {}

    public static List<Component> of(MetaMachine machine, boolean client) {
        List<Component> lines = new ArrayList<>();
        lines.add(title((client ? "Client" : "Server") + " view of " + machine.getDefinition().getId() + " at " +
                machine.getBlockPos().toShortString()));
        lines.add(line("class", machine.getClass().getName()));

        if (machine instanceof MultiblockControllerMachine controller) {
            lines.add(line("isFormed", String.valueOf(controller.isFormed())));
            lines.add(line("parts", String.valueOf(controller.getParts().size())));
            for (String name : controller.getStructureNames()) {
                PatternState state = controller.getPatternState(name);
                if (state == null) {
                    lines.add(line("structure " + name, "no state"));
                    continue;
                }
                lines.add(line("structure " + name,
                        (state.isFormed() ? "formed" : "not formed") + ", check " + state.getState() + ", " +
                                state.getErrors().size() + " error(s), " + state.getCache().size() + " cached"));
                int shown = 0;
                for (var error : state.getErrors()) {
                    if (shown++ == 3) {
                        lines.add(line("  ...", (state.getErrors().size() - 3) + " more"));
                        break;
                    }
                    lines.add(line("  error", error.type().id() + " at " + error.pos().toShortString()));
                }
            }
        }

        lines.add(title("Synced fields"));
        var fields = new TreeMap<>(ClassSyncData.getClassData(machine.getClass()).getClientSyncFields());
        if (fields.isEmpty()) lines.add(line("(none)", ""));
        for (var entry : fields.entrySet()) {
            String value;
            try {
                value = String.valueOf(entry.getValue().handle.get(machine));
            } catch (Throwable t) {
                value = "<" + t.getClass().getSimpleName() + ">";
            }
            if (value.length() > 80) value = value.substring(0, 77) + "...";
            lines.add(line(entry.getKey() + (entry.getValue().autoDetectable ? "" : " (marked)"), value));
        }
        return lines;
    }

    private static Component title(String text) {
        return Component.literal(text).withStyle(ChatFormatting.GOLD);
    }

    private static MutableComponent line(String key, String value) {
        return Component.literal(key + ": ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(value).withStyle(ChatFormatting.WHITE));
    }
}
