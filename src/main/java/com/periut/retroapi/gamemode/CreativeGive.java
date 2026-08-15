package com.periut.retroapi.gamemode;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

/**
 * Handing a creative player the stack they clicked in the creative screen.
 *
 * <p>Beta's creative-less inventory has no notion of an infinite item source, so this is a plain
 * "put it in their inventory, or drop it at their feet if there is no room" - and it is the single
 * place that does it, so the mode check lives here rather than at each caller.
 */
public final class CreativeGive {
    private CreativeGive() {
    }

    /** @return false when the player is not entitled to it */
    public static boolean give(final PlayerEntity player, final ItemStack stack) {
        if (player == null || stack == null || stack.count <= 0) {
            return false;
        }
        if (RetroGameModes.get(player) != RetroGameMode.CREATIVE) {
            return false;
        }

        final ItemStack copy = stack.copy();
        if (!player.inventory.addStack(copy)) {
            player.dropItem(copy, false);
        }
        player.inventory.dirty = true;
        return true;
    }

    /**
     * Sets one of the player's own slots outright - what the creative screen does when a stack is
     * dragged into the hotbar. Same permission check: a client saying it is in creative is not
     * evidence that it is.
     */
    public static boolean setSlot(final PlayerEntity player, final int slot, final ItemStack stack) {
        if (player == null || RetroGameModes.get(player) != RetroGameMode.CREATIVE) {
            return false;
        }
        if (slot < 0 || slot >= player.inventory.main.length) {
            return false;
        }

        player.inventory.main[slot] = stack;
        player.inventory.dirty = true;
        return true;
    }
}
