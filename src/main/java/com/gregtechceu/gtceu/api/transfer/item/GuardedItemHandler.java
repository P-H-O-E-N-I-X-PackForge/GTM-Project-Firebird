package com.gregtechceu.gtceu.api.transfer.item;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandlerModifiable;

import org.jetbrains.annotations.NotNull;

import java.util.function.BooleanSupplier;

/**
 * Lets nothing in or out of a handler while it is blocked, for the UI of a locked bus: the slots of the UI go through
 * this, and what the machine's recipes do goes to the handler itself.
 */
public class GuardedItemHandler implements IItemHandlerModifiable {

    private final IItemHandlerModifiable inner;
    private final BooleanSupplier blocked;

    public GuardedItemHandler(IItemHandlerModifiable inner, BooleanSupplier blocked) {
        this.inner = inner;
        this.blocked = blocked;
    }

    @Override
    public void setStackInSlot(int slot, @NotNull ItemStack stack) {
        if (!blocked.getAsBoolean()) inner.setStackInSlot(slot, stack);
    }

    @Override
    public int getSlots() {
        return inner.getSlots();
    }

    @Override
    public @NotNull ItemStack getStackInSlot(int slot) {
        return inner.getStackInSlot(slot);
    }

    @Override
    public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
        return blocked.getAsBoolean() ? stack : inner.insertItem(slot, stack, simulate);
    }

    @Override
    public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
        return blocked.getAsBoolean() ? ItemStack.EMPTY : inner.extractItem(slot, amount, simulate);
    }

    @Override
    public int getSlotLimit(int slot) {
        return inner.getSlotLimit(slot);
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack stack) {
        return !blocked.getAsBoolean() && inner.isItemValid(slot, stack);
    }
}
