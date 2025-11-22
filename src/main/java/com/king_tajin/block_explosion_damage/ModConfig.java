package com.king_tajin.block_explosion_damage;

import net.minecraft.world.level.block.Block;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ModConfig {

    private static double defaultHitsMultiplier = 6.0;
    private static int damageDecayTime = 2400;
    private static final Map<Block, Integer> customBlockHits = new HashMap<>();
    private static final Set<Block> protectiveBlocks = new HashSet<>();

    public static void init() {
        ConfigFileHandler.ConfigData config = ConfigFileHandler.loadConfig();

        defaultHitsMultiplier = config.defaultHitsMultiplier;
        damageDecayTime = config.damageDecayTime;

        customBlockHits.clear();
        for (Map.Entry<String, Integer> entry : config.customBlockHits.entrySet()) {
            Block block = ConfigFileHandler.getBlockFromString(entry.getKey());
            if (block != null) {
                customBlockHits.put(block, entry.getValue());
            }
        }

        protectiveBlocks.clear();
        for (String blockId : config.protectiveBlocks) {
            Block block = ConfigFileHandler.getBlockFromString(blockId);
            if (block != null) {
                protectiveBlocks.add(block);
            }
        }
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

    public static double getDefaultHitsMultiplier() {
        return defaultHitsMultiplier;
    }

    public static int getDamageDecayTime() {
        return damageDecayTime;
    }
}