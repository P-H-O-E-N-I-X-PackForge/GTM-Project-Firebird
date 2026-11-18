package com.gregtechceu.gtceu.integration.kjs.events;

import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;

import dev.latvian.mods.kubejs.event.KubeEvent;
import org.jetbrains.annotations.Nullable;

/**
 * Invoked, on the server, when a machine starts or finishes a recipe, or changes status. {@code recipe} is null when
 * the
 * machine no longer knows it; {@code oldStatus} and {@code newStatus} are only set for status changes.
 */
public class MachineRecipeEventJS implements KubeEvent {

    private final IRecipeLogicMachine machine;
    private final @Nullable GTRecipe recipe;
    private final @Nullable String oldStatus;
    private final @Nullable String newStatus;

    public MachineRecipeEventJS(IRecipeLogicMachine machine, @Nullable GTRecipe recipe, @Nullable String oldStatus,
                                @Nullable String newStatus) {
        this.machine = machine;
        this.recipe = recipe;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
    }

    public IRecipeLogicMachine getMachine() {
        return machine;
    }

    public @Nullable GTRecipe getRecipe() {
        return recipe;
    }

    /** One of IDLE, WORKING, WAITING and SUSPEND. */
    public @Nullable String getOldStatus() {
        return oldStatus;
    }

    public @Nullable String getNewStatus() {
        return newStatus;
    }
}
