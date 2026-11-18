package com.gregtechceu.gtceu.api.multiblock.error;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;

import brachy.modularui.api.drawable.Text;
import brachy.modularui.drawable.ItemDrawable;
import brachy.modularui.widgets.layout.Flow;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.Getter;

import java.util.List;

public class BlockMatchingError extends PatternError {

    // spotless:off
    public static final MapCodec<BlockMatchingError> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BlockPos.CODEC.fieldOf("pos").forGetter(PatternError::pos),
            BuiltInRegistries.BLOCK.byNameCodec().listOf().fieldOf("blocks").forGetter(BlockMatchingError::getBlocks))
    .apply(instance, BlockMatchingError::new));
    //spotless:on

    @Getter
    private final List<Block> blocks;

    public BlockMatchingError(BlockPos pos, List<Block> blocks) {
        super(pos);
        this.blocks = blocks;
    }

    @Override
    public PatternErrorUI getPatternErrorUIModifier() {
        return (parent) -> {
            parent.child(Text.lang("gtceu.multiblock.pattern.error.needs_one_of").color(0xFFE0E0E0).asWidget());
            // the blocks, as their pictures in rows, not each a line of its name with the place repeated under it
            int perRow = 8;
            for (int i = 0; i < blocks.size(); i += perRow) {
                Flow row = Flow.row().coverChildren().childPadding(2);
                for (int j = i; j < Math.min(blocks.size(), i + perRow); j++) {
                    Block block = blocks.get(j);
                    row.child(new ItemDrawable(block.asItem()).asIcon().size(16).asWidget().size(16)
                            .tooltip(r -> r.add(block.getName())));
                }
                parent.child(row);
            }
            parent.child(Text.of(Component.translatable("gtceu.multiblock.pattern.error.position", pos.getX(),
                    pos.getY(), pos.getZ())).color(0xFFB0B0B0).asWidget());
        };
    }

    @Override
    public PatternErrorType type() {
        return GTPatternErrors.BLOCK_MATCHING_ERROR.value();
    }
}
