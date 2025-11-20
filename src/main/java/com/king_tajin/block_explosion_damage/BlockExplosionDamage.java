package com.king_tajin.block_explosion_damage;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
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
    public void onExplosionDetonate(ExplosionEvent.Detonate event) {
        Level level = event.getLevel();

        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        // Clear the affected blocks list to prevent any blocks from breaking
        // We'll handle everything through our damage system
        Iterator<BlockPos> iterator = event.getAffectedBlocks().iterator();

        while (iterator.hasNext()) {
            BlockPos pos = iterator.next();
            BlockState state = level.getBlockState(pos);

            // Skip air and bedrock
            if (state.isAir() || state.is(Blocks.BEDROCK)) {
                iterator.remove();
                continue;
            }

            // Get required hits for this block type
            int requiredHits = ModConfig.getHitsForBlock(state.getBlock());

            // Get current damage
            BlockDamageData damageData = BlockDamageManager.getDamageData(serverLevel, pos);
            int currentDamage = damageData.getDamage() + 1;

            // Check if block should break
            if (currentDamage >= requiredHits) {
                BlockDamageManager.removeDamage(serverLevel, pos);
                // Allow this block to break by leaving it in the list
            } else {
                // Save damage with current timestamp
                BlockDamageManager.setDamage(serverLevel, pos, currentDamage);

                // Show visual feedback (mining cracks)
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
        }
    }

    private void showDamageEffects(ServerLevel level, BlockPos pos, int damage, int maxDamage) {
        // Calculate damage stage (0-9 like block breaking animation)
        int damageStage = Math.min(9, (int) ((float) damage / maxDamage * 10));

        // Send block damage packet to all players
        level.destroyBlockProgress(-1 - pos.hashCode(), pos, damageStage);
    }
}