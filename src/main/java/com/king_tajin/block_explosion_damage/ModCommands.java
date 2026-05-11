package com.king_tajin.block_explosion_damage;

import com.king_tajin.block_explosion_damage.config.ModConfig;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

public class ModCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("block_explosion_damage")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.literal("reload")
                                .executes(ModCommands::reloadConfig)
                        )
                        .then(Commands.literal("cleardamage")
                                .executes(ModCommands::clearAllDamage)
                        )
        );
    }

    private static int reloadConfig(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        try {
            ModConfig.init();
            source.sendSuccess(
                    () -> Component.literal("§aBlock Explosion Damage config reloaded successfully!"),
                    true
            );
            return 1;
        } catch (Exception e) {
            source.sendFailure(
                    Component.literal("§cFailed to reload config: " + e.getMessage())
            );
            return 0;
        }
    }

    private static int clearAllDamage(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        int clearedCount = 0;

        for (ServerLevel level : source.getServer().getAllLevels()) {
            clearedCount += BlockDamageManager.clearAllDamage(level);
        }

        final int total = clearedCount;
        source.sendSuccess(
                () -> Component.literal("Cleared damage from " + total + " blocks"),
                true
        );

        return total;
    }
}