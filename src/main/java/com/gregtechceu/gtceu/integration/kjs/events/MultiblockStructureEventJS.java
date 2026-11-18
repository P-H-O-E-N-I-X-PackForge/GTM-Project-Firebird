package com.gregtechceu.gtceu.integration.kjs.events;

import com.gregtechceu.gtceu.api.events.MultiblockStructureEvent;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;

import dev.latvian.mods.kubejs.event.KubeEvent;

/** Invoked, on the server, when one of a multiblock controller's structures forms or is broken. */
public class MultiblockStructureEventJS implements KubeEvent {

    private final MultiblockStructureEvent event;

    public MultiblockStructureEventJS(MultiblockStructureEvent event) {
        this.event = event;
    }

    public MultiblockControllerMachine getController() {
        return event.getController();
    }

    /** The structure; {@code "main"} is the default one. */
    public String getStructureName() {
        return event.getStructureName();
    }
}
