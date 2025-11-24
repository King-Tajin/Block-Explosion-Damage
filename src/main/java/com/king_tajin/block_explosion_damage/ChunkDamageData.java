package com.king_tajin.block_explosion_damage;

import com.king_tajin.block_explosion_damage.config.ModConfig;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class ChunkDamageData {

    public static final MapCodec<ChunkDamageData> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    Codec.unboundedMap(
                            BlockPos.CODEC,
                            BlockDamageData.CODEC
                    ).fieldOf("damageMap").forGetter(data -> data.damageMap)
            ).apply(instance, ChunkDamageData::new)
    );

    private final Map<BlockPos, BlockDamageData> damageMap;

    public ChunkDamageData() {
        this.damageMap = new HashMap<>();
    }

    private ChunkDamageData(Map<BlockPos, BlockDamageData> damageMap) {
        this.damageMap = new HashMap<>(damageMap);
    }

    public BlockDamageData getDamage(BlockPos pos) {
        return damageMap.getOrDefault(pos, new BlockDamageData(0, 0));
    }

    public void setDamage(BlockPos pos, int damage, long time) {
        damageMap.put(pos.immutable(), new BlockDamageData(damage, time));
    }

    public void removeDamage(BlockPos pos) {
        damageMap.remove(pos);
    }

    public boolean isEmpty() {
        return damageMap.isEmpty();
    }

    public void refreshVisuals(ServerLevel level) {
        for (Map.Entry<BlockPos, BlockDamageData> entry : damageMap.entrySet()) {
            BlockPos pos = entry.getKey();
            BlockDamageData data = entry.getValue();

            BlockState state = level.getBlockState(pos);
            if (state.isAir()) {
                continue;
            }

            int maxDamage = ModConfig.getHitsForBlock(state.getBlock());
            int damageStage = Math.min(9, (int) ((float) data.damage() / maxDamage * 10));
            level.destroyBlockProgress(-1 - pos.hashCode(), pos, damageStage);
        }
    }

    public int clearAllDamage(ServerLevel level) {
        int count = damageMap.size();

        for (BlockPos pos : damageMap.keySet()) {
            level.destroyBlockProgress(-1 - pos.hashCode(), pos, -1);
        }

        damageMap.clear();

        return count;
    }

    public boolean processDecay(ServerLevel level, long currentTime, int decayTime) {
        Iterator<Map.Entry<BlockPos, BlockDamageData>> iterator = damageMap.entrySet().iterator();
        boolean modified = false;

        while (iterator.hasNext()) {
            Map.Entry<BlockPos, BlockDamageData> entry = iterator.next();
            BlockPos pos = entry.getKey();
            BlockDamageData data = entry.getValue();

            BlockState state = level.getBlockState(pos);
            if (state.isAir()) {
                iterator.remove();
                level.destroyBlockProgress(-1 - pos.hashCode(), pos, -1);
                modified = true;
                continue;
            }

            long timeSinceDamage = currentTime - data.lastDamageTime();
            if (timeSinceDamage >= decayTime) {
                int newDamage = data.damage() - 1;
                if (newDamage <= 0) {
                    iterator.remove();
                    level.destroyBlockProgress(-1 - pos.hashCode(), pos, -1);
                } else {
                    entry.setValue(new BlockDamageData(newDamage, currentTime));

                    int maxDamage = ModConfig.getHitsForBlock(state.getBlock());
                    int damageStage = Math.min(9, (int) ((float) newDamage / maxDamage * 10));
                    level.destroyBlockProgress(-1 - pos.hashCode(), pos, damageStage);
                }
                modified = true;
            }
        }

        return modified;
    }
}