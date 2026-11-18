package com.gregtechceu.gtceu.integration.jade.provider;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.capability.ILockableHatch;
import com.gregtechceu.gtceu.api.machine.MetaMachine;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;

import snownee.jade.api.BlockAccessor;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

/** Says that a bus or hatch is locked by its multiblock. */
public class LockableHatchProvider extends MachineInfoProvider<MetaMachine, CompoundTag> {

    public LockableHatchProvider() {
        super(GTCEu.id("lockable_hatch_provider"), MetaMachine.class);
    }

    @Override
    protected CompoundTag write(MetaMachine machine) {
        var tag = new CompoundTag();
        tag.putBoolean("locked", machine instanceof ILockableHatch hatch && hatch.isLocked());
        return tag;
    }

    @Override
    protected void addTooltip(CompoundTag data, ITooltip tooltip, Player player, BlockAccessor block,
                              BlockEntity blockEntity, IPluginConfig config) {
        if (data.getBoolean("locked")) {
            tooltip.add(Component.translatable("gtceu.jade.hatch_locked").withStyle(ChatFormatting.RED));
        }
    }
}
