package com.king_tajin.block_explosion_damage;

import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.server.level.ServerLevel;

import java.util.Iterator;

@Mod("block_explosion_damage")
public class BlockExplosionDamage {

    public BlockExplosionDamage(IEventBus modEventBus) {
        modEventBus.addListener(this::commonSetup);

        // Register attachment types
        BlockDamageManager.ATTACHMENT_TYPES.register(modEventBus);

        NeoForge.EVENT_BUS.register(this);
        NeoForge.EVENT_BUS.register(new BlockDamageEventHandler());
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        // Load config
        ModConfig.init();
    }

    @SubscribeEvent
    public void onChunkLoad(ChunkEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel && event.getChunk() instanceof LevelChunk chunk) {
            ChunkDamageData chunkData = chunk.getData(BlockDamageManager.CHUNK_DAMAGE);
            BlockDamageManager.registerLoadedChunk(chunk.getPos(), chunkData);
        }
    }

    @SubscribeEvent
    public void onExplosionDetonate(ExplosionEvent.Detonate event) {
        Level level = event.getLevel();

        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        Iterator<BlockPos> iterator = event.getAffectedBlocks().iterator();

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

            int requiredHits = ModConfig.getHitsForBlock(state.getBlock());

            BlockDamageData damageData = BlockDamageManager.getDamageData(serverLevel, pos);
            int currentDamage = damageData.getDamage() + 1;

            // Check if block should break
            if (currentDamage >= requiredHits) {
                BlockDamageManager.removeDamage(serverLevel, pos);

            } else {
                BlockDamageManager.setDamage(serverLevel, pos, currentDamage);

                showDamageEffects(serverLevel, pos, currentDamage, requiredHits);

                // Remove from list to prevent breaking
                iterator.remove();
            }
        }
    }

    @SubscribeEvent
    public void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            // Every 20 ticks (1 second), process decay
            if (serverLevel.getGameTime() % 20 == 0) {
                BlockDamageManager.processDecay(serverLevel);
            }

            if (serverLevel.getGameTime() % 10 == 0) {
                BlockDamageManager.refreshVisuals(serverLevel);
            }
        }
    }

    private void showDamageEffects(ServerLevel level, BlockPos pos, int damage, int maxDamage) {
        // Calculate damage stage (0-9 like block breaking animation)
        int damageStage = Math.min(9, (int) ((float) damage / maxDamage * 10));

        // Send block damage packet to all players
        level.destroyBlockProgress(-1 - pos.hashCode(), pos, damageStage);
    }
}