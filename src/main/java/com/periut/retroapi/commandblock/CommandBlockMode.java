package com.periut.retroapi.commandblock;

/**
 * Modern's three command block kinds, and modern's rule for telling them apart: the mode is a
 * property of <em>which block it is</em>, not a setting stored on it.
 *
 * <ul>
 *   <li>{@link #REDSTONE} - the plain impulse block: runs once on the rising edge of a signal.</li>
 *   <li>{@link #AUTO} - the repeating block: runs every tick while powered or always-active.</li>
 *   <li>{@link #SEQUENCE} - the chain block: runs only when the block pointing into it succeeds.</li>
 * </ul>
 */
public enum CommandBlockMode {
    SEQUENCE("Chain"),
    AUTO("Repeat"),
    REDSTONE("Impulse");

    private final String displayName;

    CommandBlockMode(final String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /** The order modern's edit screen cycles through: Impulse -> Chain -> Repeat. */
    public CommandBlockMode next() {
        return switch (this) {
            case REDSTONE -> SEQUENCE;
            case SEQUENCE -> AUTO;
            case AUTO -> REDSTONE;
        };
    }
}
