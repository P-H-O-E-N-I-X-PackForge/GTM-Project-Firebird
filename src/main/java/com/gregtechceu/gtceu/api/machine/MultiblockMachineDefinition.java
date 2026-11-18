package com.gregtechceu.gtceu.api.machine;

import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.part.MultiblockPartMachine;
import com.gregtechceu.gtceu.api.multiblock.pattern.IBlockPattern;
import com.gregtechceu.gtceu.api.registry.GTRegistries;
import com.gregtechceu.gtceu.utils.memoization.GTMemoizer;

import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import brachy.modularui.api.widget.IWidget;
import brachy.modularui.value.sync.PanelSyncManager;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import io.netty.handler.codec.DecoderException;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.function.TriFunction;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

public class MultiblockMachineDefinition extends MachineDefinition {

    // spotless:off
    public static final Codec<MultiblockMachineDefinition> CODEC = GTRegistries.MACHINES.byNameCodec().comapFlatMap(def -> {
        if (def instanceof MultiblockMachineDefinition mDef) return DataResult.success(mDef);
        else return DataResult.error(() -> "%s is not a multiblock machine definition".formatted(def.getId()));
    }, v -> v);

    public static final StreamCodec<RegistryFriendlyByteBuf, MultiblockMachineDefinition> STREAM_CODEC = ByteBufCodecs.registry(GTRegistries.Keys.MACHINE)
            .map(v -> {
                if (v instanceof MultiblockMachineDefinition multiblockMachineDefinition) {
                    return multiblockMachineDefinition;
                }
                throw new DecoderException("%s is not a multiblock machine definition".formatted(v.getId()));
            }, UnaryOperator.identity());
    //spotless:on

    @Getter
    private final boolean generator;
    private volatile Map<String, Supplier<IBlockPattern>> structurePatterns;
    private final Map<String, com.gregtechceu.gtceu.api.multiblock.pattern.StructureKind> structureKinds;
    @Getter
    private final boolean allowFlip;
    @Getter
    private final boolean renderXEIPreview;
    @Getter
    private final boolean renderWorldPreview;
    @Getter
    private final Supplier<ItemStack[]> recoveryItems;
    @Getter
    private final Function<MultiblockControllerMachine, Comparator<MultiblockPartMachine>> partSorter;
    @Getter
    private final TriFunction<MultiblockControllerMachine, MultiblockPartMachine, Direction, BlockState> partAppearance;
    @Getter
    private final BiFunction<MultiblockControllerMachine, PanelSyncManager, List<IWidget>> additionalDisplay;

    /** How the structure relates to the main one; structures are alternatives unless they were added as additions. */
    public com.gregtechceu.gtceu.api.multiblock.pattern.StructureKind getStructureKind(String name) {
        return structureKinds.getOrDefault(name,
                com.gregtechceu.gtceu.api.multiblock.pattern.StructureKind.ALTERNATIVE);
    }

    /** The names of the structures that are built on top of the main one, in the order they were added. */
    public java.util.List<String> getAdditionStructures() {
        return structurePatterns.keySet().stream()
                .filter(n -> getStructureKind(n) == com.gregtechceu.gtceu.api.multiblock.pattern.StructureKind.ADDITION)
                .toList();
    }

    public Map<String, Supplier<IBlockPattern>> getStructurePatterns() {
        return structurePatterns;
    }

    /**
     * Replaces one of the multiblock's structures, or adds one under a new name, for addons that change another mod's
     * multiblock. Call it during setup, for example in {@code FMLCommonSetupEvent#enqueueWork}, before any world is
     * loaded: controllers read their patterns when they are created. Previews and the terminal pick the change up
     * with no further work.
     *
     * @param name    the structure's name; {@code "main"} is the default one
     * @param pattern builds the pattern, the first time it is needed
     */
    public synchronized void setStructurePattern(String name,
                                                 Function<MultiblockMachineDefinition, IBlockPattern> pattern) {
        Map<String, Supplier<IBlockPattern>> updated = new LinkedHashMap<>(structurePatterns);
        updated.put(name, GTMemoizer.memoize(() -> pattern.apply(this)));
        structurePatterns = Collections.unmodifiableMap(updated);
    }

