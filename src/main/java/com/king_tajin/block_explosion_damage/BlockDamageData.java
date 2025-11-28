package com.king_tajin.block_explosion_damage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;

public record BlockDamageData(BlockPos pos, int damage, long lastDamageTime) {

    public static final Codec<BlockDamageData> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    BlockPos.CODEC.fieldOf("pos").forGetter(BlockDamageData::pos),
                    Codec.INT.fieldOf("damage").forGetter(BlockDamageData::damage),
                    Codec.LONG.fieldOf("lastDamageTime").forGetter(BlockDamageData::lastDamageTime)
            ).apply(instance, BlockDamageData::new)
    );

    public BlockDamageData(int damage, long lastDamageTime) {
        this(null, damage, lastDamageTime);
    }
}