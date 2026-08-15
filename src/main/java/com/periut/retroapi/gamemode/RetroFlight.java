package com.periut.retroapi.gamemode;

import com.periut.retroapi.commands.builtin.NoclipCommand;
import net.minecraft.entity.player.PlayerEntity;

/**
 * The single answer to "is this player flying, and does the world stop them".
 *
 * <p>Three things put a player in the air and they must not each grow their own copy of the physics:
 * {@code /noclip}, creative flight, and spectator. They differ only in whether terrain is solid and
 * where the throttle comes from, so everything downstream - the movement override, the suffocation
 * check, the server's rubber-band correction, the scroll wheel - asks here instead of asking about a
 * mode.
 */
public final class RetroFlight {
    private RetroFlight() {
    }

    /** True when RetroAPI, not beta, is driving this player's movement. */
    public static boolean isFlying(final PlayerEntity player) {
        return player != null && isFlying(player.name);
    }

    public static boolean isFlying(final String playerName) {
        return NoclipCommand.isActive(playerName) || RetroGameModes.isFlying(playerName);
    }

    /**
     * True when terrain is not solid for this player. Creative flight stops at walls exactly as it
     * does in modern; {@code /noclip} and spectator go straight through them.
     */
    public static boolean ignoresBlocks(final PlayerEntity player) {
        return player != null && ignoresBlocks(player.name);
    }

    public static boolean ignoresBlocks(final String playerName) {
        return NoclipCommand.isActive(playerName)
            || RetroGameModes.get(playerName) == RetroGameMode.SPECTATOR;
    }

    /**
     * The scroll-wheel throttle, which only the modes that HAVE one answer with.
     *
     * <p>Spectator and {@code /noclip} are throttled; creative flight is a fixed speed in modern and
     * so it is here. Without this a spectator who wound the wheel up to 5x and switched to creative
     * kept flying at 5x, because the number outlived the mode that owned it.
     */
    public static double speed(final String playerName) {
        if (NoclipCommand.isActive(playerName)
            || RetroGameModes.get(playerName) == RetroGameMode.SPECTATOR) {
            return NoclipCommand.speed(playerName);
        }
        return 1.0;
    }
}
