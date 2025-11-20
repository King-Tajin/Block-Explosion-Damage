package com.king_tajin.block_explosion_damage;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.HashMap;
import java.util.Map;

public class ModConfig {

    private static int defaultHits = 6;
    private static int damageDecayTime = 6000;
    private static final Map<Block, Integer> customBlockHits = new HashMap<>();

    public static void init() {
        customBlockHits.put(Blocks.DIRT, 3);
        customBlockHits.put(Blocks.GRASS_BLOCK, 3);
        customBlockHits.put(Blocks.SAND, 3);
        customBlockHits.put(Blocks.GRAVEL, 3);
        customBlockHits.put(Blocks.GLASS, 1);

        customBlockHits.put(Blocks.STONE, 3);
        customBlockHits.put(Blocks.COBBLESTONE, 3);
        customBlockHits.put(Blocks.OAK_PLANKS, 3);
        customBlockHits.put(Blocks.SPRUCE_PLANKS, 3);
        customBlockHits.put(Blocks.BIRCH_PLANKS, 3);
        customBlockHits.put(Blocks.JUNGLE_PLANKS, 3);
        customBlockHits.put(Blocks.ACACIA_PLANKS, 3);
        customBlockHits.put(Blocks.DARK_OAK_PLANKS, 3);

        customBlockHits.put(Blocks.IRON_BLOCK, 5);
        customBlockHits.put(Blocks.GOLD_BLOCK, 4);
        customBlockHits.put(Blocks.DIAMOND_BLOCK, 6);
        customBlockHits.put(Blocks.EMERALD_BLOCK, 6);
        customBlockHits.put(Blocks.NETHERITE_BLOCK, 8);

        customBlockHits.put(Blocks.COAL_ORE, 3);
        customBlockHits.put(Blocks.IRON_ORE, 4);
        customBlockHits.put(Blocks.GOLD_ORE, 4);
        customBlockHits.put(Blocks.DIAMOND_ORE, 5);
        customBlockHits.put(Blocks.EMERALD_ORE, 5);
        customBlockHits.put(Blocks.DEEPSLATE_COAL_ORE, 4);
        customBlockHits.put(Blocks.DEEPSLATE_IRON_ORE, 5);
        customBlockHits.put(Blocks.DEEPSLATE_GOLD_ORE, 5);
        customBlockHits.put(Blocks.DEEPSLATE_DIAMOND_ORE, 6);

        customBlockHits.put(Blocks.OBSIDIAN, 10);
        customBlockHits.put(Blocks.CRYING_OBSIDIAN, 10);

        customBlockHits.put(Blocks.END_STONE, 4);
        customBlockHits.put(Blocks.PURPUR_BLOCK, 4);

        customBlockHits.put(Blocks.NETHERRACK, 2);
        customBlockHits.put(Blocks.NETHER_BRICKS, 4);
        customBlockHits.put(Blocks.BLACKSTONE, 4);

        customBlockHits.put(Blocks.DEEPSLATE, 4);
        customBlockHits.put(Blocks.COBBLED_DEEPSLATE, 4);
    }

    public static int getHitsForBlock(Block block) {
        return customBlockHits.getOrDefault(block, defaultHits);
    }

    public static int getDefaultHits() {
        return defaultHits;
    }

    public static void setDefaultHits(int hits) {
        defaultHits = Math.max(1, hits);
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