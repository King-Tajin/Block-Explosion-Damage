package com.king_tajin.block_explosion_damage;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

public class ClearDamageCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("cleardamage")
                        .requires(source -> source.hasPermission(2))
                        .executes(ClearDamageCommand::clearAllDamage)
        );
    }

    private static int clearAllDamage(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();

        int clearedCount = BlockDamageManager.clearAllDamage(level);

        source.sendSuccess(
                () -> Component.literal("Cleared damage from " + clearedCount + " blocks"),
                true
        );

        return clearedCount;
    }
}