    /** Takes away a structure added with {@link #setStructurePattern}. The default structure cannot be removed. */
    public synchronized void removeStructurePattern(String name) {
        if (name.equals(MultiblockControllerMachine.DEFAULT_STRUCTURE) || !structurePatterns.containsKey(name)) return;
        Map<String, Supplier<IBlockPattern>> updated = new LinkedHashMap<>(structurePatterns);
        updated.remove(name);
        structurePatterns = Collections.unmodifiableMap(updated);
    }

    public MultiblockMachineDefinition(ResourceLocation id, Properties properties) {
        super(id, properties);
        this.generator = properties.generator;

        // in the order they were added, with the default one first: previews and menus list them in this order
        Map<String, Supplier<IBlockPattern>> structurePatterns = new LinkedHashMap<>();
        if (properties.patterns().isEmpty()) {
            throw new IllegalStateException("Missing default structure pattern for " + getName());
        }
        var defaultPattern = properties.patterns.get(MultiblockControllerMachine.DEFAULT_STRUCTURE);
        if (defaultPattern != null) {
            structurePatterns.put(MultiblockControllerMachine.DEFAULT_STRUCTURE,
                    GTMemoizer.memoize(() -> defaultPattern.apply(this)));
        }
        for (Map.Entry<String, Function<MultiblockMachineDefinition, IBlockPattern>> entry : properties.patterns
                .entrySet()) {
            if (entry.getKey().equals(MultiblockControllerMachine.DEFAULT_STRUCTURE)) continue;
            structurePatterns.put(entry.getKey(), GTMemoizer.memoize(() -> entry.getValue().apply(this)));
        }
        this.structurePatterns = Collections.unmodifiableMap(structurePatterns);
        this.structureKinds = Map.copyOf(properties.kinds);
        this.allowFlip = properties.allowFlip();
        this.renderXEIPreview = properties.renderMultiblockXEIPreview;
        this.renderWorldPreview = properties.renderMultiblockWorldPreview;
        this.recoveryItems = () -> properties.recoveryItems.stream().map(Supplier::get).flatMap(Arrays::stream)
                .toArray(ItemStack[]::new);
        this.partSorter = GTMemoizer.memoizeFunctionWeakIdent(properties.partSorter());
        this.partAppearance = properties.partAppearance();
        this.additionalDisplay = properties.additionalDisplay();
    }

    @Accessors(fluent = true)
    @Getter
    @Setter
    public static class Properties extends MachineDefinition.Properties {

        private boolean generator = false;
        private final Map<String, Function<MultiblockMachineDefinition, IBlockPattern>> patterns = new java.util.LinkedHashMap<>();
        private final Map<String, com.gregtechceu.gtceu.api.multiblock.pattern.StructureKind> kinds = new java.util.HashMap<>();
        private boolean allowFlip = true;
        private final List<Supplier<ItemStack[]>> recoveryItems = new ArrayList<>();
        private Function<MultiblockControllerMachine, Comparator<MultiblockPartMachine>> partSorter = (c) -> (a,
                                                                                                              b) -> 0;
        private TriFunction<MultiblockControllerMachine, MultiblockPartMachine, Direction, BlockState> partAppearance = (controller,
                                                                                                                         part,
                                                                                                                         side) -> controller
                                                                                                                                 .getDefinition()
                                                                                                                                 .getAppearance()
                                                                                                                                 .get();
        private boolean renderMultiblockWorldPreview = true;
        private boolean renderMultiblockXEIPreview = true;
        private BiFunction<MultiblockControllerMachine, PanelSyncManager, List<IWidget>> additionalDisplay = (m,
                                                                                                              sm) -> Collections
                                                                                                                      .emptyList();
    }
}
