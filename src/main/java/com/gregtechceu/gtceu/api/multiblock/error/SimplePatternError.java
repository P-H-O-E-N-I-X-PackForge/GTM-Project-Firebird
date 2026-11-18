package com.gregtechceu.gtceu.api.multiblock.error;

import com.gregtechceu.gtceu.api.multiblock.util.BlockInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import brachy.modularui.api.drawable.Text;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.Getter;

import java.util.List;

public class SimplePatternError extends PatternError {

    // spotless:off
    public static final MapCodec<SimplePatternError> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BlockPos.CODEC.fieldOf("pos").forGetter(PatternError::pos),
            BlockInfo.CODEC.listOf().listOf().fieldOf("candidates").forGetter(SimplePatternError::getCandidates))
    .apply(instance, SimplePatternError::new));
    //spotless:on

    @Getter
    private final List<List<BlockInfo>> candidates;

    public SimplePatternError(BlockPos pos, List<List<BlockInfo>> candidates) {
        super(pos);
        this.candidates = candidates;
    }

    @Override
    public PatternErrorUI getPatternErrorUIModifier() {
        return (parent) -> {
            parent.child(Text.lang("gtceu.multiblock.pattern.error.0").color(0xFFE0E0E0).asWidget());
            parent.child(Text.of(Component.translatable("gtceu.multiblock.pattern.error.position", pos.getX(),
                    pos.getY(), pos.getZ())).color(0xFFB0B0B0).asWidget());
            // each block that fits there, with its picture
            for (List<BlockInfo> candidate : candidates) {
                if (candidate.isEmpty()) continue;
                var stack = candidate.get(0).getItemStackForm();
                parent.child(brachy.modularui.widgets.layout.Flow.row()
                        .widthRel(1f)
                        .coverChildrenHeight()
                        .childPadding(4)
                        .crossAxisAlignment(brachy.modularui.utils.Alignment.CrossAxis.CENTER)
                        .child(new brachy.modularui.drawable.ItemDrawable(stack).asIcon().size(16).asWidget()
                                .size(16))
                        .child(Text.of(stack.getHoverName()).color(0xFFFFFFFF).asWidget()));
            }
        };
    }

    @Override
    public PatternErrorType type() {
        return GTPatternErrors.SIMPLE_PATTERN_ERROR.value();
    }
}
