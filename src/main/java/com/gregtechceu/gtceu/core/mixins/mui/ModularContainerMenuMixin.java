package com.gregtechceu.gtceu.core.mixins.mui;

import com.gregtechceu.gtceu.GTCEu;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import brachy.modularui.screen.ModularContainerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * A UI is built on both the server and the client, and the two have to come out the same. When they do not, for example
 * because a machine builds its UI from state the client has not received, the server sends the contents of more or
 * fewer slots than the client's menu has. ModularUI only logs that, and vanilla then reads past the end of the slot
 * list, which fails the packet and kicks the player with a network protocol error.
 * <p>
 * This closes the UI instead, with a message and a log entry that says what differed.
 */
@Mixin(value = ModularContainerMenu.class, remap = false)
public abstract class ModularContainerMenuMixin {

    @Inject(method = "initializeContents", at = @At("HEAD"), cancellable = true, require = 0)
    private void gtceu$closeOnMismatch(int stateId, List<ItemStack> items, ItemStack carried, CallbackInfo ci) {
        AbstractContainerMenu menu = (AbstractContainerMenu) (Object) this;
        if (menu.slots.size() == items.size()) return;

        GTCEu.LOGGER.error(
                "The UI was built with {} slots on this client, but the server's has {}. The client and the server built different UIs, which is usually state the client has not received yet (a synced field that was not marked dirty, or data only the server has). The UI was closed. Menu: {}",
                menu.slots.size(), items.size(), menu.getClass().getName());
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            minecraft.player.displayClientMessage(Component.translatable("gtceu.ui.mismatch"), false);
            minecraft.player.closeContainer();
        }
        ci.cancel();
    }
}
