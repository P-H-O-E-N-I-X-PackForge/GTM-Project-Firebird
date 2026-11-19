package com.gregtechceu.gtceu.api.machine;

import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.part.MultiblockPartMachine;
import com.gregtechceu.gtceu.api.multiblock.pattern.IBlockPattern;
import com.gregtechceu.gtceu.api.registry.GTRegistries;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import brachy.modularui.api.widget.IWidget;
import brachy.modularui.value.sync.PanelSyncManager;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.apache.commons.lang3.function.TriFunction;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public class MultiblockMachineDefinition extends MachineDefinition {

    // spotless:off
    public static final Codec<MultiblockMachineDefinition> CODEC = GTRegistries.MACHINES.codec().comapFlatMap(def -> {
        if (def instanceof MultiblockMachineDefinition mDef) return DataResult.success(mDef);
        else return DataResult.error(() -> "%s is not a multiblock machine definition".formatted(def.getId()));
    }, v -> v);
    //spotless:on

    @Getter
    @Setter
    private boolean generator;
    /** In the order they were added, with the default one first. Replaced whole, as it is read from other threads. */
    private volatile Map<String, Supplier<IBlockPattern>> structurePatterns = new LinkedHashMap<>();
    private final Map<String, com.gregtechceu.gtceu.api.multiblock.pattern.StructureKind> structureKinds = new HashMap<>();
    @Getter
    @Setter
    private boolean allowFlip;
    @Getter
    @Setter
    private boolean renderXEIPreview;
    @Setter
    @Getter
    @Nullable
    private Supplier<ItemStack[]> recoveryItems;
    @Setter
    @Getter
    private Function<MultiblockControllerMachine, Comparator<MultiblockPartMachine>> partSorter;
    @Getter
    @Setter
    private TriFunction<MultiblockControllerMachine, MultiblockPartMachine, Direction, BlockState> partAppearance;
    @Getter
    @Setter
    private BiFunction<MultiblockControllerMachine, PanelSyncManager, List<IWidget>> additionalDisplay;

    public MultiblockMachineDefinition(ResourceLocation id) {
        super(id);
    }

    public @NonNull Map<String, Supplier<IBlockPattern>> getStructurePatterns() {
        return structurePatterns;
    }

    public void setPattern(String structureName, Supplier<IBlockPattern> pattern) {
        structurePatterns.put(structureName, pattern);
    }

    public void setStructureKind(String structureName, com.gregtechceu.gtceu.api.multiblock.pattern.StructureKind kind) {
        structureKinds.put(structureName, kind);
    }

    /** How the structure relates to the main one; structures are alternatives unless they were added as additions. */
    public com.gregtechceu.gtceu.api.multiblock.pattern.StructureKind getStructureKind(String name) {
        return structureKinds.getOrDefault(name,
                com.gregtechceu.gtceu.api.multiblock.pattern.StructureKind.ALTERNATIVE);
    }

    /** The names of the structures that are built on top of the main one, in the order they were added. */
    public List<String> getAdditionStructures() {
        return structurePatterns.keySet().stream()
                .filter(n -> getStructureKind(n) == com.gregtechceu.gtceu.api.multiblock.pattern.StructureKind.ADDITION)
                .toList();
    }

    /**
     * Replaces one of the multiblock's structures, or adds one under a new name, for addons that change another mod's
     * multiblock. Call it during setup, before any world is loaded: controllers read their patterns when they are
     * created. Previews and the terminal pick the change up with no further work.
     *
     * @param name    the structure's name; {@code "main"} is the default one
     * @param pattern builds the pattern, the first time it is needed
     */
    public synchronized void setStructurePattern(String name,
                                                 Function<MultiblockMachineDefinition, IBlockPattern> pattern) {
        Map<String, Supplier<IBlockPattern>> updated = new LinkedHashMap<>(structurePatterns);
        updated.put(name, com.gregtechceu.gtceu.utils.memoization.GTMemoizer.memoize(() -> pattern.apply(this)));
        structurePatterns = Collections.unmodifiableMap(updated);
    }

    /** Takes away a structure added with {@link #setStructurePattern}. The default structure cannot be removed. */
    public synchronized void removeStructurePattern(String name) {
        if (name.equals(MultiblockControllerMachine.DEFAULT_STRUCTURE) || !structurePatterns.containsKey(name)) return;
        Map<String, Supplier<IBlockPattern>> updated = new LinkedHashMap<>(structurePatterns);
        updated.remove(name);
        structurePatterns = Collections.unmodifiableMap(updated);
    }
}
