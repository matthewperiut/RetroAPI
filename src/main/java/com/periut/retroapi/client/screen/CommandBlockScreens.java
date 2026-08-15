package com.periut.retroapi.client.screen;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerEntity;

/**
 * Opens the command block editor. Client-only, and reached from the block's {@code onUse} behind an
 * {@code isRemote} check, so a dedicated server never loads the screen.
 */
public final class CommandBlockScreens {
    private CommandBlockScreens() {
    }

    /**
     * Opens the editor if this game is a client and {@code player} is the person sitting at it.
     *
     * <p>Called from the block's {@code onUse}, which runs on both sides: a dedicated server has no
     * game instance to cast and does nothing, and on a client only the local player's own click
     * opens anything.
     */
    public static void openFor(final PlayerEntity player, final int x, final int y, final int z) {
        final Object game = FabricLoader.getInstance().getGameInstance();
        if (!(game instanceof Minecraft minecraft) || minecraft.player != player) {
            return;
        }
        minecraft.setScreen(new CommandBlockScreen(x, y, z));
    }
}
