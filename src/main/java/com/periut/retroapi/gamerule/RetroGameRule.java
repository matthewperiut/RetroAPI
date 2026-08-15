package com.periut.retroapi.gamerule;

/**
 * One rule: its name, what kind of value it holds, and what that value is when nobody has said
 * otherwise.
 *
 * <p>Names follow modern Minecraft's, so a rule that exists there is spelled the same here
 * ({@code doFireTick}, {@code keepInventory}). A mod's own rule should be namespaced
 * ({@code mymod:something}) so two mods cannot collide.
 */
public final class RetroGameRule {

    public enum Type {
        BOOLEAN,
        INTEGER
    }

    private final String key;
    private final Type type;
    private final String defaultValue;

    RetroGameRule(final String key, final Type type, final String defaultValue) {
        this.key = key;
        this.type = type;
        this.defaultValue = defaultValue;
    }

    public String getKey() {
        return key;
    }

    public Type getType() {
        return type;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public boolean getDefaultBoolean() {
        return Boolean.parseBoolean(defaultValue);
    }

    public int getDefaultInt() {
        try {
            return Integer.parseInt(defaultValue);
        } catch (final NumberFormatException ignored) {
            return 0;
        }
    }

    /** @return true when the text is a value this rule could hold */
    public boolean accepts(final String value) {
        return switch (type) {
            case BOOLEAN -> "true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value);
            case INTEGER -> {
                try {
                    Integer.parseInt(value);
                    yield true;
                } catch (final NumberFormatException ignored) {
                    yield false;
                }
            }
        };
    }

    @Override
    public String toString() {
        return key;
    }
}
