package com.king_tajin.block_explosion_damage;

public class BlockDamageData {
    private final int damage;
    private final long lastDamageTime;

    public BlockDamageData(int damage, long lastDamageTime) {
        this.damage = damage;
        this.lastDamageTime = lastDamageTime;
    }

    public int getDamage() {
        return damage;
    }

    public long getLastDamageTime() {
        return lastDamageTime;
    }
}