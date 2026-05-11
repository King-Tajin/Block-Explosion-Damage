package com.king_tajin.block_explosion_damage;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.serialization.Codec;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleCategory;
import net.minecraft.world.level.gamerules.GameRuleType;
import net.minecraft.world.level.gamerules.GameRuleTypeVisitor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModGameRules {
    public static final DeferredRegister<GameRule<?>> GAME_RULES =
            DeferredRegister.create(BuiltInRegistries.GAME_RULE, "block_explosion_damage");

    public static final DeferredHolder<GameRule<?>, GameRule<Boolean>> RULE_BLOCK_DAMAGE_DECAY =
            GAME_RULES.register("tnt_block_damage_decay", () -> new GameRule<>(
                    GameRuleCategory.UPDATES,
                    GameRuleType.BOOL,
                    BoolArgumentType.bool(),
                    GameRuleTypeVisitor::visitBoolean,
                    Codec.BOOL,
                    value -> value ? 1 : 0,
                    true,
                    FeatureFlagSet.of()
            ));

    public static void register(IEventBus modEventBus) {
        GAME_RULES.register(modEventBus);
    }
}