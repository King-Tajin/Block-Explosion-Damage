package com.king_tajin.block_explosion_damage;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult;

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
        Vec3 explosionCenter = new Vec3(explosionX, explosionY, explosionZ);

        Set<BlockPos> alreadyProcessed = new HashSet<>();
        Set<BlockPos> blocksToBreak = new HashSet<>();

        for (int x = -radiusInt; x <= radiusInt; x++) {
            for (int y = -radiusInt; y <= radiusInt; y++) {
                for (int z = -radiusInt; z <= radiusInt; z++) {
                    BlockPos checkPos = explosionPos.offset(x, y, z);
                    double distance = Math.sqrt(x * x + y * y + z * z);

                    if (distance <= radius) {
                        BlockState state = level.getBlockState(checkPos);

                        if (!state.isAir() && !state.is(Blocks.BEDROCK) && !state.is(Blocks.TNT)) {

                            // Check if bedrock is blocking the explosion
                            if (isBlockedByBedrock(level, explosionCenter, checkPos)) {
                                continue;
                            }

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

            if (!blocksToBreak.contains(pos)) {
                iterator.remove();
            }
        }

        for (BlockPos pos : blocksToBreak) {
            if (!affectedBlocks.contains(pos)) {
                affectedBlocks.add(pos);
            }
        }
    }

    private static boolean isBlockedByBedrock(ServerLevel level, Vec3 explosionCenter, BlockPos targetPos) {
        Vec3 targetCenter = Vec3.atCenterOf(targetPos);
        Vec3 direction = targetCenter.subtract(explosionCenter).normalize();
        double distance = explosionCenter.distanceTo(targetCenter);

        // Step through the ray in small increments
        double step = 0.5;
        for (double d = 0; d < distance; d += step) {
            Vec3 checkPoint = explosionCenter.add(direction.scale(d));
            BlockPos checkPos = BlockPos.containing(checkPoint);

            // Don't check the target position itself
            if (checkPos.equals(targetPos)) {
                continue;
            }

            BlockState state = level.getBlockState(checkPos);
            if (state.is(Blocks.BEDROCK)) {
                return true;
            }
        }

        return false;
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