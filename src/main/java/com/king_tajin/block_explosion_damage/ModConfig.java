package com.king_tajin.block_explosion_damage;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ModConfig {

    private static final double defaultHitsMultiplier = 6.0;
    private static int damageDecayTime = 2400;
    private static final Map<Block, Integer> customBlockHits = new HashMap<>();
    private static final Set<Block> protectiveBlocks = new HashSet<>();

    public static void init() {
        customBlockHits.put(Blocks.GLASS, 2);
        customBlockHits.put(Blocks.OBSIDIAN, 12);
        customBlockHits.put(Blocks.CRYING_OBSIDIAN, 12);

        protectiveBlocks.add(Blocks.BEDROCK);
        protectiveBlocks.add(Blocks.BARRIER);
        protectiveBlocks.add(Blocks.REINFORCED_DEEPSLATE);
    }

    public static int getHitsForBlock(Block block) {
        if (customBlockHits.containsKey(block)) {
            return customBlockHits.get(block);
        }

        float hardness = block.defaultDestroyTime();

        if (hardness < 0) {
            return 999;
        }

        return Math.max(1, (int) Math.ceil(hardness * defaultHitsMultiplier));
    }

    public static boolean isProtectiveBlock(Block block) {
        return protectiveBlocks.contains(block);
    }

    public static int getDamageDecayTime() {
        return damageDecayTime;
    }

    public static void setDamageDecayTime(int ticks) {
        damageDecayTime = Math.max(0, ticks);
    }

    public static void setCustomBlockHits(Block block, int hits) {
        if (hits > 0) {
            customBlockHits.put(block, hits);
        } else {
            customBlockHits.remove(block);
        }
    }
}