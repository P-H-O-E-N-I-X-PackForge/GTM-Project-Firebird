package com.gregtechceu.gtceu.client.mui.schema;

import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.part.MultiblockPartMachine;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine;
import com.gregtechceu.gtceu.api.machine.property.GTMachineModelProperties;
import com.gregtechceu.gtceu.api.machine.trait.recipe.RecipeLogic;
import com.gregtechceu.gtceu.api.block.property.GTBlockStateProperties;
import com.gregtechceu.gtceu.api.mui.PreviewMode;
import com.gregtechceu.gtceu.api.multiblock.pattern.PatternState;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import brachy.modularui.drawable.schema.ISchema;
import brachy.modularui.drawable.schema.SchemaLevel;
import brachy.modularui.utils.BlockPosUtil;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.longs.Long2ReferenceMap;
import it.unimi.dsi.fastutil.longs.Long2ReferenceOpenHashMap;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.*;

import static com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine.DEFAULT_STRUCTURE;

public class MutableSchema implements ISchema {

    @Getter
    protected final Level level = new SchemaLevel();
    @Getter
    protected @NotNull BlockPos origin = BlockPos.ZERO;
    @Getter
    protected @NotNull Vector3f center = new Vector3f();
    @Getter
    private BlockPos controllerPos = BlockPos.ZERO;
    @Getter
    private Pair<BlockPos, BlockPos> bounds;

    @Getter
    protected final Long2ReferenceMap<BlockState> blocks = new Long2ReferenceOpenHashMap<>();

    /** The structure of the multiblock this holds, which is the one formed in it. */
    @Setter
    private String structureName = DEFAULT_STRUCTURE;

    /** Whether the controller is formed when the blocks are set, and whether it is shown working. */
    @Setter
    private PreviewMode previewMode = PreviewMode.FORMED;

    public MutableSchema() {}

    public MutableSchema(Long2ReferenceMap<BlockState> blocks) {
        this();
        setBlocks(blocks);
    }

    public MutableSchema setBlocks(Long2ReferenceMap<BlockState> blocks) {
        // clear world
        for (Map.Entry<BlockPos, BlockState> entry : this) {
            getLevel().removeBlock(entry.getKey(), false);
        }
        this.blocks.clear();

        BlockPos.MutableBlockPos min = BlockPosUtil.MAX.mutable();
        BlockPos.MutableBlockPos max = BlockPosUtil.MIN.mutable();
        MultiblockControllerMachine controller = null;
        List<MultiblockPartMachine> parts = new ArrayList<>();

        for (long packed : blocks.keySet()) {
            BlockState block = blocks.get(packed);
            if (block.isAir()) continue;
            BlockPos pos = BlockPos.of(packed);

            // BE creation is already handled through here
            updateBlockState(pos, block);
            BlockPosUtil.setMin(min, pos);
            BlockPosUtil.setMax(max, pos);

            BlockEntity blockEntity = getLevel().getBlockEntity(pos);
            if (blockEntity instanceof MultiblockControllerMachine mcm && controller == null) {
                controller = mcm;
                controllerPos = pos;
            } else if (blockEntity instanceof MultiblockPartMachine part) {
                parts.add(part);
            }
        }

        if (controller != null && previewMode != PreviewMode.UNFORMED) {
            PatternState patternState = controller.getPatternState(structureName);
            controller.checkStructurePattern(structureName);
            if (patternState.getState().isValid()) {
                controller.formStructure(structureName);
                if (previewMode == PreviewMode.ACTIVE) {
                    showWorking(controller);
                    for (var part : parts) showWorking(part);
                    // what a running recipe does: the logic is working, which the renders of the machine read, and
                    // the machine turns its active blocks on and its parts to the working look
                    controller.getTraitOptional(RecipeLogic.class).ifPresent(RecipeLogic::showWorkingForPreview);
                    if (controller instanceof IRecipeLogicMachine machine) {
                        machine.recipeLogicStatusChanged(RecipeLogic.Status.IDLE, RecipeLogic.Status.WORKING);
                    }
                    // the view draws the blocks of this map, not those of the level, which the machine just changed
                    // (this.blocks: the parameter of this method has the same name, and is not what is drawn)
                    for (long packed : this.blocks.keySet().toLongArray()) {
                        var current = getLevel().getBlockState(BlockPos.of(packed));
                        if (current != this.blocks.get(packed) &&
                                current.hasProperty(GTBlockStateProperties.ACTIVE)) {
                            this.blocks.put(packed, current);
                        }
                    }
                }
            }
        }

        this.origin = min.immutable();
        this.center = BlockPosUtil.getCenterF(min, max);
        this.bounds = Pair.of(min, max);
        return this;
    }

    /**
     * Has the machine look as it does while a recipe runs. The recipe logic does not run here, so only how it looks.
     */
    private static void showWorking(MetaMachine machine) {
        var state = machine.getRenderState();
        var property = GTMachineModelProperties.RECIPE_LOGIC_STATUS;
        if (state.hasProperty(property)) {
            machine.setRenderState(state.setValue(property, RecipeLogic.Status.WORKING));
        }
    }

    public MutableSchema updateBlockState(BlockPos pos, BlockState state) {
        this.blocks.put(pos.asLong(), state);
        getLevel().setBlockAndUpdate(pos, state);
        return this;
    }

    @Override
    public Vector3fc getFocus() {
        return center;
    }

    @Override
    public @NotNull Iterator<Map.Entry<BlockPos, BlockState>> iterator() {
        return blocks.long2ReferenceEntrySet().stream()
                .map(e -> Map.entry(BlockPos.of(e.getLongKey()), e.getValue()))
                .iterator();
    }
}
