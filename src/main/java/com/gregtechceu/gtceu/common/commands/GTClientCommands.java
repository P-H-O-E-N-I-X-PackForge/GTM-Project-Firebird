package com.gregtechceu.gtceu.common.commands;

import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;

import com.mojang.brigadier.CommandDispatcher;

public class GTClientCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext) {
        // the client half of /gtceu debug machine, as /gtceu debug client_machine: what this side has for the machine
        // being looked at
        dispatcher.register(net.minecraft.commands.Commands.literal("gtceu")
                .then(net.minecraft.commands.Commands.literal("debug")
                        .then(net.minecraft.commands.Commands.literal("client_machine")
                                .executes(ctx -> {
                                    var minecraft = net.minecraft.client.Minecraft.getInstance();
                                    if (!(minecraft.hitResult instanceof net.minecraft.world.phys.BlockHitResult block) ||
                                            minecraft.level == null ||
                                            !(com.gregtechceu.gtceu.api.machine.MetaMachine.getMachine(minecraft.level,
                                                    block.getBlockPos()) instanceof com.gregtechceu.gtceu.api.machine.MetaMachine machine)) {
                                        ctx.getSource().sendFailure(net.minecraft.network.chat.Component
                                                .literal("Look at a machine."));
                                        return 0;
                                    }
                                    MachineDebugReport.of(machine, true)
                                            .forEach(line -> ctx.getSource().sendSuccess(() -> line, false));
                                    return 1;
                                }))));
    }
}
