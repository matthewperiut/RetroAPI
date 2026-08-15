package com.periut.retroapi.text;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * The vanilla {@code §} codes.
 *
 * <p>b1.7.3's font renderer only understands the sixteen colours - {@code §k} through {@code §o}
 * were added years later - so the formats here carry no meaning for the vanilla renderer and are
 * applied by {@link com.periut.retroapi.client.gui.RetroTextDrawer} instead. They still round-trip
 * through {@link Texts#toLegacy} so that a message sent to a vanilla client degrades to plain text
 * rather than to visible junk.
 */
public enum Formatting {
    BLACK("BLACK", '0', 0x000000),
    DARK_BLUE("DARK_BLUE", '1', 0x0000AA),
    DARK_GREEN("DARK_GREEN", '2', 0x00AA00),
    DARK_AQUA("DARK_AQUA", '3', 0x00AAAA),
    DARK_RED("DARK_RED", '4', 0xAA0000),
    DARK_PURPLE("DARK_PURPLE", '5', 0xAA00AA),
    GOLD("GOLD", '6', 0xFFAA00),
    GRAY("GRAY", '7', 0xAAAAAA),
    DARK_GRAY("DARK_GRAY", '8', 0x555555),
    BLUE("BLUE", '9', 0x5555FF),
    GREEN("GREEN", 'a', 0x55FF55),
    AQUA("AQUA", 'b', 0x55FFFF),
    RED("RED", 'c', 0xFF5555),
    LIGHT_PURPLE("LIGHT_PURPLE", 'd', 0xFF55FF),
    YELLOW("YELLOW", 'e', 0xFFFF55),
    WHITE("WHITE", 'f', 0xFFFFFF),
    OBFUSCATED("OBFUSCATED", 'k', true),
    BOLD("BOLD", 'l', true),
    STRIKETHROUGH("STRIKETHROUGH", 'm', true),
    UNDERLINE("UNDERLINE", 'n', true),
    ITALIC("ITALIC", 'o', true),
    RESET("RESET", 'r', -1);

    public static final char CODE_PREFIX = '§';

    private static final Map<String, Formatting> BY_NAME = new HashMap<>();
    private static final Map<Character, Formatting> BY_CODE = new HashMap<>();

    static {
        for (final Formatting formatting : values()) {
            BY_NAME.put(formatting.name.toLowerCase(Locale.ROOT), formatting);
            BY_CODE.put(formatting.code, formatting);
        }
    }

    private final String name;
    private final char code;
    private final boolean modifier;
    private final int colorValue;

    Formatting(final String name, final char code, final int colorValue) {
        this.name = name;
        this.code = code;
        this.modifier = false;
        this.colorValue = colorValue;
    }

    Formatting(final String name, final char code, final boolean modifier) {
        this.name = name;
        this.code = code;
        this.modifier = modifier;
        this.colorValue = -1;
    }

    public char getCode() {
        return code;
    }

    public boolean isModifier() {
        return modifier;
    }

    public boolean isColor() {
        return !modifier && this != RESET;
    }

    /** The RGB this colour renders as, or -1 for formats and RESET. */
    public int getColorValue() {
        return colorValue;
    }

    public String getName() {
        return name.toLowerCase(Locale.ROOT);
    }

    @Override
    public String toString() {
        return String.valueOf(CODE_PREFIX) + code;
    }

    public static Formatting byCode(final char code) {
        return BY_CODE.get(Character.toLowerCase(code));
    }

    public static Formatting byName(final String name) {
        return name == null ? null : BY_NAME.get(name.toLowerCase(Locale.ROOT));
    }

    /** The closest of the sixteen vanilla colours to an arbitrary RGB, for legacy output. */
    public static Formatting nearest(final int rgb) {
        Formatting best = WHITE;
        int bestDistance = Integer.MAX_VALUE;
        final int r = rgb >> 16 & 0xFF;
        final int g = rgb >> 8 & 0xFF;
        final int b = rgb & 0xFF;

        for (final Formatting formatting : values()) {
            if (!formatting.isColor()) {
                continue;
            }
            final int dr = r - (formatting.colorValue >> 16 & 0xFF);
            final int dg = g - (formatting.colorValue >> 8 & 0xFF);
            final int db = b - (formatting.colorValue & 0xFF);
            final int distance = dr * dr + dg * dg + db * db;
            if (distance < bestDistance) {
                bestDistance = distance;
                best = formatting;
            }
        }

        return best;
    }

    /** Strips every {@code §x} pair, for width measurements and for logging to the console. */
    public static String strip(final String text) {
        if (text == null || text.indexOf(CODE_PREFIX) < 0) {
            return text;
        }
        final StringBuilder result = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            final char c = text.charAt(i);
            if (c == CODE_PREFIX && i + 1 < text.length() && byCode(text.charAt(i + 1)) != null) {
                i++;
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }
}
