package com.king_tajin.block_explosion_damage;

import com.king_tajin.block_explosion_damage.config.ModConfig;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public class BlockDamageManager {

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create(
        NeoForgeRegistries.ATTACHMENT_TYPES,
        "block_explosion_damage"
    );

    public static final Supplier<AttachmentType<ChunkDamageData>> CHUNK_DAMAGE = ATTACHMENT_TYPES.register(
        "chunk_damage",
        () -> AttachmentType.serializable(ChunkDamageData::new).build()
    );

    private static final Map<ResourceKey<Level>, Set<ChunkPos>> damagedChunks = new HashMap<>();

    private static Set<ChunkPos> getChunksForLevel(ServerLevel level) {
        return damagedChunks.computeIfAbsent(level.dimension(), key -> new HashSet<>());
    }

    public static void onLevelUnload(ServerLevel level) {
        damagedChunks.remove(level.dimension());
    }

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
        getChunksForLevel(level).add(chunkPos);
    }

    public static void removeDamage(ServerLevel level, BlockPos pos) {
        LevelChunk chunk = level.getChunkAt(pos);
        ChunkDamageData chunkData = chunk.getData(CHUNK_DAMAGE);
        chunkData.removeDamage(pos);
        chunk.setUnsaved(true);
        ChunkPos chunkPos = chunk.getPos();

        if (chunkData.isEmpty()) {
            getChunksForLevel(level).remove(chunkPos);
        }
    }

    public static void registerLoadedChunk(ServerLevel level, ChunkPos chunkPos, ChunkDamageData chunkData) {
        if (!chunkData.isEmpty()) {
            getChunksForLevel(level).add(chunkPos);
        }
    }

    public static void unregisterChunk(ServerLevel level, ChunkPos chunkPos) {
        getChunksForLevel(level).remove(chunkPos);
    }

    public static void processDecay(ServerLevel level) {
        if (!level.getGameRules().getBoolean(ModGameRules.RULE_BLOCK_DAMAGE_DECAY)) {
            return;
        }

        long currentTime = level.getGameTime();
        int decayTime = ModConfig.getDamageDecayTime();

        if (decayTime <= 0) {
            return;
        }

        Iterator<ChunkPos> iterator = getChunksForLevel(level).iterator();
        while (iterator.hasNext()) {
            ChunkPos chunkPos = iterator.next();

            if (!level.getChunkSource().hasChunk(chunkPos.x, chunkPos.z)) {
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
            }
        }
    }

    public static void refreshVisuals(ServerLevel level) {
        for (ChunkPos chunkPos : getChunksForLevel(level)) {
            if (!level.getChunkSource().hasChunk(chunkPos.x, chunkPos.z)) {
                continue;
            }

            LevelChunk chunk = level.getChunk(chunkPos.x, chunkPos.z);
            ChunkDamageData chunkData = chunk.getData(CHUNK_DAMAGE);
            chunkData.refreshVisuals(level);
        }
    }

    public static int clearAllDamage(ServerLevel level) {
        int totalCleared = 0;

        Set<ChunkPos> chunks = getChunksForLevel(level);
        for (ChunkPos chunkPos : chunks) {
            if (!level.getChunkSource().hasChunk(chunkPos.x, chunkPos.z)) {
                continue;
            }

            LevelChunk chunk = level.getChunk(chunkPos.x, chunkPos.z);
            ChunkDamageData chunkData = chunk.getData(CHUNK_DAMAGE);
            totalCleared += chunkData.clearAllDamage(level);
            chunk.setUnsaved(true);
        }

        chunks.clear();
        return totalCleared;
    }
}
