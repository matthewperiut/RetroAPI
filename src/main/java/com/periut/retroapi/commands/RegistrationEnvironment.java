package com.periut.retroapi.commands;

/**
 * Which side is building the command tree.
 *
 * <p>Mirrors modern Minecraft's dedicated/integrated split. Beta has no integrated server - a
 * singleplayer client runs commands against its own world - so {@link #INTEGRATED} means exactly
 * that case, and commands that only make sense with other players connected skip it.
 */
public enum RegistrationEnvironment {
    INTEGRATED,
    DEDICATED;

    public boolean isDedicated() {
        return this == DEDICATED;
    }

    public boolean isIntegrated() {
        return this == INTEGRATED;
    }
}
