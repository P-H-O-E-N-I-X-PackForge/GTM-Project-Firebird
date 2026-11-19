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

import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;

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

    // NeoForge's bus refuses a listener for an abstract event, so each of the two events has its own
    @SubscribeEvent
    public static void onStructureFormed(MultiblockStructureEvent.Formed event) {
        postStructure(GTCEuServerEvents.STRUCTURE_FORMED, event);
    }

    @SubscribeEvent
    public static void onStructureInvalidated(MultiblockStructureEvent.Invalidated event) {
        postStructure(GTCEuServerEvents.STRUCTURE_INVALIDATED, event);
    }

    private static void postStructure(dev.latvian.mods.kubejs.event.EventHandler handler,
                                      MultiblockStructureEvent event) {
        try {
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
