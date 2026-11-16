package com.gregtechceu.gtceu.api.events;

import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;

import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

import lombok.Getter;
import org.jetbrains.annotations.Nullable;

/**
 * Posted on the game event bus, on the server, before a multiblock controller forms one of its structures. Cancel it to
 * keep the structure from forming, for example while the multiblock is locked behind research. The controller checks
 * again later, so the event is posted again, and a reason is told to the nearest player each time.
 */
public class MultiblockFormEvent extends Event implements ICancellableEvent {

    @Getter
    private final MultiblockControllerMachine controller;
    /** The structure, {@code "main"} for the default one. */
    @Getter
    private final String structureName;
    @Getter
    private @Nullable Component reason;

    public MultiblockFormEvent(MultiblockControllerMachine controller, String structureName) {
        this.controller = controller;
        this.structureName = structureName;
    }

    /** Stops the structure forming, and tells the nearest player why. */
    public void deny(Component reason) {
        this.reason = reason;
        setCanceled(true);
    }
}
