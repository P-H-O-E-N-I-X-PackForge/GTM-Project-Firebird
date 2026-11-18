package com.gregtechceu.gtceu.api.events;

import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine;
import com.gregtechceu.gtceu.api.machine.trait.recipe.RecipeLogic;

import net.neoforged.bus.api.Event;

import lombok.Getter;

/**
 * Posted on the game event bus, on the server, when a machine's recipe logic changes status: from idle to working,
 * working to waiting because something is missing, to suspended, and so on. The machine already has the new status
 * when this is posted.
 */
public class MachineRecipeStatusEvent extends Event {

    @Getter
    private final IRecipeLogicMachine machine;
    @Getter
    private final RecipeLogic.Status oldStatus;
    @Getter
    private final RecipeLogic.Status newStatus;

    public MachineRecipeStatusEvent(IRecipeLogicMachine machine, RecipeLogic.Status oldStatus,
                                    RecipeLogic.Status newStatus) {
        this.machine = machine;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
    }
}
