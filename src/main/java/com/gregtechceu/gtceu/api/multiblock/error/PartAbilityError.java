package com.gregtechceu.gtceu.api.multiblock.error;

import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;

import brachy.modularui.api.drawable.Text;
import brachy.modularui.drawable.ItemDrawable;
import brachy.modularui.utils.Alignment;
import brachy.modularui.widgets.layout.Flow;
import brachy.modularui.widgets.menu.ContextMenuButton;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.Getter;

import java.util.Collection;

public class PartAbilityError extends PatternError {

    // spotless:off
    public static final MapCodec<PartAbilityError> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BlockPos.CODEC.fieldOf("pos").forGetter(PatternError::pos),
            Codec.STRING.fieldOf("name").forGetter(PartAbilityError::getPartAbilityName))
    .apply(instance, PartAbilityError::new));
    //spotless:on

    @Getter
    private final String partAbilityName;

    public PartAbilityError(BlockPos pos, PartAbility partAbility) {
        this(pos, partAbility.getName());
    }

    private PartAbilityError(BlockPos pos, String partAbilityName) {
        super(pos);
        this.partAbilityName = partAbilityName;
    }

    @Override
    public PatternErrorUI getPatternErrorUIModifier() {
        return (parent) -> {
            Collection<Block> blocks = PartAbility.VALUES.get(partAbilityName).getAllBlocks();
            Flow row = Flow.row()
                    .widthRel(1f)
                    .coverChildrenHeight()
                    .childPadding(4)
                    .crossAxisAlignment(Alignment.CrossAxis.CENTER);
            // the first block that does, to show what to look for
            Block first = blocks.stream().findFirst().orElse(null);
            if (first != null) {
                row.child(new ItemDrawable(first.asItem()).asIcon().size(16).asWidget().size(16)
                        .tooltip(r -> r.add(first.asItem().getDescription())));
            }
            row.child(Flow.col()
                    .expanded()
                    .coverChildrenHeight()
                    .child(Text.lang("gtceu.multiblock.pattern.error.missing").color(0xFFE0E0E0).asWidget())
                    .child(Text.dynamic(() -> displayName(partAbilityName)).asWidget().color(0xFFFFFFFF)));
            if (blocks.size() > 1) {
                row.child(new ContextMenuButton<>(partAbilityName)
                        .size(16)
                        .overlay(Text.str("...").asIcon())
                        .tooltip(r -> r.addLine(Component.translatable("gtceu.multiblock.pattern.error.options")))
                        .requiresClick()
                        .openRightDown()
                        .menu(PatternErrorMenus.blockGrid(blocks.stream().map(Block::asItem).toList())));
            }
            parent.child(row);
        };
    }

    /** The name of an ability for the player: its translation if it has one, or its key made readable. */
    public static Component displayName(String abilityName) {
        String key = "gtceu.part_ability." + abilityName;
        if (net.minecraft.client.resources.language.I18n.exists(key)) return Component.translatable(key);
        StringBuilder readable = new StringBuilder();
        for (String word : abilityName.split("_")) {
            if (word.isEmpty()) continue;
            if (!readable.isEmpty()) readable.append(' ');
            readable.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return Component.literal(readable.toString());
    }

    @Override
    public PatternErrorType type() {
        return GTPatternErrors.PART_ABILITY_ERROR.value();
    }
}
