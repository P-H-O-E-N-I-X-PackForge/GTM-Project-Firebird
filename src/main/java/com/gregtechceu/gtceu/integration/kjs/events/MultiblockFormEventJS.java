package com.gregtechceu.gtceu.integration.kjs.events;

import com.gregtechceu.gtceu.api.events.MultiblockFormEvent;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;

import net.minecraft.network.chat.Component;

import dev.latvian.mods.kubejs.event.KubeEvent;

/**
 * Invoked, on the server, before a multiblock controller forms one of its structures. Call {@code deny} to stop it.
 * 
 * <pre>{@code
 * GTCEuServerEvents.multiblockForm(event => {
 *     if (event.controller.definition.id == 'gtceu:electric_blast_furnace') {
 *         event.deny('The furnace is not unlocked yet')
 *     }
 * })
 * }</pre>
 */
public class MultiblockFormEventJS implements KubeEvent {

    private final MultiblockFormEvent event;

    public MultiblockFormEventJS(MultiblockFormEvent event) {
        this.event = event;
    }

    public MultiblockControllerMachine getController() {
        return event.getController();
    }

    /** The structure that is about to form; {@code "main"} is the default one. */
    public String getStructureName() {
        return event.getStructureName();
    }

    /** Stops the structure forming and tells the nearest player why. */
    public void deny(Component reason) {
        event.deny(reason);
    }

    /** Stops the structure forming, without a message. */
    public void deny() {
        event.setCanceled(true);
    }
}
