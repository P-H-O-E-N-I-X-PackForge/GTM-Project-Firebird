package com.gregtechceu.gtceu.common.mui;

import net.minecraft.network.FriendlyByteBuf;

import brachy.modularui.value.sync.PanelSyncManager;
import io.netty.buffer.Unpooled;

import java.util.function.Consumer;

/**
 * ModularUI's {@code callSyncedAction} takes a finished buffer rather than a writer, so this fills one first. Client
 * side only, since the buffer is built with the client level's registries.
 */
public final class GTMuiSync {

    private GTMuiSync() {}

    public static void call(PanelSyncManager syncManager, String name, Consumer<FriendlyByteBuf> writer) {
        var buf = new FriendlyByteBuf(Unpooled.buffer());
        writer.accept(buf);
        syncManager.callSyncedAction(name, buf);
    }
}
