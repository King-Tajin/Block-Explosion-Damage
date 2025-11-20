package com.king_tajin.block_explosion_damage;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Supplier;

public class BlockDamageManager {

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, "block_explotion_damage");

    public static final Supplier<AttachmentType<ChunkDamageData>> CHUNK_DAMAGE = ATTACHMENT_TYPES.register(
            "chunk_damage",
            () -> AttachmentType.serializable(ChunkDamageData::new).build()
    );

    public static BlockDamageData getDamageData(ServerLevel level, BlockPos pos) {
        LevelChunk chunk = level.getChunkAt(pos);
        ChunkDamageData chunkData = chunk.getData(CHUNK_DAMAGE);
        return chunkData.getDamage(pos);
    }

    public static void setDamage(ServerLevel level, BlockPos pos, int damage) {
        LevelChunk chunk = level.getChunkAt(pos);
        ChunkDamageData chunkData = chunk.getData(CHUNK_DAMAGE);
        chunkData.setDamage(pos, damage, level.getGameTime());
        chunk.setUnsaved(true);
    }

    public static void removeDamage(ServerLevel level, BlockPos pos) {
        LevelChunk chunk = level.getChunkAt(pos);
        ChunkDamageData chunkData = chunk.getData(CHUNK_DAMAGE);
        chunkData.removeDamage(pos);
        chunk.setUnsaved(true);
    }

    public static void processDecay(ServerLevel level) {
        long currentTime = level.getGameTime();
        int decayTime = ModConfig.getDamageDecayTime();

        if (decayTime <= 0) {
            return;
        }

        Iterable<LevelChunk> chunks = level.getChunkSource().chunkMap.getLoadedChunks();
        for (LevelChunk chunk : chunks) {
            ChunkDamageData chunkData = chunk.getData(CHUNK_DAMAGE);
            boolean modified = chunkData.processDecay(level, currentTime, decayTime);

            if (modified) {
                chunk.setUnsaved(true);
            }
        }
    }

    public static class ChunkDamageData implements net.neoforged.neoforge.common.util.INBTSerializable<CompoundTag> {
        private final Map<BlockPos, BlockDamageData> damageMap = new HashMap<>();

        public ChunkDamageData() {}

        public BlockDamageData getDamage(BlockPos pos) {
            return damageMap.getOrDefault(pos, new BlockDamageData(0, 0));
        }

        public void setDamage(BlockPos pos, int damage, long time) {
            damageMap.put(pos.immutable(), new BlockDamageData(damage, time));
        }

        public void removeDamage(BlockPos pos) {
            damageMap.remove(pos);
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
                    modified = true;
                    continue;
                }

                long timeSinceDamage = currentTime - data.getLastDamageTime();
                if (timeSinceDamage >= decayTime) {
                    int newDamage = data.getDamage() - 1;
                    if (newDamage <= 0) {
                        iterator.remove();
                    } else {
                        entry.setValue(new BlockDamageData(newDamage, currentTime));
                    }
                    modified = true;
                }
            }

            return modified;
        }

        @Override
        public CompoundTag serializeNBT(HolderLookup.@NotNull Provider provider) {
            CompoundTag tag = new CompoundTag();
            ListTag listTag = new ListTag();

            for (Map.Entry<BlockPos, BlockDamageData> entry : damageMap.entrySet()) {
                CompoundTag entryTag = new CompoundTag();
                BlockPos pos = entry.getKey();
                BlockDamageData data = entry.getValue();

                entryTag.putInt("x", pos.getX());
                entryTag.putInt("y", pos.getY());
                entryTag.putInt("z", pos.getZ());
                entryTag.putInt("damage", data.getDamage());
                entryTag.putLong("time", data.getLastDamageTime());

                listTag.add(entryTag);
            }

            tag.put("blocks", listTag);
            return tag;
        }

        @Override
        public void deserializeNBT(HolderLookup.@NotNull Provider provider, CompoundTag tag) {
            damageMap.clear();

            ListTag listTag = tag.getList("blocks", Tag.TAG_COMPOUND);
            for (int i = 0; i < listTag.size(); i++) {
                CompoundTag entryTag = listTag.getCompound(i);

                BlockPos pos = new BlockPos(
                        entryTag.getInt("x"),
                        entryTag.getInt("y"),
                        entryTag.getInt("z")
                );

                int damage = entryTag.getInt("damage");
                long time = entryTag.getLong("time");

                damageMap.put(pos, new BlockDamageData(damage, time));
            }
        }
    }
}