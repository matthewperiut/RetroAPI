package com.periut.retroapi.movement;

import net.fabricmc.loader.api.FabricLoader;

/**
 * Shared state for the movement backport (sprinting, swimming, and the FOV kick that goes with them).
 *
 * <p>Whether either is ALLOWED is not decided here - that is the {@code sprinting} and {@code swimming}
 * game rules, both off by default, enforced in {@code EntityMixin} where the flags are set.
 */
public final class RetroMovement {
    private RetroMovement() {
    }

    // Resolved on first use rather than from onInitialize: the mixins here run before any
    // entrypoint would fire on some loader setups, and both answers are already known by then.
    /** StationAPI owns the terrain atlas and its own keybinds when it is installed. */
    public static final boolean stapi = FabricLoader.getInstance().isModLoaded("stationapi");

    public static float movementFovMultiplier;
    public static float lastMovementFovMultiplier;
    public static int runKeyCode = 29;
}
