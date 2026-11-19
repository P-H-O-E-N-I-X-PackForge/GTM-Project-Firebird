package com.gregtechceu.gtceu.api.events;

import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine;
import com.gregtechceu.gtceu.api.machine.trait.recipe.RecipeLogic;

import net.minecraftforge.eventbus.api.Event;

import lombok.Getter;

/**
 * Posted on the game event bus, on the server, when the status of a machine's recipe logic changes: from idle to
 * working, from working to waiting, and so on.
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
