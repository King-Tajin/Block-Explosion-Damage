package com.king_tajin.block_explosion_damage;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Explosion;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class ExplosionHandler {

    public static void handleExplosion(ServerLevel level, Explosion explosion, List<BlockPos> affectedBlocks) {
        double explosionX = explosion.center().x;
        double explosionY = explosion.center().y;
        double explosionZ = explosion.center().z;
        float radius = explosion.radius();

        int radiusInt = (int) Math.ceil(radius);
        BlockPos explosionPos = BlockPos.containing(explosionX, explosionY, explosionZ);

        Set<BlockPos> alreadyProcessed = new HashSet<>();
        Set<BlockPos> blocksToBreak = new HashSet<>();

        // First pass: process all blocks in explosion radius
        for (int x = -radiusInt; x <= radiusInt; x++) {
            for (int y = -radiusInt; y <= radiusInt; y++) {
                for (int z = -radiusInt; z <= radiusInt; z++) {
                    BlockPos checkPos = explosionPos.offset(x, y, z);
                    double distance = Math.sqrt(x * x + y * y + z * z);

                    if (distance <= radius) {
                        BlockState state = level.getBlockState(checkPos);

                        if (!state.isAir() && !state.is(Blocks.BEDROCK) && !state.is(Blocks.TNT)) {
                            int damageAmount = calculateDamage(distance, radius);

                            if (applyBlockDamage(level, checkPos, state, damageAmount)) {
                                blocksToBreak.add(checkPos);
                            }

                            alreadyProcessed.add(checkPos);
                        }
                    }
                }
            }
        }

        // Second pass: update the affected blocks list
        Iterator<BlockPos> iterator = affectedBlocks.iterator();

        while (iterator.hasNext()) {
            BlockPos pos = iterator.next();
            BlockState state = level.getBlockState(pos);

            if (state.isAir() || state.is(Blocks.BEDROCK)) {
                iterator.remove();
                continue;
            }

            if (state.is(Blocks.TNT)) {
                continue;
            }

            // If we didn't process this in the first pass, remove it
            if (!blocksToBreak.contains(pos)) {
                iterator.remove();
            }
        }

        // Add any blocks that should break but weren't in the original list
        for (BlockPos pos : blocksToBreak) {
            if (!affectedBlocks.contains(pos)) {
                affectedBlocks.add(pos);
            }
        }
    }

    private static int calculateDamage(double distance, float radius) {
        double normalizedDistance = distance / radius;
        double damageMultiplier = 3.0 - (2.0 * normalizedDistance);
        return Math.max(1, (int) Math.round(damageMultiplier));
    }

    private static boolean applyBlockDamage(ServerLevel level, BlockPos pos, BlockState state, int damageAmount) {
        int requiredHits = ModConfig.getHitsForBlock(state.getBlock());
        BlockDamageData damageData = BlockDamageManager.getDamageData(level, pos);
        int currentDamage = damageData.getDamage() + damageAmount;

        if (currentDamage >= requiredHits) {
            BlockDamageManager.removeDamage(level, pos);
            return true;
        } else {
            BlockDamageManager.setDamage(level, pos, currentDamage);
            showDamageEffects(level, pos, currentDamage, requiredHits);
            return false;
        }
    }

    private static void showDamageEffects(ServerLevel level, BlockPos pos, int damage, int maxDamage) {
        int damageStage = Math.min(9, (int) ((float) damage / maxDamage * 10));
        level.destroyBlockProgress(-1 - pos.hashCode(), pos, damageStage);
    }
}