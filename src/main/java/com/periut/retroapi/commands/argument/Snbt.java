package com.periut.retroapi.commands.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.periut.retroapi.text.Text;
import net.minecraft.nbt.NbtByte;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtDouble;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtFloat;
import net.minecraft.nbt.NbtInt;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtLong;
import net.minecraft.nbt.NbtShort;
import net.minecraft.nbt.NbtString;

/**
 * Modern Minecraft's stringified NBT, read into beta's own {@link NbtCompound}.
 *
 * <p>{@code {Color:14,Sheared:1b}} is what a player types on any modern version and it is what they
 * type here, down to the type suffixes: {@code 1b} is a byte, {@code 1s} a short, {@code 1L} a long,
 * {@code 1.0f} a float, {@code 1.0} or {@code 1.0d} a double, and a bare whole number an int. Beta
 * stores exactly these types, so nothing is approximated - the compound this produces is the same
 * shape the game writes to disk.
 *
 * <p>The convenience modern also allows is here: {@code true} and {@code false} are bytes, quotes are
 * optional on a key or a string value that has no spaces in it, and whitespace between tokens is free.
 *
 * <p>Not supported, because beta has no tag for them: typed arrays ({@code [I;1,2]}). A plain list
 * ({@code [0.0d,1.0d,0.0d]}, which is how {@code Motion} and {@code Rotation} are written) is.
 */
public final class Snbt {
    public static final SimpleCommandExceptionType EXPECTED_KEY = new SimpleCommandExceptionType(
        Text.literal("Expected an attribute name"));
    public static final SimpleCommandExceptionType EXPECTED_VALUE = new SimpleCommandExceptionType(
        Text.literal("Expected a value"));

    private Snbt() {
    }

    /** Reads one {@code {...}} compound, leaving the reader just past its closing brace. */
    public static NbtCompound parseCompound(final StringReader reader) throws CommandSyntaxException {
        final NbtCompound compound = new NbtCompound();

        reader.expect('{');
        reader.skipWhitespace();

        while (reader.canRead() && reader.peek() != '}') {
            final int keyStart = reader.getCursor();
            final String key = readKey(reader);
            if (key.isEmpty()) {
                reader.setCursor(keyStart);
                throw EXPECTED_KEY.createWithContext(reader);
            }

            reader.skipWhitespace();
            reader.expect(':');
            reader.skipWhitespace();
            compound.put(key, parseValue(reader));
            reader.skipWhitespace();

            if (!reader.canRead() || reader.peek() != ',') {
                break;
            }
            reader.skip();
            reader.skipWhitespace();
        }

        reader.expect('}');
        return compound;
    }

    private static NbtList parseList(final StringReader reader) throws CommandSyntaxException {
        final NbtList list = new NbtList();

        reader.expect('[');
        reader.skipWhitespace();

        while (reader.canRead() && reader.peek() != ']') {
            list.add(parseValue(reader));
            reader.skipWhitespace();

            if (!reader.canRead() || reader.peek() != ',') {
                break;
            }
            reader.skip();
            reader.skipWhitespace();
        }

        reader.expect(']');
        return list;
    }

    private static NbtElement parseValue(final StringReader reader) throws CommandSyntaxException {
        if (!reader.canRead()) {
            throw EXPECTED_VALUE.createWithContext(reader);
        }

        final char next = reader.peek();
        if (next == '{') {
            return parseCompound(reader);
        }
        if (next == '[') {
            return parseList(reader);
        }
        if (StringReader.isQuotedStringStart(next)) {
            return new NbtString(reader.readQuotedString());
        }

        final int start = reader.getCursor();
        final String token = readUnquoted(reader, true);
        if (token.isEmpty()) {
            reader.setCursor(start);
            throw EXPECTED_VALUE.createWithContext(reader);
        }
        return typed(token);
    }

    /**
     * The suffix decides the tag, exactly as it does in modern. Anything that does not parse as the
     * number its suffix claims falls back to a string rather than failing, which is how an unquoted
     * word such as {@code Motive:Kebab} stays readable.
     */
    private static NbtElement typed(final String token) {
        if ("true".equalsIgnoreCase(token)) {
            return new NbtByte((byte) 1);
        }
        if ("false".equalsIgnoreCase(token)) {
            return new NbtByte((byte) 0);
        }

        final char suffix = token.charAt(token.length() - 1);
        final String body = token.substring(0, token.length() - 1);
        try {
            switch (suffix) {
                case 'b': case 'B': return new NbtByte(Byte.parseByte(body));
                case 's': case 'S': return new NbtShort(Short.parseShort(body));
                case 'l': case 'L': return new NbtLong(Long.parseLong(body));
                case 'f': case 'F': return new NbtFloat(Float.parseFloat(body));
                case 'd': case 'D': return new NbtDouble(Double.parseDouble(body));
                default: break;
            }
        } catch (final NumberFormatException ignored) {
            return new NbtString(token);
        }

        // No suffix: a whole number is an int and anything else numeric is a double, which is the
        // pair modern defaults to and the pair beta writes for the untagged fields it has.
        try {
            return new NbtInt(Integer.parseInt(token));
        } catch (final NumberFormatException ignored) {
            // Not an int, so try the wider type before giving up on it being a number at all.
        }
        try {
            return new NbtDouble(Double.parseDouble(token));
        } catch (final NumberFormatException ignored) {
            return new NbtString(token);
        }
    }

    private static String readKey(final StringReader reader) throws CommandSyntaxException {
        if (reader.canRead() && StringReader.isQuotedStringStart(reader.peek())) {
            return reader.readQuotedString();
        }
        return readUnquoted(reader, false);
    }

    /**
     * Reads a bare word up to the structural characters, rather than through Brigadier's own unquoted
     * reader - which stops at a colon and at most of what NBT allows in one.
     *
     * <p>The colon is the one character the two sides disagree about, which is why they are read
     * differently: it ends a KEY, because it is what separates the key from its value, but inside a
     * VALUE it is ordinary text - that is what lets {@code Type:mymod:moa} be written without quotes.
     */
    private static String readUnquoted(final StringReader reader, final boolean colonIsText) {
        final int start = reader.getCursor();
        while (reader.canRead() && isUnquotedChar(reader.peek(), colonIsText)) {
            reader.skip();
        }
        return reader.getString().substring(start, reader.getCursor());
    }

    private static boolean isUnquotedChar(final char c, final boolean colonIsText) {
        if (c == ':') {
            return colonIsText;
        }
        return c != ',' && c != '{' && c != '}' && c != '[' && c != ']'
            && c != '"' && c != '\'' && !Character.isWhitespace(c);
    }
}
