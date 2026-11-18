package com.gregtechceu.gtceu.api.events;

import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;

import net.neoforged.bus.api.Event;

import lombok.Getter;

/**
 * Posted on the game event bus, on the server, when one of a multiblock controller's structures forms or is broken.
 * {@code "main"} is the default structure; the others are the ones the multiblock added with
 * {@code MultiblockMachineBuilder#pattern} and {@code #additionalPattern}.
 */
public abstract class MultiblockStructureEvent extends Event {

    @Getter
    private final MultiblockControllerMachine controller;
    @Getter
    private final String structureName;

    protected MultiblockStructureEvent(MultiblockControllerMachine controller, String structureName) {
        this.controller = controller;
        this.structureName = structureName;
    }

    /** The structure has formed. It was not formed before. */
    public static class Formed extends MultiblockStructureEvent {

        public Formed(MultiblockControllerMachine controller, String structureName) {
            super(controller, structureName);
        }
    }

    /** The structure was formed and is not any more. */
    public static class Invalidated extends MultiblockStructureEvent {

        public Invalidated(MultiblockControllerMachine controller, String structureName) {
            super(controller, structureName);
        }
    }
}
