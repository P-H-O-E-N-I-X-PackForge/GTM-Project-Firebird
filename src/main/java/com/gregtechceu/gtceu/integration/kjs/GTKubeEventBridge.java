package com.gregtechceu.gtceu.integration.kjs;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.events.MachineRecipeFinishedEvent;
import com.gregtechceu.gtceu.api.events.MachineRecipeStartedEvent;
import com.gregtechceu.gtceu.api.events.MachineRecipeStatusEvent;
import com.gregtechceu.gtceu.api.events.MultiblockFormEvent;
import com.gregtechceu.gtceu.api.events.MultiblockStructureEvent;
import com.gregtechceu.gtceu.integration.kjs.events.MachineRecipeEventJS;
import com.gregtechceu.gtceu.integration.kjs.events.MultiblockFormEventJS;
import com.gregtechceu.gtceu.integration.kjs.events.MultiblockStructureEventJS;

import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import dev.latvian.mods.kubejs.script.ScriptType;

/**
 * Passes GT's game bus events to KubeJS scripts as {@code GTCEuServerEvents}. Registered on the game bus only when
 * KubeJS is loaded. A script that throws is logged and does not take the machine's tick down with it.
 */
public final class GTKubeEventBridge {

    private GTKubeEventBridge() {}

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onFormStructure(MultiblockFormEvent event) {
        if (event.isCanceled()) return;
        try {
            if (GTCEuServerEvents.MULTIBLOCK_FORM.hasListeners()) {
                GTCEuServerEvents.MULTIBLOCK_FORM.post(ScriptType.SERVER, new MultiblockFormEventJS(event));
            }
        } catch (Throwable t) {
            GTCEu.LOGGER.error("A multiblockForm script failed", t);
        }
    }

    @SubscribeEvent
    public static void onStructure(MultiblockStructureEvent event) {
        try {
            var handler = event instanceof MultiblockStructureEvent.Formed ? GTCEuServerEvents.STRUCTURE_FORMED :
                    GTCEuServerEvents.STRUCTURE_INVALIDATED;
            if (handler.hasListeners()) handler.post(ScriptType.SERVER, new MultiblockStructureEventJS(event));
        } catch (Throwable t) {
            GTCEu.LOGGER.error("A structure script failed", t);
        }
    }

    @SubscribeEvent
    public static void onRecipeStarted(MachineRecipeStartedEvent event) {
        try {
            if (GTCEuServerEvents.RECIPE_STARTED.hasListeners()) {
                GTCEuServerEvents.RECIPE_STARTED.post(ScriptType.SERVER,
                        new MachineRecipeEventJS(event.getMachine(), event.getRecipe(), null, null));
            }
        } catch (Throwable t) {
            GTCEu.LOGGER.error("A recipeStarted script failed", t);
        }
    }

    @SubscribeEvent
    public static void onRecipeFinished(MachineRecipeFinishedEvent event) {
        try {
            if (GTCEuServerEvents.RECIPE_FINISHED.hasListeners()) {
                GTCEuServerEvents.RECIPE_FINISHED.post(ScriptType.SERVER,
                        new MachineRecipeEventJS(event.getMachine(), event.getRecipe(), null, null));
            }
        } catch (Throwable t) {
            GTCEu.LOGGER.error("A recipeFinished script failed", t);
        }
    }

    @SubscribeEvent
    public static void onRecipeStatus(MachineRecipeStatusEvent event) {
        try {
            if (GTCEuServerEvents.RECIPE_STATUS.hasListeners()) {
                GTCEuServerEvents.RECIPE_STATUS.post(ScriptType.SERVER, new MachineRecipeEventJS(event.getMachine(),
                        null, event.getOldStatus().name(), event.getNewStatus().name()));
            }
        } catch (Throwable t) {
            GTCEu.LOGGER.error("A recipeStatus script failed", t);
        }
    }
}
