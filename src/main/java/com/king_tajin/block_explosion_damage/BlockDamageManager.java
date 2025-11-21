package com.king_tajin.block_explosion_damage;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Supplier;

public class BlockDamageManager {

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, "block_explosion_damage");

    public static final Supplier<AttachmentType<ChunkDamageData>> CHUNK_DAMAGE = ATTACHMENT_TYPES.register(
            "chunk_damage",
            () -> AttachmentType.serializable(ChunkDamageData::new).build()
    );

    private static final Set<ChunkPos> dirtyChunks = new HashSet<>();
    private static final Set<ChunkPos> damagedChunks = new HashSet<>();

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
        ChunkPos chunkPos = chunk.getPos();
        dirtyChunks.add(chunkPos);
        damagedChunks.add(chunkPos);
    }

    public static void removeDamage(ServerLevel level, BlockPos pos) {
        LevelChunk chunk = level.getChunkAt(pos);
        ChunkDamageData chunkData = chunk.getData(CHUNK_DAMAGE);
        chunkData.removeDamage(pos);
        chunk.setUnsaved(true);
        ChunkPos chunkPos = chunk.getPos();
        dirtyChunks.add(chunkPos);

        if (chunkData.isEmpty()) {
            damagedChunks.remove(chunkPos);
        }
    }

    public static void processDecay(ServerLevel level) {
        long currentTime = level.getGameTime();
        int decayTime = ModConfig.getDamageDecayTime();

        if (decayTime <= 0) {
            return;
        }

        Iterator<ChunkPos> iterator = dirtyChunks.iterator();
        while (iterator.hasNext()) {
            ChunkPos chunkPos = iterator.next();

            if (!level.hasChunk(chunkPos.x, chunkPos.z)) {
                iterator.remove();
                continue;
            }

            LevelChunk chunk = level.getChunk(chunkPos.x, chunkPos.z);
            ChunkDamageData chunkData = chunk.getData(CHUNK_DAMAGE);
            boolean modified = chunkData.processDecay(level, currentTime, decayTime);

            if (modified) {
                chunk.setUnsaved(true);
            }

            if (chunkData.isEmpty()) {
                iterator.remove();
                damagedChunks.remove(chunkPos);
            }
        }
    }

    public static void refreshVisuals(ServerLevel level) {
        for (ChunkPos chunkPos : damagedChunks) {
            if (!level.hasChunk(chunkPos.x, chunkPos.z)) {
                continue;
            }

            LevelChunk chunk = level.getChunk(chunkPos.x, chunkPos.z);
            ChunkDamageData chunkData = chunk.getData(CHUNK_DAMAGE);
            chunkData.refreshVisuals(level);
        }
    }

    public static void registerLoadedChunk(ChunkPos chunkPos, ChunkDamageData chunkData) {
        if (!chunkData.isEmpty()) {
            damagedChunks.add(chunkPos);
        }
    }

    public static int clearAllDamage(ServerLevel level) {
        int totalCleared = 0;

        for (ChunkPos chunkPos : damagedChunks) {
            if (!level.hasChunk(chunkPos.x, chunkPos.z)) {
                continue;
            }

            LevelChunk chunk = level.getChunk(chunkPos.x, chunkPos.z);
            ChunkDamageData chunkData = chunk.getData(CHUNK_DAMAGE);

            totalCleared += chunkData.clearAllDamage(level);
            chunk.setUnsaved(true);
        }

        damagedChunks.clear();
        dirtyChunks.clear();

        return totalCleared;
    }
}