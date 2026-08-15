package com.periut.retroapi.gamemode;

import java.util.Locale;

/**
 * The four modern game modes, backported.
 *
 * <p>Beta has exactly one way to play, so all four are RetroAPI's own: nothing in the vanilla code
 * knows they exist, and each one is a set of rules applied on top of survival by the
 * {@code mixin/gamemode} package.
 *
 * <p>Ids match modern's, so {@code /gamemode 1} means creative here too.
 */
public enum RetroGameMode {
    SURVIVAL(0, "survival"),
    CREATIVE(1, "creative"),
    ADVENTURE(2, "adventure"),
    SPECTATOR(3, "spectator");

    private final int id;
    private final String name;

    RetroGameMode(final int id, final String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    /**
     * Whether this mode hands out flight without anyone asking - modern's {@code mayfly} default.
     *
     * <p>Creative and spectator do; survival and adventure do not, and need {@code /fly}. Changing
     * mode resets the permission to this, so the wings come and go with the mode unless someone says
     * otherwise afterwards.
     */
    public boolean allowsFlightByDefault() {
        return this == CREATIVE || this == SPECTATOR;
    }

    /** True for the modes that may not change the world. */
    public boolean isReadOnly() {
        return this == ADVENTURE || this == SPECTATOR;
    }

    /** True for the modes with no health, hunger or damage. */
    public boolean isInvulnerable() {
        return this == CREATIVE || this == SPECTATOR;
    }

    /** @return the mode, or null when nothing answers to that name or number */
    public static RetroGameMode byName(final String name) {
        if (name == null) {
            return null;
        }
        final String lower = name.toLowerCase(Locale.ROOT);
        for (final RetroGameMode mode : values()) {
            if (mode.name.equals(lower) || String.valueOf(mode.id).equals(lower)) {
                return mode;
            }
        }
        // Modern accepts the shorthand too: /gamemode c, /gamemode sp.
        return switch (lower) {
            case "s" -> SURVIVAL;
            case "c" -> CREATIVE;
            case "a" -> ADVENTURE;
            case "sp" -> SPECTATOR;
            default -> null;
        };
    }

    public static RetroGameMode byId(final int id) {
        for (final RetroGameMode mode : values()) {
            if (mode.id == id) {
                return mode;
            }
        }
        return SURVIVAL;
    }
}
