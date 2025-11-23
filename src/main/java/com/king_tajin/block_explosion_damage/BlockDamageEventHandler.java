package com.king_tajin.block_explosion_damage;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

public class BlockDamageEventHandler {

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            BlockPos pos = event.getPos();
            clearBlockDamage(serverLevel, pos);
        }
    }

    private void clearBlockDamage(ServerLevel level, BlockPos pos) {
        BlockDamageData damageData = BlockDamageManager.getDamageData(level, pos);

        if (damageData.damage() > 0) {
            BlockDamageManager.removeDamage(level, pos);
            level.destroyBlockProgress(-1 - pos.hashCode(), pos, -1);
        }
    }
}