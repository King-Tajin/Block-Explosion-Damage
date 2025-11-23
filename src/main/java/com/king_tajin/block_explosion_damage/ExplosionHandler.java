package com.king_tajin.block_explosion_damage;

import com.king_tajin.block_explosion_damage.config.ModConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ExplosionHandler {

    public static void handleExplosion(ServerLevel level, Explosion explosion, List<BlockPos> affectedBlocks) {
        Vec3 explosionCenter = explosion.center();
        float radius = explosion.radius();

        BlockPos explosionPos = BlockPos.containing(explosionCenter.x, explosionCenter.y, explosionCenter.z);
        if (!level.getFluidState(explosionPos).isEmpty()) {
            affectedBlocks.clear();
            return;
        }

        Set<BlockPos> blocksToBreak = processExplosionRadius(level, explosionCenter, radius);
        updateAffectedBlocksList(affectedBlocks, blocksToBreak);
    }

    private static Set<BlockPos> processExplosionRadius(ServerLevel level, Vec3 explosionCenter, float radius) {
        Set<BlockPos> blocksToBreak = new HashSet<>();
        int radiusInt = (int) Math.ceil(radius);
        BlockPos explosionPos = BlockPos.containing(explosionCenter.x, explosionCenter.y, explosionCenter.z);

        for (int x = -radiusInt; x <= radiusInt; x++) {
            for (int y = -radiusInt; y <= radiusInt; y++) {
                for (int z = -radiusInt; z <= radiusInt; z++) {
                    BlockPos checkPos = explosionPos.offset(x, y, z);
                    double distance = Math.sqrt(x * x + y * y + z * z);

                    if (distance > radius) {
                        continue;
                    }

                    BlockState state = level.getBlockState(checkPos);

                    if (state.is(Blocks.TNT)) {
                        blocksToBreak.add(checkPos);
                        continue;
                    }

                    if (shouldProcessBlock(level, explosionCenter, checkPos, state)) {
                        int damageAmount = calculateDamageAmount(distance, radius);

                        if (applyBlockDamage(level, checkPos, damageAmount)) {
                            blocksToBreak.add(checkPos);
                        }
                    }
                }
            }
        }

        return blocksToBreak;
    }

    private static boolean shouldProcessBlock(ServerLevel level, Vec3 explosionCenter, BlockPos pos, BlockState state) {
        if (state.isAir()) {
            return false;
        }

        if (ModConfig.isProtectiveBlock(state.getBlock())) {
            return false;
        }

        return !isBlockedByProtectiveBlock(level, explosionCenter, pos);
    }

    private static boolean isBlockedByProtectiveBlock(ServerLevel level, Vec3 explosionCenter, BlockPos targetPos) {
        Vec3 targetCenter = Vec3.atCenterOf(targetPos);
        Vec3 direction = targetCenter.subtract(explosionCenter).normalize();
        double distance = explosionCenter.distanceTo(targetCenter);

        double step = 0.5;
        for (double d = step; d < distance; d += step) {
            Vec3 checkPoint = explosionCenter.add(direction.scale(d));
            BlockPos checkPos = BlockPos.containing(checkPoint);

            if (checkPos.equals(targetPos)) {
                continue;
            }

            BlockState state = level.getBlockState(checkPos);
            if (ModConfig.isProtectiveBlock(state.getBlock())) {
                return true;
            }
        }

        return false;
    }

    private static int calculateDamageAmount(double distance, float radius) {
        double normalizedDistance = distance / radius;
        double radiusMultiplier = Math.max(1.0, radius / 4.0);
        double distanceMultiplier = 3.0 - (2.0 * normalizedDistance);

        return Math.max(1, (int) Math.round(distanceMultiplier * radiusMultiplier));
    }

    private static boolean applyBlockDamage(ServerLevel level, BlockPos pos, int damageAmount) {
        BlockState state = level.getBlockState(pos);
        int requiredHits = ModConfig.getHitsForBlock(state.getBlock());
        BlockDamageData damageData = BlockDamageManager.getDamageData(level, pos);
        int currentDamage = damageData.damage() + damageAmount;

        if (currentDamage >= requiredHits) {
            BlockDamageManager.removeDamage(level, pos);
            return true;
        } else {
            BlockDamageManager.setDamage(level, pos, currentDamage);
            showDamageEffects(level, pos, currentDamage, requiredHits);
            return false;
        }
    }

    private static void updateAffectedBlocksList(List<BlockPos> affectedBlocks, Set<BlockPos> blocksToBreak) {

        affectedBlocks.removeIf(pos -> !blocksToBreak.contains(pos));

        for (BlockPos pos : blocksToBreak) {
            if (!affectedBlocks.contains(pos)) {
                affectedBlocks.add(pos);
            }
        }
    }

    private static void showDamageEffects(ServerLevel level, BlockPos pos, int damage, int maxDamage) {
        int damageStage = Math.min(9, (int) ((float) damage / maxDamage * 10));
        level.destroyBlockProgress(-1 - pos.hashCode(), pos, damageStage);
    }
}