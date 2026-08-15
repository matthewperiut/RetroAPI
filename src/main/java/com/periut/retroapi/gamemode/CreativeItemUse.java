package com.periut.retroapi.gamemode;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

/**
 * Creative does not spend what it uses.
 *
 * <p>Modern says this once, in the two places an item is used: {@code if (player.hasInfiniteMaterials())
 * { int count = stack.getCount(); ...use it...; stack.setCount(count); }}. Beta has no such notion, so
 * every use path spends the stack itself - {@code BlockItem} decrements it as it places, food
 * decrements it as it is eaten, and a bucket does not decrement at all but hands back a <em>different</em>
 * stack for the slot, which is how an emptied bucket appears in your hand.
 *
 * <p>So this restores both: the stack that was in the slot, and the count it had. Take the snapshot
 * before the use and put it back after, and creative stops spending - blocks, food and buckets alike -
 * without any of those paths having to know about game modes.
 */
public final class CreativeItemUse {
    private CreativeItemUse() {
    }

    /** What the player was holding, or null when nothing needs restoring. */
    public record Snapshot(int slot, ItemStack stack, int count) {
    }

    /** @return the state to hand back to {@link #after}, or null if this use should be left alone */
    public static Snapshot before(final PlayerEntity player) {
        if (player == null || RetroGameModes.get(player) != RetroGameMode.CREATIVE) {
            return null;
        }

        final int slot = player.inventory.selectedSlot;
        if (slot < 0 || slot >= player.inventory.main.length) {
            return null;
        }

        final ItemStack held = player.inventory.main[slot];
        return held == null ? null : new Snapshot(slot, held, held.count);
    }

    public static void after(final PlayerEntity player, final Snapshot snapshot) {
        if (player == null || snapshot == null) {
            return;
        }
        player.inventory.main[snapshot.slot()] = snapshot.stack();
        snapshot.stack().count = snapshot.count();
    }
}
