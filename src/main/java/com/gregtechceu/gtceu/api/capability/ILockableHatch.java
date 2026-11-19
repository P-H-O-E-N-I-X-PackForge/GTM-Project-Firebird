package com.gregtechceu.gtceu.api.capability;

/**
 * A bus or hatch that a multiblock can lock. While it is locked, nothing is put into or taken out of it by hand in its
 * UI, by auto IO or by anything next to it. What the multiblock's recipes do with it is not affected. It is for the
 * multiblocks that use locking as a mechanic.
 */
public interface ILockableHatch {

    boolean isLocked();

    void setLocked(boolean locked);
}
