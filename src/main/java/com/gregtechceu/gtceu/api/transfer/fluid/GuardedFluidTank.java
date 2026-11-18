package com.gregtechceu.gtceu.api.transfer.fluid;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.IFluidTank;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import org.jetbrains.annotations.NotNull;

import java.util.function.BooleanSupplier;

/**
 * Lets nothing in or out of a tank while it is blocked, for the UI of a locked hatch: the slots of the UI go through
 * this, and what the machine's recipes do goes to the tank itself.
 */
public class GuardedFluidTank implements IFluidTank {

    private final IFluidTank inner;
    private final BooleanSupplier blocked;

    public GuardedFluidTank(IFluidTank inner, BooleanSupplier blocked) {
        this.inner = inner;
        this.blocked = blocked;
    }

    @Override
    public @NotNull FluidStack getFluid() {
        return inner.getFluid();
    }

    @Override
    public int getFluidAmount() {
        return inner.getFluidAmount();
    }

    @Override
    public int getCapacity() {
        return inner.getCapacity();
    }

    @Override
    public boolean isFluidValid(@NotNull FluidStack stack) {
        return !blocked.getAsBoolean() && inner.isFluidValid(stack);
    }

    @Override
    public int fill(@NotNull FluidStack resource, IFluidHandler.@NotNull FluidAction action) {
        return blocked.getAsBoolean() ? 0 : inner.fill(resource, action);
    }

    @Override
    public @NotNull FluidStack drain(int maxDrain, IFluidHandler.@NotNull FluidAction action) {
        return blocked.getAsBoolean() ? FluidStack.EMPTY : inner.drain(maxDrain, action);
    }

    @Override
    public @NotNull FluidStack drain(@NotNull FluidStack resource, IFluidHandler.@NotNull FluidAction action) {
        return blocked.getAsBoolean() ? FluidStack.EMPTY : inner.drain(resource, action);
    }
}
