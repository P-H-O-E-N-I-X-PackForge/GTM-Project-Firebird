package com.gregtechceu.gtceu.api.registry.registrate;

import com.gregtechceu.gtceu.api.block.MetaMachineBlock;
import com.gregtechceu.gtceu.api.item.MetaMachineItem;
import com.gregtechceu.gtceu.api.machine.MachineInstanceFactory;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.part.MultiblockPartMachine;
import com.gregtechceu.gtceu.api.machine.property.GTMachineModelProperties;
import com.gregtechceu.gtceu.api.multiblock.pattern.IBlockPattern;
import com.gregtechceu.gtceu.utils.memoization.GTMemoizer;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

import brachy.modularui.api.widget.IWidget;
import brachy.modularui.value.sync.PanelSyncManager;
import dev.latvian.mods.rhino.util.HideFromJS;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.experimental.Tolerate;
import org.apache.commons.lang3.function.TriFunction;

import java.util.*;
import java.util.function.*;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Accessors(chain = true, fluent = true)
public class MultiblockMachineBuilder<DEFINITION extends MultiblockMachineDefinition,
        MACHINE extends MultiblockControllerMachine,
        SELF extends MultiblockMachineBuilder<DEFINITION, MACHINE, SELF>>
                                     extends MachineBuilder<DEFINITION, MACHINE, SELF> {

    private boolean generator;
    private Map<String, Function<MultiblockMachineDefinition, IBlockPattern>> patterns;
    private final Map<String, com.gregtechceu.gtceu.api.multiblock.pattern.StructureKind> kinds = new java.util.HashMap<>();
    private boolean allowFlip = true;
    private final List<Supplier<ItemStack[]>> recoveryItems = new ArrayList<>();
    private Function<MultiblockControllerMachine, Comparator<MultiblockPartMachine>> partSorter = (c) -> (a, b) -> 0;
    private TriFunction<MultiblockControllerMachine, MultiblockPartMachine, Direction, BlockState> partAppearance;

    @Getter
    private BiFunction<MultiblockControllerMachine, PanelSyncManager, List<IWidget>> additionalDisplay = (m,
                                                                                                          sm) -> Collections
                                                                                                                  .emptyList();

    public MultiblockMachineBuilder(GTRegistrate registrate, String name,
                                    BiFunction<BlockBehaviour.Properties, DEFINITION, MetaMachineBlock> blockFactory,
                                    BiFunction<MetaMachineBlock, Item.Properties, MetaMachineItem> itemFactory,
                                    MachineInstanceFactory<MACHINE> blockEntityFactory) {
        super(registrate, name, (loc -> (DEFINITION) new MultiblockMachineDefinition(loc)),
                blockFactory,
                itemFactory, blockEntityFactory);
        // in the order they were added: previews and menus list them in this order
        patterns = new java.util.LinkedHashMap<>();
        allowExtendedFacing(true);
        allowCoverOnFront(true);
        // always add the formed property to multi controllers
        modelProperty(GTMachineModelProperties.IS_FORMED, false);
    }

    public SELF generator(boolean generator) {
        this.generator = generator;
        return getThis();
    }

    public SELF pattern(Function<MultiblockMachineDefinition, IBlockPattern> pattern) {
        this.patterns.put(MultiblockControllerMachine.DEFAULT_STRUCTURE, pattern);
        return getThis();
    }

    public SELF pattern(String structureName, Function<MultiblockMachineDefinition, IBlockPattern> pattern) {
        this.patterns.put(structureName, pattern);
        return getThis();
    }

    /**
     * Adds a structure that is built on top of the main one instead of in its place, such as an optional extension. The
     * pattern is anchored on the controller like any other and only has to check the blocks it adds; everything else
     * can be left to {@code Predicates.any()}. Previews show it with the main structure and the terminal builds it with
     * it. See {@link com.gregtechceu.gtceu.api.multiblock.pattern.StructureKind#ADDITION}.
     */
    public SELF additionalPattern(String structureName, Function<MultiblockMachineDefinition, IBlockPattern> pattern) {
        this.patterns.put(structureName, pattern);
        this.kinds.put(structureName, com.gregtechceu.gtceu.api.multiblock.pattern.StructureKind.ADDITION);
        return getThis();
    }

    public SELF allowFlip(boolean allowFlip) {
        this.allowFlip = allowFlip;
        return getThis();
    }

    public SELF partSorter(Function<MultiblockControllerMachine, Comparator<MultiblockPartMachine>> partSorter) {
        this.partSorter = partSorter;
        return getThis();
    }

    public SELF partAppearance(TriFunction<MultiblockControllerMachine, MultiblockPartMachine, Direction, BlockState> partAppearance) {
        this.partAppearance = partAppearance;
        return getThis();
    }

    public SELF additionalDisplay(BiFunction<MultiblockControllerMachine, PanelSyncManager, List<IWidget>> additionalDisplay) {
        this.additionalDisplay = additionalDisplay;
        return getThis();
    }

    public SELF recoveryItems(Supplier<ItemLike[]> items) {
        this.recoveryItems.add(() -> Arrays.stream(items.get()).map(ItemLike::asItem).map(Item::getDefaultInstance)
                .toArray(ItemStack[]::new));
        return getThis();
    }

    public SELF recoveryStacks(Supplier<ItemStack[]> stacks) {
        this.recoveryItems.add(stacks);
        return getThis();
    }

    @Tolerate
    public SELF partSorter(Comparator<MultiblockPartMachine> sorter) {
        this.partSorter = $ -> sorter;
        return getThis();
    }

    @Override
    @HideFromJS
    public DEFINITION register() {
        var definition = super.register();
        definition.setGenerator(generator);
        if (patterns.isEmpty()) {
            throw new IllegalStateException("Missing default structure pattern for " + name);
        }
        // the default structure first, then the others as they were added
        var defaultPattern = patterns.get(MultiblockControllerMachine.DEFAULT_STRUCTURE);
        if (defaultPattern != null) {
            definition.setPattern(MultiblockControllerMachine.DEFAULT_STRUCTURE,
                    GTMemoizer.memoize(() -> defaultPattern.apply(definition)));
        }
        for (Map.Entry<String, Function<MultiblockMachineDefinition, IBlockPattern>> entry : patterns.entrySet()) {
            if (entry.getKey().equals(MultiblockControllerMachine.DEFAULT_STRUCTURE)) continue;
            definition.setPattern(entry.getKey(), GTMemoizer.memoize(() -> entry.getValue().apply(definition)));
        }
        kinds.forEach(definition::setStructureKind);

        definition.setAllowFlip(allowFlip);
        if (!recoveryItems.isEmpty()) {
            definition.setRecoveryItems(
                    () -> recoveryItems.stream().map(Supplier::get).flatMap(Arrays::stream).toArray(ItemStack[]::new));
        }
        definition.setPartSorter(GTMemoizer.memoizeFunctionWeakIdent(partSorter));
        if (partAppearance == null) {
            partAppearance = (controller, part, side) -> definition.getAppearance().get();
        }
        definition.setPartAppearance(partAppearance);
        definition.setAdditionalDisplay(additionalDisplay);
        return value = definition;
    }
}
