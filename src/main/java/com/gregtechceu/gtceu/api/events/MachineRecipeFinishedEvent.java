package com.gregtechceu.gtceu.api.events;

import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;

import net.neoforged.bus.api.Event;

import lombok.Getter;
import org.jetbrains.annotations.Nullable;

/**
 * Posted on the game event bus, on the server, after a machine finishes a recipe cycle. For multiblocks it comes after
 * the parts have been told.
 */
public class MachineRecipeFinishedEvent extends Event {

    @Getter
    private final IRecipeLogicMachine machine;
    /** The recipe that was run, or null when the machine does not know it any more. */
    @Getter
    private final @Nullable GTRecipe recipe;

    public MachineRecipeFinishedEvent(IRecipeLogicMachine machine, @Nullable GTRecipe recipe) {
        this.machine = machine;
        this.recipe = recipe;
    }
}
