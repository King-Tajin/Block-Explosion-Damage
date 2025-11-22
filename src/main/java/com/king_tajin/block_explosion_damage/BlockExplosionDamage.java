package com.king_tajin.block_explosion_damage;

import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;

@Mod("block_explosion_damage")
public class BlockExplosionDamage {

    public BlockExplosionDamage(IEventBus modEventBus) {
        modEventBus.addListener(this::commonSetup);

        BlockDamageManager.ATTACHMENT_TYPES.register(modEventBus);

        NeoForge.EVENT_BUS.register(this);
        NeoForge.EVENT_BUS.register(new BlockDamageEventHandler());
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        ModConfig.init();
        ModGameRules.register();
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        ModCommands.register(event.getDispatcher());
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
        ExplosionHandler.handleExplosion(serverLevel, event.getExplosion(), event.getAffectedBlocks());
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

}