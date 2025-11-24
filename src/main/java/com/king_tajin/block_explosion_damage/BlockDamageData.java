package com.king_tajin.block_explosion_damage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record BlockDamageData(int damage, long lastDamageTime) {

    public static final Codec<BlockDamageData> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.INT.fieldOf("damage").forGetter(BlockDamageData::damage),
                    Codec.LONG.fieldOf("lastDamageTime").forGetter(BlockDamageData::lastDamageTime)
            ).apply(instance, BlockDamageData::new)
    );
}