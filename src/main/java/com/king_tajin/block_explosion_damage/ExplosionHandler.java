package com.king_tajin.block_explosion_damage;

import com.king_tajin.block_explosion_damage.config.ModConfig;
import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.companion.SubLevelAccess;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class ExplosionHandler {

    private static final double RAY_STEP = 0.5;
    private static final double TARGET_RAY_SPACING = 0.5;
    private static final double MIN_ANGLE_STEP = 0.02;
    private static final double MAX_ANGLE_STEP = Math.PI / 6;

    public static void handleExplosion(ServerLevel level, Explosion explosion, List<BlockPos> affectedBlocks) {
        Vec3 explosionCenter = explosion.center();
        float radius = explosion.radius();

        List<SubLevelAccess> nearbySubLevels = collectNearbySubLevels(level, explosionCenter, radius);

        BlockPos explosionPos = resolveBlockPos(level, explosionCenter, nearbySubLevels);
        if (!level.getFluidState(explosionPos).isEmpty()) {
            affectedBlocks.clear();
            return;
        }

        Set<BlockPos> blocksToBreak = processExplosionRadius(level, explosionCenter, radius, nearbySubLevels);
        updateAffectedBlocksList(affectedBlocks, blocksToBreak);
    }

    private static List<SubLevelAccess> collectNearbySubLevels(ServerLevel level, Vec3 explosionCenter, float radius) {
        if (!ModConfig.isSableCompatibilityEnabled()) {
            return List.of();
        }

        List<SubLevelAccess> nearbySubLevels = new ArrayList<>();

        BoundingBox3d searchBounds = new BoundingBox3d(
            explosionCenter.x - radius,
            explosionCenter.y - radius,
            explosionCenter.z - radius,
            explosionCenter.x + radius,
            explosionCenter.y + radius,
            explosionCenter.z + radius
        );

        for (SubLevelAccess subLevel : SableCompanion.INSTANCE.getAllIntersecting(level, searchBounds)) {
            nearbySubLevels.add(subLevel);
        }

        return nearbySubLevels;
    }

    private static BlockPos resolveBlockPos(ServerLevel level, Vec3 globalPos, List<SubLevelAccess> nearbySubLevels) {
        for (SubLevelAccess subLevel : nearbySubLevels) {
            Vec3 localPos = subLevel.logicalPose().transformPositionInverse(globalPos);
            BlockPos localBlockPos = BlockPos.containing(localPos);

            if (!level.getBlockState(localBlockPos).isAir()) {
                return localBlockPos;
            }
        }

        return BlockPos.containing(globalPos);
    }

    private static Set<BlockPos> processExplosionRadius(
        ServerLevel level,
        Vec3 explosionCenter,
        float radius,
        List<SubLevelAccess> nearbySubLevels
    ) {
        Set<BlockPos> blocksToBreak = new HashSet<>();
        Map<BlockPos, Integer> damageMap = new HashMap<>();

        double angleStep = Math.clamp(TARGET_RAY_SPACING / Math.max(radius, 0.5), MIN_ANGLE_STEP, MAX_ANGLE_STEP);
        int polarSteps = (int) Math.ceil(Math.PI / angleStep);

        for (int i = 0; i <= polarSteps; i++) {
            double theta = i * angleStep;
            double sinTheta = Math.sin(theta);
            double cosTheta = Math.cos(theta);

            int azimuthSteps = Math.max(1, (int) Math.ceil((2 * Math.PI * sinTheta) / angleStep));

            for (int j = 0; j < azimuthSteps; j++) {
                double phi = (j * 2 * Math.PI) / azimuthSteps;
                Vec3 direction = new Vec3(sinTheta * Math.cos(phi), cosTheta, sinTheta * Math.sin(phi)).normalize();

                castExplosionRay(level, explosionCenter, direction, radius, nearbySubLevels, blocksToBreak, damageMap);
            }
        }

        damageMap.forEach((pos, damageAmount) -> applyBlockDamage(level, pos, damageAmount, false));

        return blocksToBreak;
    }

    private static void castExplosionRay(
        ServerLevel level,
        Vec3 origin,
        Vec3 direction,
        float radius,
        List<SubLevelAccess> nearbySubLevels,
        Set<BlockPos> blocksToBreak,
        Map<BlockPos, Integer> damageMap
    ) {
        for (double d = RAY_STEP; d <= radius; d += RAY_STEP) {
            Vec3 point = origin.add(direction.scale(d));
            BlockPos pos = resolveBlockPos(level, point, nearbySubLevels);
            BlockState state = level.getBlockState(pos);

            if (state.isAir()) {
                continue;
            }

            if (state.is(Blocks.TNT)) {
                blocksToBreak.add(pos);
                continue;
            }

            if (ModConfig.isProtectiveBlock(state.getBlock())) {
                return;
            }

            int damageAmount = calculateDamageAmount(d, radius);
            boolean destroyed = applyBlockDamage(level, pos, damageAmount, true);

            damageMap.merge(pos, damageAmount, Math::max);

            if (!destroyed) {
                return;
            }

            blocksToBreak.add(pos);
        }
    }

    private static int calculateDamageAmount(double distance, float radius) {
        double normalizedDistance = distance / radius;
        double radiusMultiplier = Math.max(1.0, radius / 4.0);
        double distanceMultiplier = 3.0 - 2.0 * normalizedDistance;
        return Math.max(1, (int) Math.round(distanceMultiplier * radiusMultiplier));
    }

    private static boolean applyBlockDamage(ServerLevel level, BlockPos pos, int damageAmount, boolean simulate) {
        BlockState state = level.getBlockState(pos);
        int requiredHits = ModConfig.getHitsForBlock(state.getBlock());
        BlockDamageData damageData = BlockDamageManager.getDamageData(level, pos);
        int currentDamage = damageData.damage() + damageAmount;

        if (currentDamage >= requiredHits) {
            if (!simulate) {
                BlockDamageManager.removeDamage(level, pos);
            }
            return true;
        }

        if (!simulate) {
            BlockDamageManager.setDamage(level, pos, currentDamage);
            showDamageEffects(level, pos, currentDamage, requiredHits);
        }

        return false;
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
        int damageStage = Math.min(9, (int) (((float) damage / maxDamage) * 10));
        level.destroyBlockProgress(-1 - pos.hashCode(), pos, damageStage);
    }
}
