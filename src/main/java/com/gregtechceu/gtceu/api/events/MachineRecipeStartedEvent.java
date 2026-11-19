package com.gregtechceu.gtceu.api.events;

import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;

import net.minecraftforge.eventbus.api.Event;

import lombok.Getter;

/**
 * Posted on the game event bus, on the server, when a machine starts a recipe: its inputs have been taken and it is
 * working. It is not posted for a recipe that was turned down or could not start.
 */
public class MachineRecipeStartedEvent extends Event {

    @Getter
    private final IRecipeLogicMachine machine;
    /** The recipe that is running, after the machine's modifiers and the rolls for chanced inputs. */
    @Getter
    private final GTRecipe recipe;

    public MachineRecipeStartedEvent(IRecipeLogicMachine machine, GTRecipe recipe) {
        this.machine = machine;
        this.recipe = recipe;
    }
}
