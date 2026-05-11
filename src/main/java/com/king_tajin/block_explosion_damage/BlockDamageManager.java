package com.king_tajin.block_explosion_damage;

import com.king_tajin.block_explosion_damage.config.ModConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public class BlockDamageManager {

    private static final Logger LOGGER = LoggerFactory.getLogger("block_explosion_damage");

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, "block_explosion_damage");

    public static final Supplier<AttachmentType<ChunkDamageData>> CHUNK_DAMAGE = ATTACHMENT_TYPES.register(
            "chunk_damage",
            () -> AttachmentType.builder(ChunkDamageData::new)
                    .serialize(new IAttachmentSerializer<>() {
                        @Override
                        public @NonNull ChunkDamageData read(@NotNull IAttachmentHolder holder, @NotNull ValueInput input) {
                            return input.read("data", ChunkDamageData.CODEC).orElseGet(() -> {
                                if (!input.childrenListOrEmpty("damages").isEmpty()) {
                                    if (holder instanceof LevelChunk chunk) {
                                        LOGGER.warn("block_explosion_damage: Found incompatible chunk damage data at chunk [{}, {}] (likely from a previous version), clearing it.", chunk.getPos().x(), chunk.getPos().z());
                                    } else {
                                        LOGGER.warn("block_explosion_damage: Found incompatible chunk damage data (likely from a previous version), clearing it.");
                                    }
                                }
                                return new ChunkDamageData();
                            });
                        }

                        @Override
                        public boolean write(@NotNull ChunkDamageData attachment, @NotNull ValueOutput output) {
                            if (attachment.isEmpty()) return false;
                            output.store("data", ChunkDamageData.CODEC, attachment);
                            return true;
                        }
                    })
                    .build()
    );

    private static final Map<ResourceKey<Level>, Set<ChunkPos>> damagedChunks = new HashMap<>();

    private static Set<ChunkPos> getChunksForLevel(ServerLevel level) {
        return damagedChunks.computeIfAbsent(level.dimension(), _ -> new HashSet<>());
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
        chunk.markUnsaved();
        getChunksForLevel(level).add(chunk.getPos());
    }

    public static void removeDamage(ServerLevel level, BlockPos pos) {
        LevelChunk chunk = level.getChunkAt(pos);
        ChunkDamageData chunkData = chunk.getData(CHUNK_DAMAGE);
        chunkData.removeDamage(pos);
        chunk.markUnsaved();
        if (chunkData.isEmpty()) {
            getChunksForLevel(level).remove(chunk.getPos());
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
        if (!level.getGameRules().get(ModGameRules.RULE_BLOCK_DAMAGE_DECAY.get())) {
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

            if (!level.getChunkSource().hasChunk(chunkPos.x(), chunkPos.z())) {
                continue;
            }

            LevelChunk chunk = level.getChunk(chunkPos.x(), chunkPos.z());
            ChunkDamageData chunkData = chunk.getData(CHUNK_DAMAGE);
            boolean modified = chunkData.processDecay(level, currentTime, decayTime);

            if (modified) {
                chunk.markUnsaved();
            }

            if (chunkData.isEmpty()) {
                iterator.remove();
            }
        }
    }

    public static void refreshVisuals(ServerLevel level) {
        for (ChunkPos chunkPos : getChunksForLevel(level)) {
            if (!level.getChunkSource().hasChunk(chunkPos.x(), chunkPos.z())) {
                continue;
            }

            LevelChunk chunk = level.getChunk(chunkPos.x(), chunkPos.z());
            ChunkDamageData chunkData = chunk.getData(CHUNK_DAMAGE);
            chunkData.refreshVisuals(level);
        }
    }

    public static int clearAllDamage(ServerLevel level) {
        int totalCleared = 0;

        Set<ChunkPos> chunks = getChunksForLevel(level);
        for (ChunkPos chunkPos : chunks) {
            if (!level.getChunkSource().hasChunk(chunkPos.x(), chunkPos.z())) {
                continue;
            }

            LevelChunk chunk = level.getChunk(chunkPos.x(), chunkPos.z());
            ChunkDamageData chunkData = chunk.getData(CHUNK_DAMAGE);
            totalCleared += chunkData.clearAllDamage(level);
            chunk.markUnsaved();
        }

        chunks.clear();
        return totalCleared;
    }
}