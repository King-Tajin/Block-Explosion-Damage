package com.king_tajin.block_explosion_damage;

import net.minecraft.world.level.GameRules;

public class ModGameRules {

    public static GameRules.Key<GameRules.BooleanValue> RULE_BLOCK_DAMAGE_DECAY;

    public static void register() {
        RULE_BLOCK_DAMAGE_DECAY = GameRules.register(
                "tntBlockDamageDecay",
                GameRules.Category.UPDATES,
                GameRules.BooleanValue.create(true)
        );
    }
}